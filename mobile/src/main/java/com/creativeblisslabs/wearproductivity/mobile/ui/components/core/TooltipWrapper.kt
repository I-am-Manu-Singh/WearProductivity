package com.creativeblisslabs.wearproductivity.mobile.ui.components.core

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.creativeblisslabs.wearproductivity.mobile.ui.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipWrapper(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHaptic.current
    val state = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text)
            }
        },
        state = state,
        modifier = modifier,
        content = {
            // Intercepting long press for haptics if needed
            content()
        },
    )
}
