package com.creativeblisslabs.wearproductivity.wear.workout

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val images: List<String> = emptyList(),
    // Optional metadata. Defaults keep older JSON compatible.
    val description: String? = null,
    val tips: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val bodyPart: String? = null,
    val bodypart: String? = null,
    val isBodyweight: Boolean? = null,
    val bodyweight: Boolean? = null,
    val unilateral: Boolean? = null,
    val isUnilateral: Boolean? = null,
    val met: Double? = null,
    val relations: List<String> = emptyList()
) {
    val effectiveBodyPart: String? get() = bodyPart ?: bodypart
    val effectiveBodyweight: Boolean? get() = isBodyweight ?: bodyweight
    val effectiveUnilateral: Boolean? get() = isUnilateral ?: unilateral
}

@Serializable
data class WorkoutPlanDef(
    val id: String,
    val name: String,
    val discipline: String = "GYM",
    val muscleOrZone: String = "Full Body",
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Double = 0.0,
    val restSeconds: Int = 60,
    val stepDurationSeconds: Int = 60,
    val exercises: List<String> = emptyList()
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

data class WorkoutPreferences(
    val goal: String = "General fitness",
    val experience: String = "Beginner",
    val targetMuscles: Set<String> = setOf("Full Body"),
    val equipment: Set<String> = setOf("bodyweight"),
    val discipline: String = "GYM",
    val durationMinutes: Int = 20,
    val avoidTargets: Set<String> = emptySet()
)

class ExerciseRepository(private val context: Context) {
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    var allExercises: List<ExerciseDef> = emptyList()
        private set
    var allWorkouts: List<WorkoutPlanDef> = emptyList()
        private set

    init { loadDatabase() }

    private inline fun <reified T> loadJsonFile(path: String): T? = try {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use {
            jsonParser.decodeFromString<T>(it.readText())
        }
    } catch (e: Exception) {
        Log.e("ExerciseRepository", "Failed to load $path", e)
        null
    }

    private fun loadDatabase() {
        allExercises = loadJsonFile<List<ExerciseDef>>("repdb/exercises.json").orEmpty()
        allWorkouts = loadJsonFile<List<WorkoutPlanDef>>("repdb/workouts.json").orEmpty()

        Log.i(
            "WorkoutDebug",
            "Database loaded: exercises=${allExercises.size}, workouts=${allWorkouts.size}"
        )

        Log.d(
            "WorkoutDebug",
            "Sample exercises=${allExercises.take(10).map { "${it.id}:${it.name}" }}"
        )

        Log.d(
            "WorkoutDebug",
            "Duplicate exercise IDs=${allExercises.groupingBy { it.id }.eachCount().filter { it.value > 1 }}"
        )
    }
    fun getExercisesByEquipment(equipmentList: List<String>): List<ExerciseDef> {
        if (equipmentList.isEmpty()) return allExercises
        val wanted = equipmentList.map(::normalize).toSet()
        return allExercises.filter { ex ->
            val eq = normalize(ex.equipment.orEmpty())
            eq.isBlank() || eq in wanted || eq == "bodyweight" && "bodyweight" in wanted
        }
    }

    fun getExercisesByPrimaryMuscle(muscle: String): List<ExerciseDef> {
        val target = normalize(muscle)
        return allExercises.filter { ex ->
            (ex.primaryMuscles + ex.secondaryMuscles + listOfNotNull(ex.effectiveBodyPart))
                .any { normalize(it).contains(target) || target.contains(normalize(it)) }
        }
    }

    /**
     * Preference-aware recommendation. A matching predefined plan is preferred when
     * discipline and target are compatible; otherwise score the exercise library.
     */
    fun generatePersonalizedWorkout(preferences: WorkoutPreferences): List<ExerciseDef> {
        val targetTokens = preferences.targetMuscles
            .filterNot { normalize(it) == "full body" }
            .map(::normalize)
        val avoidTokens = preferences.avoidTargets.map(::normalize)
        val allowedEquipment = preferences.equipment
            .map(::normalize)
            .toSet()
            .ifEmpty { setOf("bodyweight") }
        val beginner = normalize(preferences.experience) == "beginner"

        Log.d(
            "WorkoutDebug",
            "Preferences: goal=${preferences.goal}, level=${preferences.experience}, " +
                    "muscles=${preferences.targetMuscles}, equipment=${preferences.equipment}, " +
                    "discipline=${preferences.discipline}, duration=${preferences.durationMinutes}, " +
                    "avoid=${preferences.avoidTargets}"
        )

        Log.d("WorkoutDebug", "Initial exercise pool=${allExercises.size}")

        val eligible = allExercises.filter { ex ->
            val eq = normalize(ex.equipment.orEmpty())
            val equipmentOk =
                eq.isBlank() ||
                        eq in allowedEquipment ||
                        (
                                ex.effectiveBodyweight == true &&
                                        "bodyweight" in allowedEquipment
                                )
            val avoided = (ex.primaryMuscles + ex.secondaryMuscles + listOfNotNull(ex.effectiveBodyPart))
                .any { muscle -> avoidTokens.any { it.isNotBlank() && normalize(muscle).contains(it) } }
            val level = normalize(ex.level)
            val levelOk = !beginner || level !in setOf("advanced", "expert")
            val discipline = normalize(preferences.discipline)
            val category = normalize(ex.category)
            val disciplineOk = when (discipline) {
                "running" -> category.contains("cardio") || category.contains("conditioning") ||
                    normalize(ex.tags.joinToString(" ")).contains("running")
                "swimming" -> normalize(ex.tags.joinToString(" ")).contains("swim") ||
                    normalize(ex.name).contains("swim")
                "calisthenics" -> ex.effectiveBodyweight == true || eq == "bodyweight" ||
                    normalize(ex.tags.joinToString(" ")).contains("calisthenics")
                else -> true
            }
            equipmentOk && !avoided && levelOk && disciplineOk
        }
        Log.d("WorkoutDebug", "Eligible after filters=${eligible.size}")

        Log.d(
            "WorkoutDebug",
            "Eligible exercises=${eligible.map { "${it.id}:${it.name}" }}"
        )
        fun score(ex: ExerciseDef): Int {
            val muscles = (ex.primaryMuscles + ex.secondaryMuscles + listOfNotNull(ex.effectiveBodyPart))
                .map(::normalize)
            val targets = targetTokens.sumOf { t ->
                if (muscles.any { it.contains(t) || t.contains(it) }) 5 else 0
            }
            val goal = normalize(preferences.goal)
            val goalScore = if ((ex.goals + ex.tags + listOfNotNull(ex.description))
                    .joinToString(" ").let(::normalize).contains(goal)) 3 else 0
            val equipmentScore = if (normalize(ex.equipment.orEmpty()) in allowedEquipment) 2 else 0
            val levelScore = if (beginner && normalize(ex.level) in setOf("beginner", "novice")) 2 else 0
            return targets + goalScore + equipmentScore + levelScore
        }

        val ranked = eligible.sortedWith(
            compareByDescending<ExerciseDef> { score(it) }
                .thenBy { normalize(it.level) }
                .thenBy { it.name }
        )

        val maxCount = when {
            preferences.durationMinutes <= 10 -> 3
            preferences.durationMinutes <= 20 -> 5
            preferences.durationMinutes <= 30 -> 7
            else -> 9
        }

        val muscleAliases = mapOf(
            "arms" to setOf(
                "biceps", "biceps_brachii",
                "triceps", "triceps_brachii",
                "brachialis", "forearm", "forearms",
                "forearm_flexors", "forearm_extensors"
            ),
            "shoulders" to setOf(
                "deltoid", "anterior_deltoid",
                "lateral_deltoid", "posterior_deltoid",
                "rotator_cuff", "supraspinatus",
                "trapezius"
            ),
            "core" to setOf(
                "rectus_abdominis", "transverse_abdominis",
                "obliques", "serratus_anterior",
                "erector_spinae"
            ),
            "chest" to setOf(
                "pectoralis_major", "pectoralis_minor"
            ),
            "back" to setOf(
                "latissimus_dorsi", "rhomboids",
                "trapezius", "erector_spinae"
            ),
            "legs" to setOf(
                "quadriceps", "hamstrings", "gluteus_maximus",
                "gluteus_medius", "calves", "gastrocnemius",
                "soleus", "adductors"
            )
        )

        val matching = ranked.filter { ex ->
            val exerciseMuscles = (
                    ex.primaryMuscles +
                            ex.secondaryMuscles +
                            listOfNotNull(ex.effectiveBodyPart)
                    )
                .map(::normalize)
                .filter { it.isNotBlank() }
                .toSet()

            targetTokens.isEmpty() || targetTokens.any { target ->
                val aliases = muscleAliases[target].orEmpty() + target

                exerciseMuscles.any { muscle ->
                    aliases.any { alias ->
                        muscle == alias ||
                                muscle.contains(alias) ||
                                alias.contains(muscle)
                    }
                }
            }
        }
        Log.d(
            "WorkoutDebug",
            "Target tokens=$targetTokens; " +
                    "sample muscle data=" +
                    ranked.take(15).map { ex ->
                        "${ex.name}: primary=${ex.primaryMuscles}, " +
                                "secondary=${ex.secondaryMuscles}, " +
                                "bodyPart=${ex.effectiveBodyPart}"
                    }
        )

        val targeted = matching
            .sortedWith(
                compareByDescending<ExerciseDef> { score(it) }
                    .thenBy { normalize(it.name) }
            )
            .take(maxCount)
        if (matching.isEmpty()) {
            Log.w(
                "WorkoutDebug",
                "No exercises matched targets=$targetTokens; using fallback"
            )
        }
        val result = (
                targeted
                    .ifEmpty { ranked.take(maxCount) }
                    .ifEmpty { allExercises.take(maxCount) }
                )
            .distinctBy { it.id }

        Log.d(
            "WorkoutDebug",
            "Selection: eligible=${eligible.size}, " +
                    "muscleMatches=${matching.size}, " +
                    "maxCount=$maxCount, final=${result.size}"
        )

        Log.d(
            "WorkoutDebug",
            "Final workout IDs=${result.map { it.id }}"
        )

        return result

        Log.d(
            "WorkoutDebug",
            "Selection: ranked=${ranked.size}, matching=${matching.size}, " +
                    "maxCount=$maxCount, targeted=${targeted.size}, final=${result.size}"
        )

        Log.d(
            "WorkoutDebug",
            "Final workout=${result.map { "${it.id}:${it.name}" }}"
        )

        return result

    }

    /** Backwards-compatible API used by existing callers. */
    fun generateWorkoutRoutine(targetSplit: String, availableEquipment: List<String>): List<ExerciseDef> {
        val prefs = WorkoutPreferences(
            targetMuscles = setOf(targetSplit),
            equipment = availableEquipment.toSet(),
            durationMinutes = 20
        )
        return generatePersonalizedWorkout(prefs)
    }

    fun getWorkoutExercises(workoutId: String): List<ExerciseDef> {
        val plan = allWorkouts.firstOrNull { it.id == workoutId } ?: return emptyList()
        val map = allExercises.associateBy { it.id }
        return plan.exercises.mapNotNull(map::get)
    }

    data class ExerciseImageSet(
        val style: String,
        val start: String?,
        val peak: String?,
        val main: String?
    ) {
        val hasAnimation get() = start != null && peak != null
        val preferredImage get() = start ?: peak ?: main
    }

    private fun assetExists(path: String): Boolean = try {
        context.assets.open(path).use { }
        true
    } catch (_: Exception) { false }

    private fun findExerciseImage(id: String, style: String, frame: String): String? {
        val base = "repdb/images/exercises/$id/$style-$frame"
        return listOf("webp", "png", "jpg", "jpeg").asSequence()
            .map { "$base.$it" }.firstOrNull(::assetExists)
    }

    fun getExerciseImageSet(exercise: ExerciseDef): ExerciseImageSet? {
        for (style in listOf("classic", "flat")) {
            val set = ExerciseImageSet(
                style,
                findExerciseImage(exercise.id, style, "start"),
                findExerciseImage(exercise.id, style, "peak"),
                findExerciseImage(exercise.id, style, "main")
            )
            if (set.hasAnimation || set.main != null) return set
        }
        return null
    }

    fun getExerciseAnimationFrames(exercise: ExerciseDef): Pair<String, String>? {
        for (style in listOf("classic", "flat")) {
            val start = findExerciseImage(exercise.id, style, "start")
            val peak = findExerciseImage(exercise.id, style, "peak")
            if (start != null && peak != null) return start to peak
        }
        return null
    }

    fun getExerciseImagePath(exercise: ExerciseDef): String? =
        getExerciseImageSet(exercise)?.preferredImage

    private fun normalize(value: String): String = value.trim().lowercase()
        .replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ")
}
