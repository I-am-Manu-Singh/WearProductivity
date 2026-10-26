package com.creativeblisslabs.wearproductivity.shared.platform

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.creativeblisslabs.wearproductivity.shared.CompanionSyncTransport
import com.creativeblisslabs.wearproductivity.shared.LifeModule
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import com.creativeblisslabs.wearproductivity.shared.SyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidWearSyncTransport(
    private val context: Context,
    private val json: Json
) : CompanionSyncTransport {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun publish(packet: SyncPacket) {
        scope.launch {
            try {
                val request = PutDataMapRequest.create(SyncPaths.LIFE_STATE).apply {
                    dataMap.putString("json", json.encodeToString(packet))
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                
                dataClient.putDataItem(request).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun latestPacket(): SyncPacket? {
        // We use a blocking call or a simplified pattern here as it's usually called from a background load
        // But since this is a KMP shared interface, we might need a better way.
        // For now, returning null and relying on the WearableListenerService is the standard Wear OS way.
        // However, we can attempt a one-time fetch:
        return null
    }

    suspend fun fetchLatestPacket(): SyncPacket? {
        return try {
            val uri = android.net.Uri.Builder()
                .scheme(com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME)
                .path(SyncPaths.LIFE_STATE)
                .build()
            val dataItem = dataClient.getDataItem(uri).await()
            dataItem?.let {
                val jsonStr = com.google.android.gms.wearable.DataMapItem.fromDataItem(it).dataMap.getString("json")
                jsonStr?.let { json.decodeFromString<SyncPacket>(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun sendQuickCapture(title: String, module: LifeModule) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, SyncPaths.QUICK_CAPTURE, title.toByteArray()).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun acknowledgeReminder(reminderId: String) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, "/acknowledge_reminder", reminderId.toByteArray()).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun sendFile(fileUri: String, path: String) {
        scope.launch {
            try {
                val uri = android.net.Uri.parse(fileUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val asset = com.google.android.gms.wearable.Asset.createFromBytes(bytes)
                
                val fileName = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
                
                val request = PutDataMapRequest.create(path).apply {
                    dataMap.putAsset("file_asset", asset)
                    dataMap.putString("file_name", fileName)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                
                dataClient.putDataItem(request).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
