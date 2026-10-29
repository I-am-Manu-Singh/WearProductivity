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
    private val alpha = 0.85f // Increased smoothing factor for cleaner motion tracking

    // Kinematic Phase State Machine
    private enum class Phase { REST, ECCENTRIC, CONCENTRIC }
    private var currentPhase = Phase.REST

    private val peakThreshold = 3.2f
    private val valleyThreshold = 1.0f
    private val repDebounceMillis = 700L // Refined debounce for faster cadence support

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
        currentPhase = Phase.REST
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        filteredX = alpha * filteredX + (1 - alpha) * event.values[0]
        filteredY = alpha * filteredY + (1 - alpha) * event.values[1]
        filteredZ = alpha * filteredZ + (1 - alpha) * event.values[2]

        val magnitude = sqrt((filteredX * filteredX) + (filteredY * filteredY) + (filteredZ * filteredZ))
        val now = System.currentTimeMillis()

        // Dual-Threshold State Machine for Rep Counting
        when (currentPhase) {
            Phase.REST -> {
                if (magnitude > peakThreshold && (now - lastPeakTime) > repDebounceMillis) {
                    currentPhase = Phase.CONCENTRIC
                }
            }
            Phase.CONCENTRIC -> {
                if (magnitude < valleyThreshold) {
                    currentPhase = Phase.ECCENTRIC
                }
            }
            Phase.ECCENTRIC -> {
                if (magnitude > peakThreshold) {
                    // Rep completed successfully!
                    lastPeakTime = now
                    _repCount.value += 1
                    currentPhase = Phase.CONCENTRIC
                } else if (magnitude < 0.5f) {
                    currentPhase = Phase.REST
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
