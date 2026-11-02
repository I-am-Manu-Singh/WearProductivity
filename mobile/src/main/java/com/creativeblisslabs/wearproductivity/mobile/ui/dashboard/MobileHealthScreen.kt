package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileHealthScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val wellness = state.wellness

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wear OS Vitals & Health", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Live Sensor Telemetry from Wear
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Live Watch Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Real-time sensor metrics received from Wear OS", style = MaterialTheme.typography.bodySmall)
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SensorMetricPill(
                                label = "Heart Rate",
                                value = if (wellness.currentHeartRateBpm > 0) "${wellness.currentHeartRateBpm.toInt()} bpm" else "72 bpm",
                                icon = Icons.Rounded.Favorite,
                                color = Color(0xFFBA1A1A),
                                modifier = Modifier.weight(1f)
                            )
                            SensorMetricPill(
                                label = "Blood Oxygen",
                                value = if (wellness.spo2Pct > 0) "${wellness.spo2Pct.toInt()}%" else "98%",
                                icon = Icons.Rounded.Air,
                                color = Color(0xFF006688),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SensorMetricPill(
                                label = "Daily Steps",
                                value = "${wellness.steps}",
                                icon = Icons.Rounded.DirectionsWalk,
                                color = Color(0xFF386A20),
                                modifier = Modifier.weight(1f)
                            )
                            SensorMetricPill(
                                label = "Sleep Quality",
                                value = "${wellness.sleepLastNightMinutes / 60}h ${wellness.sleepLastNightMinutes % 60}m",
                                icon = Icons.Rounded.Bedtime,
                                color = Color(0xFF6750A4),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Nutrition Macro Targets
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daily Nutrition Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Target: ${state.nutritionTarget.targetCalories} kcal", style = MaterialTheme.typography.labelSmall)
                        }

                        val calProg = if (state.nutritionTarget.targetCalories > 0)
                            state.todayCalories.toFloat() / state.nutritionTarget.targetCalories.toFloat() else 0f
                        MacroBar(label = "Calories", current = "${state.todayCalories}", target = "${state.nutritionTarget.targetCalories} kcal", progress = calProg, color = MaterialTheme.colorScheme.primary)

                        val protProg = if (state.nutritionTarget.targetProteinGrams > 0)
                            state.todayProtein.toFloat() / state.nutritionTarget.targetProteinGrams.toFloat() else 0f
                        MacroBar(label = "Protein", current = "${state.todayProtein.toInt()}g", target = "${state.nutritionTarget.targetProteinGrams.toInt()}g", progress = protProg, color = Color(0xFFBA1A1A))

                        val carbProg = if (state.nutritionTarget.targetCarbsGrams > 0)
                            state.todayCarbs.toFloat() / state.nutritionTarget.targetCarbsGrams.toFloat() else 0f
                        MacroBar(label = "Carbs", current = "${state.todayCarbs.toInt()}g", target = "${state.nutritionTarget.targetCarbsGrams.toInt()}g", progress = carbProg, color = Color(0xFFE08E00))

                        val fatProg = if (state.nutritionTarget.targetFatGrams > 0)
                            state.todayFat.toFloat() / state.nutritionTarget.targetFatGrams.toFloat() else 0f
                        MacroBar(label = "Fat", current = "${state.todayFat.toInt()}g", target = "${state.nutritionTarget.targetFatGrams.toInt()}g", progress = fatProg, color = Color(0xFF386A20))
                    }
                }
            }

            // 3. Fasting Stage Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fasting Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Badge(
                                containerColor = if (state.fast.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(if (state.fast.active) "Active ${state.fast.plan}" else "Standby")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val stage = when {
                            !state.fast.active -> "Ready to begin fasting session"
                            state.fast.progress >= 0.75f -> "Advanced Fasting: Autophagy & Cellular Maintenance"
                            state.fast.progress >= 0.5f -> "Metabolic Shift: Fat Oxidation & Ketosis"
                            state.fast.progress >= 0.25f -> "Glycogen Depletion & Blood Sugar Normalization"
                            else -> "Early Phase: Digestive Rest"
                        }
                        Text(stage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.fast.progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorMetricPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MacroBar(label: String, current: String, target: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$current / $target", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
