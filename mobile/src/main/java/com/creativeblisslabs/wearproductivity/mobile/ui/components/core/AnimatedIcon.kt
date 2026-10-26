package com.creativeblisslabs.wearproductivity.mobile.ui.components.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = LocalContentColor.current
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.2f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "IconScale"
    )

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}
