package com.creativeblisslabs.wearproductivity.wear.ui.workout

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import org.json.JSONArray
import org.json.JSONObject
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import com.creativeblisslabs.wearproductivity.wear.sensors.RepDetectionEngine
import com.creativeblisslabs.wearproductivity.wear.sensors.StepCounterEngine
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository
import com.creativeblisslabs.wearproductivity.wear.workout.ExerciseDef
import com.creativeblisslabs.wearproductivity.wear.workout.WorkoutScienceEngine
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.RunWorkout
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import coil.compose.AsyncImage
import coil.request.ImageRequest


enum class WorkoutDiscipline(val label: String, val shortLabel: String, val accent: Color) {
    GYM("Gym Strength", "Gym", Color(0xFFFF9A7A)),
    RUNNING("Running", "Run", Color(0xFF79E2B0)),
    SWIMMING("Swimming", "Swim", Color(0xFF68B6FF)),
    CALISTHENICS("Calisthenics", "Calis", Color(0xFFBBA5FF));

    val icon: ImageVector
        get() = when (this) {
            GYM -> Icons.Rounded.FitnessCenter
            RUNNING -> Icons.Rounded.DirectionsRun
            SWIMMING -> Icons.Rounded.Pool
            CALISTHENICS -> Icons.Rounded.SportsGymnastics
        }
}
private enum class GuidedOverlayMode {
    NONE,
    INSTRUCTIONS,
    TIPS
}
data class WorkoutProgramItem(
    val id: String,
    val name: String,
    val discipline: WorkoutDiscipline,
    val muscleOrZone: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Double,
    val restSeconds: Int,
    val stepDurationSeconds: Int,
    val steps: List<String>
)

data class PersonalRecordEntry(
    val id: String,
    val exerciseName: String,
    val discipline: WorkoutDiscipline,
    val currentValue: Double,
    val previousBest: Double,
    val targetGoal: Double,
    val unit: String
)

data class WorkoutHistoryRecord(
    val id: String,
    val title: String,
    val discipline: WorkoutDiscipline,
    val setsCompleted: Int,
    val totalRepsOrLaps: Int,
    val totalVolumeOrDist: Double,
    val durationSeconds: Int,
    val isNewPr: Boolean,
    val timestampLabel: String,
    val timestampEpochMillis: Long = 0L
)

private fun formatDurationClock(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

private fun currentWorkoutTimeStamp(): String =
    SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date())

// =============================================================================
// REAL-TIME TTS VOICE COACH & ACOUSTIC BEEP SYNTHESIZER ENGINE
// =============================================================================
private class WorkoutAudioCoachEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching {
                        tts?.language = Locale.US
                        tts?.setSpeechRate(1.05f)
                        isTtsReady = true
                    }
                }
            }
        }
    }

    fun announce(cueText: String, isMuted: Boolean, playToneHz: Double = 880.0) {
        if (isMuted) return
        playSynthesizedBeep(playToneHz, durationMs = 140)
        try {
            if (isTtsReady) {
                tts?.speak(cueText, TextToSpeech.QUEUE_FLUSH, null, "WorkoutCoachCue")
            }
        } catch (_: Exception) {}
    }



    private fun playSynthesizedBeep(
        freqHz: Double,
        durationMs: Int
    ) {
        scope.launch(Dispatchers.Default) {
            var track: AudioTrack? = null

            try {
                val sampleRate = 22_050
                val safeDuration = durationMs.coerceIn(40, 1_000)
                val numSamples = (
                        sampleRate * safeDuration / 1_000.0
                        ).toInt().coerceAtLeast(256)

                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                if (minBuffer <= 0) return@launch

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(
                        maxOf(minBuffer, numSamples * 2)
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    return@launch
                }

                val pcm = ShortArray(numSamples)

                for (i in pcm.indices) {
                    val envelope = sin(Math.PI * i / numSamples)
                    val sample = sin(
                        2.0 * Math.PI * i * freqHz.coerceIn(80.0, 8_000.0) /
                                sampleRate
                    ) * envelope * 0.35

                    pcm[i] = (
                            sample * Short.MAX_VALUE
                            ).toInt().toShort()
                }

                val written = track.write(
                    pcm,
                    0,
                    pcm.size,
                    AudioTrack.WRITE_BLOCKING
                )

                if (written != pcm.size) return@launch

                track.play()
                delay(safeDuration.toLong() + 80L)

            } catch (error: Exception) {
                Log.e("WorkoutAudioCoach", "Beep playback failed", error)
            } finally {
                runCatching {
                    track?.let {
                        if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            it.stop()
                        }
                        it.release()
                    }
                }
            }
        }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }
}

// =============================================================================
// PERSISTENT STORAGE FOR CUSTOM PROGRAMS, PR TARGETS & SESSION LOGS
// =============================================================================
private object WorkoutStudioPrefs {
    private const val KEY_HISTORY_RETENTION = "workout_history_retention"

    private const val PREFS_NAME = "lifeos_workout_studio_prefs_v3"
    private const val KEY_PROGRAMS = "custom_workout_programs_v3"
    private const val KEY_ACTIVE_ID = "active_workout_program_id"
    private const val KEY_PR_LIST = "workout_pr_records_v3"
    private const val KEY_HISTORY = "workout_session_history_v3"
    private const val KEY_TTS_MUTED = "workout_tts_muted"
    private const val KEY_MUSIC_MUTED = "workout_music_muted"
    
    // Active Multi-Week Program Tracker keys
    private const val KEY_ACTIVE_TRAINING_PROGRAM_ID = "active_training_program_id"
    private const val KEY_COMPLETED_TRAINING_DAYS = "completed_training_days_set"

    fun loadActiveTrainingProgramId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACTIVE_TRAINING_PROGRAM_ID, null)
    }
    fun getHistoryRetention(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HISTORY_RETENTION, 500)
            .coerceIn(30, 5000)

    fun setHistoryRetention(context: Context, limit: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HISTORY_RETENTION, limit.coerceIn(30, 5000))
            .apply()
    }
    fun saveActiveTrainingProgramId(context: Context, programId: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_TRAINING_PROGRAM_ID, programId).apply()
    }

    fun loadCompletedTrainingDays(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getStringSet(KEY_COMPLETED_TRAINING_DAYS, emptySet()) ?: emptySet()
    }

    fun markTrainingDayCompleted(context: Context, programId: String, week: Int, day: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_COMPLETED_TRAINING_DAYS, emptySet()) ?: emptySet()
        val next = current + "${programId}_W${week}_D${day}"
        prefs.edit().putStringSet(KEY_COMPLETED_TRAINING_DAYS, next).apply()
    }


    fun loadPrograms(
        context: Context,
        repoRuns: List<RunWorkout>
    ): List<WorkoutProgramItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PROGRAMS, null)

        if (!raw.isNullOrBlank() && raw.trimStart().startsWith("[")) {
            val parsed = runCatching {
                val array = JSONArray(raw)

                buildList {
                    for (index in 0 until array.length()) {
                        val obj = array.optJSONObject(index) ?: continue

                        val discipline = runCatching {
                            WorkoutDiscipline.valueOf(
                                obj.optString("discipline")
                            )
                        }.getOrDefault(WorkoutDiscipline.GYM)

                        val stepsArray = obj.optJSONArray("steps")
                        val steps = buildList {
                            if (stepsArray != null) {
                                for (stepIndex in 0 until stepsArray.length()) {
                                    stepsArray.optString(stepIndex)
                                        .takeIf { it.isNotBlank() }
                                        ?.let(::add)
                                }
                            }
                        }

                        val item = WorkoutProgramItem(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            discipline = discipline,
                            muscleOrZone = obj.optString("muscleOrZone"),
                            targetSets = obj.optInt("targetSets", 4).coerceIn(1, 30),
                            targetReps = obj.optInt("targetReps", 10).coerceIn(1, 200),
                            targetWeightKg = obj.optDouble("targetWeightKg", 60.0)
                                .takeIf { it.isFinite() }
                                ?.coerceIn(0.0, 500.0) ?: 60.0,
                            restSeconds = obj.optInt("restSeconds", 60).coerceIn(10, 600),
                            stepDurationSeconds = obj.optInt("stepDurationSeconds", 180)
                                .coerceIn(15, 7200),
                            steps = steps
                        )

                        if (item.id.isNotBlank() && item.name.isNotBlank()) {
                            add(item)
                        }
                    }
                }
            }.getOrDefault(emptyList())

            if (parsed.isNotEmpty()) return parsed
        }

        // Legacy migration: read old delimiter-based records once.
        if (!raw.isNullOrBlank()) {
            val migrated = runCatching {
                raw.split(";;").mapNotNull { entry ->
                    val p = entry.split("|")
                    if (p.size < 10) return@mapNotNull null

                    WorkoutProgramItem(
                        id = p[0],
                        name = p[1],
                        discipline = runCatching {
                            WorkoutDiscipline.valueOf(p[2])
                        }.getOrDefault(WorkoutDiscipline.GYM),
                        muscleOrZone = p[3],
                        targetSets = p[4].toIntOrNull()?.coerceIn(1, 30) ?: 4,
                        targetReps = p[5].toIntOrNull()?.coerceIn(1, 200) ?: 10,
                        targetWeightKg = p[6].toDoubleOrNull()
                            ?.takeIf { it.isFinite() }
                            ?.coerceIn(0.0, 500.0) ?: 60.0,
                        restSeconds = p[7].toIntOrNull()?.coerceIn(10, 600) ?: 60,
                        stepDurationSeconds = p[8].toIntOrNull()
                            ?.coerceIn(15, 7200) ?: 180,
                        steps = p[9].split("~").filter { it.isNotBlank() }
                    )
                }
            }.getOrDefault(emptyList())

            if (migrated.isNotEmpty()) {
                savePrograms(context, migrated)
                return migrated
            }
        }

        return repoRuns.take(3).map { run ->
            WorkoutProgramItem(
                id = run.stamp.id,
                name = run.title,
                discipline = WorkoutDiscipline.RUNNING,
                muscleOrZone = "W${run.week} · D${run.day}",
                targetSets = run.intervals.size.coerceAtLeast(3),
                targetReps = 1,
                targetWeightKg = run.distanceKm ?: 3.5,
                restSeconds = 60,
                stepDurationSeconds = 180,
                steps = run.intervals.ifEmpty {
                    listOf("Warmup 5m", "Main Run", "Cooldown 5m")
                }
            )
        }
    }

    fun savePrograms(
        context: Context,
        list: List<WorkoutProgramItem>
    ) {
        val array = JSONArray()

        list.forEach { item ->
            val steps = JSONArray().apply {
                item.steps.forEach(::put)
            }

            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("discipline", item.discipline.name)
                    put("muscleOrZone", item.muscleOrZone)
                    put("targetSets", item.targetSets)
                    put("targetReps", item.targetReps)
                    put("targetWeightKg", item.targetWeightKg)
                    put("restSeconds", item.restSeconds)
                    put("stepDurationSeconds", item.stepDurationSeconds)
                    put("steps", steps)
                }
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROGRAMS, array.toString())
            .apply()
    }

    fun loadActiveId(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACTIVE_ID, "gym-bench") ?: "gym-bench"

    fun saveActiveId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    fun loadPrList(context: Context): List<PersonalRecordEntry> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PR_LIST, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { entry ->
                val p = entry.split("|")
                if (p.size >= 7) {
                    PersonalRecordEntry(
                        id = p[0],
                        exerciseName = p[1],
                        discipline = runCatching { WorkoutDiscipline.valueOf(p[2]) }.getOrDefault(WorkoutDiscipline.GYM),
                        currentValue = p[3].toDoubleOrNull() ?: 60.0,
                        previousBest = p[4].toDoubleOrNull() ?: 55.0,
                        targetGoal = p[5].toDoubleOrNull() ?: 75.0,
                        unit = p[6]
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    fun savePrList(context: Context, list: List<PersonalRecordEntry>) {
        val encoded = list.joinToString(";;") {
            "${it.id}|${it.exerciseName.replace("|", "")}|${it.discipline.name}|${it.currentValue}|${it.previousBest}|${it.targetGoal}|${it.unit}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PR_LIST, encoded).apply()
    }

    fun loadHistory(context: Context): List<WorkoutHistoryRecord> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_HISTORY, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { entry ->
                val p = entry.split("|")
                if (p.size >= 9) {
                    WorkoutHistoryRecord(
                        id = p[0],
                        title = p[1],
                        discipline = runCatching { WorkoutDiscipline.valueOf(p[2]) }.getOrDefault(WorkoutDiscipline.GYM),
                        setsCompleted = p[3].toIntOrNull() ?: 4,
                        totalRepsOrLaps = p[4].toIntOrNull() ?: 40,
                        totalVolumeOrDist = p[5].toDoubleOrNull() ?: 2400.0,
                        durationSeconds = p[6].toIntOrNull() ?: 1800,
                        isNewPr = p[7].toBoolean(),
                        timestampLabel = p[8],
                        timestampEpochMillis = p[0]
                            .removePrefix("wh-")
                            .toLongOrNull()
                            ?: 0L
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    fun saveHistory(context: Context, list: List<WorkoutHistoryRecord>) {
        val limit = getHistoryRetention(context)

        val encoded = list
            .take(limit)
            .joinToString(";;") { record ->
                "${record.id}|${record.title.replace("|", "")}|" +
                        "${record.discipline.name}|${record.setsCompleted}|" +
                        "${record.totalRepsOrLaps}|${record.totalVolumeOrDist}|" +
                        "${record.durationSeconds}|${record.isNewPr}|" +
                        "${record.timestampLabel}|${record.timestampEpochMillis}"
            }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, encoded)
            .apply()
    }

    fun loadTtsMuted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_TTS_MUTED, false)

    fun saveTtsMuted(context: Context, muted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_TTS_MUTED, muted).apply()
    }

    fun loadMusicMuted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_MUSIC_MUTED, false)

    fun saveMusicMuted(context: Context, muted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_MUSIC_MUTED, muted).apply()
    }
}


// =============================================================================
// MAIN ENTRY SCREEN
// =============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearWorkoutScreen(
    state: LifeState,
    repository: LifeRepository,
    exerciseRepository: com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository,
    mediaViewModel: com.creativeblisslabs.wearproductivity.wear.media.SharedMediaViewModel? = null,
    initialRoutineIds: List<String>? = null
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberLifeOSHapticProvider()
    val liveState by repository.state.collectAsState()

    val activeRuns = liveState.runs.filter { it.stamp.deletedAt == null }

    val repDetectionEngine = remember { RepDetectionEngine(context) }
    val hardwareReps by repDetectionEngine.repCount.collectAsState()

    DisposableEffect(repDetectionEngine) {
        onDispose { repDetectionEngine.stopTracking() }
    }
    // Custom Programs, PR Records & History State
    val programs = remember(initialRoutineIds, exerciseRepository.allExercises) {
        mutableStateListOf<WorkoutProgramItem>().apply {
            if (!initialRoutineIds.isNullOrEmpty()) {
                val exerciseMap = exerciseRepository.allExercises.associateBy { it.id }
                val customItems = initialRoutineIds.mapNotNull { id ->
                    exerciseMap[id]?.let { ex ->
                        WorkoutProgramItem(
                            id = ex.id,
                            name = ex.name,
                            discipline = when (ex.category.lowercase()) {
                                "cardio", "running" -> WorkoutDiscipline.RUNNING
                                "swimming" -> WorkoutDiscipline.SWIMMING
                                "calisthenics", "stretching", "plyometrics" -> WorkoutDiscipline.CALISTHENICS
                                else -> WorkoutDiscipline.GYM
                            },
                            muscleOrZone = ex.primaryMuscles.firstOrNull()
                                ?.replace('_', ' ')
                                ?.replaceFirstChar { it.uppercase() } ?: "Full Body",
                            targetSets = 4,
                            targetReps = 10,
                            targetWeightKg = if (ex.effectiveBodyweight == true) 0.0 else 50.0,
                            restSeconds = 60,
                            stepDurationSeconds = 60,
                            steps = ex.instructions.ifEmpty {
                                listOf("Working Set 1", "Working Set 2", "Working Set 3", "Working Set 4")
                            }
                        )
                    }
                }
                addAll(customItems)
            }
            if (isEmpty()) {
                val savedPrograms = WorkoutStudioPrefs.loadPrograms(context, activeRuns)
                val jsonPrograms = exerciseRepository.allWorkouts.map { plan ->
                    plan.toWorkoutProgramItem()
                }
                addAll((savedPrograms + jsonPrograms).distinctBy { it.id })
            }
        }
    }
    var activeTrainingWeek by remember { mutableStateOf<Int?>(null) }
    var activeTrainingDay by remember { mutableStateOf<Int?>(null) }
    var activeTrainingProgramId by remember { mutableStateOf(WorkoutStudioPrefs.loadActiveTrainingProgramId(context)) }
    var completedTrainingDays by remember(activeTrainingProgramId) {
        mutableStateOf(
            WorkoutStudioPrefs.loadCompletedTrainingDays(context)
        )
    }
    var showTrainingProgramsBrowser by remember { mutableStateOf(false) }
// Default to the first exercise in initialRoutineIds when launched from WearWorkoutListScreen
    var selectedProgramId by remember(initialRoutineIds) {
        mutableStateOf(
            initialRoutineIds?.firstOrNull { id -> programs.any { it.id == id } }
                ?: WorkoutStudioPrefs.loadActiveId(context)
        )
    }
    val activeProgram = programs.firstOrNull { it.id == selectedProgramId }
        ?: programs.firstOrNull()
        ?: WorkoutProgramItem(
            id = "gym-bench",
            name = "Barbell Bench Press",
            discipline = WorkoutDiscipline.GYM,
            muscleOrZone = "Chest",
            targetSets = 4,
            targetReps = 10,
            targetWeightKg = 60.0,
            restSeconds = 60,
            stepDurationSeconds = 60,
            steps = listOf("Warmup Set", "Working Set 1", "Working Set 2", "Working Set 3")
        )

    val currentProgramIndex = programs.indexOfFirst { it.id == activeProgram.id }.coerceAtLeast(0)

    val prRecords = remember {
        mutableStateListOf<PersonalRecordEntry>().apply { addAll(WorkoutStudioPrefs.loadPrList(context)) }
    }
    val workoutHistory = remember {
        mutableStateListOf<WorkoutSessionLogRecord>().apply {
            addAll(
                WorkoutStudioPrefs.loadHistory(context).map {
                    WorkoutSessionLogRecord(it)
                }
            )
        }
    }

    // Active Workout & Break Session State
    var hasStarted by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isRestingBreak by remember { mutableStateOf(false) }
    var elapsedWorkoutSeconds by remember { mutableIntStateOf(0) }
    var activeStepElapsedSeconds by remember { mutableIntStateOf(0) }
    var customRestDurationSeconds by remember(activeProgram.id) { mutableIntStateOf(activeProgram.restSeconds) }
    var restRemainingSeconds by remember(activeProgram.id) { mutableIntStateOf(activeProgram.restSeconds) }
    var customStepDurationSeconds by remember(activeProgram.id) { mutableIntStateOf(activeProgram.stepDurationSeconds) }
    var sessionBestLoadKg by remember {
        mutableDoubleStateOf(0.0)
    }
    // Sets, Reps, Load & Step Tracking
    var currentSetNumber by remember(activeProgram.id) { mutableIntStateOf(1) }
    var targetSetsCount by remember(activeProgram.id) { mutableIntStateOf(activeProgram.targetSets) }
    var manualRepCount by remember { mutableIntStateOf(activeProgram.targetReps) }
    var currentWeightOrDist by remember { mutableDoubleStateOf(activeProgram.targetWeightKg) }
    var sessionTotalRepsOrLaps by remember { mutableIntStateOf(0) }
    var sessionTotalVolumeOrDist by remember { mutableDoubleStateOf(0.0) }
    var selectedStepIndex by remember { mutableIntStateOf(0) }
    val currentRepOrLapCount = if (
        hasStarted &&
        !isRestingBreak &&
        activeProgram.discipline == WorkoutDiscipline.GYM &&
        hardwareReps > 0
    ) {
        hardwareReps
    } else {
        manualRepCount
    }


    // Audio Coach (TTS + Acoustic Tone) & Music Mute Controls
    val audioCoach = remember { WorkoutAudioCoachEngine(context, coroutineScope) }
    var isTtsMuted by remember { mutableStateOf(WorkoutStudioPrefs.loadTtsMuted(context)) }
    var isMusicMuted by remember { mutableStateOf(WorkoutStudioPrefs.loadMusicMuted(context)) }

    DisposableEffect(Unit) {
        onDispose { audioCoach.shutdown() }
    }

    // Filter & Editor States
    var selectedDisciplineFilter by remember { mutableStateOf(WorkoutDiscipline.GYM) }
    var showProgramEditor by remember { mutableStateOf(false) }
    var editingProgramId by remember { mutableStateOf<String?>(null) }
    var draftProgramName by remember { mutableStateOf("") }
    var draftSetsRepsRest by remember { mutableStateOf("4x10 · 60s") }
    var draftWeightAndZone by remember { mutableStateOf("60kg · Chest") }

    var showPrEditor by remember { mutableStateOf(false) }
    var editingPrEntry by remember { mutableStateOf<PersonalRecordEntry?>(null) }

    var feedbackOverlay by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var completedWorkoutSummary by remember { mutableStateOf<WorkoutHistoryRecord?>(null) }
    val latestIsRestingBreak by rememberUpdatedState(isRestingBreak)
    val latestRestRemaining by rememberUpdatedState(restRemainingSeconds)
    val latestRestDuration by rememberUpdatedState(customRestDurationSeconds)
    val latestStepDuration by rememberUpdatedState(customStepDurationSeconds)
    val latestSetNumber by rememberUpdatedState(currentSetNumber)
    val latestProgramName by rememberUpdatedState(activeProgram.name)
    val latestTtsMuted by rememberUpdatedState(isTtsMuted)

    LaunchedEffect(isRunning, activeProgram.id) {
        while (isRunning) {
            delay(1_000L)

            elapsedWorkoutSeconds =
                (elapsedWorkoutSeconds + 1).coerceAtMost(86_400)

            if (latestIsRestingBreak) {
                if (latestRestRemaining > 1) {
                    restRemainingSeconds = latestRestRemaining - 1

                    if (restRemainingSeconds == 10) {
                        audioCoach.announce(
                            "10 seconds of rest left.",
                            latestTtsMuted,
                            660.0
                        )
                    }
                } else {
                    isRestingBreak = false
                    restRemainingSeconds = latestRestDuration
                    activeStepElapsedSeconds = 0

                    haptic.click()
                    audioCoach.announce(
                        "Rest complete. Begin Set $latestSetNumber of $latestProgramName.",
                        latestTtsMuted,
                        1046.5
                    )

                    feedbackOverlay = "Set $latestSetNumber Active!" to false
                }
            } else {
                activeStepElapsedSeconds += 1

                if (activeStepElapsedSeconds == latestStepDuration) {
                    audioCoach.announce(
                        "Target step time reached.",
                        latestTtsMuted,
                        880.0
                    )
                }
            }
        }
    }
// Automatically start/stop hardware rep tracking when exercise or play state changes
    LaunchedEffect(activeProgram.id, activeProgram.discipline, isRunning, isRestingBreak) {
        if (
            activeProgram.discipline == WorkoutDiscipline.GYM &&
            isRunning &&
            !isRestingBreak
        ) {
            repDetectionEngine.startTracking()
        } else {
            repDetectionEngine.stopTracking()
        }
    }
    // Select a Workout Program
    fun selectProgram(program: WorkoutProgramItem) {
        if (hasStarted && program.id != activeProgram.id) {
            feedbackOverlay = "Finish Current Workout First" to true
            return
        }

        activeTrainingWeek = null
        activeTrainingDay = null
        haptic.click()
        selectedProgramId = program.id
        selectedDisciplineFilter = program.discipline
        WorkoutStudioPrefs.saveActiveId(context, program.id)
        currentSetNumber = 1
        targetSetsCount = program.targetSets
        manualRepCount = program.targetReps
        repDetectionEngine.reset()
        currentWeightOrDist = program.targetWeightKg
        customRestDurationSeconds = program.restSeconds
        restRemainingSeconds = program.restSeconds
        customStepDurationSeconds = program.stepDurationSeconds
        selectedStepIndex = 0
        activeStepElapsedSeconds = 0
        feedbackOverlay = "${program.name} Selected" to false
    }

    // Update PR Entry if current session beat previous best
    fun checkAndRecordPr(
        program: WorkoutProgramItem,
        metricValue: Double
    ): Boolean {
        if (!metricValue.isFinite() || metricValue <= 0.0) return false

        val idx = prRecords.indexOfFirst {
            it.exerciseName.equals(program.name, ignoreCase = true) ||
                    it.id == "pr-${program.id}"
        }

        val unitLabel = when (program.discipline) {
            WorkoutDiscipline.GYM -> "kg"
            WorkoutDiscipline.RUNNING -> "km"
            WorkoutDiscipline.SWIMMING -> "m"
            WorkoutDiscipline.CALISTHENICS -> "reps"
        }

        if (idx >= 0) {
            val existing = prRecords[idx]

            if (metricValue <= existing.currentValue) return false

            prRecords[idx] = existing.copy(
                previousBest = existing.currentValue,
                currentValue = metricValue
            )
        } else {
            prRecords.add(
                0,
                PersonalRecordEntry(
                    id = "pr-${program.id}",
                    exerciseName = program.name,
                    discipline = program.discipline,
                    currentValue = metricValue,
                    previousBest = 0.0,
                    targetGoal = metricValue,
                    unit = unitLabel
                )
            )
        }

        WorkoutStudioPrefs.savePrList(context, prRecords)
        return true
    }

    fun resetWorkoutSession(
        program: WorkoutProgramItem = activeProgram
    ) {
        currentSetNumber = 1
        targetSetsCount = program.targetSets
        manualRepCount = program.targetReps
        currentWeightOrDist = program.targetWeightKg

        customRestDurationSeconds = program.restSeconds
        restRemainingSeconds = program.restSeconds
        customStepDurationSeconds = program.stepDurationSeconds
        sessionBestLoadKg = 0.0
        elapsedWorkoutSeconds = 0
        activeStepElapsedSeconds = 0

        sessionTotalRepsOrLaps = 0
        sessionTotalVolumeOrDist = 0.0

        selectedStepIndex = 0
        isRestingBreak = false

        repDetectionEngine.reset()
    }
    fun markActiveTrainingDayCompleted() {
        val programId = activeTrainingProgramId ?: return
        val week = activeTrainingWeek ?: return
        val day = activeTrainingDay ?: return

        WorkoutStudioPrefs.markTrainingDayCompleted(
            context,
            programId,
            week,
            day
        )

        completedTrainingDays = completedTrainingDays +
                "${programId}_W${week}_D${day}"
    }
    fun finishWorkout(completedSets: Int) {
        if (completedSets <= 0 || sessionTotalRepsOrLaps <= 0) {
            isRunning = false
            hasStarted = false
            isRestingBreak = false

            resetWorkoutSession(activeProgram)
            activeTrainingWeek = null
            activeTrainingDay = null

            feedbackOverlay = "No Completed Sets to Save" to true
            return
        }

        val finalReps = sessionTotalRepsOrLaps
        val finalVolume = sessionTotalVolumeOrDist

        val prMetric = when (activeProgram.discipline) {
            WorkoutDiscipline.GYM -> sessionBestLoadKg
            WorkoutDiscipline.RUNNING,
            WorkoutDiscipline.SWIMMING -> finalVolume
            WorkoutDiscipline.CALISTHENICS -> finalReps.toDouble()
        }

        val hitPr = checkAndRecordPr(activeProgram, prMetric)
        val completedAt = System.currentTimeMillis()

        val record = WorkoutHistoryRecord(
            id = "wh-$completedAt",
            title = activeProgram.name,
            discipline = activeProgram.discipline,
            setsCompleted = completedSets,
            totalRepsOrLaps = finalReps,
            totalVolumeOrDist = finalVolume,
            durationSeconds = elapsedWorkoutSeconds,
            isNewPr = hitPr,
            timestampLabel = currentWorkoutTimeStamp(),
            timestampEpochMillis = completedAt
        )

        workoutHistory.add(0, WorkoutSessionLogRecord(record))

        WorkoutStudioPrefs.saveHistory(
            context,
            workoutHistory.map { it.record }
        )

        completedWorkoutSummary = record
        markActiveTrainingDayCompleted()

        coroutineScope.launch {
            if (activeProgram.discipline == WorkoutDiscipline.RUNNING) {
                val matchingRun = activeRuns.firstOrNull {
                    !it.completed &&
                            it.title.equals(activeProgram.name, ignoreCase = true)
                }

                if (matchingRun != null) {
                    runCatching {
                        repository.completeRunWorkout(matchingRun.stamp.id)
                    }.onFailure { error ->
                        Log.e("WearWorkout", "Could not complete running plan entry", error)
                    }
                }
            }
        }

        isRunning = false
        hasStarted = false
        isRestingBreak = false

        activeTrainingWeek = null
        activeTrainingDay = null

        resetWorkoutSession(activeProgram)

        audioCoach.announce(
            if (hitPr) {
                "Workout saved. Congratulations on a new personal record!"
            } else {
                "Workout saved to history."
            },
            isTtsMuted,
            1318.5
        )

        feedbackOverlay = (
                if (hitPr) "Saved + NEW PR!" else "Workout Logged!"
                ) to false
    }

    fun completeCurrentSetOrInterval() {
        haptic.click()

        if (!hasStarted) {
            hasStarted = true
            isRunning = true
        }

        val reps = currentRepOrLapCount.coerceAtLeast(0)

        val addedVolumeOrDist = when (activeProgram.discipline) {
            WorkoutDiscipline.GYM -> currentWeightOrDist * reps
            WorkoutDiscipline.RUNNING ->
                (currentWeightOrDist / targetSetsCount.coerceAtLeast(1))
                    .coerceAtLeast(0.0)
            WorkoutDiscipline.SWIMMING -> reps * 25.0
            WorkoutDiscipline.CALISTHENICS -> reps.toDouble()
        }

        sessionTotalRepsOrLaps += reps
        sessionTotalVolumeOrDist += addedVolumeOrDist

        if (activeProgram.discipline == WorkoutDiscipline.GYM) {
            sessionBestLoadKg = maxOf(sessionBestLoadKg, currentWeightOrDist)

            coroutineScope.launch {
                runCatching {
                    repository.updateGymRestSeconds(customRestDurationSeconds)

                    val matchedEx = liveState.gymWorkout.exercises.firstOrNull {
                        it.name.equals(activeProgram.name, ignoreCase = true)
                    }

                    if (matchedEx != null) {
                        repository.logGymSet(
                            matchedEx.stamp.id,
                            "${currentWeightOrDist.toInt()}kg x $reps"
                        )
                    }
                }.onFailure { error ->
                    Log.e("WearWorkout", "Could not log gym set", error)
                    feedbackOverlay = "Set Saved Locally; Sync Failed" to true
                }
            }
        }

        if (currentSetNumber >= targetSetsCount) {
            finishWorkout(currentSetNumber)
            return
        }

        currentSetNumber += 1
        isRestingBreak = true
        restRemainingSeconds = customRestDurationSeconds

        repDetectionEngine.reset()
        manualRepCount = activeProgram.targetReps

        selectedStepIndex = (selectedStepIndex + 1)
            .coerceAtMost((activeProgram.steps.lastIndex).coerceAtLeast(0))

        feedbackOverlay =
            "Set Logged · Total: ${sessionTotalVolumeOrDist.toInt()}" to false

        audioCoach.announce(
            "Set completed. Rest timer started.",
            isTtsMuted,
            880.0
        )
    }

    // Switch between Active Work Set and Rest Break (Next Button)
    fun switchWorkAndRestPhase() {
        haptic.click()
        if (!hasStarted) {
            resetWorkoutSession()

            hasStarted = true
            isRunning = true
            isRestingBreak = false

            audioCoach.announce(
                "Starting ${activeProgram.name}.",
                isTtsMuted,
                880.0
            )

            feedbackOverlay = "Workout Started" to false
            return
        }

        if (isRestingBreak) {
            // Skip rest and start next work set immediately
            isRestingBreak = false
            restRemainingSeconds = customRestDurationSeconds
            activeStepElapsedSeconds = 0
            audioCoach.announce("Rest skipped. Set $currentSetNumber active.", isTtsMuted, 1046.5)
            feedbackOverlay = "Set $currentSetNumber Active" to false
        } else {
            // Log current work set and enter rest break
            completeCurrentSetOrInterval()
        }
    }

    // Previous / Restart Step or Set
    fun previousOrRestartStep() {
        haptic.click()
        if (activeStepElapsedSeconds > 8) {
            activeStepElapsedSeconds = 0
            if (isRestingBreak) restRemainingSeconds = customRestDurationSeconds
            feedbackOverlay = "Restarted Current Step" to false
        } else {
            if (isRestingBreak) {
                isRestingBreak = false
                feedbackOverlay = "Back to Work Set $currentSetNumber" to false
            } else if (currentSetNumber > 1) {
                activeStepElapsedSeconds = 0

                feedbackOverlay =
                    "Previous Set Undo Is Not Supported" to true
            } else {
                activeStepElapsedSeconds = 0
                feedbackOverlay = "Set 1 Reset" to false
            }
        }
    }
    fun stopAndFinishWorkout() {
        haptic.click()

        // currentSetNumber points to the next set after a completed set.
        val completedSets = (currentSetNumber - 1).coerceAtLeast(0)

        finishWorkout(completedSets)
    }

    // Save or Edit Custom Workout Program
    fun saveCustomProgram(nameInput: String, setsRepsRestInput: String, weightZoneInput: String) {
        val cleanName = nameInput.trim().take(36)
        if (cleanName.isBlank()) {
            feedbackOverlay = "Enter Exercise/Plan Name" to true
            haptic.click()
            return
        }
        val setsMatch = Regex(
            """^\s*(\d{1,2})\s*[x×]\s*(\d{1,3})\s*·\s*(\d{1,3})\s*s?\s*$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(setsRepsRestInput)

        if (setsMatch == null) {
            feedbackOverlay = "Use Sets x Reps · Rest Seconds" to true
            haptic.click()
            return
        }

        val parsedSets = setsMatch.groupValues[1].toIntOrNull()
        val parsedReps = setsMatch.groupValues[2].toIntOrNull()
        val parsedRest = setsMatch.groupValues[3].toIntOrNull()

        if (
            parsedSets == null || parsedSets !in 1..30 ||
            parsedReps == null || parsedReps !in 1..200 ||
            parsedRest == null || parsedRest !in 15..300
        ) {
            feedbackOverlay = "Invalid Sets, Reps or Rest Duration" to true
            haptic.click()
            return
        }

        val weightMatch = Regex(
            """^\s*(\d+(?:\.\d+)?)\s*(?:kg|km|m)?(?:\s*·\s*(.*))?$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(weightZoneInput)

        if (weightMatch == null) {
            feedbackOverlay = "Enter a Valid Load or Distance" to true
            haptic.click()
            return
        }

        val weightValue = weightMatch.groupValues[1].toDoubleOrNull()

        if (
            weightValue == null ||
            !weightValue.isFinite() ||
            weightValue !in 0.0..500.0
        ) {
            feedbackOverlay = "Load or Distance Must Be 0–500" to true
            haptic.click()
            return
        }

        val weightMatchValue = weightValue

        val zoneLabel = weightMatch.groupValues[2]
            .trim()
            .take(40)
            .ifBlank { selectedDisciplineFilter.label }

        val generatedSteps = (1..parsedSets).map { setIdx ->
            when (selectedDisciplineFilter) {
                WorkoutDiscipline.GYM ->
                    "Set $setIdx · ${weightMatchValue.toInt()}kg x $parsedReps reps"

                WorkoutDiscipline.RUNNING ->
                    "Interval $setIdx · ${
                        "%.1f".format(
                            Locale.ROOT,
                            weightMatchValue / parsedSets
                        )
                    } km"

                WorkoutDiscipline.SWIMMING ->
                    "Lap Block $setIdx · ${parsedReps * 25}m swim"

                WorkoutDiscipline.CALISTHENICS ->
                    "Round $setIdx · $parsedReps reps ($zoneLabel)"
            }
        }

        val editId = editingProgramId
        if (editId != null) {
            val idx = programs.indexOfFirst { it.id == editId }
            if (idx >= 0) {
                val updated = programs[idx].copy(
                    name = cleanName,
                    discipline = selectedDisciplineFilter,
                    muscleOrZone = zoneLabel,
                    targetSets = parsedSets,
                    targetReps = parsedReps,
                    targetWeightKg = weightMatchValue,
                    restSeconds = parsedRest,
                    steps = generatedSteps
                )
                programs[idx] = updated
                selectProgram(updated)
            }
            coroutineScope.launch {
                repository.editGymExercise(editId, cleanName, zoneLabel, selectedDisciplineFilter.label)
            }
            feedbackOverlay = "Updated $cleanName" to false
        } else {
            val newProg = WorkoutProgramItem(
                id = "prog-${System.currentTimeMillis()}",
                name = cleanName,
                discipline = selectedDisciplineFilter,
                muscleOrZone = zoneLabel,
                targetSets = parsedSets,
                targetReps = parsedReps,
                targetWeightKg = weightMatchValue,
                restSeconds = parsedRest,
                stepDurationSeconds = 90,
                steps = generatedSteps
            )
            programs.add(0, newProg)
            selectProgram(newProg)
            coroutineScope.launch {
                if (selectedDisciplineFilter == WorkoutDiscipline.RUNNING) {
                    repository.addRunWorkout(1, 1, cleanName, generatedSteps)
                } else {
                    repository.addGymExercise(cleanName, zoneLabel, selectedDisciplineFilter.label, "${weightMatch.toInt()}kg x $parsedReps")
                }
            }
            feedbackOverlay = "Created $cleanName" to false
        }

        WorkoutStudioPrefs.savePrograms(context, programs)
        showProgramEditor = false
        editingProgramId = null
        haptic.click()
    }

    fun deleteCustomProgram(id: String) {
        if (hasStarted && id == activeProgram.id) {
            feedbackOverlay = "Finish Current Workout Before Deleting" to true
            return
        }

        if (programs.size <= 1) {
            feedbackOverlay = "Keep at least 1 program" to true
            return
        }
        programs.removeAll { it.id == id }
        WorkoutStudioPrefs.savePrograms(context, programs)
        coroutineScope.launch {
            repository.deleteGymExercise(id)
            repository.deleteRunWorkout(id)
        }
        if (selectedProgramId == id) {
            programs.firstOrNull()?.let { selectProgram(it) }
        }
        showProgramEditor = false
        editingProgramId = null
        feedbackOverlay = "Program Deleted" to false
        haptic.click()
    }

    val displayedHeartRate =
        liveState.wellness.currentHeartRateBpm
            .takeIf { it.isFinite() && it > 0f }
            ?.toInt()
            ?: 0

    // Boundary Ring Progress Calculation
    val primaryRingProgress = when {
        isRestingBreak -> (restRemainingSeconds.toFloat() / customRestDurationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
        hasStarted -> ((currentSetNumber - 1).toFloat() / targetSetsCount.coerceAtLeast(1) +
                (activeStepElapsedSeconds.toFloat() / (customStepDurationSeconds.coerceAtLeast(1) * targetSetsCount.coerceAtLeast(1)))).coerceIn(0.06f, 1f)
        else -> (currentSetNumber.toFloat() / targetSetsCount.coerceAtLeast(1)).coerceIn(0.15f, 1f)
    }

    val currentPrEntry = prRecords.firstOrNull {
        it.exerciseName.equals(activeProgram.name, ignoreCase = true)
    } ?: prRecords.firstOrNull { it.discipline == activeProgram.discipline }

    // 4-Page Vertical Pager
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )


    WearBaselineViewport {
        // 1. Retrieve the spec from CompositionLocal inside the block
        val coreSpec = LocalWearAdaptiveSpec.current
        val adaptiveSpec = WearAdaptiveSpec(
            screenWidth = coreSpec.screenWidth,
            screenHeight = coreSpec.screenHeight,
            isLargeScreen = coreSpec.isLargeScreen,
            isSmallScreen = coreSpec.isSmallScreen,
            outerHorizontalMargin = coreSpec.outerHorizontalMargin,
            topBottomSlotHorizontalMargin = coreSpec.topBottomSlotHorizontalMargin,
            topSlotPadding = coreSpec.topSlotPadding,
            bottomEdgeButtonPadding = coreSpec.bottomEdgeButtonPadding
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior = pagerFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    // =========================================================================
                    // PAGE 0: FULL-SCREEN BOUNDARY CIRCULAR PROGRESS & TRANSPORT CANVAS
                    // =========================================================================
                    0 -> ActiveWorkoutBoundaryCanvasPage(
                        spec = adaptiveSpec,
                        activeProgram = activeProgram,
                        currentExerciseIndex = currentProgramIndex,
                        totalExercisesCount = programs.size,
                        hasStarted = hasStarted,
                        isRunning = isRunning,
                        isRestingBreak = isRestingBreak,
                        elapsedWorkoutSeconds = elapsedWorkoutSeconds,
                        restRemainingSeconds = restRemainingSeconds,
                        currentSetNumber = currentSetNumber,
                        targetSetsCount = targetSetsCount,
                        currentRepOrLapCount = currentRepOrLapCount,
                        currentWeightOrDist = currentWeightOrDist,
                        displayedHeartRate = displayedHeartRate,
                        primaryProgress = primaryRingProgress,
                        currentPrEntry = currentPrEntry,
                        isTtsMuted = isTtsMuted,
                        exerciseRepository = exerciseRepository,
                        onCycleDisciplineOrProgram = {
                            val nextIdx = (currentProgramIndex + 1) % programs.size
                            selectProgram(programs[nextIdx])
                        },
                        onPreviousExercise = {
                            if (currentProgramIndex > 0) {
                                selectProgram(programs[currentProgramIndex - 1])
                            }
                        },
                        onNextExercise = {
                            if (currentProgramIndex < programs.lastIndex) {
                                selectProgram(programs[currentProgramIndex + 1])
                            } else {
                                stopAndFinishWorkout()
                            }
                        },
                        onResetReps = {
                            haptic.click()
                            repDetectionEngine.reset()
                            manualRepCount = 0
                            feedbackOverlay = "Reps Reset" to false
                        },
                        onTogglePlayPause = {
                            haptic.click()
                            if (!hasStarted) {
                                resetWorkoutSession()
                                hasStarted = true
                                isRunning = true

                                audioCoach.announce(
                                    "Starting ${activeProgram.name}. Set 1, $manualRepCount reps.",
                                    isTtsMuted,
                                    880.0
                                )

                                feedbackOverlay = "Started ${activeProgram.name}" to false
                            } else {
                                isRunning = !isRunning
                                audioCoach.announce(
                                    if (isRunning) "Workout resumed." else "Workout paused.",
                                    isTtsMuted,
                                    660.0
                                )
                                feedbackOverlay = (if (isRunning) "Resumed" else "Paused") to false
                            }
                        },
                        onPreviousOrRestart = ::previousOrRestartStep,
                        onNextWorkOrRestSwitch = ::switchWorkAndRestPhase,
                        onIncrementRepOrRest = {
                            haptic.click()
                            if (isRestingBreak) {
                                restRemainingSeconds = (restRemainingSeconds + 15).coerceAtMost(600)
                                feedbackOverlay = "+15s Rest Added" to false
                            } else {
                                manualRepCount += 1
                                feedbackOverlay = "$currentRepOrLapCount ${if (activeProgram.discipline == WorkoutDiscipline.SWIMMING) "Laps" else "Reps"}" to false
                            }
                        },
                        onToggleTtsMute = {
                            haptic.click()
                            isTtsMuted = !isTtsMuted
                            WorkoutStudioPrefs.saveTtsMuted(context, isTtsMuted)
                            if (!isTtsMuted) {
                                audioCoach.announce("Voice coach enabled.", false, 880.0)
                            }
                            feedbackOverlay = (if (isTtsMuted) "TTS Coach Muted" else "TTS Coach Active") to false
                        },
                        onStopAndSave = ::stopAndFinishWorkout,
                        onOpenControlsPage = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )

                    // =========================================================================
                    // PAGE 1: LIVE SESSION CONTROLLER, SETS/REPS/LOAD & INTERVAL STEPS
                    // =========================================================================
                    1 -> WorkoutStepsAndLiveControlsPage(
                        spec = adaptiveSpec,
                        activeProgram = activeProgram,
                        hasStarted = hasStarted,
                        isRunning = isRunning,
                        isRestingBreak = isRestingBreak,
                        elapsedWorkoutSeconds = elapsedWorkoutSeconds,
                        restRemainingSeconds = restRemainingSeconds,
                        customRestDurationSeconds = customRestDurationSeconds,
                        currentSetNumber = currentSetNumber,
                        targetSetsCount = targetSetsCount,
                        currentRepOrLapCount = currentRepOrLapCount,
                        currentWeightOrDist = currentWeightOrDist,
                        selectedStepIndex = selectedStepIndex,
                        isTtsMuted = isTtsMuted,
                        isMusicMuted = isMusicMuted,
                        onTogglePlayPause = {
                            haptic.click()
                            if (!hasStarted) {
                                resetWorkoutSession()
                                hasStarted = true
                                isRunning = true

                                audioCoach.announce(
                                    "Workout started.",
                                    isTtsMuted,
                                    880.0
                                )
                            } else {
                                isRunning = !isRunning
                            }
                        },
                        onPreviousOrRestart = ::previousOrRestartStep,
                        onNextWorkOrRestSwitch = ::switchWorkAndRestPhase,
                        onStopAndSave = ::stopAndFinishWorkout,
                        onAdjustReps = { delta ->
                            haptic.click()
                            manualRepCount = (manualRepCount + delta).coerceIn(1, 300)
                        },
                        onAdjustWeightOrDist = { delta ->
                            haptic.click()
                            currentWeightOrDist = (currentWeightOrDist + delta).coerceIn(0.0, 500.0)
                        },
                        onAdjustTargetSets = { delta ->
                            haptic.click()
                            targetSetsCount = (targetSetsCount + delta).coerceIn(1, 30)
                        },
                        onAdjustRestSeconds = { delta ->
                            haptic.click()
                            customRestDurationSeconds = (customRestDurationSeconds + delta).coerceIn(15, 300)
                            restRemainingSeconds = customRestDurationSeconds
                        },
                        onSelectStep = { idx ->
                            haptic.click()
                            selectedStepIndex = idx
                            currentSetNumber = (idx + 1).coerceAtMost(targetSetsCount)
                            audioCoach.announce(activeProgram.steps.getOrNull(idx) ?: "Step ${idx + 1}", isTtsMuted, 880.0)
                            feedbackOverlay = "Step ${idx + 1} Active" to false
                        },
                        onToggleTtsMute = {
                            haptic.click()
                            isTtsMuted = !isTtsMuted
                            WorkoutStudioPrefs.saveTtsMuted(context, isTtsMuted)
                            feedbackOverlay = (if (isTtsMuted) "TTS Muted" else "TTS Active") to false
                        },
                        onToggleMusicMute = {
                            haptic.click()

                            isMusicMuted = !isMusicMuted
                            WorkoutStudioPrefs.saveMusicMuted(context, isMusicMuted)

                            feedbackOverlay = (
                                    if (isMusicMuted) {
                                        "Music Mute Preference Saved"
                                    } else {
                                        "Music Enabled Preference Saved"
                                    }
                                    ) to false
                        },
                        onJumpToCanvas = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )

                    // =========================================================================
                    // PAGE 2: CUSTOM WORKOUTS, MULTI-DISCIPLINE PROGRAMS & EDITOR
                    // =========================================================================
                    2 -> WorkoutProgramsAndEditorPage(
                        spec = adaptiveSpec,
                        trainingPrograms = exerciseRepository.allTrainingPrograms,
                        activeTrainingProgramId = activeTrainingProgramId,
                        completedTrainingDays = completedTrainingDays,
                        showTrainingProgramsBrowser = showTrainingProgramsBrowser,
                        onJoinTrainingProgram = {
                            activeTrainingProgramId = it
                            WorkoutStudioPrefs.saveActiveTrainingProgramId(context, it)
                            showTrainingProgramsBrowser = false
                            haptic.click()
                        },
                        onQuitTrainingProgram = {
                            activeTrainingProgramId = null
                            WorkoutStudioPrefs.saveActiveTrainingProgramId(context, null)
                            haptic.click()
                        },
                        onToggleTrainingBrowser = {
                            showTrainingProgramsBrowser = !showTrainingProgramsBrowser
                            haptic.click()
                        },
                        programs = programs,
                        activeProgram = activeProgram,
                        selectedDiscipline = selectedDisciplineFilter,
                        showProgramEditor = showProgramEditor,
                        isEditingProgram = editingProgramId != null,
                        draftProgramName = draftProgramName,
                        draftSetsRepsRest = draftSetsRepsRest,
                        draftWeightAndZone = draftWeightAndZone,
                        onSelectDiscipline = {
                            haptic.click()
                            selectedDisciplineFilter = it
                        },
                        onSelectProgram = ::selectProgram,
                        onStartProgramNow = { prog, week, day ->
                            if (hasStarted) {
                                feedbackOverlay = "Finish Current Workout First" to true
                            } else {
                                selectProgram(prog)

                                resetWorkoutSession(prog)

                                activeTrainingWeek = week
                                activeTrainingDay = day

                                hasStarted = true
                                isRunning = true
                                isRestingBreak = false

                                audioCoach.announce(
                                    "Starting ${prog.name}.",
                                    isTtsMuted,
                                    880.0
                                )

                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        },
                        onOpenNewProgramEditor = {
                            haptic.click()
                            editingProgramId = null
                            draftProgramName = ""
                            draftSetsRepsRest = "4x10 · 60s"
                            draftWeightAndZone = when (selectedDisciplineFilter) {
                                WorkoutDiscipline.GYM -> "60kg · Chest"
                                WorkoutDiscipline.RUNNING -> "5km · Zone 3"
                                WorkoutDiscipline.SWIMMING -> "100m · Freestyle"
                                WorkoutDiscipline.CALISTHENICS -> "0kg · Bodyweight"
                            }
                            showProgramEditor = true
                        },
                        onOpenEditProgramEditor = { prog ->
                            haptic.click()
                            selectedDisciplineFilter = prog.discipline
                            editingProgramId = prog.id
                            draftProgramName = prog.name
                            draftSetsRepsRest = "${prog.targetSets}x${prog.targetReps} · ${prog.restSeconds}s"
                            draftWeightAndZone = "${prog.targetWeightKg.toInt()} · ${prog.muscleOrZone}"
                            showProgramEditor = true
                        },
                        onSaveProgram = ::saveCustomProgram,
                        onCancelProgramEditor = {
                            haptic.click()
                            showProgramEditor = false
                            editingProgramId = null
                        },
                        onDeleteProgram = ::deleteCustomProgram,
                        onLoadPresetPlan = { level, label ->
                            haptic.click()

                            coroutineScope.launch {
                                feedbackOverlay = "Loading $label..." to false

                                try {
                                    repository.loadRunningPreset(level)

                                    val updatedRuns = repository.current().runs
                                        .filter { it.stamp.deletedAt == null }

                                    updatedRuns.forEach { run ->
                                        if (programs.none {
                                                it.name.equals(run.title, ignoreCase = true)
                                            }
                                        ) {
                                            programs.add(
                                                WorkoutProgramItem(
                                                    id = run.stamp.id,
                                                    name = run.title,
                                                    discipline = WorkoutDiscipline.RUNNING,
                                                    muscleOrZone = "W${run.week} · D${run.day}",
                                                    targetSets = run.intervals.size.coerceAtLeast(3),
                                                    targetReps = 1,
                                                    targetWeightKg = run.distanceKm ?: 5.0,
                                                    restSeconds = 60,
                                                    stepDurationSeconds = 180,
                                                    steps = run.intervals
                                                )
                                            )
                                        }
                                    }

                                    WorkoutStudioPrefs.savePrograms(context, programs)
                                    feedbackOverlay = "$label Loaded" to false

                                } catch (error: Exception) {
                                    Log.e("WearWorkout", "Running preset loading failed", error)

                                    feedbackOverlay = (
                                            error.message
                                                ?.take(55)
                                                ?.let { "Preset Failed: $it" }
                                                ?: "Could Not Load Running Preset"
                                            ) to true
                                }
                            }
                        },
                        onOpenAnalyticsPage = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )

                    // =========================================================================
                    // PAGE 3: PR TARGETS (CURRENT VS PREVIOUS BEST), HEATMAP & HISTORY
                    // =========================================================================
                    else -> WorkoutAnalyticsAndPrPage(
                        spec = adaptiveSpec,
                        state = liveState,
                        prRecords = prRecords,
                        history = workoutHistory.map { it.record },
                        showPrEditor = showPrEditor,
                        editingPrEntry = editingPrEntry,
                        onOpenEditPr = { entry ->
                            haptic.click()
                            editingPrEntry = entry
                            showPrEditor = true
                        },
                        onClosePrEditor = {
                            haptic.click()
                            showPrEditor = false
                            editingPrEntry = null
                        },
                        onSavePrEntry = { name, currentAndPrevStr, targetAndUnitStr ->
                            val cleanName = name.trim()
                            val currentPrevMatch = Regex(
                                """^\s*(\d+(?:\.\d+)?)\s*/\s*(\d+(?:\.\d+)?)\s*$"""
                            ).matchEntire(currentAndPrevStr)

                            val targetMatch = Regex(
                                """^\s*(\d+(?:\.\d+)?)\s*([a-zA-Z]+)\s*$"""
                            ).matchEntire(targetAndUnitStr)

                            val current = currentPrevMatch
                                ?.groupValues?.get(1)?.toDoubleOrNull()

                            val previous = currentPrevMatch
                                ?.groupValues?.get(2)?.toDoubleOrNull()

                            val target = targetMatch
                                ?.groupValues?.get(1)?.toDoubleOrNull()

                            val unit = targetMatch?.groupValues?.get(2)?.lowercase()

                            val valid =
                                cleanName.isNotBlank() &&
                                        current != null && current.isFinite() && current > 0.0 &&
                                        previous != null && previous.isFinite() && previous >= 0.0 &&
                                        target != null && target.isFinite() && target > 0.0 &&
                                        current >= previous &&
                                        !unit.isNullOrBlank()

                            if (!valid) {
                                feedbackOverlay = "Invalid PR Values. Use 70/65 and 85 kg." to true
                            } else {
                                val targetEdit = editingPrEntry

                                if (targetEdit != null) {
                                    val idx = prRecords.indexOfFirst { it.id == targetEdit.id }

                                    if (idx >= 0) {
                                        prRecords[idx] = prRecords[idx].copy(
                                            exerciseName = cleanName,
                                            currentValue = current,
                                            previousBest = previous,
                                            targetGoal = target,
                                            unit = unit
                                        )
                                    }
                                } else {
                                    prRecords.add(
                                        0,
                                        PersonalRecordEntry(
                                            id = "pr-${System.currentTimeMillis()}",
                                            exerciseName = cleanName,
                                            discipline = activeProgram.discipline,
                                            currentValue = current,
                                            previousBest = previous,
                                            targetGoal = target,
                                            unit = unit
                                        )
                                    )
                                }

                                WorkoutStudioPrefs.savePrList(context, prRecords)
                                showPrEditor = false
                                editingPrEntry = null
                                feedbackOverlay = "PR Target Saved" to false
                            }

                            haptic.click()
                        },
                        onRepeatHistorySession = { rec ->
                            val matched = programs.firstOrNull {
                                it.name.equals(rec.title, ignoreCase = true)
                            }

                            if (matched == null) {
                                feedbackOverlay = "Workout Program Not Found" to true
                            } else {
                                selectProgram(matched)

                                currentSetNumber = 1
                                targetSetsCount = matched.targetSets
                                manualRepCount = matched.targetReps
                                currentWeightOrDist = matched.targetWeightKg

                                customRestDurationSeconds = matched.restSeconds
                                restRemainingSeconds = matched.restSeconds
                                customStepDurationSeconds = matched.stepDurationSeconds

                                sessionTotalRepsOrLaps = 0
                                sessionTotalVolumeOrDist = 0.0
                                elapsedWorkoutSeconds = 0
                                activeStepElapsedSeconds = 0
                                selectedStepIndex = 0

                                isRestingBreak = false
                                hasStarted = true
                                isRunning = true

                                repDetectionEngine.reset()

                                audioCoach.announce(
                                    "Repeating ${matched.name}.",
                                    isTtsMuted,
                                    880.0
                                )

                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        },
                        onDeleteHistorySession = { id ->
                            haptic.click()
                            workoutHistory.removeAll { it.record.id == id }
                            WorkoutStudioPrefs.saveHistory(context, workoutHistory.map { it.record })
                            feedbackOverlay = "Log Deleted" to false
                        },
                        onJumpToTop = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }

            // Right Bezel 4-Page Vertical Pager Indicator
            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 4,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            // In-Viewport Constrained M3 Curved Confirmation Overlay
            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedWorkoutConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }

            // Post-Workout Summary Celebration Card (Hevy / Strong Style)
            completedWorkoutSummary?.let { record ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.95f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${record.title} 🦾",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        if (record.isNewPr) {
                            Card(
                                onClick = {},
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD177))
                            ) {
                                Text(
                                    text = "🏆 NEW PR!",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(formatDurationClock(record.durationSeconds), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Volume", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${record.totalVolumeOrDist.toInt()}kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sets", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${record.setsCompleted}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { completedWorkoutSummary = null },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("DONE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

private data class WorkoutSessionLogRecord(val record: WorkoutHistoryRecord)

// =============================================================================
// PAGE 0: FULL-SCREEN BOUNDARY PROGRESS CONTAINER & ACTIVE TRANSPORT CANVAS
// =============================================================================
@Composable
private fun ActiveWorkoutBoundaryCanvasPage(
    spec: WearAdaptiveSpec,
    activeProgram: WorkoutProgramItem,
    currentExerciseIndex: Int,
    totalExercisesCount: Int,
    hasStarted: Boolean,
    isRunning: Boolean,
    isRestingBreak: Boolean,
    elapsedWorkoutSeconds: Int,
    restRemainingSeconds: Int,
    currentSetNumber: Int,
    targetSetsCount: Int,
    currentRepOrLapCount: Int,
    currentWeightOrDist: Double,
    displayedHeartRate: Int,
    primaryProgress: Float,
    currentPrEntry: PersonalRecordEntry?,
    isTtsMuted: Boolean,
    exerciseRepository: ExerciseRepository,
    onCycleDisciplineOrProgram: () -> Unit,
    onPreviousExercise: () -> Unit,
    onNextExercise: () -> Unit,
    onResetReps: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextWorkOrRestSwitch: () -> Unit,
    onIncrementRepOrRest: () -> Unit,
    onToggleTtsMute: () -> Unit,
    onStopAndSave: () -> Unit,
    onOpenControlsPage: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var overlayMode by remember(activeProgram.id) { mutableStateOf(GuidedOverlayMode.NONE) }

    val exerciseDef = remember(activeProgram.id, activeProgram.name, exerciseRepository.allExercises) {
        exerciseRepository.allExercises.find {
            it.id.equals(activeProgram.id, ignoreCase = true) ||
                    it.name.equals(activeProgram.name, ignoreCase = true)
        }
    }

    val imageFrames = remember(exerciseDef, exerciseRepository) {
        if (exerciseDef != null) {
            val animFrames = exerciseRepository.getExerciseAnimationFrames(exerciseDef)
            if (animFrames != null) {
                listOf(animFrames.first, animFrames.second)
                    .distinct()
                    .filter(String::isNotBlank)
            } else {
                listOfNotNull(exerciseRepository.getExerciseImagePath(exerciseDef))
                    .filter(String::isNotBlank)
            }
        } else {
            emptyList()
        }
    }

    var currentImageIndex by remember(activeProgram.id) { mutableIntStateOf(0) }
    LaunchedEffect(activeProgram.id, imageFrames) {
        currentImageIndex = 0
        while (imageFrames.size > 1) {
            delay(1_100L)
            currentImageIndex = (currentImageIndex + 1) % imageFrames.size
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = primaryProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "workoutBoundaryProgress"
    )

    val primaryAccent = when {
        isRestingBreak -> Color(0xFF68B6FF)
        hasStarted && !isRunning -> Color(0xFFFFD177)
        else -> activeProgram.discipline.accent
    }

    val bottomPillHeight = (spec.screenHeight * 0.13f).coerceIn(28.dp, 34.dp)
    val isFirstExercise = currentExerciseIndex <= 0
    val isLastExercise = currentExerciseIndex >= totalExercisesCount - 1

    ScreenScaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment =
                Alignment.Center
        ) {
            // 1. FULL-SCREEN EXERCISE IMAGE LAYER + GRADIENT SCRIMS (From WearGuidedWorkoutScreen)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isRunning) 0.28f else 0.15f),
                                scheme.surfaceContainerLow.copy(alpha = 0.94f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val currentImage = imageFrames.getOrNull(currentImageIndex)
                if (currentImage != null) {
                    AsyncImage(
                        model = remember(currentImage, context) {
                            ImageRequest.Builder(context)
                                .data("file:///android_asset/$currentImage")
                                .crossfade(true)
                                .listener(
                                    onError = { _, result ->
                                        Log.e(
                                            "ExerciseImages",
                                            "Failed to load exercise image: $currentImage",
                                            result.throwable
                                        )
                                    }
                                )
                                .build()
                        },
                        contentDescription = "${activeProgram.name} demonstration",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    // Top & Bottom readability gradient scrims over the demonstration image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.86f),
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Black.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                } else {
                    Icon(
                        imageVector = if (isRestingBreak) Icons.Rounded.Timer else activeProgram.discipline.icon,
                        contentDescription = null,
                        modifier = Modifier.size(spec.screenWidth * 0.54f),
                        tint = primaryAccent.copy(alpha = 0.12f)
                    )
                }
            }

            // 2. FULL-SCREEN BOUNDARY CIRCULAR PROGRESS CONTAINER (OUTER + INNER SET RING)
            val trackColor = scheme.surfaceContainerHigh.copy(alpha = 0.65f)
            val setRatio = (currentSetNumber.toFloat() / targetSetsCount.coerceAtLeast(1)).coerceIn(0f, 1f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val strokePx = 5.5.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                val inset = 9.dp.toPx()
                drawArc(
                    color = primaryAccent.copy(alpha = 0.38f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    startAngle = -90f,
                    sweepAngle = 360f * setRatio,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. LEFT EDGE REP COUNTER BADGE + RESET TAP (From WearGuidedWorkoutScreen)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.primaryContainer.copy(alpha = 0.92f))
                    .clickable(onClick = onResetReps)
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = scheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentRepOrLapCount.coerceAtLeast(0).toString(),
                    style = MaterialTheme.typography.labelMedium.tabularNums(),
                    color = scheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black
                )
            }

            // 4. RIGHT EDGE ANIMATION FRAME DOTS INDICATOR (From WearGuidedWorkoutScreen)
            if (imageFrames.size > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageFrames.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentImageIndex) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentImageIndex) primaryAccent
                                    else Color.White.copy(alpha = 0.65f)
                                )
                        )
                    }
                }
            }

            // 5. FOREGROUND EXPRESSIVE WORKOUT CONTROLS, GUIDED BUTTONS & TELEMETRY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = spec.screenHeight * 0.11f,
                        bottom = spec.screenHeight * 0.07f,
                        start = spec.screenWidth * 0.08f,
                        end = spec.screenWidth * 0.08f
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Slot: Exercise Counter + Program Selector Pill + Primary Readout + Muscles/Telemetry
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "EXERCISE ${currentExerciseIndex + 1} OF ${totalExercisesCount.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 9.sp),
                        color = primaryAccent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHigh.copy(alpha = 0.88f))
                            .clickable(onClick = onCycleDisciplineOrProgram)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = activeProgram.discipline.icon,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = primaryAccent
                        )
                        Text(
                            text = activeProgram.name,
                            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val mainReadout = when {
                        isRestingBreak -> "REST ${formatDurationClock(restRemainingSeconds)}"
                        hasStarted -> formatDurationClock(elapsedWorkoutSeconds)
                        activeProgram.discipline == WorkoutDiscipline.GYM -> "${currentWeightOrDist.toInt()}kg × $currentRepOrLapCount"
                        activeProgram.discipline == WorkoutDiscipline.RUNNING -> "${"%.1f".format(Locale.ROOT, currentWeightOrDist)} km"
                        activeProgram.discipline == WorkoutDiscipline.SWIMMING -> "${currentRepOrLapCount * 25}m Pool"
                        else -> "$currentRepOrLapCount Reps"
                    }

                    Text(
                        text = mainReadout,
                        style = MaterialTheme.typography.displaySmall.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.clickable(onClick = onOpenControlsPage)
                    )

                    val musclesLabel = exerciseDef?.primaryMuscles
                        ?.take(2)
                        ?.joinToString { it.replace('_', ' ') }
                        ?.replaceFirstChar { it.uppercase() }
                        ?.takeIf { it.isNotBlank() }
                        ?: activeProgram.muscleOrZone

                    Text(
                        text = if (isRestingBreak) {
                            "UP NEXT: SET $currentSetNumber/$targetSetsCount · $musclesLabel"
                        } else {
                            "SET $currentSetNumber/$targetSetsCount · $musclesLabel · " +
                                    if (displayedHeartRate > 0) {
                                        "$displayedHeartRate BPM"
                                    } else {
                                        "HR unavailable"
                                    }
                        },
                        style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Transport: Universal PlayerControlBar
                com.creativeblisslabs.wearproductivity.wear.ui.components.core.PlayerControlBar(
                    isPlaying = isRunning,
                    progress = animatedProgress,
                    onTogglePlay = onTogglePlayPause,
                    onPrevious = onPreviousOrRestart,
                    onNext = onNextWorkOrRestSwitch
                )

                // Bottom Guided & Utility Action Rows (Info, Tips, Replay/Reset, +1R, Prev/Next Exercise, TTS, Stop)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: Guided Action Buttons (Info, Tips, Reset Reps, +1R/+15s, TTS Mute, Stop/Settings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // HOW TO / Instructions Button
                        Button(
                            onClick = { overlayMode = GuidedOverlayMode.INSTRUCTIONS },
                            modifier = Modifier
                                .size(bottomPillHeight)
                                .semantics { contentDescription = "Instructions" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.surfaceContainerHigh,
                                contentColor = scheme.onSurface
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Instructions", modifier = Modifier.size(15.dp))
                        }

                        // FORM TIPS Button
                        Button(
                            onClick = { overlayMode = GuidedOverlayMode.TIPS },
                            modifier = Modifier
                                .size(bottomPillHeight)
                                .semantics { contentDescription = "Form tips" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.surfaceContainerHigh,
                                contentColor = scheme.onSurface
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Form tips", modifier = Modifier.size(15.dp))
                        }

                        // Reset Reps Button
                        Button(
                            onClick = onResetReps,
                            modifier = Modifier
                                .size(bottomPillHeight)
                                .semantics { contentDescription = "Reset repetitions" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = "Reset repetitions", modifier = Modifier.size(15.dp))
                        }

                        // +1 Rep or +15s Rest Button
                        Button(
                            onClick = onIncrementRepOrRest,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = if (isRestingBreak) "Add 15 Seconds Rest" else "Add 1 Rep" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.tertiaryContainer,
                                contentColor = scheme.onTertiaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isRestingBreak) "+15s" else "+1R",
                                style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // TTS Mute Button
                        Button(
                            onClick = onToggleTtsMute,
                            modifier = Modifier
                                .size(bottomPillHeight)
                                .semantics { contentDescription = if (isTtsMuted) "Enable Voice Coach" else "Mute Voice Coach" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isTtsMuted) scheme.secondaryContainer else scheme.errorContainer,
                                contentColor = if (!isTtsMuted) scheme.onSecondaryContainer else scheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (!isTtsMuted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Row 2: Prev Exercise | Next / Finish Routine | Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.78f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPreviousExercise,
                            enabled = !isFirstExercise,
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer,
                                disabledContainerColor = scheme.surfaceContainerLow,
                                disabledContentColor = scheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous exercise", modifier = Modifier.size(14.dp))
                        }

                        Button(
                            onClick = if (isLastExercise && hasStarted) onStopAndSave else onNextExercise,
                            modifier = Modifier
                                .weight(1.6f)
                                .height(26.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastExercise) scheme.errorContainer else scheme.primary,
                                contentColor = if (isLastExercise) scheme.onErrorContainer else scheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isLastExercise) Icons.Default.Check else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (isLastExercise) "FINISH" else "NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 6. FULL-SCREEN OPAQUE INSTRUCTIONS & FORM TIPS OVERLAY (From WearGuidedWorkoutScreen)
            AnimatedVisibility(
                visible = overlayMode != GuidedOverlayMode.NONE,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.background.copy(alpha = 0.98f))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (overlayMode == GuidedOverlayMode.INSTRUCTIONS) {
                                Icons.Default.Info
                            } else {
                                Icons.Default.Lightbulb
                            },
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text = if (overlayMode == GuidedOverlayMode.INSTRUCTIONS) "HOW TO" else "FORM TIPS",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        val lines = if (overlayMode == GuidedOverlayMode.INSTRUCTIONS) {
                            exerciseDef?.instructions?.ifEmpty { activeProgram.steps } ?: activeProgram.steps
                        } else {
                            exerciseDef?.tips.orEmpty()
                        }

                        if (lines.isEmpty()) {
                            Text(
                                text = "No details available for this exercise.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            lines.take(4).forEachIndexed { index, line ->
                                Text(
                                    text = "${index + 1}. $line",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = scheme.onBackground,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!exerciseDef?.description.isNullOrBlank()) {
                            Text(
                                text = exerciseDef?.description.orEmpty(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        Button(
                            onClick = { overlayMode = GuidedOverlayMode.NONE },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            )
                        ) {
                            Text("CLOSE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
// =============================================================================
// PAGE 1: LIVE SESSION CONTROLLER, SETS/REPS/LOAD STEPPERS & INTERVAL STEPS
// =============================================================================
@Composable
private fun WorkoutStepsAndLiveControlsPage(
    spec: WearAdaptiveSpec,
    activeProgram: WorkoutProgramItem,
    hasStarted: Boolean,
    isRunning: Boolean,
    isRestingBreak: Boolean,
    elapsedWorkoutSeconds: Int,
    restRemainingSeconds: Int,
    customRestDurationSeconds: Int,
    currentSetNumber: Int,
    targetSetsCount: Int,
    currentRepOrLapCount: Int,
    currentWeightOrDist: Double,
    selectedStepIndex: Int,
    isTtsMuted: Boolean,
    isMusicMuted: Boolean,
    onTogglePlayPause: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextWorkOrRestSwitch: () -> Unit,
    onStopAndSave: () -> Unit,
    onAdjustReps: (Int) -> Unit,
    onAdjustWeightOrDist: (Double) -> Unit,
    onAdjustTargetSets: (Int) -> Unit,
    onAdjustRestSeconds: (Int) -> Unit,
    onSelectStep: (Int) -> Unit,
    onToggleTtsMute: () -> Unit,
    onToggleMusicMute: () -> Unit,
    onJumpToCanvas: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToCanvas,
                label = "Workout Ring",
                icon = Icons.Rounded.KeyboardArrowUp
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement
            = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "SESSION CONTROLS",
                    badgeText = "SET $currentSetNumber/$targetSetsCount"
                )
            }

            // PROFESSIONAL SET-BY-SET LOGGING TABLE (Hevy / Strong Style - Adapted for all Disciplines)
            item {
                Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val metricHeader = when (activeProgram.discipline) {
                            WorkoutDiscipline.GYM -> "KG × REPS"
                            WorkoutDiscipline.RUNNING -> "KM · PACE"
                            WorkoutDiscipline.SWIMMING -> "LAPS · TIME"
                            WorkoutDiscipline.CALISTHENICS -> "HOLD · REPS"
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("DISCIPLINE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(metricHeader, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("✓", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        for (s in 1..targetSetsCount) {
                            val isDone = s < currentSetNumber
                            val isActive = s == currentSetNumber && isRunning
                            val cellText = when (activeProgram.discipline) {
                                WorkoutDiscipline.GYM -> "${currentWeightOrDist.toInt()}kg × $currentRepOrLapCount"
                                WorkoutDiscipline.RUNNING -> "${"%.1f".format(Locale.ROOT, currentWeightOrDist)} km"
                                WorkoutDiscipline.SWIMMING -> "${currentRepOrLapCount} Laps"
                                WorkoutDiscipline.CALISTHENICS -> "$currentRepOrLapCount Reps"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (s == 1 && activeProgram.discipline == WorkoutDiscipline.GYM) "W" else "${s}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (s == 1) Color(0xFFFFD177) else MaterialTheme.colorScheme.onSurface)
                                Text(activeProgram.discipline.shortLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(cellText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = if (isDone) Icons.Default.Check else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isDone) Color(0xFF79E2B0) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. WORKOUT INTERVALS & STEPS CHECKLIST
            item {
                CatalogM3ListHeaderSample(title = "WORKOUT STEPS (${activeProgram.steps.size})")
            }

            items(activeProgram.steps.size) { index ->
                val stepText = activeProgram.steps[index]
                val isSelected = selectedStepIndex == index
                SplitCheckboxButton(
                    checked = index < currentSetNumber - 1 || isSelected,
                    onCheckedChange = { onSelectStep(index) },
                    onContainerClick = { onSelectStep(index) },
                    toggleContentDescription = "Step ${index + 1}",
                    label = {
                        Text(
                            text = "Step ${index + 1}",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    secondaryLabel = {
                        Text(stepText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // 2. CUSTOM REPS, LOAD, SETS & REST TIMERS
            item {
                CatalogM3ListHeaderSample(title = "LIVE SET & TIMER METRICS")
            }

            item {
                val repLabel = when (activeProgram.discipline) {
                    WorkoutDiscipline.SWIMMING -> "Pool Lengths ($currentRepOrLapCount)"
                    WorkoutDiscipline.RUNNING -> "Intervals ($currentRepOrLapCount)"
                    else -> "Reps ($currentRepOrLapCount)"
                }
                WearExpressiveHorizontalSlider(
                    title = repLabel,
                    valueText = "$currentRepOrLapCount",
                    onValueDecrease = { onAdjustReps(-1) },
                    onValueIncrease = { onAdjustReps(1) },
                    canDecrease = currentRepOrLapCount > 1,
                    canIncrease = currentRepOrLapCount < 300
                )
            }

            item {
                val loadTitle = when (activeProgram.discipline) {
                    WorkoutDiscipline.RUNNING -> "Target Distance"
                    WorkoutDiscipline.SWIMMING -> "Lap Distance (x100m)"
                    else -> "Working Load"
                }
                val loadVal = when (activeProgram.discipline) {
                    WorkoutDiscipline.RUNNING -> "${"%.1f".format(Locale.ROOT, currentWeightOrDist)} km"
                    WorkoutDiscipline.SWIMMING -> "${(currentWeightOrDist * 100).toInt()} m"
                    else -> "${"%.1f".format(Locale.ROOT, currentWeightOrDist)} kg"
                }
                val stepDelta = if (activeProgram.discipline == WorkoutDiscipline.RUNNING) 0.5 else 2.5
                WearExpressiveHorizontalSlider(
                    title = loadTitle,
                    valueText = loadVal,
                    onValueDecrease = { onAdjustWeightOrDist(-stepDelta) },
                    onValueIncrease = { onAdjustWeightOrDist(stepDelta) },
                    canDecrease = currentWeightOrDist > 0.0,
                    canIncrease = currentWeightOrDist < 500.0
                )
            }

            item {
                WearExpressiveHorizontalSlider(
                    title = "Target Sets",
                    valueText = "$targetSetsCount sets",
                    onValueDecrease = { onAdjustTargetSets(-1) },
                    onValueIncrease = { onAdjustTargetSets(1) },
                    canDecrease = targetSetsCount > 1,
                    canIncrease = targetSetsCount < 30
                )
            }

            item {
                WearExpressiveHorizontalSlider(
                    title = "Rest Break Timer",
                    valueText = "${customRestDurationSeconds}s",
                    onValueDecrease = { onAdjustRestSeconds(-15) },
                    onValueIncrease = { onAdjustRestSeconds(15) },
                    canDecrease = customRestDurationSeconds > 15,
                    canIncrease = customRestDurationSeconds < 300
                )
            }




        

}
    }
}

// =============================================================================
// PAGE 2: MULTI-DISCIPLINE WORKOUT PROGRAMS & CUSTOM UNIVERSAL EDITOR
// =============================================================================
@Composable
private fun WorkoutProgramsAndEditorPage(
    spec: WearAdaptiveSpec,
    trainingPrograms: List<com.creativeblisslabs.wearproductivity.wear.workout.TrainingProgramDef>,
    activeTrainingProgramId: String?,
    completedTrainingDays: Set<String>,
    showTrainingProgramsBrowser: Boolean,
    onJoinTrainingProgram: (String) -> Unit,
    onQuitTrainingProgram: () -> Unit,
    onToggleTrainingBrowser: () -> Unit,
    programs: List<WorkoutProgramItem>,
    activeProgram: WorkoutProgramItem,
    selectedDiscipline: WorkoutDiscipline,
    showProgramEditor: Boolean,
    isEditingProgram: Boolean,
    draftProgramName: String,
    draftSetsRepsRest: String,
    draftWeightAndZone: String,
    onSelectDiscipline: (WorkoutDiscipline) -> Unit,
    onSelectProgram: (WorkoutProgramItem) -> Unit,
    onStartProgramNow: (WorkoutProgramItem, week: Int?, day: Int?) -> Unit,
    onOpenNewProgramEditor: () -> Unit,
    onOpenEditProgramEditor: (WorkoutProgramItem) -> Unit,
    onSaveProgram: (String, String, String) -> Unit,
    onCancelProgramEditor: () -> Unit,
    onDeleteProgram: (String) -> Unit,
    onLoadPresetPlan: (String, String) -> Unit,
    onOpenAnalyticsPage: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val filteredPrograms = programs.filter { it.discipline == selectedDiscipline }.ifEmpty { programs }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = if (showProgramEditor) onCancelProgramEditor else onOpenNewProgramEditor,
                label = if (showProgramEditor) "Close" else "New Workout",
                icon = if (showProgramEditor) Icons.Rounded.Close else Icons.Rounded.Add
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement
            = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "WORKOUT STUDIO",
                    badgeText = "${filteredPrograms.size} PLANS"
                )
            }

            // Discipline Filter Tabs: Gym, Run, Swim, Calis
            item {
                WearActionButtonGroup(
                    options = WorkoutDiscipline.entries.map { it.shortLabel },
                    selectedIndex = selectedDiscipline.ordinal,
                    onOptionSelected = { idx -> onSelectDiscipline(WorkoutDiscipline.entries[idx]) }
                )
            }

            // Universal Custom Exercise / Program Editor
            if (showProgramEditor) {
                item {
                    CatalogM3ListHeaderSample(
                        title = if (isEditingProgram) "EDIT ${selectedDiscipline.shortLabel.uppercase()}"
                        else "NEW ${selectedDiscipline.shortLabel.uppercase()} PLAN"
                    )
                }
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = selectedDiscipline.shortLabel.uppercase(Locale.getDefault()),
                        initialTitle = draftProgramName,
                        initialTimeOrDate = draftSetsRepsRest,
                        initialSubtitle = draftWeightAndZone,
                        isEditingExisting = isEditingProgram,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Sets x Reps · Rest (4x10 60s)",
                        subtitleFieldLabel = "Load/Dist & Muscle/Zone",
                        onSave = { name, setsReps, weightZone ->
                            onSaveProgram(name, setsReps, weightZone)
                        },
                        onCancel = onCancelProgramEditor,
                        onDelete = if (isEditingProgram) onDeleteProgram else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                CatalogM3ListHeaderSample(title = "${selectedDiscipline.label.uppercase()} PROGRAMS")
            }

            items(filteredPrograms.size) { idx ->
                val prog = filteredPrograms[idx]
                val isSelected = prog.id == activeProgram.id
                M3WorkoutProgramRow(
                    program = prog,
                    isSelected = isSelected,
                    onSelect = { onSelectProgram(prog) },
                    onStartNow = { onStartProgramNow(prog, null, null) },
                    onEdit = { onOpenEditProgramEditor(prog) },
                    onDelete = {
                        onDeleteProgram(prog.id)
                    }
                )
            }

            // Quick Running Plan Presets when on Running Tab
            if (selectedDiscipline == WorkoutDiscipline.RUNNING) {
                item {
                    CatalogM3ListHeaderSample(title = "ENDURANCE PRESETS")
                }
                val presets = listOf(
                    "Level 1: C25K" to "Load C25K 5K Plan",
                    "Level 2: 10K Bridge" to "Load 10K Bridge Plan",
                    "Level 3: Half-Marathon" to "Load 21.1K Half-Marathon"
                )
                items(presets.size) { idx ->
                    val (level, label) = presets[idx]
                    FilledTonalButton(
                        onClick = { onLoadPresetPlan(level, label) },
                        icon = { Icon(Icons.Rounded.DirectionsRun, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
            }

            item {
                CatalogM3ChildButtonSample(
                    label = "PR Targets & Weekly Reports",
                    onClick = onOpenAnalyticsPage
                )
            }
        

}
    }
}

@Composable
private fun TransformingLazyColumnItemScope.M3WorkoutProgramRow(
    program: WorkoutProgramItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStartNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    WearSwipeableActionCard(
        onCardClick = onSelect,
        onEditClick = onEdit,
        onDeleteClick = onDelete,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) program.discipline.accent
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable(onClick = onStartNow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Start ${program.name}",
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) Color.Black else program.discipline.accent
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = when (program.discipline) {
                    WorkoutDiscipline.GYM -> "${program.targetSets}x${program.targetReps} @ ${program.targetWeightKg.toInt()}kg · ${program.muscleOrZone}"
                    WorkoutDiscipline.RUNNING -> "${"%.1f".format(Locale.ROOT, program.targetWeightKg)}km · ${program.steps.size} intervals"
                    WorkoutDiscipline.SWIMMING -> "${program.targetSets}x${program.targetReps * 25}m · Rest ${program.restSeconds}s"
                    WorkoutDiscipline.CALISTHENICS -> "${program.targetSets}x${program.targetReps} reps · ${program.muscleOrZone}"
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalIconButton(
                onClick = onEdit,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit Plan", modifier = Modifier.size(14.dp))
            }
        }
    }
}

// =============================================================================
// PAGE 3: PR TARGETS (CURRENT VS PREVIOUS BEST), WEEKLY VOLUME & HEATMAP
// =============================================================================
@Composable
private fun WorkoutAnalyticsAndPrPage(
    spec: WearAdaptiveSpec,
    state: LifeState,
    prRecords: List<PersonalRecordEntry>,
    history: List<WorkoutHistoryRecord>,
    showPrEditor: Boolean,
    editingPrEntry: PersonalRecordEntry?,
    onOpenEditPr: (PersonalRecordEntry?) -> Unit,
    onClosePrEditor: () -> Unit,
    onSavePrEntry: (String, String, String) -> Unit,
    onRepeatHistorySession: (WorkoutHistoryRecord) -> Unit,
    onDeleteHistorySession: (String) -> Unit,
    onJumpToTop: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val totalSessions = history.size
    val totalMinutes = (history.sumOf { it.durationSeconds } / 60).coerceAtLeast(1)

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToTop,
                label = "Active Ring",
                icon = Icons.Rounded.KeyboardArrowUp
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement
            = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {



            item {
                FilledTonalButton(
                    onClick = { if (showPrEditor) onClosePrEditor() else onOpenEditPr(null) },
                    icon = { Icon(if (showPrEditor) Icons.Rounded.Close else Icons.Rounded.EmojiEvents, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showPrEditor) "Close PR Editor" else "Add / Update PR Target")
                }
            }

            if (showPrEditor) {
                item {
                    Card(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "7-DAY TRAINING LOAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            val weeklyData = remember(history) {
                                val today = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }

                                (6 downTo 0).map { daysAgo ->
                                    val day = (today.clone() as java.util.Calendar).apply {
                                        add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                                    }

                                    val start = day.timeInMillis

                                    val end = (day.clone() as java.util.Calendar).apply {
                                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                                    }.timeInMillis

                                    val records = history.filter {
                                        it.timestampEpochMillis >= start &&
                                                it.timestampEpochMillis < end
                                    }

                                    val total = records.sumOf { it.totalVolumeOrDist }

                                    val label = SimpleDateFormat(
                                        "EE",
                                        Locale.getDefault()
                                    ).format(day.time).take(1)

                                    label to total
                                }
                            }

                            val maxWeeklyLoad = weeklyData.maxOfOrNull { it.second }
                                ?.coerceAtLeast(1.0)
                                ?: 1.0

                            val weeklyBars = weeklyData.map { (_, value) ->
                                (value / maxWeeklyLoad).toFloat().coerceIn(0f, 1f)
                            }
                            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                val barWidth = size.width / (weeklyBars.size * 2f)
                                weeklyBars.forEachIndexed { index, ratio ->
                                    val barHeight = (size.height * ratio).coerceAtLeast(2f)
                                    val x = (index + 0.5f) * barWidth

                                    drawRoundRect(
                                        color = MaterialTheme.colorScheme.primary,
                                        topLeft = Offset(x, size.height - barHeight),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(
                                            4.dp.toPx(),
                                            4.dp.toPx()
                                        )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                weeklyData.forEach { (label, _) ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (history.isEmpty()) {
                                    "No completed workouts recorded yet."
                                } else {
                                    "Weekly load unavailable. Full dated session records are required."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "PR RECORD",
                        initialTitle = editingPrEntry?.exerciseName ?: "",
                        initialTimeOrDate = editingPrEntry
                            ?.let { "${it.currentValue}/${it.previousBest}" }
                            ?: "",

                        initialSubtitle = editingPrEntry
                            ?.let { "${it.targetGoal} ${it.unit}" }
                            ?: "",
                        isEditingExisting = editingPrEntry != null,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Current / Prev Best (e.g. 70/65)",
                        subtitleFieldLabel = "PR Target Goal & Unit (85 kg)",
                        onSave = { name, curPrev, targetUnit ->
                            onSavePrEntry(name, curPrev, targetUnit)
                        },
                        onCancel = onClosePrEditor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(prRecords.size) { idx ->
                val pr = prRecords[idx]
                val progressToTarget = (pr.currentValue / pr.targetGoal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                val delta = pr.currentValue - pr.previousBest
                val deltaLabel = if (delta >= 0) "+${"%.1f".format(Locale.ROOT, delta)}${pr.unit}"
                else "${"%.1f".format(Locale.ROOT, delta)}${pr.unit}"

                Card(
                    onClick = { onOpenEditPr(pr) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = pr.discipline.icon,
                                    contentDescription = null,
                                    tint = pr.discipline.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pr.exerciseName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = deltaLabel,
                                style = MaterialTheme.typography.labelSmall.tabularNums(),
                                color = Color(0xFF79E2B0),
                                fontWeight = FontWeight.Black
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Now: ${"%.1f".format(Locale.ROOT, pr.currentValue)}${pr.unit}",
                                style = MaterialTheme.typography.bodySmall.tabularNums(),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prev: ${"%.1f".format(Locale.ROOT, pr.previousBest)}${pr.unit}",
                                style = MaterialTheme.typography.bodySmall.tabularNums(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Goal: ${"%.1f".format(Locale.ROOT, pr.targetGoal)}${pr.unit}",
                                style = MaterialTheme.typography.bodySmall.tabularNums(),
                                color = pr.discipline.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressToTarget },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ProgressIndicatorDefaults.colors(
                                indicatorColor = pr.discipline.accent,
                                trackColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }
                }
            }




            // 3. PAST WORKOUT SESSION LOG HISTORY (TAP TO REPEAT, SWIPE/BUTTON TO DELETE)
            item {
                CatalogM3ListHeaderSample(title = "WORKOUT LOG HISTORY (${history.size})")

            }

            items(history.size) { idx ->
                val rec = history[idx]
                WearSwipeableActionCard(
                    onCardClick = { onRepeatHistorySession(rec) },
                    onEditClick = { onRepeatHistorySession(rec) },
                    onDeleteClick = { onDeleteHistorySession(rec.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = rec.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (rec.isNewPr) {
                                    WearStatusBadge(text = "PR")
                                }
                            }
                            val volLabel = when (rec.discipline) {
                                WorkoutDiscipline.GYM -> "${rec.setsCompleted} sets · ${rec.totalRepsOrLaps} reps · ${rec.totalVolumeOrDist.toInt()}kg vol"
                                WorkoutDiscipline.RUNNING -> "${"%.2f".format(Locale.ROOT, rec.totalVolumeOrDist)}km · ${formatDurationClock(rec.durationSeconds)}"
                                WorkoutDiscipline.SWIMMING -> "${rec.totalVolumeOrDist.toInt()}m swim · ${formatDurationClock(rec.durationSeconds)}"
                                WorkoutDiscipline.CALISTHENICS -> "${rec.setsCompleted} sets · ${rec.totalRepsOrLaps} reps"
                            }
                            Text(
                                text = "$volLabel · ${rec.timestampLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDeleteHistorySession(rec.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Workout Log",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED VIEWPORT UTILITIES
// =============================================================================
@Composable
private fun ViewportConstrainedWorkoutConfirmation(
    label: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

    LaunchedEffect(label) {
        delay(1_800L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        CurvedLayout(modifier = Modifier.fillMaxSize()) {
            confirmationDialogCurvedText(text = label, style = curvedTextStyle)
        }
        if (isError) {
            ConfirmationDialogDefaults.FailureIcon()
        } else {
            ConfirmationDialogDefaults.SuccessIcon()
        }
    }
}

