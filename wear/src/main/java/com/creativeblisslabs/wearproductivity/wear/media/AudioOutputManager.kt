package com.creativeblisslabs.wearproductivity.wear.media

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

data class SystemAudioDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val isConnected: Boolean = true
)

object AudioOutputManager {

    fun getConnectedAudioOutputs(context: Context): List<SystemAudioDevice> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return listOf(
                SystemAudioDevice(1, "Watch Speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                SystemAudioDevice(2, "Bluetooth Buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            )

        val devices = mutableListOf<SystemAudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            outputs.forEach { device ->
                val typeName = when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Headset"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Watch Speaker"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headphones"
                    else -> null
                }
                if (typeName != null) {
                    val name = device.productName?.toString()?.ifBlank { null } ?: typeName
                    devices.add(
                        SystemAudioDevice(
                            id = device.id,
                            name = name,
                            type = device.type,
                            isConnected = true
                        )
                    )
                }
            }
        }

        if (devices.isEmpty()) {
            devices.add(SystemAudioDevice(1, "Watch Speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
            devices.add(SystemAudioDevice(2, "Bluetooth Buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        }

        return devices.distinctBy { it.name }
    }
}
