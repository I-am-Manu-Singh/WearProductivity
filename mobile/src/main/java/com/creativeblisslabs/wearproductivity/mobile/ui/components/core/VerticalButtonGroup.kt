package com.creativeblisslabs.wearproductivity.mobile.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.creativeblisslabs.wearproductivity.mobile.ui.util.rememberLifeOSHapticProvider

@OptIn(ExperimentalMaterial3Api::class)
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.fastForEachIndexed { index, option ->
            val isSelected = option == selected
            
            val shape = when {
                options.size == 1 -> ButtonDefaults.shape
                index == 0 -> MaterialTheme.shapes.large
                index == options.lastIndex -> MaterialTheme.shapes.large
                else -> MaterialTheme.shapes.medium
            }

            OutlinedButton(
                onClick = {
                    haptic.click()
                    onSelectedChange(option)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                shape = shape,
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(text = label(option))
            }
        }
    }
}
