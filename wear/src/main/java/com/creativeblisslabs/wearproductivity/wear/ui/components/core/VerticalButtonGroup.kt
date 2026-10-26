package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import androidx.compose.ui.util.fastForEachIndexed
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider

@Composable
fun <T> VerticalButtonGroup(
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (T) -> String = { it.toString() },
) {
    val haptic = rememberLifeOSHapticProvider()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.fastForEachIndexed { index, option ->
            val isSelected = option == selected
            
            Button(
                onClick = {
                    haptic.click()
                    onSelectedChange(option)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                Text(text = label(option))
            }
        }
    }
}
