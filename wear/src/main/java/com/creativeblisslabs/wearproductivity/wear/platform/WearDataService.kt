package com.creativeblisslabs.wearproductivity.wear.platform

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import com.creativeblisslabs.wearproductivity.shared.SyncPaths
import com.creativeblisslabs.wearproductivity.wear.LifeOSWearApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class WearDataService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == SyncPaths.LIFE_STATE) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val jsonStr = dataMap.getString("json")
                if (jsonStr != null) {
                    try {
                        val packet = json.decodeFromString<SyncPacket>(jsonStr)
                        scope.launch {
                            val repository = (application as LifeOSWearApplication).repository
                            repository.handleRemoteSync(packet)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onChannelOpened(channel: com.google.android.gms.wearable.ChannelClient.Channel) {
        val meta = com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.parseChannelPath(channel.path)
        if (meta != null) {
            val fileName = meta.fileName
            val transferId = meta.transferId
            val expectedSize = meta.expectedSize
            val targetDir = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "LifeOS"
            ).apply { if (!exists()) mkdirs() }

            val partFile = java.io.File(targetDir, "$fileName.$transferId.part")

            scope.launch {
                val wifiManager = applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                @Suppress("DEPRECATION")
                val wifiLock = wifiManager?.createWifiLock(
                    android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "LifeOS:WearFileReceive"
                )?.apply {
                    setReferenceCounted(false)
                    try { acquire() } catch (_: Exception) {}
                }

                val channelClient = com.google.android.gms.wearable.Wearable.getChannelClient(this@WearDataService)
                try {
                    val inputStream = channelClient.getInputStream(channel).await()
                    var bytesReceived = 0L

                    java.io.FileOutputStream(partFile).use { outputStream ->
                        val buffer = ByteArray(32768)
                        var bytesRead = inputStream.read(buffer)
                        while (bytesRead != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesReceived += bytesRead
                            bytesRead = inputStream.read(buffer)
                        }
                        outputStream.flush()
                    }
                    inputStream.close()

                    // Ensure unique filename
                    val finalFile = getUniqueTargetFile(targetDir, fileName)
                    if (partFile.renameTo(finalFile)) {
                        // Send success ACK to sender
                        com.google.android.gms.wearable.Wearable.getMessageClient(this@WearDataService).sendMessage(
                            channel.nodeId,
                            com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.STATUS_PATH,
                            "$transferId:${com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.ACK_SUCCESS}".toByteArray(Charsets.UTF_8)
                        )
                    } else {
                        partFile.delete()
                        com.google.android.gms.wearable.Wearable.getMessageClient(this@WearDataService).sendMessage(
                            channel.nodeId,
                            com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.STATUS_PATH,
                            "$transferId:${com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.ACK_ERROR}".toByteArray(Charsets.UTF_8)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    partFile.delete()
                    try {
                        com.google.android.gms.wearable.Wearable.getMessageClient(this@WearDataService).sendMessage(
                            channel.nodeId,
                            com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.STATUS_PATH,
                            "$transferId:${com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.ACK_ERROR}".toByteArray(Charsets.UTF_8)
                        )
                    } catch (_: Exception) {}
                } finally {
                    try {
                        channelClient.close(channel).await()
                    } catch (_: Exception) {}
                    if (wifiLock?.isHeld == true) {
                        try { wifiLock.release() } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun getUniqueTargetFile(dir: java.io.File, fileName: String): java.io.File {
        var file = java.io.File(dir, fileName)
        if (!file.exists()) return file

        val baseName = fileName.substringBeforeLast(".")
        val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        var count = 1
        while (file.exists()) {
            file = java.io.File(dir, "$baseName ($count)$ext")
            count++
        }
        return file
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        val path = messageEvent.path
        if (path == com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.STATUS_PATH) {
            try {
                val payload = String(messageEvent.data, Charsets.UTF_8)
                val parts = payload.split(":")
                if (parts.size >= 2) {
                    val transferId = parts[0]
                    val status = parts[1]
                    val state = when (status) {
                        com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.ACK_SUCCESS -> com.creativeblisslabs.wearproductivity.shared.FileTransferAckState.SUCCESS
                        com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol.ACK_CANCELLED -> com.creativeblisslabs.wearproductivity.shared.FileTransferAckState.CANCELLED
                        else -> com.creativeblisslabs.wearproductivity.shared.FileTransferAckState.ERROR
                    }
                    com.creativeblisslabs.wearproductivity.shared.FileTransferAckRegistry.complete(transferId, state)
                }
            } catch (_: Exception) {}
            return
        }
        if (path == "/lifeos/ping") {
            try {
                // Respond back with pong
                com.google.android.gms.wearable.Wearable.getMessageClient(this).sendMessage(
                    messageEvent.sourceNodeId,
                    "/lifeos/pong",
                    "pong".toByteArray(Charsets.UTF_8)
                )
            } catch (_: Exception) {}
        } else if (path.startsWith("/lifeos/workout/")) {
            val cmd = path.removePrefix("/lifeos/workout/")
            // Broadcast intent or trigger WearReminderReceiver / launch workout activity
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action_workout_cmd", cmd)
            }
            launchIntent?.let { startActivity(it) }

            // Also post milestone / start notification via WearReminderReceiver
            val reminderIntent = android.content.Intent(this, WearReminderReceiver::class.java).apply {
                val actionTitle = if (cmd == "start_run") "Run Workout Started" else "Gym Workout Started"
                putExtra("title", actionTitle)
                putExtra("message", "Workout synchronized from companion phone")
                putExtra("type", "workout")
            }
            sendBroadcast(reminderIntent)
        } else if (path.startsWith("/lifeos/phone_media_control/")) {
            val action = path.removePrefix("/lifeos/phone_media_control/")
            val mediaServiceIntent = android.content.Intent(this, com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService::class.java).apply {
                this.action = when (action) {
                    "play" -> com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService.ACTION_PLAY
                    "pause" -> com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService.ACTION_PAUSE
                    "play_pause" -> com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService.ACTION_PLAY
                    "next" -> com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService.ACTION_NEXT
                    "previous" -> com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService.ACTION_PREV
                    else -> null
                }
            }
            if (mediaServiceIntent.action != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(mediaServiceIntent)
                } else {
                    startService(mediaServiceIntent)
                }
            }
        }
    }
}
