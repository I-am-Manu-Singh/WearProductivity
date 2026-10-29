package com.creativeblisslabs.wearproductivity.wear.ui.workout

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
    val timestampLabel: String
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

    fun toggleSystemMediaMute(context: Context, muteMusic: Boolean) {
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (muteMusic) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
        }
    }

    private fun playSynthesizedBeep(freqHz: Double, durationMs: Int) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(256)
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(numSamples * 2)

                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                val pcm = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val env = sin(Math.PI * i / numSamples)
                    val sample = sin(2.0 * Math.PI * i * freqHz / sampleRate) * env * 0.45
                    pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(pcm, 0, pcm.size)
                track.play()
                delay(durationMs.toLong() + 60L)
                track.stop()
                track.release()
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

    fun loadPrograms(context: Context, repoRuns: List<RunWorkout>): List<WorkoutProgramItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PROGRAMS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { entry ->
                val p = entry.split("|")
                if (p.size >= 10) {
                    WorkoutProgramItem(
                        id = p[0],
                        name = p[1],
                        discipline = runCatching { WorkoutDiscipline.valueOf(p[2]) }.getOrDefault(WorkoutDiscipline.GYM),
                        muscleOrZone = p[3],
                        targetSets = p[4].toIntOrNull()?.coerceIn(1, 30) ?: 4,
                        targetReps = p[5].toIntOrNull()?.coerceIn(1, 200) ?: 10,
                        targetWeightKg = p[6].toDoubleOrNull()?.coerceIn(0.0, 500.0) ?: 60.0,
                        restSeconds = p[7].toIntOrNull()?.coerceIn(10, 600) ?: 60,
                        stepDurationSeconds = p[8].toIntOrNull()?.coerceIn(15, 7200) ?: 180,
                        steps = p[9].split("~").filter { it.isNotBlank() }
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }

        val defaults = mutableListOf<WorkoutProgramItem>()

        repoRuns.take(3).forEach { run ->
            if (defaults.none { it.name.equals(run.title, ignoreCase = true) }) {
                defaults.add(
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
                        steps = run.intervals.ifEmpty { listOf("Warmup 5m", "Main Run", "Cooldown 5m") }
                    )
                )
            }
        }
        return defaults
    }

    fun savePrograms(context: Context, list: List<WorkoutProgramItem>) {
        val encoded = list.joinToString(";;") { item ->
            val safeSteps = item.steps.joinToString("~") { it.replace("|", "").replace("~", "") }
            "${item.id}|${item.name.replace("|", "")}|${item.discipline.name}|${item.muscleOrZone.replace("|", "")}|${item.targetSets}|${item.targetReps}|${item.targetWeightKg}|${item.restSeconds}|${item.stepDurationSeconds}|$safeSteps"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PROGRAMS, encoded).apply()
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
        return listOf(
            PersonalRecordEntry("pr-bench", "Barbell Bench Press", WorkoutDiscipline.GYM, 65.0, 62.5, 80.0, "kg"),
            PersonalRecordEntry("pr-squat", "Back Squat Power", WorkoutDiscipline.GYM, 90.0, 85.0, 110.0, "kg"),
            PersonalRecordEntry("pr-run5k", "5K VO2 Max Intervals", WorkoutDiscipline.RUNNING, 5.2, 5.0, 10.0, "km"),
            PersonalRecordEntry("pr-swim", "10x100m Freestyle Swim", WorkoutDiscipline.SWIMMING, 1000.0, 800.0, 1500.0, "m"),
            PersonalRecordEntry("pr-pullup", "Pull-Ups & Front Lever", WorkoutDiscipline.CALISTHENICS, 14.0, 12.0, 20.0, "reps")
        )
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
                        timestampLabel = p[8]
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            WorkoutHistoryRecord("wh-1", "Barbell Bench Press", WorkoutDiscipline.GYM, 4, 40, 2600.0, 1650, true, "Yesterday"),
            WorkoutHistoryRecord("wh-2", "5K VO2 Max Intervals", WorkoutDiscipline.RUNNING, 5, 5, 5.2, 1520, false, "2d ago"),
            WorkoutHistoryRecord("wh-3", "Pull-Ups & Front Lever", WorkoutDiscipline.CALISTHENICS, 5, 60, 60.0, 1400, true, "4d ago")
        )
    }

    fun saveHistory(context: Context, list: List<WorkoutHistoryRecord>) {
        val encoded = list.take(30).joinToString(";;") {
            "${it.id}|${it.title.replace("|", "")}|${it.discipline.name}|${it.setsCompleted}|${it.totalRepsOrLaps}|${it.totalVolumeOrDist}|${it.durationSeconds}|${it.isNewPr}|${it.timestampLabel}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, encoded).apply()
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

    // Custom Programs, PR Records & History State
    val programs = remember(initialRoutineIds) {
        mutableStateListOf<WorkoutProgramItem>().apply {
            if (!initialRoutineIds.isNullOrEmpty()) {
                val customItems = initialRoutineIds.mapNotNull { id ->
                    exerciseRepository.allExercises.find { it.id == id }?.let { ex ->
                        WorkoutProgramItem(
                            id = ex.id,
                            name = ex.name,
                            discipline = WorkoutDiscipline.GYM,
                            muscleOrZone = ex.primaryMuscles.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Custom",
                            targetSets = 4,
                            targetReps = 10,
                            targetWeightKg = 50.0,
                            restSeconds = 60,
                            stepDurationSeconds = 60,
                            steps = ex.instructions.ifEmpty { listOf("Working Set 1", "Working Set 2", "Working Set 3") }
                        )
                    }
                }
                if (customItems.isNotEmpty()) {
                    addAll(customItems)
                }
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
    val completedTrainingDays by remember(activeTrainingProgramId) { mutableStateOf(WorkoutStudioPrefs.loadCompletedTrainingDays(context)) }
    var showTrainingProgramsBrowser by remember { mutableStateOf(false) }
    var selectedProgramId by remember { mutableStateOf(WorkoutStudioPrefs.loadActiveId(context)) }
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

    // Sets, Reps, Load & Step Tracking
    var currentSetNumber by remember(activeProgram.id) { mutableIntStateOf(1) }
    var targetSetsCount by remember(activeProgram.id) { mutableIntStateOf(activeProgram.targetSets) }
    var manualRepCount by remember { mutableIntStateOf(activeProgram.targetReps) }
    var currentWeightOrDist by remember { mutableDoubleStateOf(activeProgram.targetWeightKg) }
    var sessionTotalRepsOrLaps by remember { mutableIntStateOf(0) }
    var sessionTotalVolumeOrDist by remember { mutableDoubleStateOf(0.0) }
    var selectedStepIndex by remember { mutableIntStateOf(0) }

    val currentRepOrLapCount = if (isRunning && activeProgram.discipline == WorkoutDiscipline.GYM) {
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

    // Live 1-Second Workout & Rest Break Ticker
    LaunchedEffect(isRunning, isRestingBreak, restRemainingSeconds, customStepDurationSeconds) {
        while (isRunning) {
            delay(1_000L)
            elapsedWorkoutSeconds = (elapsedWorkoutSeconds + 1).coerceAtMost(86400)

            if (isRestingBreak) {
                if (restRemainingSeconds > 1) {
                    restRemainingSeconds -= 1
                    if (restRemainingSeconds == 10) {
                        audioCoach.announce("10 seconds of rest left.", isTtsMuted, 660.0)
                    }
                } else {
                    isRestingBreak = false
                    restRemainingSeconds = customRestDurationSeconds
                    activeStepElapsedSeconds = 0
                    haptic.click()
                    audioCoach.announce(
                        "Rest complete. Begin Set $currentSetNumber of ${activeProgram.name}.",
                        isTtsMuted,
                        1046.5
                    )
                    feedbackOverlay = "Set $currentSetNumber Active!" to false
                }
            } else {
                activeStepElapsedSeconds += 1
                if (activeStepElapsedSeconds == customStepDurationSeconds) {
                    audioCoach.announce("Target step time reached.", isTtsMuted, 880.0)
                }
            }
        }
    }

    // Select a Workout Program
    fun selectProgram(program: WorkoutProgramItem) {
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
    fun checkAndRecordPr(program: WorkoutProgramItem, metricValue: Double): Boolean {
        val idx = prRecords.indexOfFirst {
            it.exerciseName.equals(program.name, ignoreCase = true) || it.id.contains(program.id)
        }
        val unitLabel = when (program.discipline) {
            WorkoutDiscipline.GYM -> "kg"
            WorkoutDiscipline.RUNNING -> "km"
            WorkoutDiscipline.SWIMMING -> "m"
            WorkoutDiscipline.CALISTHENICS -> "reps"
        }
        if (idx >= 0) {
            val existing = prRecords[idx]
            val beatPr = metricValue > existing.currentValue && metricValue > existing.previousBest
            val updated = existing.copy(
                previousBest = if (beatPr) existing.currentValue else existing.previousBest,
                currentValue = maxOf(existing.currentValue, metricValue)
            )
            prRecords[idx] = updated
            WorkoutStudioPrefs.savePrList(context, prRecords)
            return beatPr
        } else if (metricValue > 0.0) {
            prRecords.add(
                0,
                PersonalRecordEntry(
                    id = "pr-${program.id}",
                    exerciseName = program.name,
                    discipline = program.discipline,
                    currentValue = metricValue,
                    previousBest = (metricValue * 0.9).roundToInt().toDouble(),
                    targetGoal = (metricValue * 1.15).roundToInt().toDouble(),
                    unit = unitLabel
                )
            )
            WorkoutStudioPrefs.savePrList(context, prRecords)
            return true
        }
        return false
    }

    // Log current Set / Interval & Transition to Rest Break
    fun completeCurrentSetOrInterval() {
        haptic.click()
        if (!hasStarted) {
            hasStarted = true
            isRunning = true
        }

        val addedVolumeOrDist = when (activeProgram.discipline) {
            WorkoutDiscipline.GYM -> currentWeightOrDist * currentRepOrLapCount
            WorkoutDiscipline.RUNNING -> (currentWeightOrDist / targetSetsCount.coerceAtLeast(1)).coerceAtLeast(0.4)
            WorkoutDiscipline.SWIMMING -> (currentRepOrLapCount * 25.0)
            WorkoutDiscipline.CALISTHENICS -> currentRepOrLapCount.toDouble()
        }

        sessionTotalRepsOrLaps += currentRepOrLapCount
        sessionTotalVolumeOrDist += addedVolumeOrDist

        val prCandidate = when (activeProgram.discipline) {
            WorkoutDiscipline.GYM -> currentWeightOrDist
            WorkoutDiscipline.RUNNING -> sessionTotalVolumeOrDist
            WorkoutDiscipline.SWIMMING -> sessionTotalVolumeOrDist
            WorkoutDiscipline.CALISTHENICS -> currentRepOrLapCount.toDouble()
        }
        val isNewPr = checkAndRecordPr(activeProgram, prCandidate)

        coroutineScope.launch {
            repository.updateGymRestSeconds(customRestDurationSeconds)
            val matchedEx = liveState.gymWorkout.exercises.firstOrNull {
                it.name.equals(activeProgram.name, ignoreCase = true)
            }
            if (matchedEx != null) {
                repository.logGymSet(
                    matchedEx.stamp.id,
                    "${currentWeightOrDist.toInt()}kg x $currentRepOrLapCount"
                )
            }
        }

        if (currentSetNumber < targetSetsCount) {
            currentSetNumber += 1
            isRestingBreak = true
            restRemainingSeconds = customRestDurationSeconds
            repDetectionEngine.reset()
            manualRepCount = activeProgram.targetReps
            selectedStepIndex = (selectedStepIndex + 1).coerceAtMost(activeProgram.steps.lastIndex)
            val msg = if (isNewPr) "Set logged! NEW PR! 🏆" else "Set logged! Total: ${sessionTotalVolumeOrDist.toInt()}"
            feedbackOverlay = msg to true
            audioCoach.announce("Set completed. Rest timer started.", isTtsMuted, 880.0)
        } else {
            val record = WorkoutHistoryRecord(
                id = "wh-${System.currentTimeMillis()}",
                title = activeProgram.name,
                discipline = activeProgram.discipline,
                setsCompleted = currentSetNumber,
                totalRepsOrLaps = sessionTotalRepsOrLaps,
                totalVolumeOrDist = sessionTotalVolumeOrDist,
                durationSeconds = elapsedWorkoutSeconds,
                isNewPr = isNewPr,
                timestampLabel = currentWorkoutTimeStamp()
            )

            val updatedHistory = listOf(record) + WorkoutStudioPrefs.loadHistory(context)
            WorkoutStudioPrefs.saveHistory(context, updatedHistory)

            isRunning = false
            hasStarted = false
            feedbackOverlay = "Session Complete! Great job 🏆" to true
            audioCoach.announce("Workout session complete. Great job!", isTtsMuted, 1046.5)
        }
    }

    // Switch between Active Work Set and Rest Break (Next Button)
    fun switchWorkAndRestPhase() {
        haptic.click()
        if (!hasStarted) {
            hasStarted = true
            isRunning = true
            isRestingBreak = false
            audioCoach.announce("Starting ${activeProgram.name}.", isTtsMuted, 880.0)
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
                currentSetNumber -= 1
                selectedStepIndex = (selectedStepIndex - 1).coerceAtLeast(0)
                feedbackOverlay = "Set $currentSetNumber Active" to false
            } else {
                activeStepElapsedSeconds = 0
                feedbackOverlay = "Set 1 Reset" to false
            }
        }
    }

    // Stop, Save & Log Full Workout Session to History
    fun stopAndFinishWorkout() {
        haptic.click()
        val finalSets = currentSetNumber.coerceAtLeast(1)
        val finalReps = sessionTotalRepsOrLaps.takeIf { it > 0 } ?: (currentRepOrLapCount * finalSets)
        val finalVol = sessionTotalVolumeOrDist.takeIf { it > 0.0 } ?: when (activeProgram.discipline) {
            WorkoutDiscipline.GYM -> currentWeightOrDist * finalReps
            WorkoutDiscipline.RUNNING -> currentWeightOrDist
            WorkoutDiscipline.SWIMMING -> finalReps * 25.0
            WorkoutDiscipline.CALISTHENICS -> finalReps.toDouble()
        }
        val prMetric = when (activeProgram.discipline) {
            WorkoutDiscipline.GYM -> currentWeightOrDist
            WorkoutDiscipline.RUNNING -> finalVol
            WorkoutDiscipline.SWIMMING -> finalVol
            WorkoutDiscipline.CALISTHENICS -> currentRepOrLapCount.toDouble()
        }
        val hitPr = checkAndRecordPr(activeProgram, prMetric)

        val newRecord = WorkoutHistoryRecord(
            id = "wh-${System.currentTimeMillis()}",
            title = activeProgram.name,
            discipline = activeProgram.discipline,
            setsCompleted = finalSets,
            totalRepsOrLaps = finalReps,
            totalVolumeOrDist = finalVol,
            durationSeconds = elapsedWorkoutSeconds.coerceAtLeast(60),
            isNewPr = hitPr,
            timestampLabel = currentWorkoutTimeStamp()
        )
        workoutHistory.add(0, WorkoutSessionLogRecord(newRecord))
        WorkoutStudioPrefs.saveHistory(context, workoutHistory.map { it.record })
        completedWorkoutSummary = newRecord

        if (activeTrainingProgramId != null && activeTrainingWeek != null && activeTrainingDay != null) {
            WorkoutStudioPrefs.markTrainingDayCompleted(context, activeTrainingProgramId!!, activeTrainingWeek!!, activeTrainingDay!!)
        }


        coroutineScope.launch {
            if (activeProgram.discipline == WorkoutDiscipline.RUNNING) {
                val matchingRun = activeRuns.firstOrNull { !it.completed && it.title.equals(activeProgram.name, ignoreCase = true) }
                    ?: activeRuns.firstOrNull { !it.completed }
                if (matchingRun != null) {
                    repository.completeRunWorkout(matchingRun.stamp.id)
                }
            }
        }

        isRunning = false
        hasStarted = false
        isRestingBreak = false
        elapsedWorkoutSeconds = 0
        activeStepElapsedSeconds = 0
        sessionTotalRepsOrLaps = 0
        sessionTotalVolumeOrDist = 0.0
        currentSetNumber = 1

        audioCoach.announce(
            if (hitPr) "Workout saved. Congratulations on a new personal record!" else "Workout saved to history.",
            isTtsMuted,
            1318.5
        )
        feedbackOverlay = (if (hitPr) "Saved + NEW PR!" else "Workout Logged!") to false
    }

    // Save or Edit Custom Workout Program
    fun saveCustomProgram(nameInput: String, setsRepsRestInput: String, weightZoneInput: String) {
        val cleanName = nameInput.trim().take(36)
        if (cleanName.isBlank()) {
            feedbackOverlay = "Enter Exercise/Plan Name" to true
            haptic.click()
            return
        }
        val nums = Regex("""\d+""").findAll(setsRepsRestInput).mapNotNull { it.value.toIntOrNull() }.toList()
        val parsedSets = nums.getOrNull(0)?.coerceIn(1, 30) ?: 4
        val parsedReps = nums.getOrNull(1)?.coerceIn(1, 200) ?: 10
        val parsedRest = nums.getOrNull(2)?.coerceIn(10, 600) ?: 60

        val weightMatch = Regex("""(\d+(?:\.\d+)?)""").find(weightZoneInput)?.value?.toDoubleOrNull() ?: 40.0
        val zoneLabel = weightZoneInput.replace(Regex("""\d+(?:\.\d+)?\s*(?:kg|km|m)?""", RegexOption.IGNORE_CASE), "")
            .replace("·", "")
            .trim()
            .ifBlank { selectedDisciplineFilter.label }

        val generatedSteps = (1..parsedSets).map { setIdx ->
            when (selectedDisciplineFilter) {
                WorkoutDiscipline.GYM -> "Set $setIdx · ${weightMatch.toInt()}kg x $parsedReps reps"
                WorkoutDiscipline.RUNNING -> "Interval $setIdx · ${"%.1f".format(Locale.ROOT, weightMatch / parsedSets)} km pace"
                WorkoutDiscipline.SWIMMING -> "Lap Block $setIdx · ${parsedReps * 25}m swim"
                WorkoutDiscipline.CALISTHENICS -> "Round $setIdx · $parsedReps reps ($zoneLabel)"
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
                    targetWeightKg = weightMatch,
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
                targetWeightKg = weightMatch,
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

    val displayedHeartRate = liveState.wellness.currentHeartRateBpm.toInt().takeIf { it > 0 } ?: 128

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
                .background(Color.Black)
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
                            val idx = programs.indexOfFirst { it.id == activeProgram.id }.coerceAtLeast(0)
                            selectProgram(programs[(idx + 1) % programs.size])
                        },
                        onTogglePlayPause = {
                            haptic.click()
                            if (!hasStarted) {
                                hasStarted = true
                                isRunning = true
                                isRestingBreak = false
                                audioCoach.announce(
                                    "Starting ${activeProgram.name}. Set 1, $currentRepOrLapCount reps.",
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
                                hasStarted = true
                                isRunning = true
                                audioCoach.announce("Workout started.", isTtsMuted, 880.0)
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
                            audioCoach.toggleSystemMediaMute(context, isMusicMuted)
                            feedbackOverlay = (if (isMusicMuted) "Music Muted" else "Music Unmuted") to false
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
                            selectProgram(prog)
                            hasStarted = true
                            isRunning = true
                            isRestingBreak = false
                            elapsedWorkoutSeconds = 0
                            
                            activeTrainingWeek = week
                            activeTrainingDay = day
                            
                            audioCoach.announce("Starting ${prog.name}.", isTtsMuted, 880.0)
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
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
                        onDeleteProgram = {
                            editingProgramId?.let { deleteCustomProgram(it) }
                        },
                        onLoadPresetPlan = { level, label ->
                            haptic.click()
                            coroutineScope.launch {
                                repository.loadRunningPreset(level)
                                val updatedRuns = repository.current().runs.filter { it.stamp.deletedAt == null }
                                updatedRuns.forEach { r ->
                                    if (programs.none { it.name.equals(r.title, ignoreCase = true) }) {
                                        programs.add(
                                            WorkoutProgramItem(
                                                id = r.stamp.id,
                                                name = r.title,
                                                discipline = WorkoutDiscipline.RUNNING,
                                                muscleOrZone = "W${r.week} · D${r.day}",
                                                targetSets = r.intervals.size.coerceAtLeast(3),
                                                targetReps = 1,
                                                targetWeightKg = r.distanceKm ?: 5.0,
                                                restSeconds = 60,
                                                stepDurationSeconds = 180,
                                                steps = r.intervals
                                            )
                                        )
                                    }
                                }
                                WorkoutStudioPrefs.savePrograms(context, programs)
                                feedbackOverlay = "$label Loaded" to false
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
                            val nums1 = Regex("""\d+(?:\.\d+)?""").findAll(currentAndPrevStr).mapNotNull { it.value.toDoubleOrNull() }.toList()
                            val curVal = nums1.getOrNull(0) ?: 65.0
                            val prevVal = nums1.getOrNull(1) ?: (curVal * 0.9)

                            val targetVal = Regex("""\d+(?:\.\d+)?""").find(targetAndUnitStr)?.value?.toDoubleOrNull() ?: (curVal * 1.15)
                            val unitStr = targetAndUnitStr.replace(Regex("""\d+(?:\.\d+)?"""), "").trim().ifBlank { "kg" }

                            val targetEdit = editingPrEntry
                            if (targetEdit != null) {
                                val idx = prRecords.indexOfFirst { it.id == targetEdit.id }
                                if (idx >= 0) {
                                    prRecords[idx] = prRecords[idx].copy(
                                        exerciseName = name.ifBlank { targetEdit.exerciseName },
                                        currentValue = curVal,
                                        previousBest = prevVal,
                                        targetGoal = targetVal,
                                        unit = unitStr
                                    )
                                }
                            } else {
                                prRecords.add(
                                    0,
                                    PersonalRecordEntry(
                                        id = "pr-${System.currentTimeMillis()}",
                                        exerciseName = name.ifBlank { activeProgram.name },
                                        discipline = activeProgram.discipline,
                                        currentValue = curVal,
                                        previousBest = prevVal,
                                        targetGoal = targetVal,
                                        unit = unitStr
                                    )
                                )
                            }
                            WorkoutStudioPrefs.savePrList(context, prRecords)
                            showPrEditor = false
                            editingPrEntry = null
                            feedbackOverlay = "PR Target Saved" to false
                            haptic.click()
                        },
                        onRepeatHistorySession = { rec ->
                            val matched = programs.firstOrNull { it.name.equals(rec.title, ignoreCase = true) }
                            if (matched != null) selectProgram(matched)
                            hasStarted = true
                            isRunning = true
                            elapsedWorkoutSeconds = 0
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
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
                        .background(Color.Black.copy(alpha = 0.95f))
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
    onTogglePlayPause: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextWorkOrRestSwitch: () -> Unit,
    onIncrementRepOrRest: () -> Unit,
    onToggleTtsMute: () -> Unit,
    onStopAndSave: () -> Unit,
    onOpenControlsPage: () -> Unit
) {
    val context = LocalContext.current
    var showInstructions by remember { mutableStateOf(false) }

    val exerciseDef = remember(activeProgram.id, activeProgram.name, exerciseRepository) {
        exerciseRepository.allExercises.find { 
            it.id.equals(activeProgram.id, ignoreCase = true) || 
            it.name.equals(activeProgram.name, ignoreCase = true) 
        }
    }

    val imageFrames = remember(exerciseDef, exerciseRepository) {
        if (exerciseDef != null) {
            val animFrames = exerciseRepository.getExerciseAnimationFrames(exerciseDef)
            if (animFrames != null) {
                listOf(animFrames.first, animFrames.second).filter(String::isNotBlank)
            } else {
                listOfNotNull(exerciseRepository.getExerciseImagePath(exerciseDef))
            }
        } else emptyList()
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(exerciseDef, imageFrames) {
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

    val infiniteTransition = rememberInfiniteTransition(label = "workoutWavePhase")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "workoutWaveAnim"
    )

    val primaryAccent = when {
        isRestingBreak -> Color(0xFF68B6FF)
        hasStarted && !isRunning -> Color(0xFFFFD177)
        else -> activeProgram.discipline.accent
    }

    val centerButtonSize = (spec.screenWidth * 0.32f).coerceIn(58.dp, 74.dp)
    val sideButtonWidth = (spec.screenWidth * 0.21f).coerceIn(42.dp, 52.dp)
    val sideButtonHeight = (spec.screenWidth * 0.25f).coerceIn(48.dp, 60.dp)
    val bottomPillHeight = (spec.screenHeight * 0.14f).coerceIn(30.dp, 36.dp)

    ScreenScaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
                        // 1. FULL-SCREEN EXERCISE IMAGE (with fallback to ambient icon)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isRunning) 0.28f else 0.15f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageFrames.isNotEmpty()) {
                    val currentImage = imageFrames.getOrNull(currentImageIndex)
                    if (currentImage != null) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data("file:///android_asset/$currentImage")
                                .crossfade(true)
                                .build(),
                            contentDescription = "${activeProgram.name} demonstration",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (isRestingBreak) Icons.Rounded.Timer else activeProgram.discipline.icon,
                        contentDescription = null,
                        modifier = Modifier.size(spec.screenWidth * 0.54f),
                        tint = primaryAccent.copy(alpha = 0.07f)
                    )
                }
            }

            // 2. FULL-SCREEN BOUNDARY CIRCULAR PROGRESS CONTAINER (OUTER + INNER SET RING)
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
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

                // Inner secondary ring tracking overall Set / Interval completion
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

            // 3. FOREGROUND EXPRESSIVE WORKOUT CONTROLS & TELEMETRY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = spec.screenHeight * 0.12f,
                        bottom = spec.screenHeight * 0.08f,
                        start = spec.screenWidth * 0.07f,
                        end = spec.screenWidth * 0.07f
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Slot: Program Selector Pill + Primary Clock/Rep Readout + PR/HR Telemetry
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
                            .clickable(onClick = onCycleDisciplineOrProgram)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
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
                            text = "${activeProgram.discipline.shortLabel.uppercase()} · ${activeProgram.name}",
                            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // Center Primary Readout: Rest Countdown OR Elapsed Timer / Set-Rep Count
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.clickable(onClick = onOpenControlsPage)
                    )

                    val scientificMetricText = when (activeProgram.discipline) {
                        WorkoutDiscipline.GYM -> {
                            val est1RM = WorkoutScienceEngine.calculateEstimated1RM(currentWeightOrDist, currentRepOrLapCount)
                            "Est 1RM: ${est1RM.toInt()}kg · Vol: ${(currentWeightOrDist * currentRepOrLapCount).toInt()}kg"
                        }
                        WorkoutDiscipline.RUNNING -> {
                            val pace = WorkoutScienceEngine.calculatePace(currentWeightOrDist.coerceAtLeast(0.1), elapsedWorkoutSeconds)
                            "Pace: $pace · HR ${displayedHeartRate}bpm"
                        }
                        WorkoutDiscipline.SWIMMING -> {
                            val swolf = WorkoutScienceEngine.calculateSwolf(currentRepOrLapCount, elapsedWorkoutSeconds.coerceAtLeast(10))
                            "SWOLF: $swolf · Laps ${currentRepOrLapCount}"
                        }
                        WorkoutDiscipline.CALISTHENICS -> {
                            "Power: ${(currentRepOrLapCount * 1.5).toInt()}W · Reps $currentRepOrLapCount"
                        }
                    }

                    val prSubText = currentPrEntry?.let {
                        "PR ${it.currentValue.toInt()}${it.unit} · $scientificMetricText"
                    } ?: "$scientificMetricText · ${displayedHeartRate} BPM"

                    Text(
                        text = if (isRestingBreak) {
                            "UP NEXT: SET $currentSetNumber/$targetSetsCount · $prSubText"
                        } else {
                            "SET $currentSetNumber/$targetSetsCount · $displayedHeartRate BPM · $prSubText"
                        },
                        style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Universal PlayerControlBar
                com.creativeblisslabs.wearproductivity.wear.ui.components.core.PlayerControlBar(
                    isPlaying = isRunning,
                    progress = animatedProgress,
                    onTogglePlay = onTogglePlayPause,
                    onPrevious = onPreviousOrRestart,
                    onNext = onNextWorkOrRestSwitch
                )

                // Bottom Slot: TTS Voice Coach Mute Toggle | +Rep / +15s Rest | Stop & Save or Studio Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.05f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                        // Instruction Overlay / Info Button
                    Button(
                        onClick = { showInstructions = !showInstructions },
                        modifier = Modifier
                            .height(bottomPillHeight)
                            .semantics { contentDescription = "Show Instructions" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                            Text("INFO", style = MaterialTheme.typography.labelSmall)
                        }
                    }

// TTS Coach Mute / Unmute Pill
                    Button(
                        onClick = onToggleTtsMute,
                        modifier = Modifier
                            .height(bottomPillHeight)
                            .semantics { contentDescription = if (isTtsMuted) "Enable Voice Coach" else "Mute Voice Coach" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isTtsMuted) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (!isTtsMuted) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (!isTtsMuted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )

                        }
                    }

                    // Quick +1 Rep / Lap or +15s Rest Pill
                    Button(
                        onClick = onIncrementRepOrRest,
                        modifier = Modifier
                            .height(bottomPillHeight)
                            .semantics { contentDescription = if (isRestingBreak) "Add 15 Seconds Rest" else "Add 1 Rep or Lap" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isRestingBreak) "+15s" else "+1R",
                                style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Stop & Save (when started) or Open Workout Deck (when idle)
                    if (hasStarted) {
                        Button(
                            onClick = onStopAndSave,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = "Finish and Log Workout" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onOpenControlsPage,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = "Open Workout Steps and Controls" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawWorkoutWavyRing(
    progress: Float,
    phase: Float,
    trackColor: Color,
    activeColor: Color,
    strokeWidthPx: Float,
    waveAmplitudePx: Float,
    lobes: Int
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseRadius = (minOf(size.width, size.height) / 2f) - waveAmplitudePx - (strokeWidthPx / 2f)
    val totalSteps = 160

    fun buildPath(maxStep: Int): Path {
        val path = Path()
        for (step in 0..maxStep) {
            val fraction = step.toFloat() / totalSteps.toFloat()
            val angleRad = (-Math.PI / 2.0) + (fraction * 2.0 * Math.PI)
            val waveOffset = waveAmplitudePx * sin((lobes * angleRad) + phase).toFloat()
            val r = baseRadius + waveOffset
            val x = center.x + (r * cos(angleRad)).toFloat()
            val y = center.y + (r * sin(angleRad)).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    drawPath(
        path = buildPath(totalSteps),
        color = trackColor,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val activeSteps = (totalSteps * progress.coerceIn(0.04f, 1f)).toInt()
    if (activeSteps > 0) {
        drawPath(
            path = buildPath(activeSteps),
            color = activeColor,
            style = Stroke(width = strokeWidthPx * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
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
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
    onDeleteProgram: () -> Unit,
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
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    onEdit = { onOpenEditProgramEditor(prog) }
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
    onEdit: () -> Unit
) {
    WearSwipeableActionCard(
        onCardClick = onSelect,
        onEditClick = onEdit,
        onDeleteClick = onEdit,
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
                    .size(32.dp)
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
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            val weeklyBars = listOf(0.45f, 0.70f, 0.30f, 0.85f, 0.60f, 0.95f, 0.80f)
                            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                val barWidth = size.width / (weeklyBars.size * 2f)
                                weeklyBars.forEachIndexed { i, ratio ->
                                    val barHeight = (size.height * ratio).coerceAtLeast(6f)
                                    val x = (i * 2 + 0.5f) * barWidth
                                    drawRoundRect(
                                        color = if (i == weeklyBars.lastIndex) Color(0xFF79E2B0) else Color(0xFF8EAEFF),
                                        topLeft = Offset(x, size.height - barHeight),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                dayLabels.forEach { d ->
                                    Text(d, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "PR RECORD",
                        initialTitle = editingPrEntry?.exerciseName ?: "",
                        initialTimeOrDate = editingPrEntry?.let { "${it.currentValue}/${it.previousBest}" } ?: "70/65",
                        initialSubtitle = editingPrEntry?.let { "${it.targetGoal} ${it.unit}" } ?: "85 kg",
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
        


            item {
                val mediaViewModel: com.creativeblisslabs.wearproductivity.wear.media.SharedMediaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val isPlaying = mediaViewModel.isPlaying.collectAsState().value
                val progress = mediaViewModel.progress.collectAsState().value
                com.creativeblisslabs.wearproductivity.wear.ui.components.core.PlayerControlBar(
                    isPlaying = isPlaying,
                    progress = progress,
                    onTogglePlay = { mediaViewModel.togglePlay() },
                    onPrevious = { mediaViewModel.skipToPrevious() },
                    onNext = { mediaViewModel.skipToNext() }
                )
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
            .background(Color.Black.copy(alpha = 0.92f))
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

