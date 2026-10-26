package com.creativeblisslabs.wearproductivity.wear.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Responsive {
    /**
     * Standard horizontal padding for content that isn't full-width on round screens.
     */
    val horizontalPadding: Dp
        @Composable get() = if (LocalConfiguration.current.isScreenRound) 8.dp else 12.dp

    /**
     * Vertical spacing between list items.
     */
    val itemSpacing = 4.dp

    /**
     * Top/Bottom padding for ScalingLazyColumn to ensure content is readable.
     */
    val listVerticalPadding = 32.dp

    /**
     * Responsive size for small icons in buttons or cards.
     */
    val iconSize = 18.dp

    /**
     * Responsive size for secondary icons.
     */
    val secondaryIconSize = 14.dp
}
