package com.creativeblisslabs.wearproductivity.wear.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class FirebaseSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val jsonPayload = inputData.getString("jsonPayload")

            if (jsonPayload != null) {
                // Direct Firebase Cloud Sync is disabled natively on Wear OS (No Firebase Auth dep)
                // The mobile companion app handles all Firebase transactions.
                // We fallback immediately to WearDataLayer proxy syncing.
                val dataClient = Wearable.getDataClient(appContext)
                val putDataReq = PutDataMapRequest.create("/lifeos/sync_path").apply {
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                    dataMap.putString("status", "pending")
                    dataMap.putString("jsonPayload", jsonPayload)
                    setUrgent()
                }.asPutDataRequest()

                dataClient.putDataItem(putDataReq).await()
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, jsonPayload: String? = null) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = androidx.work.Data.Builder().apply {
                if (jsonPayload != null) putString("jsonPayload", jsonPayload)
            }.build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
        }
    }
}
