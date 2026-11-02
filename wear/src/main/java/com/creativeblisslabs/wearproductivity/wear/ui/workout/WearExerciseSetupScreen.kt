package com.creativeblisslabs.wearproductivity.wear.ui.workout

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.wear.compose.material3.Icon
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearActionButtonGroup
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHeader
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository
import com.creativeblisslabs.wearproductivity.wear.workout.WorkoutPreferences

@Composable
fun WearExerciseSetupScreen(
    repository: ExerciseRepository,
    onWorkoutGenerated: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val savedPrefs = remember { loadPreferences(context) }

    val state = rememberTransformingLazyColumnState()
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableStateOf(savedPrefs.goal) }
    var experience by remember { mutableStateOf(savedPrefs.experience) }
    var discipline by remember { mutableStateOf(savedPrefs.discipline) }
    var duration by remember {
        mutableIntStateOf(savedPrefs.durationMinutes.coerceIn(5, 60))
    }
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    val muscles = listOf("Full Body", "Chest", "Back", "Shoulders", "Arms", "Legs", "Core")
    val equipment = listOf("bodyweight", "dumbbell", "barbell", "machine", "cable", "resistance band")
    val goals = listOf("General fitness", "Build muscle", "Strength", "Weight loss", "Endurance", "Mobility")
    val experiences = listOf("Beginner", "Intermediate", "Advanced")
    val disciplines = listOf("GYM", "CALISTHENICS", "RUNNING", "SWIMMING")
    
    val selectedMuscles = remember { mutableStateListOf<String>().apply { addAll(savedPrefs.targetMuscles) } }
    val selectedEquipment = remember { mutableStateListOf<String>().apply { addAll(savedPrefs.equipment) } }
    val avoidTargets = remember { mutableStateListOf<String>().apply { addAll(savedPrefs.avoidTargets) } }
    val avoidOptions = listOf("Shoulders", "Knees", "Back", "Wrists")
    ScreenScaffold(scrollState = state, timeText = { TimeText() }) { padding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 12.dp,
                start = 12.dp, end = 12.dp
            ),
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = when (step) {
                        0 -> "YOUR GOAL"
                        1 -> "TRAINING STYLE"
                        2 -> "TARGET MUSCLES"
                        else -> "YOUR EQUIPMENT"
                    },
                    badgeText = "STEP ${step + 1} OF 4"
                )
            }
            when (step) {
                0 -> {
                    item { Text("What matters most?", style = MaterialTheme.typography.bodySmall) }
                    items(goals.size) { index ->
                        ChoiceCard(goals[index], goal == goals[index]) { goal = goals[index] }
                    }
                    item { Text("Experience", style = MaterialTheme.typography.labelMedium) }
                    items(experiences.size) { index ->
                        ChoiceCard(experiences[index], experience == experiences[index]) {
                            experience = experiences[index]
                        }
                    }
                }
                1 -> {
                    items(disciplines.size) { index ->
                        ChoiceCard(disciplines[index].lowercase().replaceFirstChar { it.uppercase() },
                            discipline == disciplines[index]) { discipline = disciplines[index] }
                    }
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Session length", style = MaterialTheme.typography.labelMedium)
                            Text("$duration minutes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                androidx.wear.compose.material3.FilledTonalIconButton(
                                    onClick = { duration = (duration - 5).coerceAtLeast(5) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease 5 minutes", modifier = Modifier.size(18.dp))
                                }
                                androidx.wear.compose.material3.FilledTonalIconButton(
                                    onClick = { duration = (duration + 5).coerceAtMost(60) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase 5 minutes", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item { Text("Choose one or more. Full Body clears the others.", style = MaterialTheme.typography.bodySmall) }
                    items(muscles.size) { index ->
                        val muscle = muscles[index]
                        ChoiceCard(muscle, muscle in selectedMuscles) {
                            if (muscle == "Full Body") {
                                selectedMuscles.clear(); selectedMuscles.add(muscle)
                            } else {
                                selectedMuscles.remove("Full Body")
                                if (muscle in selectedMuscles) selectedMuscles.remove(muscle)
                                else selectedMuscles.add(muscle)
                            }
                        }
                    }
                }
                else -> {
                    item { Text("Select what you have available.", style = MaterialTheme.typography.bodySmall) }
                    items(equipment.size) { index ->
                        val item = equipment[index]
                        val selected = item in selectedEquipment

                        ChoiceCard(
                            label = item.replaceFirstChar { it.uppercase() },
                            selected = selected,
                            onClick = {
                                if (selected) {
                                    // Keep at least one equipment option selected.
                                    if (selectedEquipment.size > 1) {
                                        selectedEquipment.remove(item)
                                    }
                                } else {
                                    selectedEquipment.add(item)
                                }
                            }
                        )
                    }
                    item { Text("Optional: areas to avoid", style = MaterialTheme.typography.labelMedium) }
                    items(avoidOptions.size) { index ->
                        val item = avoidOptions[index]
                        ChoiceCard(item, item in avoidTargets) {
                            if (item in avoidTargets) avoidTargets.remove(item) else avoidTargets.add(item)
                        }
                    }
                    item {
                        Text("Avoid choices are only used to filter matching muscle groups. They are not medical advice.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (step > 0) {
                        Button(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("BACK") }
                    }
                    Button(
                        onClick = {
                            if (isGenerating) return@Button

                            if (step < 3) {
                                if (step == 2 && selectedMuscles.isEmpty()) {
                                    generationError = "Select at least one muscle group."
                                    return@Button
                                }

                                step++
                            } else {
                                val prefs = WorkoutPreferences(
                                    goal = goal,
                                    experience = experience,
                                    targetMuscles = selectedMuscles.toSet(),
                                    equipment = selectedEquipment.toSet(),
                                    discipline = discipline,
                                    durationMinutes = duration,
                                    avoidTargets = avoidTargets.toSet()
                                )

                                if (prefs.equipment.isEmpty()) {
                                    generationError = "Select at least one equipment option."
                                    return@Button
                                }

                                isGenerating = true
                                generationError = null

                                scope.launch {
                                    try {
                                        savePreferences(context, prefs)

                                        val routine = withContext(Dispatchers.Default) {
                                            repository.generatePersonalizedWorkout(prefs)
                                        }

                                        if (routine.isEmpty()) {
                                            generationError = "No matching exercises found. Change your filters."
                                        } else {
                                            onWorkoutGenerated(routine.map { it.id })
                                        }
                                    } catch (e: Exception) {
                                        generationError = "Could not generate your workout. Try again."
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text(
                            when {
                                isGenerating -> "BUILDING..."
                                step < 3 -> "NEXT"
                                else -> "BUILD PLAN"

                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                }
            }
            generationError?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Text(if (selected) "✓" else "+", style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun savePreferences(context: Context, prefs: WorkoutPreferences) {
    val prefsFile = context.getSharedPreferences("lifeos_workout_setup_prefs", Context.MODE_PRIVATE)
    prefsFile.edit()
        .putString("goal", prefs.goal)
        .putString("experience", prefs.experience)
        .putString("discipline", prefs.discipline)
        .putInt("duration", prefs.durationMinutes)
        .putStringSet("targetMuscles", prefs.targetMuscles)
        .putStringSet("equipment", prefs.equipment)
        .putStringSet("avoidTargets", prefs.avoidTargets)
        .apply()
}

private fun loadPreferences(context: Context): WorkoutPreferences {
    val prefsFile = context.getSharedPreferences("lifeos_workout_setup_prefs", Context.MODE_PRIVATE)
    return WorkoutPreferences(
        goal = prefsFile.getString("goal", "General fitness") ?: "General fitness",
        experience = prefsFile.getString("experience", "Beginner") ?: "Beginner",
        discipline = prefsFile.getString("discipline", "GYM") ?: "GYM",
        durationMinutes = prefsFile.getInt("duration", 20),
        targetMuscles = prefsFile
            .getStringSet("targetMuscles", setOf("Full Body"))
            ?.toSet()
            ?: setOf("Full Body"),

        equipment = prefsFile
            .getStringSet("equipment", setOf("bodyweight"))
            ?.toSet()
            ?: setOf("bodyweight"),

        avoidTargets = prefsFile
            .getStringSet("avoidTargets", emptySet())
            ?.toSet()
            ?: emptySet()
    )
}
