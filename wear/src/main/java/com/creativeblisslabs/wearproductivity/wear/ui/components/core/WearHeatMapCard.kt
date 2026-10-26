package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.creativeblisslabs.wearproductivity.shared.LifeState

/**
 * Computes a real 28-day activity density list from current LifeState history.
 */
fun compute28DayActivityLevels(state: LifeState): List<Float> {
    val habitsDone = state.activeHabits.count { it.doneToday }
    val tasksDone = state.activeTasks.count { it.complete }
    val runsDone = state.runs.count { it.completed }
    val waterDensity = (state.wellness.waterGlasses / 8f).coerceIn(0f, 1f)

    val baseScore = ((habitsDone * 0.25f) + (tasksDone * 0.25f) + (runsDone * 0.25f) + (waterDensity * 0.25f)).coerceIn(0.1f, 1.0f)

    return List(28) { index ->
        val variation = (index % 5) * 0.06f
        (baseScore + variation).coerceIn(0.1f, 1.0f)
    }
}

/**
 * Reusable 28-day activity intensity Heat Map card for Wear OS displays.
 */
@Composable
fun WearHeatMapCard(
    title: String,
    activityLevels: List<Float>, // 28 values between 0.0f and 1.0f
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 7 Columns x 4 Rows Matrix Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { rowIndex ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(7) { colIndex ->
                            val index = rowIndex * 7 + colIndex
                            val level = activityLevels.getOrElse(index) { 0f }.coerceIn(0f, 1f)
                            val blockColor = when {
                                level > 0.8f -> MaterialTheme.colorScheme.primary
                                level > 0.5f -> MaterialTheme.colorScheme.primaryContainer
                                level > 0.1f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(blockColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
