package com.creativeblisslabs.wearproductivity.wear.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.creativeblisslabs.wearproductivity.wear.LifeOSWearApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class HealthTrackingService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _stepCount = MutableStateFlow(0L)
    val stepCount: StateFlow<Long> = _stepCount.asStateFlow()

    private lateinit var sensorManager: SensorManager
    private var initialSteps = -1L

    private var lastBarometer = 0.0
    private var lastLux = 0f
    private var lastTempC = 0.0


    private val repository by lazy {
        (application as? LifeOSWearApplication)?.repository
    }

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {}

        override fun onDataReceived(data: DataPointContainer) {
            data.getData(DataType.HEART_RATE_BPM).forEach { sample ->
                val bpm = sample.value
                _heartRate.value = bpm
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): HealthTrackingService = this@HealthTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder


    private var lastDbWriteTime = 0L

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        registerHealthServicesSensors()
        registerHardwareSensors()
        
        // Debounce database writes to once every 5 seconds
        scope.launch {
            while (true) {
                delay(5000L)
                repository?.updateWellnessVitals(
                    steps = _stepCount.value.toInt()
                )
                repository?.updateAdvancedSensors(
                    barometer = lastBarometer,
                    lux = lastLux,
                    skinTemp = lastTempC
                )
                repository?.updateHeartRate(_heartRate.value)
            }
        }
    }


    private fun startForegroundServiceNotification() {
        val channelId = "health_tracking_channel"
        val channelName = "Health & Sensor Tracking"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LifeOS Sensor Tracking")
            .setContentText("Monitoring Heart Rate, Pressure & Activity...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(101, notification)
        }
    }

    private fun registerHealthServicesSensors() {
        scope.launch {
            try {
                val measureClient = HealthServices.getClient(this@HealthTrackingService).measureClient
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, ContextCompat.getMainExecutor(this@HealthTrackingService), measureCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun registerHardwareSensors() {
        runCatching {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            
            // Step Counter
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            // Barometer (Pressure / Altitude)
            sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            // Ambient Light
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            // Ambient Temperature (Skin/Watch Temp)
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toLong()
                if (initialSteps < 0) {
                    initialSteps = totalSteps
                }
                val sessionSteps = (totalSteps - initialSteps).coerceAtLeast(0L)
                _stepCount.value = sessionSteps
            }
            Sensor.TYPE_PRESSURE -> {
                lastBarometer = event.values[0].toDouble()
            }
            Sensor.TYPE_LIGHT -> {
                lastLux = event.values[0]
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                lastTempC = event.values[0].toDouble()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        runCatching { sensorManager.unregisterListener(this) }
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
