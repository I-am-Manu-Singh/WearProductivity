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
import com.creativeblisslabs.wearproductivity.wear.sensors.RepDetectionEngine
import com.creativeblisslabs.wearproductivity.wear.sensors.StepCounterEngine
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private data class WorkoutSessionLogRecord(val record: WorkoutHistoryRecord)

// =============================================================================
// PAGE 0: FULL-SCREEN BOUNDARY PROGRESS CONTAINER & ACTIVE TRANSPORT CANVAS
// =============================================================================

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

// =============================================================================
// PAGE 2: MULTI-DISCIPLINE WORKOUT PROGRAMS & CUSTOM UNIVERSAL EDITOR
// =============================================================================


// =============================================================================
// PAGE 3: PR TARGETS (CURRENT VS PREVIOUS BEST), WEEKLY VOLUME & HEATMAP
// =============================================================================

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED VIEWPORT UTILITIES
// =============================================================================


private fun TextStyle.tabularNums(): TextStyle = this.copy(
    fontFeatureSettings = "tnum",
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

private fun TextStyle.compactLineHeight(): TextStyle = this.copy(
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)