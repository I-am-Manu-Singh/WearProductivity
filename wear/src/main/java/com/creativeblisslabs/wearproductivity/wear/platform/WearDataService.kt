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
        if (channel.path.startsWith("/file_transfer/") || channel.path.startsWith("/lifeos/file_transfer/")) {
            val fileName = channel.path.substringAfterLast("/")
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val targetFile = java.io.File(downloadDir, fileName)

            scope.launch {
                try {
                    val channelClient = com.google.android.gms.wearable.Wearable.getChannelClient(this@WearDataService)
                    val inputStream = channelClient.getInputStream(channel).await()
                    java.io.FileOutputStream(targetFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead = inputStream.read(buffer)
                        while (bytesRead != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesRead = inputStream.read(buffer)
                        }
                        outputStream.flush()
                    }
                    inputStream.close()
                    channelClient.close(channel)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        val path = messageEvent.path
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
