package com.creativeblisslabs.wearproductivity.wear.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class HealthTrackingService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _stepCount = MutableStateFlow(0L)
    val stepCount: StateFlow<Long> = _stepCount.asStateFlow()

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            // Handle sensor availability
        }

        override fun onDataReceived(data: DataPointContainer) {
            data.getData(DataType.HEART_RATE_BPM).forEach { sample ->
                _heartRate.value = sample.value
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): HealthTrackingService = this@HealthTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        registerSensors()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "health_tracking_channel"
        val channelName = "Health & Workout Tracking"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LifeOS Sensor Tracking")
            .setContentText("Monitoring Heart Rate & Activity...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(101, notification)
        }
    }

    private fun registerSensors() {
        scope.launch {
            try {
                val measureClient = HealthServices.getClient(this@HealthTrackingService).measureClient
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, ContextCompat.getMainExecutor(this@HealthTrackingService), measureCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            try {
                val measureClient = HealthServices.getClient(this@HealthTrackingService).measureClient
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
