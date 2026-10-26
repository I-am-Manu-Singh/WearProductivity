package com.creativeblisslabs.wearproductivity.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class RepDetectionEngine(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount

    private var isTracking = false
    private var lastPeakTime = 0L
    
    // Low-pass filter variables
    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private val alpha = 0.8f // Smoothing factor

    // Kinematic Thresholds
    private val repMagnitudeThreshold = 3.5f // m/s^2
    private val repDebounceMillis = 1200L // Minimum time between reps

    fun startTracking() {
        if (!isTracking && linearAccelSensor != null) {
            _repCount.value = 0
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
            isTracking = true
        }
    }

    fun stopTracking() {
        if (isTracking) {
            sensorManager.unregisterListener(this)
            isTracking = false
        }
    }

    fun reset() {
        _repCount.value = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        // Apply Low-Pass Filter to remove high-frequency jitter
        filteredX = alpha * filteredX + (1 - alpha) * event.values[0]
        filteredY = alpha * filteredY + (1 - alpha) * event.values[1]
        filteredZ = alpha * filteredZ + (1 - alpha) * event.values[2]

        // Calculate 3D Vector Magnitude
        val magnitude = sqrt((filteredX * filteredX) + (filteredY * filteredY) + (filteredZ * filteredZ))

        val now = System.currentTimeMillis()
        
        // Peak Detection Logic
        if (magnitude > repMagnitudeThreshold && (now - lastPeakTime) > repDebounceMillis) {
            lastPeakTime = now
            _repCount.value += 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required for linear acceleration
    }
}
