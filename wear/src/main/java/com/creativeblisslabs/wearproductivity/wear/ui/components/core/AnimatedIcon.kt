package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LocalContentColor

@Composable
fun AnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(active) {
        if (active) {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = LocalContentColor.current,
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    )
}
