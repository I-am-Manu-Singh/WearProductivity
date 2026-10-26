package com.creativeblisslabs.wearproductivity.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StepCounterEngine(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _dailySteps = MutableStateFlow(0)
    val dailySteps: StateFlow<Int> = _dailySteps

    private var initialStepCount = -1f
    private var isTracking = false

    fun startTracking() {
        if (!isTracking && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            isTracking = true
        }
    }

    fun stopTracking() {
        if (isTracking) {
            sensorManager.unregisterListener(this)
            isTracking = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsSinceBoot = event.values[0]
        
        if (initialStepCount < 0) {
            initialStepCount = totalStepsSinceBoot
        }

        val currentSessionSteps = (totalStepsSinceBoot - initialStepCount).toInt()
        
        // In a real app, you would add currentSessionSteps to the persistent daily total saved in Room
        _dailySteps.value = currentSessionSteps
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required
    }
}
