package com.creativeblisslabs.wearproductivity.wear.workout

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.InputStreamReader

import kotlinx.serialization.SerialName

@Serializable
data class ExerciseDef(
    val id: String,
    val name: String,
    val force: String? = null,
    val level: String = "intermediate",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "strength",
    val images: List<String> = emptyList()
)

@Serializable
data class WorkoutPlanDef(
    val id: String,
    val name: String,
    val discipline: String,
    val muscleOrZone: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Double,
    val restSeconds: Int,
    val stepDurationSeconds: Int,
    val exercises: List<String>
) {
    fun toWorkoutProgramItem(): com.creativeblisslabs.wearproductivity.wear.ui.workout.WorkoutProgramItem {
        val mappedDiscipline = runCatching { 
            com.creativeblisslabs.wearproductivity.wear.ui.workout.WorkoutDiscipline.valueOf(discipline) 
        }.getOrDefault(com.creativeblisslabs.wearproductivity.wear.ui.workout.WorkoutDiscipline.GYM)
        
        return com.creativeblisslabs.wearproductivity.wear.ui.workout.WorkoutProgramItem(
            id = id,
            name = name,
            discipline = mappedDiscipline,
            muscleOrZone = muscleOrZone,
            targetSets = targetSets,
            targetReps = targetReps,
            targetWeightKg = targetWeightKg,
            restSeconds = restSeconds,
            stepDurationSeconds = stepDurationSeconds,
            steps = exercises
        )
    }
}

class ExerciseRepository(private val context: Context) {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    var allExercises: List<ExerciseDef> = emptyList()
        private set
    var allWorkouts: List<WorkoutPlanDef> = emptyList()
        private set

    init {
        loadDatabase()
    }

    private fun loadDatabase() {
        try {
            context.assets.open("repdb/exercises.json").use { inputStream ->
                val jsonString = InputStreamReader(inputStream).readText()
                allExercises = jsonParser.decodeFromString<List<ExerciseDef>>(jsonString)
            }
            context.assets.open("repdb/workouts.json").use { inputStream ->
                val jsonString = InputStreamReader(inputStream).readText()
                allWorkouts = jsonParser.decodeFromString<List<WorkoutPlanDef>>(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            allExercises = emptyList()
            allWorkouts = emptyList()
        }
    }

    fun getExercisesByEquipment(equipmentList: List<String>): List<ExerciseDef> {
        if (equipmentList.isEmpty()) return allExercises
        return allExercises.filter { it.equipment != null && equipmentList.contains(it.equipment.lowercase()) }
    }

    fun getExercisesByPrimaryMuscle(muscle: String): List<ExerciseDef> {
        val target = muscle.lowercase()
        return allExercises.filter { it.primaryMuscles.map { m -> m.lowercase() }.contains(target) }
    }
    
    fun generateWorkoutRoutine(targetSplit: String, availableEquipment: List<String>): List<ExerciseDef> {
        // Try to match a predefined workout split
        val predefined = allWorkouts.find { it.muscleOrZone.equals(targetSplit, ignoreCase = true) || it.name.contains(targetSplit, ignoreCase = true) }
        
        if (predefined != null) {
            val routine = predefined.exercises.mapNotNull { exId ->
                allExercises.find { it.id == exId }
            }
            if (routine.isNotEmpty()) return routine
        }

        // Fallback generator if no exact plan matches
        val filteredByEq = getExercisesByEquipment(availableEquipment)
        val routine = mutableListOf<ExerciseDef>()

        when (targetSplit.lowercase()) {
            "push" -> {
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("chest") }.take(2))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("shoulders") }.take(2))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("triceps") }.take(1))
            }
            "pull" -> {
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("middle back") || it.primaryMuscles.contains("lats") }.take(3))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("biceps") }.take(2))
            }
            "legs" -> {
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("quadriceps") }.take(2))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("hamstrings") || it.primaryMuscles.contains("glutes") }.take(2))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("calves") }.take(1))
            }
            "core" -> {
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("abdominals") || it.primaryMuscles.contains("lower back") }.take(4))
            }
            else -> {
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("chest") }.take(1))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("middle back") }.take(1))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("quadriceps") }.take(1))
                routine.addAll(filteredByEq.filter { it.primaryMuscles.contains("shoulders") }.take(1))
            }
        }
        
        return routine.take(5)
    }
}

