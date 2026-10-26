package com.creativeblisslabs.wearproductivity.mobile.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import com.creativeblisslabs.wearproductivity.mobile.LifeOSApplication
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import com.creativeblisslabs.wearproductivity.shared.SyncPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MobileDataService : WearableListenerService() {
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
                            val repository = (application as LifeOSApplication).repository
                            repository.handleRemoteSync(packet)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path.startsWith("/lifeos/phone_media_control/")) {
            val command = messageEvent.path.removePrefix("/lifeos/phone_media_control/")
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            when (command) {
                "play_pause" -> {
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                }
                "next" -> {
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
                }
                "previous" -> {
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                }
            }
            sendPhoneMediaMetadata(messageEvent.sourceNodeId)
        } else if (messageEvent.path == "/lifeos/request_phone_media") {
            sendPhoneMediaMetadata(messageEvent.sourceNodeId)
        } else if (messageEvent.path.startsWith("/lifeos/phone_control/")) {
            val action = messageEvent.path.removePrefix("/lifeos/phone_control/")
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            when (action) {
                "unlock" -> {
                    try {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
                "ringer" -> {
                    audioManager?.let {
                        val nextMode = when (it.ringerMode) {
                            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }
                        try { it.ringerMode = nextMode } catch (_: Exception) {}
                    }
                }
            }
            sendBatteryInfo(messageEvent.sourceNodeId)
        } else if (messageEvent.path == "/lifeos/request_phone_battery") {
            sendBatteryInfo(messageEvent.sourceNodeId)
        } else if (messageEvent.path.startsWith("/lifeos/browse_phone_dir")) {
            sendPhoneDirectoryContents(messageEvent.sourceNodeId, String(messageEvent.data))
        }
    }

    private fun sendPhoneDirectoryContents(nodeId: String, pathStr: String) {
        try {
            val targetDir = if (pathStr.isBlank()) android.os.Environment.getExternalStorageDirectory() else java.io.File(pathStr)
            val files = targetDir?.listFiles() ?: emptyArray()
            val fileListPayload = files.take(30).joinToString(";") { f ->
                "${f.name},${if (f.isDirectory) 1 else 0},${f.length()}"
            }
            Wearable.getMessageClient(this).sendMessage(nodeId, "/lifeos/phone_dir_contents", fileListPayload.toByteArray())
        } catch (_: Exception) {}
    }

    private fun sendPhoneMediaMetadata(nodeId: String) {
        try {
            val metadataStr = "Phone Audio|Active Media|Phone Library|240|PLAYING"
            Wearable.getMessageClient(this).sendMessage(nodeId, "/lifeos/phone_media_metadata", metadataStr.toByteArray())
        } catch (_: Exception) {}
    }

    private fun sendBatteryInfo(nodeId: String) {
        try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPercent = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 90
            val isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

            val payload = "$batteryPercent,${if (isCharging) 1 else 0}"
            Wearable.getMessageClient(this).sendMessage(nodeId, "/lifeos/phone_battery", payload.toByteArray())
        } catch (_: Exception) {
            // Silently handle exceptions
        }
    }
}
