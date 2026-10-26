package com.creativeblisslabs.wearproductivity.mobile.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(
    private val context: Context,
    private val repository: LifeRepository,
    private val scope: CoroutineScope
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun syncHealthConnectData() {
        scope.launch(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val startOfDay = now.truncatedTo(ChronoUnit.DAYS)
                
                val timeRange = TimeRangeFilter.between(startOfDay, now)

                // 1. Fetch Steps
                val stepsRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange
                )
                val stepsResponse = healthConnectClient.readRecords(stepsRequest)
                val totalSteps = stepsResponse.records.sumOf { it.count }

                // 2. Fetch Active Calories Burned
                val caloriesRequest = ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = timeRange
                )
                val caloriesResponse = healthConnectClient.readRecords(caloriesRequest)
                val totalActiveCalories = caloriesResponse.records.sumOf { it.energy.inKilocalories }

                // 3. Fetch Sleep (Look back 24 hours for last night's sleep session)
                val sleepTimeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
                val sleepRequest = ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = sleepTimeRange
                )
                val sleepResponse = healthConnectClient.readRecords(sleepRequest)
                var lastSleepMinutes = 0
                val latestSleepSession = sleepResponse.records.maxByOrNull { it.endTime }
                if (latestSleepSession != null) {
                    val durationSeconds = ChronoUnit.SECONDS.between(latestSleepSession.startTime, latestSleepSession.endTime)
                    lastSleepMinutes = (durationSeconds / 60).toInt()
                }

                repository.updateWellnessVitals(
                    steps = totalSteps.toInt(),
                    sleepMinutes = lastSleepMinutes
                )
            } catch (e: Exception) {
                // Handle permissions or API unavailability
                e.printStackTrace()
            }
        }
    }
}
