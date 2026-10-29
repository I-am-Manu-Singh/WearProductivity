package com.creativeblisslabs.wearproductivity.wear.ui.workout

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseDef
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CustomRoutineExercise(
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int = 4,
    val reps: Int = 10,
    val weightKg: Double = 50.0
)

@Serializable
data class CustomRoutineDay(
    val dayOfWeek: String,
    val muscleFocus: String,
    val exercises: List<CustomRoutineExercise> = emptyList()
)

@Serializable
data class CustomWorkoutRoutine(
    val id: String,
    val name: String,
    val splitType: String,
    val days: List<CustomRoutineDay> = emptyList()
)

object CustomRoutinePrefs {
    private const val PREFS_NAME = "lifeos_custom_routines_v1"
    private const val KEY_ROUTINES = "saved_custom_routines_json"

    fun loadRoutines(context: Context): List<CustomWorkoutRoutine> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ROUTINES, null)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<List<CustomWorkoutRoutine>>(raw) }.getOrDefault(emptyList())
    }

    fun saveRoutines(context: Context, routines: List<CustomWorkoutRoutine>) {
        val encoded = Json.encodeToString(routines)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ROUTINES, encoded).apply()
    }
}

@Composable
fun WearCustomRoutineBuilderScreen(
    repository: ExerciseRepository,
    onRoutineSaved: (CustomWorkoutRoutine) -> Unit
) {
    val context = LocalContext.current
    val columnState = rememberTransformingLazyColumnState()

    var step by remember { mutableIntStateOf(0) } // 0: Routine Meta, 1: Day & Muscle Assign, 2: Exercise Picker, 3: Weekly Overview
    var routineName by remember { mutableStateOf("My Custom Split") }
    var splitType by remember { mutableStateOf("Push / Pull / Legs") }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var muscleFocus by remember { mutableStateOf("Chest & Triceps") }

    // Temporary storage for building days
    val routineDays = remember {
        mutableStateMapOf<String, CustomRoutineDay>().apply {
            daysOfWeek.forEach { day ->
                put(day, CustomRoutineDay(dayOfWeek = day, muscleFocus = "General", exercises = emptyList()))
            }
        }
    }

    // Exercise Picker State
    var selectedExerciseForConfig by remember { mutableStateOf<ExerciseDef?>(null) }
    var draftSets by remember { mutableIntStateOf(4) }
    var draftReps by remember { mutableIntStateOf(10) }
    var draftWeight by remember { mutableDoubleStateOf(50.0) }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding(), start = 12.dp, end = 12.dp),
            modifier = Modifier.fillMaxSize().background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                0 -> {
                    item { WearExpressiveHeader(title = "CUSTOM ROUTINE", badgeText = "STEP 1") }
                    item {
                        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Routine Name", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(routineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item {
                        WearActionButtonGroup(
                            options = listOf("PPL", "Bro Split", "Upper/Lower", "Custom"),
                            selectedIndex = when(splitType) {
                                "Push / Pull / Legs" -> 0
                                "Bro Split" -> 1
                                "Upper / Lower" -> 2
                                else -> 3
                            },
                            onOptionSelected = { idx ->
                                splitType = when(idx) {
                                    0 -> "Push / Pull / Legs"
                                    1 -> "Bro Split"
                                    2 -> "Upper / Lower"
                                    else -> "Custom"
                                }
                            }
                        )
                    }
                    item {
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("NEXT: ASSIGN DAYS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                1 -> {
                    val currentDay = daysOfWeek[selectedDayIndex]
                    val dayData = routineDays[currentDay]!!

                    item { WearExpressiveHeader(title = currentDay.uppercase(), badgeText = "STEP 2") }
                    item {
                        WearActionButtonGroup(
                            options = daysOfWeek.map { it.take(3) },
                            selectedIndex = selectedDayIndex,
                            onOptionSelected = { selectedDayIndex = it }
                        )
                    }
                    item {
                        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Muscle Focus", style = MaterialTheme.typography.labelSmall)
                                Text(dayData.muscleFocus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Assigned Exercises: ${dayData.exercises.size}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("ADD EXERCISES")
                            }
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text("REVIEW")
                            }
                        }
                    }
                }
                2 -> {
                    val currentDay = daysOfWeek[selectedDayIndex]
                    val dayData = routineDays[currentDay]!!

                    item { WearExpressiveHeader(title = "SELECT EXERCISES", badgeText = currentDay) }

                    if (selectedExerciseForConfig != null) {
                        val ex = selectedExerciseForConfig!!
                        item {
                            Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Sets: $draftSets · Reps: $draftReps · Weight: ${draftWeight}kg", style = MaterialTheme.typography.bodySmall)
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(onClick = { if (draftSets > 1) draftSets-- }, modifier = Modifier.weight(1f)) { Text("Sets-") }
                                        Button(onClick = { draftSets++ }, modifier = Modifier.weight(1f)) { Text("Sets+") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(onClick = { if (draftReps > 1) draftReps-- }, modifier = Modifier.weight(1f)) { Text("Reps-") }
                                        Button(onClick = { draftReps++ }, modifier = Modifier.weight(1f)) { Text("Reps+") }
                                    }
                                    Button(
                                        onClick = {
                                            val newEx = CustomRoutineExercise(
                                                exerciseId = ex.id,
                                                exerciseName = ex.name,
                                                sets = draftSets,
                                                reps = draftReps,
                                                weightKg = draftWeight
                                            )
                                            val updatedList = dayData.exercises + newEx
                                            routineDays[currentDay] = dayData.copy(exercises = updatedList)
                                            selectedExerciseForConfig = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("CONFIRM EXERCISE")
                                    }
                                }
                            }
                        }
                    } else {
                        items(repository.allExercises.size) { idx ->
                            val ex = repository.allExercises[idx]
                            Card(
                                onClick = { selectedExerciseForConfig = ex },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("${ex.primaryMuscles.firstOrNull()?.uppercase() ?: "GYM"} · ${ex.equipment?.uppercase() ?: "BODYWEIGHT"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        }
                        item {
                            Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                                Text("DONE ADDING")
                            }
                        }
                    }
                }
                3 -> {
                    item { WearExpressiveHeader(title = "WEEKLY SUMMARY", badgeText = routineName) }
                    daysOfWeek.forEach { day ->
                        val dData = routineDays[day]!!
                        if (dData.exercises.isNotEmpty()) {
                            item {
                                Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("$day (${dData.muscleFocus})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        dData.exercises.forEach { item ->
                                            Text("• ${item.exerciseName} (${item.sets}x${item.reps} @ ${item.weightKg.toInt()}kg)", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                val routine = CustomWorkoutRoutine(
                                    id = "custom-${System.currentTimeMillis()}",
                                    name = routineName,
                                    splitType = splitType,
                                    days = routineDays.values.toList()
                                )
                                val existing = CustomRoutinePrefs.loadRoutines(context)
                                CustomRoutinePrefs.saveRoutines(context, existing + routine)
                                onRoutineSaved(routine)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("SAVE ROUTINE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}
