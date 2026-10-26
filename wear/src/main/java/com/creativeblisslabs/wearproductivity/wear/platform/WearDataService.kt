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
}
