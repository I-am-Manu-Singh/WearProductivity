package com.creativeblisslabs.wearproductivity.mobile.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

@Stable
class LifeOSHapticProvider(private val view: View) {
    fun click() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun longClick() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun segmentTick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    inline fun withClick(crossinline action: () -> Unit): () -> Unit = {
        click()
        action()
    }
}

val LocalHaptic = staticCompositionLocalOf<LifeOSHapticProvider?> { null }

@Composable
fun rememberLifeOSHapticProvider(): LifeOSHapticProvider {
    val view = LocalView.current
    return remember(view) { LifeOSHapticProvider(view) }
}
