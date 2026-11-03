package com.creativeblisslabs.wearproductivity.mobile.sync

import android.content.Context
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import com.creativeblisslabs.wearproductivity.shared.SyncPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

class MobileWearSyncManager(
    private val context: Context,
    private val repository: LifeRepository,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val isSyncing = AtomicBoolean(false)

    fun observeAndSync() {
        scope.launch(Dispatchers.IO) {
            repository.state.collect { state ->
                if (!isSyncing.compareAndSet(false, true)) return@collect

                try {
                    val packet = SyncPacket(deviceId = "phone", state = state)
                    val jsonStr = json.encodeToString(packet)

                    val dataClient = Wearable.getDataClient(context)
                    val putDataReq = PutDataMapRequest.create(SyncPaths.LIFE_STATE).apply {
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                        dataMap.putString("json", jsonStr)
                        setUrgent()
                    }.asPutDataRequest()

                    dataClient.putDataItem(putDataReq).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isSyncing.set(false)
                }
            }
        }
    }
}

