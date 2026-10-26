package com.creativeblisslabs.wearproductivity.wear.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.wear.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearActionButtonGroup
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHeader
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository
import kotlinx.coroutines.launch

@Composable
fun WearExerciseSetupScreen(
    repository: ExerciseRepository,
    onWorkoutGenerated: (List<String>) -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }

    // Demographics
    var weightKg by remember { mutableIntStateOf(70) }
    var heightCm by remember { mutableIntStateOf(175) }

    // Equipment Selection
    val equipments = listOf("dumbbell", "barbell", "bodyweight", "machine", "cable")
    val selectedEquipment = remember { mutableStateListOf<String>() }

    // Muscle / Split Target
    val splits = listOf("Push", "Pull", "Legs", "Core", "Full Body")
    var selectedSplitIndex by remember { mutableIntStateOf(0) }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> {
                    item { WearExpressiveHeader(title = "BODY STATS", badgeText = "STEP 1") }
                    item {
                        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Weight", style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { weightKg -= 1 }) { Text("-") }
                                    Text("${weightKg}kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Button(onClick = { weightKg += 1 }) { Text("+") }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
                2 -> {
                    item { WearExpressiveHeader(title = "EQUIPMENT", badgeText = "STEP 2") }
                    items(equipments.size) { idx ->
                        val eq = equipments[idx]
                        val isSelected = selectedEquipment.contains(eq)
                        Card(
                            onClick = { if (isSelected) selectedEquipment.remove(eq) else selectedEquipment.add(eq) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(eq.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { step = 3 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
                3 -> {
                    item { WearExpressiveHeader(title = "WORKOUT TARGET", badgeText = "STEP 3") }
                    item {
                        WearActionButtonGroup(
                            options = splits,
                            selectedIndex = selectedSplitIndex,
                            onOptionSelected = { selectedSplitIndex = it }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val routine = repository.generateWorkoutRoutine(splits[selectedSplitIndex], selectedEquipment)
                                    onWorkoutGenerated(routine.map { it.id })
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("GENERATE PLAN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}
