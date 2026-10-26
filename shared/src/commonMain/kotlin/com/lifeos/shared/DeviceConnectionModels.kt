package com.creativeblisslabs.wearproductivity.shared

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    UNKNOWN
}

@Serializable
enum class TransportMedium {
    BLUETOOTH,
    WIFI,
    CLOUD_CELLULAR,
    EMULATOR,
    DIRECT
}

@Serializable
data class DevicePeerInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val isNearby: Boolean,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val medium: TransportMedium = TransportMedium.BLUETOOTH,
    val lastSeenTimestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val batteryPercentage: Int? = null
)

@Serializable
data class SensorBiometricSnapshot(
    val heartRateBpm: Double = 0.0,
    val heartRateAccuracy: String = "Normal",
    val stepsToday: Long = 0,
    val activeCalories: Double = 0.0,
    val restingHeartRate: Double = 62.0,
    val bloodOxygenSpO2: Double? = 98.5,
    val skinTemperatureCelsius: Double? = 34.2,
    val stressLevel: Int = 28,
    val deviceName: String = ""
)
