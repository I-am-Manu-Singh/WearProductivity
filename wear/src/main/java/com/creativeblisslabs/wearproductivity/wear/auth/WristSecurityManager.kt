package com.creativeblisslabs.wearproductivity.wear.auth

import android.app.KeyguardManager
import android.content.Context

object WristSecurityManager {

    private const val PIXEL_WRIST_AUTOLOCK_SETTING_STATE = "wrist_detection_auto_locking_state"

    fun isWristDetectionAutoLockingEnabled(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isSecured = keyguardManager?.isDeviceSecure == true

        val isWristDetectionOn = try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                PIXEL_WRIST_AUTOLOCK_SETTING_STATE,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }

        return isSecured && isWristDetectionOn
    }
}
