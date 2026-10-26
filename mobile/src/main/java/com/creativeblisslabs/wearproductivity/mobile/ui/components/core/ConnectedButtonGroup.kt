package com.creativeblisslabs.wearproductivity.mobile.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.creativeblisslabs.wearproductivity.mobile.ui.util.rememberLifeOSHapticProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedButtonGroup(
    options: List<T>,
    checked: T,
    onCheckedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (T) -> String = { it.toString() },
) {
    if (options.isEmpty()) return
    val haptic = rememberLifeOSHapticProvider()

    ButtonGroup(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Replaced internal constant
    ) {
        options.fastForEachIndexed { index, option ->
            val isSelected = option == checked

            val shape = when {
                options.size == 1 -> ButtonDefaults.shape
                index == 0 -> MaterialTheme.shapes.medium // Simplified shapes
                index == options.lastIndex -> MaterialTheme.shapes.medium
                else -> MaterialTheme.shapes.medium
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = {
                    if (enabled && !isSelected) {
                        haptic.click()
                        onCheckedChange(option)
                    }
                },
                enabled = enabled,
                shapes = ToggleButtonDefaults.shapes(shape, shape, shape)
            ) {
                Text(
                    text = label(option),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
