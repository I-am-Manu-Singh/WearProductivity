package com.creativeblisslabs.wearproductivity.mobile.ui.fasting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFastingScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val fast = state.fast

    val today = remember { LocalDate.now() }
    val zoneId = remember { ZoneId.systemDefault() }
    val todayStart = remember(today) { today.atStartOfDay(zoneId).toInstant().toEpochMilli() }
    val tomorrowStart = remember(today) { today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() }

    // Date-filtered nutrition for today
    val todayFoodLogs = remember(state.foodLogs, todayStart, tomorrowStart) {
        state.foodLogs.filter { it.stamp.deletedAt == null && it.loggedAtEpochMs in todayStart until tomorrowStart }
    }
    val todayCalories = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.calories.coerceAtLeast(0) } }
    val todayProtein = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.proteinGrams.coerceAtLeast(0.0) }.toInt() }
    val todayCarbs = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.carbsGrams.coerceAtLeast(0.0) }.toInt() }
    val todayFat = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.fatGrams.coerceAtLeast(0.0) }.toInt() }

    var showFoodDialog by remember { mutableStateOf(false) }
    var foodName by remember { mutableStateOf("") }
    var foodCals by remember { mutableStateOf("") }
    var foodProt by remember { mutableStateOf("") }
    var foodCarb by remember { mutableStateOf("") }
    var foodFatVal by remember { mutableStateOf("") }

    var showPlanDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fasting & Nutrition", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFoodDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Log Meal")
            }
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
            // 1. Active Fasting Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (fast.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timer, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Protocol: ${fast.plan}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { showPlanDialog = true }) {
                                Text("Change")
                            }
                        }

                        Text(
                            text = if (fast.active) {
                                val elapsedSec = fast.elapsedSeconds(System.currentTimeMillis())
                                val hrs = elapsedSec / 3600
                                val mins = (elapsedSec % 3600) / 60
                                "${hrs}h ${mins}m Elapsed"
                            } else {
                                "Ready to Fast"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        LinearProgressIndicator(
                            progress = { fast.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (fast.active) "Target: ${fast.targetSeconds / 3600} hours" else "Target not set",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                "${(fast.progress * 100).toInt()}% Done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (fast.active) {
                                OutlinedButton(
                                    onClick = { scope.launch { repository.extendFast(60) } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+1 Hour")
                                }
                                Button(
                                    onClick = { scope.launch { repository.stopFast() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Stop Fast")
                                }
                            } else {
                                Button(
                                    onClick = { scope.launch { repository.startFast(fast.plan, 16 * 3600L) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Start Fast")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Nutrition Summary Today
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
                            Text("Today's Nutrition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${state.nutritionTarget.targetCalories} kcal target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientMacro(label = "Calories", value = "$todayCalories", unit = "kcal", color = MaterialTheme.colorScheme.primary)
                            NutrientMacro(label = "Protein", value = "$todayProtein", unit = "g", color = Color(0xFFBA1A1A))
                            NutrientMacro(label = "Carbs", value = "$todayCarbs", unit = "g", color = Color(0xFFE08E00))
                            NutrientMacro(label = "Fat", value = "$todayFat", unit = "g", color = Color(0xFF386A20))
                        }
                    }
                }
            }

            // 3. Hydration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF006688).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF006688))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Water Intake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${state.wellness.waterGlasses * 250} ml / ${state.wellness.targetWaterGlasses * 250} ml", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { repository.setWaterGlasses((state.wellness.waterGlasses - 1).coerceAtLeast(0)) } }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                            }
                            IconButton(onClick = { scope.launch { repository.setWaterGlasses(state.wellness.waterGlasses + 1) } }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }

            // 4. Meal History
            item {
                Text("Today's Food Logs (${todayFoodLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (todayFoodLogs.isEmpty()) {
                item {
                    Text("No meals recorded for today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(todayFoodLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(log.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "P: ${log.proteinGrams.toInt()}g · C: ${log.carbsGrams.toInt()}g · F: ${log.fatGrams.toInt()}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${log.calories} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showFoodDialog) {
        AlertDialog(
            onDismissRequest = { showFoodDialog = false },
            title = { Text("Log Food Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Meal / Food Name") }, singleLine = true)
                    OutlinedTextField(value = foodCals, onValueChange = { foodCals = it }, label = { Text("Calories (kcal)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = foodProt, onValueChange = { foodProt = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = foodCarb, onValueChange = { foodCarb = it }, label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = foodFatVal, onValueChange = { foodFatVal = it }, label = { Text("Fat (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cals = foodCals.toIntOrNull() ?: 0
                        val p = foodProt.toDoubleOrNull() ?: 0.0
                        val c = foodCarb.toDoubleOrNull() ?: 0.0
                        val f = foodFatVal.toDoubleOrNull() ?: 0.0
                        if (foodName.isNotBlank() && cals > 0) {
                            scope.launch {
                                repository.logFood(foodName, cals, p, c, f)
                            }
                            foodName = ""
                            foodCals = ""
                            foodProt = ""
                            foodCarb = ""
                            foodFatVal = ""
                            showFoodDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFoodDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPlanDialog) {
        val protocols = listOf("16:8", "18:6", "20:4", "14:10", "12:12")
        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("Select Protocol") },
            text = {
                Column {
                    protocols.forEach { proto ->
                        TextButton(
                            onClick = {
                                val fastHours = proto.substringBefore(":").toLongOrNull() ?: 16L
                                scope.launch {
                                    repository.startFast(proto, fastHours * 3600L)
                                }
                                showPlanDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(proto, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlanDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun NutrientMacro(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(unit, style = MaterialTheme.typography.labelSmall)
    }
}
