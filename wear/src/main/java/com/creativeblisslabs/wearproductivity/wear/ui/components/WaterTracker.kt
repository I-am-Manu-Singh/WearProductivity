package com.creativeblisslabs.wearproductivity.wear.ui.components

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.creativeblisslabs.wearproductivity.wear.ui.util.normalize
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider

@Composable
fun WaterTracker(
    level: Int,
    goal: Int,
    onAddWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (level.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val waterColor = MaterialTheme.colorScheme.primary
    val haptic = rememberLifeOSHapticProvider()
    
    val morphFactor by animateFloatAsState(
        targetValue = progress,
        label = "waterMorph"
    )

    val morph = remember {
        Morph(
            normalize(
                RoundedPolygon.circle(numVertices = 32),
                true,
                RectF(-1f, -1f, 1f, 1f)
            ),
            normalize(
                RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.5f),
                true,
                RectF(-1f, -1f, 1f, 1f)
            ),
        )
    }

    val androidPath = remember { android.graphics.Path() }
    val composePath = remember { androidPath.asComposePath() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier
            .size(100.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.click()
                    onAddWater()
                }
            )
            .drawWithContent {
                androidPath.rewind()
                morph.toPath(morphFactor, androidPath)

                matrix.reset()
                matrix.setScale(size.width / 2f, size.height / 2f)
                matrix.postTranslate(size.width / 2f, size.height / 2f)
                androidPath.transform(matrix)

                drawPath(
                    path = composePath,
                    color = waterColor.copy(alpha = 0.3f + 0.7f * progress)
                )

                clipPath(path = composePath) {
                    this@drawWithContent.drawContent()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$level / $goal ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
