package com.creativeblisslabs.wearproductivity.wear.ui.fasting

import android.content.Context
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlinx.coroutines.cancelAndJoin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.creativeblisslabs.wearproductivity.shared.FoodLog
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.MealType
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_FASTING_GOAL_MINUTES = 16 * 60
private const val DEFAULT_WATER_TARGET_ML = 12 * 250
private const val WATER_SERVING_ML = 250

data class FastingAdaptiveSpec(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLargeScreen: Boolean,
    val outerHorizontalMargin: Dp,
    val topBottomSlotHorizontalMargin: Dp,
    val topSlotPadding: Dp,
    val bottomEdgeButtonPadding: Dp
)

data class FastingProgramPreset(
    val id: String,
    val name: String,
    val fastHours: Int,
    val eatHours: Int,
    val subtitle: String
)

data class FastingSessionRecord(
    val id: String,
    val programName: String,
    val phaseLabel: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val timestampLabel: String
)

data class WaterLogRecord(
    val id: String,
    val amountMl: Int,
    val timestampLabel: String
)

data class WeightLogPoint(
    val id: String,
    val weightKg: Double,
    val dateLabel: String
)

data class DietPlanPreset(
    val id: String,
    val name: String,
    val targetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int
)

enum class FastingSoundMode(val label: String) {
    OFF("Off"),
    ZEN_432("432Hz"),
    RAIN("Rain"),
    WAVES("Waves"),
    WHITE("White")
}

private enum class BoundaryDisplayMode(val label: String) {
    FASTING("Fasting"),
    WATER("Hydration")
}

private fun formatDurationClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (hrs > 0) {
        String.format(Locale.ROOT, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", mins, secs)
    }
}

private fun formatHoursMinsShort(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hrs = safe / 60
    val mins = safe % 60
    return if (hrs > 0 && mins > 0) "${hrs}h ${mins}m"
    else if (hrs > 0) "${hrs}h"
    else "${mins}m"
}

private fun currentClockStamp(): String =
    SimpleDateFormat(
        "yyyy-MM-dd HH:mm",
        Locale.getDefault()
    ).format(Date())
// =============================================================================
// REAL-TIME PCM AMBIENT SOUND SYNTHESIZER ENGINE (MINDFUL FASTING & CRAVING CALM)
// =============================================================================
private class FastingSoundEngine(
    private val scope: CoroutineScope
) {
    private var audioJob: Job? = null

    suspend fun updatePlayback(
        shouldPlay: Boolean,
        mode: FastingSoundMode,
        volume: Float = 0.22f
    ) {
        // Wait for the previous track's finally block to finish.
        audioJob?.cancelAndJoin()
        audioJob = null

        if (!shouldPlay || mode == FastingSoundMode.OFF) return

        audioJob = scope.launch(Dispatchers.Default) {
            var track: AudioTrack? = null

            try {
                val sampleRate = 22_050

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                if (minBufferSize <= 0) return@launch

                val bufferSize = maxOf(minBufferSize, 2_048)

                val createdTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (createdTrack.state != AudioTrack.STATE_INITIALIZED) {
                    createdTrack.release()
                    return@launch
                }

                track = createdTrack

                createdTrack.setVolume(volume.coerceIn(0f, 1f))
                createdTrack.play()

                val samples = ShortArray(1_024)

                var phase1 = 0.0
                var phase2 = 0.0
                var lfoPhase = 0.0
                var filterOut = 0.0

                while (isActive) {
                    for (i in samples.indices) {
                        val sample = when (mode) {
                            FastingSoundMode.ZEN_432 -> {
                                phase1 += (2.0 * Math.PI * 216.0) / sampleRate
                                phase2 += (2.0 * Math.PI * 220.0) / sampleRate

                                if (phase1 > 2.0 * Math.PI) {
                                    phase1 -= 2.0 * Math.PI
                                }

                                if (phase2 > 2.0 * Math.PI) {
                                    phase2 -= 2.0 * Math.PI
                                }

                                (sin(phase1) + sin(phase2)) * 0.20
                            }

                            FastingSoundMode.RAIN -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                filterOut += 0.08 * (white - filterOut)

                                val drop = if (Random.nextInt(1_000) > 996) {
                                    Random.nextDouble(-0.5, 0.5)
                                } else {
                                    0.0
                                }

                                filterOut * 0.65 + drop
                            }

                            FastingSoundMode.WAVES -> {
                                lfoPhase += (2.0 * Math.PI * 0.12) / sampleRate

                                if (lfoPhase > 2.0 * Math.PI) {
                                    lfoPhase -= 2.0 * Math.PI
                                }

                                val swell = 0.25 +
                                        0.75 * ((sin(lfoPhase) + 1.0) * 0.5)

                                val white = Random.nextDouble(-1.0, 1.0)
                                filterOut += 0.03 * (white - filterOut)

                                filterOut * swell * 0.75
                            }

                            FastingSoundMode.WHITE ->
                                Random.nextDouble(-1.0, 1.0) * 0.30

                            FastingSoundMode.OFF -> 0.0
                        }

                        samples[i] = (
                                sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE
                                ).toInt().toShort()
                    }

                    val written = createdTrack.write(
                        samples,
                        0,
                        samples.size
                    )

                    if (written < 0) break
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Audio device or track failure. Cleanup remains in finally.
            } finally {
                // Only the owning coroutine releases its AudioTrack.
                track?.let { ownedTrack ->
                    runCatching {
                        if (ownedTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            ownedTrack.stop()
                        }
                    }

                    runCatching {
                        ownedTrack.release()
                    }
                }
            }
        }
    }

    fun cancel() {
        // Never release the track here. Its coroutine owns cleanup.
        audioJob?.cancel()
        audioJob = null
    }
}

// =============================================================================
// PERSISTENCE MANAGER FOR CUSTOM FASTING PROGRAMS, DIET TARGETS & WEIGHT HISTORY
// =============================================================================
private object FastingStudioPrefs {

    private const val PREFS_NAME = "lifeos_fasting_studio_prefs_v2"
    private const val KEY_PROGRAMS = "custom_fasting_programs"
    private const val KEY_ACTIVE_PROGRAM_ID = "active_program_id"
    private const val KEY_FAST_HISTORY = "fasting_session_history"
    private const val KEY_WATER_TARGET = "water_target_ml"
    private const val KEY_WATER_LOGS = "water_logs_today"
    private const val KEY_WEIGHT_TARGET = "weight_target_kg"
    private const val KEY_WEIGHT_HISTORY = "weight_history_points"
    private const val KEY_DIET_PLANS = "custom_diet_plans"
    private const val KEY_ACTIVE_DIET_ID = "active_diet_id"
    private const val KEY_SOUND_MODE = "fasting_sound_mode"
    private const val KEY_SOUND_MUTED = "fasting_sound_muted"
    private const val KEY_FAST_ACTIVE = "fast_session_active"
    private const val KEY_FAST_START_EPOCH_MS = "fast_start_epoch_ms"
    private const val KEY_FAST_ELAPSED_SECONDS = "fast_elapsed_seconds"
    private const val KEY_FAST_PROGRAM_ID = "fast_session_program_id"
    private const val KEY_FAST_PAUSED = "fast_session_paused"
    fun loadPrograms(context: Context): List<FastingProgramPreset> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PROGRAMS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 5) {
                    FastingProgramPreset(
                        id = p[0],
                        name = p[1],
                        fastHours = p[2].toIntOrNull()?.coerceIn(1, 72) ?: 16,
                        eatHours = p[3].toIntOrNull()?.coerceIn(1, 24) ?: 8,
                        subtitle = p[4]
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            FastingProgramPreset(
                "prog-16-8",
                "16:8 Leangains",
                16,
                8,
                "16-hour fast · 8-hour eating window"
            ),
            FastingProgramPreset(
                "prog-18-6",
                "18:6",
                18,
                6,
                "18-hour fast · 6-hour eating window"
            ),
            FastingProgramPreset(
                "prog-20-4",
                "20:4",
                20,
                4,
                "20-hour fast · 4-hour eating window"
            ),
            FastingProgramPreset(
                "prog-14-10",
                "14:10",
                14,
                10,
                "14-hour fast · 10-hour eating window"
            )
        )
    }

    fun savePrograms(context: Context, list: List<FastingProgramPreset>) {
        val encoded = list.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.fastHours}|${it.eatHours}|${it.subtitle.replace("|", "")}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PROGRAMS, encoded).apply()
    }

    fun loadActiveProgramId(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACTIVE_PROGRAM_ID, "prog-16-8") ?: "prog-16-8"

    fun saveActiveProgramId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_PROGRAM_ID, id).apply()
    }

    fun loadFastHistory(context: Context): List<FastingSessionRecord> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_FAST_HISTORY, null)
        if (!raw.isNullOrBlank()) {
            return raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 6) {
                    FastingSessionRecord(
                        id = p[0],
                        programName = p[1],
                        phaseLabel = p[2],
                        durationMinutes = p[3].toIntOrNull() ?: 960,
                        completed = p[4].toBoolean(),
                        timestampLabel = p[5]
                    )
                } else null
            }
        }
        return emptyList()
    }

    fun saveFastHistory(context: Context, list: List<FastingSessionRecord>) {
        val encoded = list.take(25).joinToString(";;") {
            "${it.id}|${it.programName.replace("|", "")}|${it.phaseLabel}|${it.durationMinutes}|${it.completed}|${it.timestampLabel}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FAST_HISTORY, encoded).apply()
    }

    fun loadWaterTarget(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_WATER_TARGET, DEFAULT_WATER_TARGET_ML).coerceIn(1000, 5000)

    fun saveWaterTarget(context: Context, targetMl: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_WATER_TARGET, targetMl.coerceIn(1000, 5000)).apply()
    }

    fun loadWaterLogs(context: Context): List<WaterLogRecord> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_WATER_LOGS, null)
        if (!raw.isNullOrBlank()) {
            return raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 3) WaterLogRecord(p[0], p[1].toIntOrNull() ?: 250, p[2]) else null
            }
        }
        return emptyList()
    }

    fun saveWaterLogs(context: Context, logs: List<WaterLogRecord>) {
        val encoded = logs.take(20).joinToString(";;") { "${it.id}|${it.amountMl}|${it.timestampLabel}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_WATER_LOGS, encoded).apply()
    }

    fun loadDietPlans(context: Context): List<DietPlanPreset> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DIET_PLANS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 6) {
                    DietPlanPreset(
                        id = p[0],
                        name = p[1],
                        targetCalories = p[2].toIntOrNull() ?: 2200,
                        targetProteinG = p[3].toIntOrNull() ?: 150,
                        targetCarbsG = p[4].toIntOrNull() ?: 220,
                        targetFatG = p[5].toIntOrNull() ?: 70
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            DietPlanPreset("diet-balanced", "Balanced Athlete", 2200, 150, 220, 70),
            DietPlanPreset("diet-protein", "High Protein Cut", 1900, 180, 150, 60),
            DietPlanPreset("diet-keto", "Ketogenic IF", 2000, 130, 35, 150),
            DietPlanPreset("diet-bulk", "Lean Strength", 2600, 175, 300, 80)
        )
    }

    fun saveDietPlans(context: Context, plans: List<DietPlanPreset>) {
        val encoded = plans.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.targetCalories}|${it.targetProteinG}|${it.targetCarbsG}|${it.targetFatG}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_DIET_PLANS, encoded).apply()
    }

    fun loadActiveDietId(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACTIVE_DIET_ID, "diet-balanced") ?: "diet-balanced"

    fun saveActiveDietId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_DIET_ID, id).apply()
    }

    fun loadWeightTarget(context: Context, fallbackWeight: Double): Double {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getFloat(KEY_WEIGHT_TARGET, -1f)
        return if (stored > 20f) stored.toDouble() else (if (fallbackWeight > 20.0) fallbackWeight - 2.0 else 70.0)
    }

    fun saveWeightTarget(context: Context, targetKg: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putFloat(KEY_WEIGHT_TARGET, targetKg.toFloat()).apply()
    }

    fun loadWeightHistory(context: Context, currentWeight: Double): List<WeightLogPoint> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_WEIGHT_HISTORY, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 3) {
                    val w = p[1].toDoubleOrNull()
                    if (w != null && w > 0.0) WeightLogPoint(p[0], w, p[2]) else null
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return if (currentWeight > 20.0) {
            listOf(WeightLogPoint("w-initial", currentWeight, "Latest"))
        } else {
            emptyList()
        }
    }

    fun saveWeightHistory(context: Context, points: List<WeightLogPoint>) {
        val encoded = points.takeLast(14).joinToString(";;") { "${it.id}|${it.weightKg}|${it.dateLabel}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_WEIGHT_HISTORY, encoded).apply()
    }

    fun loadSoundMode(context: Context): FastingSoundMode {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SOUND_MODE, FastingSoundMode.OFF.name) ?: FastingSoundMode.OFF.name
        return runCatching { FastingSoundMode.valueOf(name) }.getOrDefault(FastingSoundMode.OFF)
    }

    fun saveSoundMode(context: Context, mode: FastingSoundMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_SOUND_MODE, mode.name).apply()
    }

    fun loadMuted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SOUND_MUTED, false)

    fun saveMuted(context: Context, muted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SOUND_MUTED, muted).apply()
    }
    fun loadFastStartEpochMs(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_FAST_START_EPOCH_MS, 0L)
    }


    fun saveFastSession(
        context: Context,
        active: Boolean,
        startEpochMs: Long,
        elapsedSeconds: Int,
        programId: String,
        paused: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FAST_ACTIVE, active)
            .putLong(KEY_FAST_START_EPOCH_MS, startEpochMs)
            .putInt(KEY_FAST_ELAPSED_SECONDS, elapsedSeconds)
            .putString(KEY_FAST_PROGRAM_ID, programId)
            .putBoolean(KEY_FAST_PAUSED, paused)
            .apply()
    }
    private const val KEY_WATER_LOG_DATE = "water_log_date"

    fun ensureTodayWaterLogs(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).format(Date())

        val savedDate = prefs.getString(KEY_WATER_LOG_DATE, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_WATER_LOG_DATE, today)
                .putString(KEY_WATER_LOGS, "")
                .apply()
        }
    }

}
private fun todayEpochBounds(
    nowMs: Long = System.currentTimeMillis()
): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val end = (start.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
    }

    return start.timeInMillis to end.timeInMillis
}
// =============================================================================
// MAIN ENTRY SCREEN
// =============================================================================
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearFastingScreen(state: LifeState, repository: LifeRepository, mediaViewModel: com.creativeblisslabs.wearproductivity.wear.media.SharedMediaViewModel? = null) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val haptic = rememberLifeOSHapticProvider()
    val liveState by repository.state.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    var todayBounds by remember {
        mutableStateOf(todayEpochBounds())
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            todayBounds = todayEpochBounds(now)

            delay(
                (todayBounds.second - now).coerceAtLeast(1_000L)
            )
        }
    }
    val visibleFoodLogs = liveState.foodLogs.filter {
        it.stamp.deletedAt == null
    }

    val todayFoodLogs = visibleFoodLogs.filter {
        it.loggedAtEpochMs >= todayBounds.first &&
                it.loggedAtEpochMs < todayBounds.second
    }
    val fast = liveState.fast
    val savedWeight = liveState.wellness.bodyWeight ?: 0.0


    // =============================================================================
// REPOSITORY-DRIVEN FASTING SESSION
// =============================================================================

    val fastSession = liveState.fast

    var selectedProgramId by remember {
        mutableStateOf(FastingStudioPrefs.loadActiveProgramId(context))
    }

    val programs = remember {
        mutableStateListOf<FastingProgramPreset>().apply {
            addAll(FastingStudioPrefs.loadPrograms(context))
        }
    }

    val activeProgram = programs.firstOrNull {
        it.id == if (fastSession.hasSession) {
            fastSession.programId
        } else {
            selectedProgramId
        }
    } ?: programs.firstOrNull()
    ?: FastingProgramPreset(
        "prog-16-8",
        "16:8 Leangains",
        16,
        8,
        "Metabolic fat burn"
    )

    var boundaryMode by remember {
        mutableStateOf(BoundaryDisplayMode.FASTING)
    }

    var feedbackOverlay by remember {
        mutableStateOf<Pair<String, Boolean>?>(null)
    }

    var currentTimeMs by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    val isEatingBreakWindow = fastSession.isEatingWindow
    val isSessionPaused = fastSession.paused
    val isAnyPhaseActive = fastSession.hasSession

    val targetPhaseSeconds = fastSession.targetSeconds
        .coerceIn(1L, Int.MAX_VALUE.toLong())
        .toInt()

    val elapsedSessionSeconds = fastSession
        .elapsedSeconds(currentTimeMs)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

    val fastingProgress = (
            elapsedSessionSeconds.toFloat() /
                    targetPhaseSeconds.coerceAtLeast(1)
            ).coerceIn(0f, 1f)

    val remainingPhaseSeconds =
        (targetPhaseSeconds - elapsedSessionSeconds).coerceAtLeast(0)

// Restore only from repository state.
    LaunchedEffect(repository) {
        repository.restoreFastSession()
    }

// UI-only ticker. Never persist every second.
    LaunchedEffect(
        fastSession.sessionId,
        fastSession.phase,
        fastSession.paused,
        fastSession.phaseStartedAtEpochMs
    ) {
        if (!fastSession.hasSession || fastSession.paused) {
            currentTimeMs = System.currentTimeMillis()
            return@LaunchedEffect
        }

        while (true) {
            currentTimeMs = System.currentTimeMillis()

            val elapsed = fastSession.elapsedSeconds(currentTimeMs)

            if (elapsed >= fastSession.targetSeconds) {
                repository.pauseFast()

                haptic.click()

                feedbackOverlay = (
                        if (fastSession.isEatingWindow) {
                            "Eating Window Complete!"
                        } else {
                            "Fasting Goal Reached!"
                        }
                        ) to false

                break
            }

            delay(1_000L)
        }
    }
    // Ambient Sound Synthesizer Engine
    val soundEngine = remember { FastingSoundEngine(scope) }
    var soundMode by remember { mutableStateOf(FastingStudioPrefs.loadSoundMode(context)) }
    var isSoundMuted by remember { mutableStateOf(FastingStudioPrefs.loadMuted(context)) }

    LaunchedEffect(fast.active, isEatingBreakWindow, isSessionPaused, soundMode, isSoundMuted) {
        val shouldPlay = (fast.active || isEatingBreakWindow) && !isSessionPaused && !isSoundMuted && soundMode != FastingSoundMode.OFF
        soundEngine.updatePlayback(shouldPlay = shouldPlay, mode = soundMode)
    }
    DisposableEffect(soundEngine) {
        onDispose {
            soundEngine.cancel()
        }
    }
    // Hydration State & Timestamped Logs
    var waterTargetMl by remember { mutableIntStateOf(FastingStudioPrefs.loadWaterTarget(context)) }
    FastingStudioPrefs.ensureTodayWaterLogs(context)

    val waterLogs = remember {
        mutableStateListOf<WaterLogRecord>().apply {
            addAll(FastingStudioPrefs.loadWaterLogs(context))
        }
    }
    fun millisUntilNextLocalMidnight(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        return (calendar.timeInMillis - System.currentTimeMillis())
            .coerceAtLeast(1_000L)
    }
    LaunchedEffect(context) {
        while (true) {
            delay(millisUntilNextLocalMidnight().milliseconds)

            FastingStudioPrefs.ensureTodayWaterLogs(context)

            waterLogs.clear()
            waterLogs.addAll(FastingStudioPrefs.loadWaterLogs(context))
        }
    }
    val effectiveWaterMl = waterLogs.sumOf { it.amountMl.coerceAtLeast(0) }
    val waterProgress = (
            effectiveWaterMl.toFloat() / waterTargetMl.coerceAtLeast(500)
            ).coerceIn(0f, 1f)

    // Fasting History Logs
    val fastHistory = remember {
        mutableStateListOf<FastingSessionRecord>().apply { addAll(FastingStudioPrefs.loadFastHistory(context)) }
    }

    // Custom Diet Plans & Meal Logging
    val dietPlans = remember {
        mutableStateListOf<DietPlanPreset>().apply { addAll(FastingStudioPrefs.loadDietPlans(context)) }
    }
    var selectedDietId by remember { mutableStateOf(FastingStudioPrefs.loadActiveDietId(context)) }
    val activeDietPlan = dietPlans.firstOrNull { it.id == selectedDietId }
        ?: dietPlans.firstOrNull()
        ?: DietPlanPreset("diet-balanced", "Balanced Athlete", 2200, 150, 220, 70)

    // Body Weight Target & Historical Points
    var weightTargetKg by remember { mutableDoubleStateOf(FastingStudioPrefs.loadWeightTarget(context, savedWeight)) }
    val weightHistory = remember {
        mutableStateListOf<WeightLogPoint>().apply { addAll(FastingStudioPrefs.loadWeightHistory(context, savedWeight)) }
    }

    // Editor States (Program Editor, Meal Editor, Diet Target Editor, Weight Editor)
    var showProgramEditor by remember { mutableStateOf(false) }
    var editingProgramId by remember { mutableStateOf<String?>(null) }
    var draftProgramName by remember { mutableStateOf("") }
    var draftFastHours by remember { mutableStateOf("16") }
    var draftEatHours by remember { mutableStateOf("8") }

    var showMealEditor by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf(MealType.Lunch) }

    var showDietTargetEditor by remember { mutableStateOf(false) }
    var showWeightEditor by remember { mutableStateOf(false) }

    // Real Hardware Sensors State (Heart Rate & Step Counter)
    var liveSensorHeartRate by remember {
        mutableDoubleStateOf(0.0)
    }

    var liveSensorAccuracy by remember {
        mutableStateOf("Waiting for sensor")
    }

    var lastHeartRateAtMs by remember {
        mutableLongStateOf(0L)
    }

    var isScanningVitals by remember {
        mutableStateOf(false)
    }

    var hasBodySensorsPermission by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val heartRatePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasBodySensorsPermission = granted
            liveSensorHeartRate = 0.0
            lastHeartRateAtMs = 0L

            liveSensorAccuracy = if (granted) {
                "Waiting for sensor"
            } else {
                "Permission denied"
            }
        }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasBodySensorsPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BODY_SENSORS
                    ) == PackageManager.PERMISSION_GRANTED

                if (!hasBodySensorsPermission) {
                    liveSensorHeartRate = 0.0
                    lastHeartRateAtMs = 0L
                    liveSensorAccuracy = "Permission required"
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    DisposableEffect(
        context,
        pagerState.currentPage,
        hasBodySensorsPermission
    ) {
        if (pagerState.currentPage != 3 || !hasBodySensorsPermission) {
            onDispose { }
        } else {
            val manager = context.getSystemService(
                Context.SENSOR_SERVICE
            ) as? SensorManager

            val sensor = manager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            var lastPersistedAtMs = 0L

            val listener = if (manager != null && sensor != null) {
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val ev = event ?: return
                        val bpm = ev.values.firstOrNull()?.toDouble() ?: return

                        if (ev.sensor.type != Sensor.TYPE_HEART_RATE ||
                            bpm !in 30.0..240.0
                        ) return

                        val accuracy = when (ev.accuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High accuracy"
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium accuracy"
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low accuracy"
                            else -> "Unreliable reading"
                        }

                        val now = System.currentTimeMillis()

                        liveSensorHeartRate = bpm
                        liveSensorAccuracy = accuracy
                        lastHeartRateAtMs = now

                        if (now - lastPersistedAtMs >= 30_000L) {
                            lastPersistedAtMs = now
                            scope.launch {
                                repository.updateHeartRate(bpm, accuracy)
                            }
                        }
                    }

                    override fun onAccuracyChanged(
                        sensor: Sensor?,
                        accuracy: Int
                    ) {
                        liveSensorAccuracy = when (accuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High accuracy"
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium accuracy"
                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low accuracy"
                            else -> "Calibrating"
                        }
                    }
                }.also {
                    if (!manager.registerListener(
                            it,
                            sensor,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                    ) {
                        liveSensorAccuracy = "Sensor registration failed"
                    }
                }
            } else {
                liveSensorAccuracy = "Heart-rate sensor unavailable"
                null
            }

            onDispose {
                listener?.let { manager?.unregisterListener(it) }
            }
        }
    }

    fun selectProgram(preset: FastingProgramPreset) {
        haptic.click()

        if (fastSession.hasSession) {
            feedbackOverlay = "Stop the current session to change program" to true
            return
        }

        selectedProgramId = preset.id
        FastingStudioPrefs.saveActiveProgramId(context, preset.id)

        feedbackOverlay = "${preset.name} (${preset.fastHours}:${preset.eatHours})" to false
    }

    fun togglePlayPauseSession() {
        haptic.click()

        scope.launch {
            when {
                !fastSession.hasSession -> {
                    repository.startFast(
                        programId = activeProgram.id,
                        targetSeconds = activeProgram.fastHours * 3600L
                    )
                    feedbackOverlay = "Started ${activeProgram.name}" to false
                }

                fastSession.paused -> {
                    repository.resumeFast()
                    feedbackOverlay = "Session Resumed" to false
                }

                else -> {
                    repository.pauseFast()
                    feedbackOverlay = "Session Paused" to false
                }
            }
        }
    }

    fun switchBetweenFastAndEatWindow() {
        if (!fastSession.hasSession) return

        haptic.click()

        val nextEatingWindow = !fastSession.isEatingWindow

        val targetSeconds = if (nextEatingWindow) {
            activeProgram.eatHours * 3600L
        } else {
            activeProgram.fastHours * 3600L
        }

        scope.launch {
            repository.switchFastPhase(
                eatingWindow = nextEatingWindow,
                targetSeconds = targetSeconds
            )
        }

        feedbackOverlay = if (nextEatingWindow) {
            "Eating Window (${activeProgram.eatHours}h)"
        } else {
            "Fasting Window (${activeProgram.fastHours}h)"
        } to false
    }

    fun previousOrRestartPhase() {
        if (!fastSession.hasSession) return

        haptic.click()

        scope.launch {
            if (elapsedSessionSeconds > 60) {
                repository.restartFastPhase()
                feedbackOverlay = "Restarted Current Window" to false
            } else {
                val nextEatingWindow = !fastSession.isEatingWindow

                repository.switchFastPhase(
                    eatingWindow = nextEatingWindow,
                    targetSeconds = if (nextEatingWindow) {
                        activeProgram.eatHours * 3600L
                    } else {
                        activeProgram.fastHours * 3600L
                    }
                )
            }
        }
    }
    fun stopAndLogSession() {
        haptic.click()

        if (!fastSession.hasSession) return

        val stoppedAtMs = System.currentTimeMillis()

        val totalDurationMinutes = (
                fastSession.totalSessionElapsedSeconds(stoppedAtMs) / 60L
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val currentPhaseCompleted =
            fastSession.elapsedSeconds(stoppedAtMs) >= fastSession.targetSeconds

        if (totalDurationMinutes > 0) {
            val record = FastingSessionRecord(
                id = "fh-${System.currentTimeMillis()}",
                programName = activeProgram.name,
                phaseLabel = "Full session",
                durationMinutes = totalDurationMinutes,
                completed = currentPhaseCompleted,
                timestampLabel = currentClockStamp()
            )

            fastHistory.add(0, record)
            FastingStudioPrefs.saveFastHistory(context, fastHistory)
        }

        scope.launch {
            repository.stopFast()
        }

        feedbackOverlay = "Session Stopped" to false
    }

    fun extendCurrentFast(minutes: Int = 60) {
        if (!fastSession.hasSession || fastSession.isEatingWindow) return

        haptic.click()

        scope.launch {
            repository.extendFast(minutes)
        }

        feedbackOverlay = "Extended +${minutes / 60}h" to false
    }

    fun logWaterAmount(amountMl: Int) {
        val safeAmount = amountMl.coerceIn(1, 1_500)

        haptic.click()

        val newEntry = WaterLogRecord(
            id = "wl-${System.currentTimeMillis()}",
            amountMl = safeAmount,
            timestampLabel = currentClockStamp()
        )

        waterLogs.add(0, newEntry)
        FastingStudioPrefs.saveWaterLogs(context, waterLogs)

        // Sync with repository
        scope.launch {
            repository.addWater(safeAmount)
        }

        feedbackOverlay = "+${safeAmount}ml Water Logged" to false
    }

    fun toggleMuteSound() {
        haptic.click()
        val nextMuted = !isSoundMuted
        isSoundMuted = nextMuted
        if (!nextMuted && soundMode == FastingSoundMode.OFF) {
            soundMode = FastingSoundMode.ZEN_432
            FastingStudioPrefs.saveSoundMode(context, soundMode)
        }
        FastingStudioPrefs.saveMuted(context, nextMuted)
        feedbackOverlay = (if (nextMuted) "Sound Muted" else "Sound: ${soundMode.label}") to false
    }

    fun saveProgramPreset(name: String, fastHrsStr: String, eatHrsStr: String) {
        val cleanName = name.trim().take(32)
        if (cleanName.isBlank()) {
            feedbackOverlay = "Enter Program Name" to true
            haptic.click()
            return
        }
        val fHrs = fastHrsStr.trim().toIntOrNull()
        val eHrs = eatHrsStr.trim().toIntOrNull()

        if (fHrs == null || fHrs !in 1..72) {
            feedbackOverlay = "Fasting hours must be 1–72" to true
            return
        }

        if (eHrs == null || eHrs !in 1..24) {
            feedbackOverlay = "Eating hours must be 1–24" to true
            return
        }
        val editId = editingProgramId

        if (editId != null) {
            val idx = programs.indexOfFirst { it.id == editId }
            if (idx >= 0) {
                val updated = programs[idx].copy(
                    name = cleanName,
                    fastHours = fHrs,
                    eatHours = eHrs,
                    subtitle = "${fHrs}h fast · ${eHrs}h eat"
                )
                programs[idx] = updated
                selectProgram(updated)
            }
            feedbackOverlay = "Program Updated" to false
        } else {
            val created = FastingProgramPreset(
                id = "prog-${System.currentTimeMillis()}",
                name = cleanName,
                fastHours = fHrs,
                eatHours = eHrs,
                subtitle = "${fHrs}h fast · ${eHrs}h eat"
            )
            programs.add(0, created)
            selectProgram(created)
            feedbackOverlay = "Program Added" to false
        }
        FastingStudioPrefs.savePrograms(context, programs)
        showProgramEditor = false
        editingProgramId = null
        haptic.click()
    }

    fun deleteProgramPreset(id: String) {
        if (fastSession.hasSession && fastSession.programId == id) {
            feedbackOverlay = "Cannot delete the active session program" to true
            return
        }
        if (programs.size <= 1) {
            feedbackOverlay = "Keep at least 1 program" to true
            return
        }
        programs.removeAll { it.id == id }
        FastingStudioPrefs.savePrograms(context, programs)
        if (selectedProgramId == id) {
            programs.firstOrNull()?.let { selectProgram(it) }
        }
        showProgramEditor = false
        editingProgramId = null
        feedbackOverlay = "Program Removed" to false
        haptic.click()
    }

    WearBaselineViewport {
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
                    // PAGE 0: FULL-SCREEN BOUNDARY PROGRESS & TRANSPORT CONTROLS (FAST + WATER)
                    // =========================================================================
                    0 -> ActiveFastingAndWaterBoundaryPage(
                        spec = adaptiveSpec,
                        boundaryMode = boundaryMode,
                        activeProgram = activeProgram,
                        isSessionActive = isAnyPhaseActive,
                        isSessionPaused = isSessionPaused,
                        isEatingBreakWindow = isEatingBreakWindow,
                        fastingProgress = fastingProgress,
                        elapsedSeconds = elapsedSessionSeconds,
                        remainingSeconds = remainingPhaseSeconds,
                        waterMl = effectiveWaterMl,
                        waterTargetMl = waterTargetMl,
                        waterProgress = waterProgress,
                        soundMode = soundMode,
                        isSoundMuted = isSoundMuted,
                        onToggleBoundaryMode = {
                            haptic.click()
                            boundaryMode = if (boundaryMode == BoundaryDisplayMode.FASTING) {
                                BoundaryDisplayMode.WATER
                            } else {
                                BoundaryDisplayMode.FASTING
                            }
                        },
                        onCycleProgram = {
                            val idx = programs.indexOfFirst { it.id == activeProgram.id }.coerceAtLeast(0)
                            selectProgram(programs[(idx + 1) % programs.size])
                        },
                        onTogglePlayPause = {
                            val wasInactive = !fast.active && !isEatingBreakWindow
                            togglePlayPauseSession()
                            if (wasInactive) {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            }
                        },
                        onPreviousOrRestart = ::previousOrRestartPhase,
                        onNextPhaseSwitch = ::switchBetweenFastAndEatWindow,
                        onStopSession = ::stopAndLogSession,
                        onExtendFast = { extendCurrentFast(60) },
                        onLogWaterQuick = { logWaterAmount(250) },
                        onToggleMute = ::toggleMuteSound,
                        onOpenStudioPage = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onSelectSoundMode = { mode ->
                            haptic.click()
                            soundMode = mode
                            isSoundMuted = (mode == FastingSoundMode.OFF)
                            FastingStudioPrefs.saveSoundMode(context, mode)
                            FastingStudioPrefs.saveMuted(context, isSoundMuted)
                            feedbackOverlay = "Sound: ${mode.label}" to false
                        }
                    )

                    // =========================================================================
                    // PAGE 1: FASTING STUDIO, CUSTOM PROGRAMS, SOUNDS, WATER & HEATMAP
                    // =========================================================================
                    1 -> FastingAndHydrationStudioPage(
                        spec = adaptiveSpec,
                        state = liveState,
                        activeProgram = activeProgram,
                        programs = programs,
                        isSessionActive = isAnyPhaseActive,
                        isSessionPaused = isSessionPaused,
                        isEatingBreakWindow = isEatingBreakWindow,
                        fastingProgress = fastingProgress,
                        elapsedSeconds = elapsedSessionSeconds,
                        remainingSeconds = remainingPhaseSeconds,
                        waterMl = effectiveWaterMl,
                        waterTargetMl = waterTargetMl,
                        waterProgress = waterProgress,
                        waterLogs = waterLogs,
                        fastHistory = fastHistory,
                        soundMode = soundMode,
                        isSoundMuted = isSoundMuted,
                        showProgramEditor = showProgramEditor,
                        isEditingProgram = editingProgramId != null,
                        draftProgramName = draftProgramName,
                        draftFastHours = draftFastHours,
                        draftEatHours = draftEatHours,
                        onTogglePlayPause = ::togglePlayPauseSession,
                        onPreviousOrRestart = ::previousOrRestartPhase,
                        onNextPhaseSwitch = ::switchBetweenFastAndEatWindow,
                        onStopSession = ::stopAndLogSession,
                        onExtendFast = { extendCurrentFast(60) },
                        onSelectProgram = ::selectProgram,
                        onStartProgramNow = { preset ->
                            if (fastSession.hasSession) {
                                feedbackOverlay = "Stop the current session before starting another" to true
                            } else {
                                scope.launch {
                                    selectedProgramId = preset.id
                                    FastingStudioPrefs.saveActiveProgramId(context, preset.id)

                                    repository.startFast(
                                        programId = preset.id,
                                        targetSeconds = preset.fastHours * 3600L
                                    )

                                    feedbackOverlay = "Started ${preset.name}" to false
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        },
                        onOpenNewProgramEditor = {
                            haptic.click()
                            editingProgramId = null
                            draftProgramName = ""
                            draftFastHours = "16"
                            draftEatHours = "8"
                            showProgramEditor = true
                        },
                        onOpenEditProgramEditor = { preset ->
                            haptic.click()
                            editingProgramId = preset.id
                            draftProgramName = preset.name
                            draftFastHours = preset.fastHours.toString()
                            draftEatHours = preset.eatHours.toString()
                            showProgramEditor = true
                        },
                        onSaveProgram = ::saveProgramPreset,
                        onCancelProgramEditor = {
                            haptic.click()
                            showProgramEditor = false
                            editingProgramId = null
                        },
                        onDeleteProgram = {
                            editingProgramId?.let { deleteProgramPreset(it) }
                        },
                        onToggleMute = ::toggleMuteSound,
                        onLogWater = ::logWaterAmount,
                        onAdjustWaterTarget = { delta ->
                            haptic.click()
                            waterTargetMl = (waterTargetMl + delta).coerceIn(1000, 5000)
                            FastingStudioPrefs.saveWaterTarget(context, waterTargetMl)
                        },
                        onDeleteWaterLog = { id ->
                            haptic.click()
                            waterLogs.removeAll { it.id == id }
                            FastingStudioPrefs.saveWaterLogs(context, waterLogs)
                        },
                        onRepeatFastHistory = { record ->
                            val matched = programs.firstOrNull {
                                it.name.equals(record.programName, ignoreCase = true)
                            }

                            if (matched != null) {
                                scope.launch {
                                    if (fastSession.hasSession) {
                                        repository.stopFast()
                                    }

                                    selectedProgramId = matched.id
                                    FastingStudioPrefs.saveActiveProgramId(context, matched.id)

                                    repository.startFast(
                                        programId = matched.id,
                                        targetSeconds = matched.fastHours * 3600L
                                    )

                                    feedbackOverlay = "Restarted ${matched.name}" to false
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        },
                        onDeleteFastHistory = { id ->
                            haptic.click()
                            fastHistory.removeAll { it.id == id }
                            FastingStudioPrefs.saveFastHistory(context, fastHistory)
                            feedbackOverlay = "Log Removed" to false
                        },
                        onJumpToRing = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onJumpToDiet = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    // =========================================================================
                    // PAGE 2: DIET LOGGING, MACRO TARGETS & CUSTOM DIET PLANS
                    // =========================================================================
                    2 -> DietAndNutritionPage(
                        spec = adaptiveSpec,
                        foodLogs = visibleFoodLogs,
                        todayFoodLogs = todayFoodLogs,
                        dietPlans = dietPlans,
                        activeDietPlan = activeDietPlan,
                        showMealEditor = showMealEditor,
                        selectedMealType = selectedMealType,
                        showDietTargetEditor = showDietTargetEditor,
                        onSelectDietPlan = { plan ->
                            haptic.click()
                            selectedDietId = plan.id
                            FastingStudioPrefs.saveActiveDietId(context, plan.id)
                            feedbackOverlay = "Diet: ${plan.name}" to false
                        },
                        onSelectMealType = {
                            haptic.click()
                            selectedMealType = it
                        },
                        onToggleMealEditor = {
                            haptic.click()
                            showMealEditor = !showMealEditor
                            showDietTargetEditor = false
                        },
                        onSaveMeal = { mealName, kcalStr, macrosStr ->
                            val cleanName = mealName.trim().ifBlank { selectedMealType.name }
                            val kcal = kcalStr.filter { it.isDigit() }.toIntOrNull()?.coerceIn(10, 4000) ?: 450
                            val nums = Regex("""\d+""").findAll(macrosStr).mapNotNull { it.value.toDoubleOrNull() }.toList()
                            val proteinG = nums.getOrNull(0) ?: 30.0
                            val carbsG = nums.getOrNull(1) ?: 45.0
                            val fatG = nums.getOrNull(2) ?: 15.0

                            scope.launch {
                                repository.quickFood(
                                    name = cleanName,
                                    mealType = selectedMealType,
                                    calories = kcal,
                                    proteinGrams = proteinG,
                                    carbsGrams = carbsG,
                                    fatGrams = fatG
                                )
                            }
                            showMealEditor = false
                            feedbackOverlay = "Logged $cleanName (${kcal} kcal)" to false
                            haptic.click()
                        },
                        onToggleDietTargetEditor = {
                            haptic.click()
                            showDietTargetEditor = !showDietTargetEditor
                            showMealEditor = false
                        },
                        onSaveCustomDietPlan = { planName, kcalStr, macrosStr ->
                            val clean = planName.trim().ifBlank { "Custom Macro Plan" }
                            val kcal = kcalStr.filter { it.isDigit() }.toIntOrNull()?.coerceIn(800, 6000) ?: 2200
                            val nums = Regex("""\d+""").findAll(macrosStr).mapNotNull { it.value.toIntOrNull() }.toList()
                            val pG = nums.getOrNull(0) ?: 150
                            val cG = nums.getOrNull(1) ?: 200
                            val fG = nums.getOrNull(2) ?: 70

                            val newPlan = DietPlanPreset(
                                id = "diet-${System.currentTimeMillis()}",
                                name = clean,
                                targetCalories = kcal,
                                targetProteinG = pG,
                                targetCarbsG = cG,
                                targetFatG = fG
                            )
                            dietPlans.add(0, newPlan)
                            selectedDietId = newPlan.id
                            FastingStudioPrefs.saveDietPlans(context, dietPlans)
                            FastingStudioPrefs.saveActiveDietId(context, newPlan.id)
                            showDietTargetEditor = false
                            feedbackOverlay = "Saved $clean" to false
                            haptic.click()
                        },
                        onDeleteFood = { id ->
                            haptic.click()
                            scope.launch { repository.deleteFood(id) }
                            feedbackOverlay = "Meal Deleted" to false
                        },
                        onJumpToVitals = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )

                    // =========================================================================
                    // PAGE 3: PROFESSIONAL VITALS TELEMETRY, SENSORS & WEIGHT GRAPH
                    // =========================================================================
                    else -> BodyVitalsAndSensorsPage(
                        spec = adaptiveSpec,
                        currentWeightKg = savedWeight.takeIf { it > 0.0 } ?: (weightHistory.lastOrNull()?.weightKg ?: 0.0),
                        weightTargetKg = weightTargetKg,
                        weightHistory = weightHistory,
                        heartRateBpm = liveSensorHeartRate.takeIf { it > 0.0 } ?: 0.0,
                        heartRateAccuracy = liveSensorAccuracy,
                        steps = liveState.wellness.steps.coerceAtLeast(0),
                        sleepMinutes = liveState.wellness.sleepLastNightMinutes.coerceAtLeast(0),
                        mood = liveState.wellness.mood,
                        energy = liveState.wellness.energy,
                        isScanningVitals = isScanningVitals,
                        showWeightEditor = showWeightEditor,
                        onTriggerLiveSensorScan = {
                            haptic.click()

                            if (!hasBodySensorsPermission) {
                                heartRatePermissionLauncher.launch(
                                    Manifest.permission.BODY_SENSORS
                                )
                            } else {
                                isScanningVitals = true

                                scope.launch {
                                    delay(1_500L)

                                    isScanningVitals = false

                                    val isFresh =
                                        lastHeartRateAtMs > 0L &&
                                                System.currentTimeMillis() - lastHeartRateAtMs < 15_000L

                                    if (liveSensorHeartRate > 35.0 && isFresh) {
                                        feedbackOverlay =
                                            "${liveSensorHeartRate.roundToInt()} BPM · Live reading" to false
                                    } else {
                                        feedbackOverlay =
                                            "No fresh heart-rate reading" to true
                                    }
                                }
                            }
                        },
                        onLogStepsQuick = {
                            haptic.click()
                            feedbackOverlay = "Steps are tracked automatically" to false
                        },
                        onToggleWeightEditor = {
                            haptic.click()
                            showWeightEditor = !showWeightEditor
                        },
                        onSaveWeightAndTarget = saveWeight@{ currentKgStr, targetKgStr, moodNote ->
                            val curKg = currentKgStr.trim().toDoubleOrNull()
                            val tgtKg = targetKgStr.trim().toDoubleOrNull()

                            if (curKg == null || curKg !in 25.0..250.0) {
                                feedbackOverlay = "Enter a valid current weight" to true
                                return@saveWeight
                            }

                            if (tgtKg == null || tgtKg !in 25.0..250.0) {
                                feedbackOverlay = "Enter a valid target weight" to true
                                return@saveWeight
                            }

                            weightTargetKg = tgtKg
                            FastingStudioPrefs.saveWeightTarget(context, tgtKg)

                            val pointDateLabel = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.ROOT))
                            val point = WeightLogPoint(
                                id = "w-${System.currentTimeMillis()}",
                                weightKg = curKg,
                                dateLabel = pointDateLabel
                            )

                            weightHistory.add(point)
                            FastingStudioPrefs.saveWeightHistory(context, weightHistory)

                            scope.launch {
                                repository.updateWellness(
                                    mood = moodNote.takeIf { it.isNotBlank() },
                                    weight = curKg
                                )
                            }

                            showWeightEditor = false
                            feedbackOverlay =
                                "Weight ${"%.1f".format(Locale.ROOT, curKg)}kg Logged" to false

                            haptic.click()
                        },
                        onDeleteWeightPoint = { id ->
                            if (weightHistory.size > 2) {
                                haptic.click()
                                weightHistory.removeAll { it.id == id }
                                FastingStudioPrefs.saveWeightHistory(context, weightHistory)
                            }
                        },
                        onJumpToTop = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(0) }
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

            // In-Viewport Constrained Curved Confirmation Overlay
            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedFastingConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }
        }
    }
}


// =============================================================================
// PAGE 0: FULL-SCREEN BOUNDARY CIRCULAR PROGRESS & TRANSPORT CONTROLS CANVAS
// =============================================================================
@Composable
private fun ActiveFastingAndWaterBoundaryPage(
    spec: WearAdaptiveSpec,
    boundaryMode: BoundaryDisplayMode,
    activeProgram: FastingProgramPreset,
    isSessionActive: Boolean,
    isSessionPaused: Boolean,
    isEatingBreakWindow: Boolean,
    fastingProgress: Float,
    elapsedSeconds: Int,
    remainingSeconds: Int,
    waterMl: Int,
    waterTargetMl: Int,
    waterProgress: Float,
    soundMode: FastingSoundMode,
    isSoundMuted: Boolean,
    onToggleBoundaryMode: () -> Unit,
    onCycleProgram: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextPhaseSwitch: () -> Unit,
    onStopSession: () -> Unit,
    onExtendFast: () -> Unit,
    onLogWaterQuick: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenStudioPage: () -> Unit,
    onSelectSoundMode: (FastingSoundMode) -> Unit
) {
    var showSoundPicker by remember { mutableStateOf(false) }
    val isWaterMode = boundaryMode == BoundaryDisplayMode.WATER
    val activeProgress = if (isWaterMode) waterProgress else fastingProgress

    val animatedProgress by animateFloatAsState(
        targetValue = activeProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fastWaterBoundaryProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fastWavePhase")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "fastWaveAnim"
    )

    val primaryAccent = when {
        isWaterMode -> Color(0xFF68B6FF)
        isEatingBreakWindow -> Color(0xFF79E2B0)
        isSessionActive && isSessionPaused -> Color(0xFFFFD177)
        isSessionActive -> Color(0xFFFF9A7A)
        else -> MaterialTheme.colorScheme.primary
    }

    val centerButtonSize = (spec.screenWidth * 0.32f).coerceIn(58.dp, 74.dp)
    val sideButtonWidth = (spec.screenWidth * 0.21f).coerceIn(42.dp, 52.dp)
    val sideButtonHeight = (spec.screenWidth * 0.25f).coerceIn(48.dp, 60.dp)
    val bottomPillHeight = (spec.screenHeight * 0.14f).coerceIn(40.dp, 44.dp)

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
            // 1. FULL-SCREEN RADIAL BACKDROP INSIDE BOUNDARY RING
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isSessionActive && !isSessionPaused) 0.28f else 0.15f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isWaterMode -> Icons.Rounded.WaterDrop
                        isEatingBreakWindow -> Icons.Rounded.Restaurant
                        else -> Icons.Rounded.LocalFireDepartment
                    },
                    contentDescription = null,
                    modifier = Modifier.size(spec.screenWidth * 0.54f),
                    tint = primaryAccent.copy(alpha = 0.07f)
                )
            }

            // 2. FULL-SCREEN BOUNDARY CIRCULAR PROGRESS CONTAINER
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
            val secondaryRingColor = if (isWaterMode) Color(0xFFFF9A7A).copy(alpha = 0.45f) else Color(0xFF68B6FF).copy(alpha = 0.45f)
            val secondaryProgress = if (isWaterMode) fastingProgress else waterProgress

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val outerStroke = 5.5.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
                // Subtle inner secondary ring showing the companion metric (Water vs Fast)
                val inset = 9.dp.toPx()
                drawArc(
                    color = secondaryRingColor,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    startAngle = -90f,
                    sweepAngle = 360f * secondaryProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. FOREGROUND EXPRESSIVE TRANSPORT & TELEMETRY CONTROLS
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
                // Top Slot: Program / Mode Toggle Pill + Primary Readout + Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Program / Topic Selector Pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
                                .clickable(onClick = onCycleProgram)
                                .heightIn(min = 40.dp)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment
                                    .CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isEatingBreakWindow) Icons.Rounded.Restaurant else Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = primaryAccent
                            )
                            Text(
                                text = activeProgram.name,
                                style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent,
                                maxLines = 1
                            )
                        }

                        // Ring Mode Switcher Pill (Fast <-> Water)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isWaterMode) Color(0xFF68B6FF).copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable(onClick = onToggleBoundaryMode),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = "Switch Fast and Water Ring",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF68B6FF)
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // Main Digital Readout
                    Text(
                        text = if (isWaterMode) {
                            "${waterMl}ml"
                        } else if (isSessionActive) {
                            formatDurationClock(remainingSeconds)
                        } else {
                            "${activeProgram.fastHours}h : ${activeProgram.eatHours}h"
                        },
                        style = MaterialTheme.typography.displaySmall.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.clickable(onClick = onToggleBoundaryMode)
                    )

                    Text(
                        text = when {
                            isWaterMode -> "GOAL ${waterTargetMl}ML · ${(waterProgress * 100).roundToInt()}% HYDRATED"
                            isSessionActive && isSessionPaused -> "PAUSED · ELAPSED ${formatHoursMinsShort(elapsedSeconds / 60)}"
                            isEatingBreakWindow -> "EATING WINDOW · ${formatHoursMinsShort(elapsedSeconds / 60)} ELAPSED"
                            isSessionActive -> "FASTING · ${(fastingProgress * 100).roundToInt()}% · ${waterMl}ML H2O"
                            else -> "READY · ${waterMl}/${waterTargetMl}ML WATER"
                        },
                        style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Universal PlayerControlBar with Snooze & Stop actions
                com.creativeblisslabs.wearproductivity.wear.ui.components.core.PlayerControlBar(
                    isPlaying = isSessionActive && !isSessionPaused,
                    progress = animatedProgress,
                    onTogglePlay = onTogglePlayPause,
                    onPrevious = onExtendFast,
                    onNext = onStopSession,
                    previousIcon = Icons.Rounded.Snooze,
                    nextIcon = Icons.Rounded.Stop
                )

                // Bottom Slot: Mute/Sound Toggle | +250ml / +1h Action | Stop or Studio Deck
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.05f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Mute / Enable Ambient Sound Pill
                    val soundActive = !isSoundMuted && soundMode != FastingSoundMode.OFF
                    Button(
                        onClick = onToggleMute,
                        modifier = Modifier.height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (soundActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = if (soundActive) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = "Toggle Sound",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick +250ml Water Pill (or +1h Fast when in Water Mode)
                    Button(
                        onClick = if (isWaterMode) onExtendFast else onLogWaterQuick,
                        modifier = Modifier
                            .height(bottomPillHeight)
                            .semantics { contentDescription = if (isWaterMode) "Extend Fast 1 Hour" else "Log 250ml Water" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isWaterMode) Icons.Rounded.Add else Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (isWaterMode) "+1h" else "+250",
                                style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Stop & Log Session (when active) or Open Studio (when idle)
                    if (isSessionActive) {
                        Button(
                            onClick = onStopSession,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = "Stop and Log Fast" },
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
                            onClick = onOpenStudioPage,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = "Open Fasting Studio" },
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
                // Add at the bottom of ActiveFastingAndWaterBoundaryPage's Box:
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSoundPicker,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)) {
                        val listState = rememberTransformingLazyColumnState()
                        TransformingLazyColumn(state = listState, contentPadding = PaddingValues(28.dp), modifier = Modifier.fillMaxSize()) {
                            item { WearExpressiveHeader(title = "FASTING SOUND", badgeText = "SELECT") }
                            items(FastingSoundMode.entries.size) { idx ->
                                val mode = FastingSoundMode.entries[idx]
                                Card(
                                    onClick = {
                                        onSelectSoundMode(mode)
                                        showSoundPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Text(mode.label, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawFastingWavyRing(
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
// PAGE 1: FASTING STUDIO, CUSTOM PROGRAMS, HYDRATION TARGETS & HEATMAP
// =============================================================================
@Composable
private fun FastingAndHydrationStudioPage(
    spec: WearAdaptiveSpec,
    state: LifeState,
    activeProgram: FastingProgramPreset,
    programs: List<FastingProgramPreset>,
    isSessionActive: Boolean,
    isSessionPaused: Boolean,
    isEatingBreakWindow: Boolean,
    fastingProgress: Float,
    elapsedSeconds: Int,
    remainingSeconds: Int,
    waterMl: Int,
    waterTargetMl: Int,
    waterProgress: Float,
    waterLogs: List<WaterLogRecord>,
    fastHistory: List<FastingSessionRecord>,
    soundMode: FastingSoundMode,
    isSoundMuted: Boolean,
    showProgramEditor: Boolean,
    isEditingProgram: Boolean,
    draftProgramName: String,
    draftFastHours: String,
    draftEatHours: String,
    onTogglePlayPause: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextPhaseSwitch: () -> Unit,
    onStopSession: () -> Unit,
    onExtendFast: () -> Unit,
    onSelectProgram: (FastingProgramPreset) -> Unit,
    onStartProgramNow: (FastingProgramPreset) -> Unit,
    onOpenNewProgramEditor: () -> Unit,
    onOpenEditProgramEditor: (FastingProgramPreset) -> Unit,
    onSaveProgram: (String, String, String) -> Unit,
    onCancelProgramEditor: () -> Unit,
    onDeleteProgram: () -> Unit,
    onToggleMute: () -> Unit,
    onLogWater: (Int) -> Unit,
    onAdjustWaterTarget: (Int) -> Unit,
    onDeleteWaterLog: (String) -> Unit,
    onRepeatFastHistory: (FastingSessionRecord) -> Unit,
    onDeleteFastHistory: (String) -> Unit,
    onJumpToRing: () -> Unit,
    onJumpToDiet: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToRing,
                label = "Progress Ring",
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "FAST & H2O",
                    badgeText = "${waterMl}ML · ${activeProgram.fastHours}:${activeProgram.eatHours}"
                )
            }



            item {
                WearExpressiveHorizontalSlider(
                    title = "Daily Water Goal",
                    valueText = "$waterTargetMl ml",
                    onValueDecrease = { onAdjustWaterTarget(-250) },
                    onValueIncrease = { onAdjustWaterTarget(250) },
                    canDecrease = waterTargetMl > 1000,
                    canIncrease = waterTargetMl < 5000
                )
            }

            item {
                Card(
                    onClick = { onLogWater(250) },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LOGGED TODAY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF68B6FF), fontWeight = FontWeight.Bold)
                                Text("$waterMl / $waterTargetMl ml", style = MaterialTheme.typography.titleSmall.tabularNums(), fontWeight = FontWeight.Bold)
                            }
                            Text("${(waterProgress * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall.tabularNums(), color = Color(0xFF68B6FF), fontWeight = FontWeight.Black)
                        }
                        LinearProgressIndicator(
                            progress = { waterProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onLogWater(250) },
                                modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("+250ml", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { onLogWater(500) },
                                modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("+500ml", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { onLogWater(750) },
                                modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("+750ml", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 3. CUSTOM FASTING PROGRAMS (CREATE, EDIT, DELETE, START)
            item {
                CatalogM3ListHeaderSample(title = "FASTING PROGRAMS")
            }

            item {
                FilledTonalButton(
                    onClick = if (showProgramEditor) onCancelProgramEditor else onOpenNewProgramEditor,
                    icon = { Icon(if (showProgramEditor) Icons.Default.Close else Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showProgramEditor) "Close Program Editor" else "Create Custom Program")
                }
            }

            if (showProgramEditor) {
                item {
                    val initialFast = remember(draftFastHours) {
                        draftFastHours.toIntOrNull() ?: 16
                    }
                    val initialEat = remember(draftEatHours) {
                        draftEatHours.toIntOrNull() ?: 8
                    }
                    var fastHours by remember { mutableStateOf(initialFast) }
                    var eatHours by remember { mutableStateOf(initialEat) }

                    val protocolChips = listOf("16:8 Lean", "18:6 Inter", "20:4 Warrior", "OMAD 23:1", "Circadian 13:11")
                    var selectedProtocolChip by remember { mutableStateOf(0) }

                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "FASTING PLAN",
                        initialTitle = draftProgramName,
                        initialTimeOrDate = draftFastHours,
                        initialSubtitle = draftEatHours,
                        isEditingExisting = isEditingProgram,
                        showTimePickerField = false,
                        showSubtitleField = false,
                        chipOptions = protocolChips,
                        selectedChipIndex = selectedProtocolChip,
                        onChipSelected = { idx ->
                            selectedProtocolChip = idx
                            when (idx) {
                                0 -> { fastHours = 16; eatHours = 8 }
                                1 -> { fastHours = 18; eatHours = 6 }
                                2 -> { fastHours = 20; eatHours = 4 }
                                3 -> { fastHours = 23; eatHours = 1 }
                                4 -> { fastHours = 13; eatHours = 11 }
                            }
                        },
                        discreteSubFields = listOf(
                            DiscreteNumberSubField(
                                key = "fast",
                                label = "Fasting Window",
                                initialValue = initialFast,
                                minimum = 8,
                                maximum = 72,
                                step = 1,
                                unitSuffix = "hrs",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "eat",
                                label = "Eating Window",
                                initialValue = initialEat,
                                minimum = 1,
                                maximum = 16,
                                step = 1,
                                unitSuffix = "hrs",
                                useSlider = true
                            )
                        ),
                        onSubFieldsChanged = { values ->
                            values["fast"]?.let { fastHours = it }
                            values["eat"]?.let { eatHours = it }
                        },
                        onSave = { name, _, _ ->
                            val defaultName = protocolChips.getOrElse(selectedProtocolChip) { "Fast $fastHours:$eatHours" }
                            val effectiveName = if (name.isBlank()) defaultName else name
                            onSaveProgram(effectiveName, fastHours.toString(), eatHours.toString())
                        },
                        onCancel = onCancelProgramEditor,
                        onDelete = if (isEditingProgram) onDeleteProgram else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(programs.size) { idx ->
                val preset = programs[idx]
                val isSelected = preset.id == activeProgram.id
                M3FastingProgramRow(
                    preset = preset,
                    isSelected = isSelected,
                    onSelect = { onSelectProgram(preset) },
                    onStartNow = { onStartProgramNow(preset) },
                    onEdit = { onOpenEditProgramEditor(preset) }
                )
            }

            // 5. 28-DAY CONSISTENCY HEATMAP & SESSION LOGS
            item {
                CatalogM3ListHeaderSample(title = "CONSISTENCY & LOG HISTORY")
            }


            items(fastHistory.take(8).size) { idx ->
                val rec = fastHistory[idx]
                WearSwipeableActionCard(
                    onCardClick = { onRepeatFastHistory(rec) },
                    onEditClick = { onRepeatFastHistory(rec) },
                    onDeleteClick = { onDeleteFastHistory(rec.id) },
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
                            Text(
                                text = "${rec.programName} (${rec.phaseLabel})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatHoursMinsShort(rec.durationMinutes)} · ${rec.timestampLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { onDeleteFastHistory(rec.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Fast Log",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (waterLogs.isNotEmpty()) {
                item {
                    CatalogM3ListHeaderSample(title = "WATER TIMELINE (${waterLogs.size})")
                }
                items(waterLogs.take(6).size) { idx ->
                    val wLog = waterLogs[idx]
                    Card(
                        onClick = { onDeleteWaterLog(wLog.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+${wLog.amountMl} ml Water", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(wLog.timestampLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.M3FastingProgramRow(
    preset: FastingProgramPreset,
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
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable(onClick = onStartNow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Start ${preset.name}",
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${preset.fastHours}h fast · ${preset.eatHours}h eat · ${preset.subtitle}",
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
                Icon(Icons.Rounded.Edit, contentDescription = "Edit Program", modifier = Modifier.size(14.dp))
            }
        }
    }
}

// =============================================================================
// PAGE 2: DIET LOGGING, MACRO CONSUMPTION TARGETS & CUSTOM DIET PLANS
// =============================================================================
@Composable
private fun DietAndNutritionPage(
    spec: WearAdaptiveSpec,
    todayFoodLogs: List<FoodLog>,
    foodLogs: List<FoodLog>,
    dietPlans: List<DietPlanPreset>,
    activeDietPlan: DietPlanPreset,
    showMealEditor: Boolean,
    selectedMealType: MealType,
    showDietTargetEditor: Boolean,
    onSelectDietPlan: (DietPlanPreset) -> Unit,
    onSelectMealType: (MealType) -> Unit,
    onToggleMealEditor: () -> Unit,
    onSaveMeal: (String, String, String) -> Unit,
    onToggleDietTargetEditor: () -> Unit,
    onSaveCustomDietPlan: (String, String, String) -> Unit,
    onDeleteFood: (String) -> Unit,
    onJumpToVitals: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    val loggedCalories = todayFoodLogs.sumOf { it.calories }
    val loggedProtein = todayFoodLogs.sumOf { it.proteinGrams }.roundToInt()
    val loggedCarbs = todayFoodLogs.sumOf { it.carbsGrams }.roundToInt()
    val loggedFat = todayFoodLogs.sumOf { it.fatGrams }.roundToInt()

    val calProgress = (loggedCalories.toFloat() / activeDietPlan.targetCalories.coerceAtLeast(500)).coerceIn(0f, 1f)
    val proProgress = (loggedProtein.toFloat() / activeDietPlan.targetProteinG.coerceAtLeast(20)).coerceIn(0f, 1f)
    val carbProgress = (loggedCarbs.toFloat() / activeDietPlan.targetCarbsG.coerceAtLeast(20)).coerceIn(0f, 1f)
    val fatProgress = (loggedFat.toFloat() / activeDietPlan.targetFatG.coerceAtLeast(15)).coerceIn(0f, 1f)

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onToggleMealEditor,
                label = if (showMealEditor) "Close" else "Log Meal",
                icon = if (showMealEditor) Icons.Default.Close else Icons.Default.Add
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
                .background(MaterialTheme.colorScheme.background)   ,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "DIET",
                    badgeText = "$loggedCalories / ${activeDietPlan.targetCalories} KCAL"
                )
            }

            // 1. DAILY CONSUMPTION TARGETS & MACRO PROGRESS CARD
            item {
                Card(
                    onClick = onToggleDietTargetEditor,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeDietPlan.name.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$loggedCalories of ${activeDietPlan.targetCalories} kcal",
                                    style = MaterialTheme.typography.titleSmall.tabularNums(),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Icon(Icons.Rounded.Settings, contentDescription = "Customize Diet Plan",
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }

                        LinearProgressIndicator(
                            progress = { calProgress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3-Column Macro Breakdown Bars (Protein, Carbs, Fat)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MacroMiniColumn(
                                modifier = Modifier.weight(1f),
                                label = "PRO",
                                value = "${loggedProtein}/${activeDietPlan.targetProteinG}g",
                                progress = proProgress,
                                accent = Color(0xFF79E2B0)
                            )
                            MacroMiniColumn(
                                modifier = Modifier.weight(1f),
                                label = "CARB",
                                value = "${loggedCarbs}/${activeDietPlan.targetCarbsG}g",
                                progress = carbProgress,
                                accent = Color(0xFF8EAEFF)
                            )
                            MacroMiniColumn(
                                modifier = Modifier.weight(1f),
                                label = "FAT",
                                value = "${loggedFat}/${activeDietPlan.targetFatG}g",
                                progress = fatProgress,
                                accent = Color(0xFFFFB76B)
                            )
                        }
                    }
                }
            }

            // 2. DIET PLAN SELECTOR & CUSTOM DIET PLAN CREATOR
            item {
                WearActionButtonGroup(
                    options = dietPlans.take(4).map { it.name.take(9) },
                    selectedIndex = dietPlans.take(4).indexOfFirst { it.id == activeDietPlan.id }.coerceAtLeast(0),
                    onOptionSelected = { idx ->
                        dietPlans.getOrNull(idx)?.let { onSelectDietPlan(it) }
                    }
                )
            }

            if (showDietTargetEditor) {
                item {
                    val initialKcal = remember(activeDietPlan) { activeDietPlan.targetCalories }
                    val initialP = remember(activeDietPlan) { activeDietPlan.targetProteinG }
                    val initialC = remember(activeDietPlan) { activeDietPlan.targetCarbsG }
                    val initialF = remember(activeDietPlan) { activeDietPlan.targetFatG }

                    var kcalTarget by remember { mutableStateOf(initialKcal) }
                    var proteinTarget by remember { mutableStateOf(initialP) }
                    var carbsTarget by remember { mutableStateOf(initialC) }
                    var fatTarget by remember { mutableStateOf(initialF) }

                    val dietGoalChips = listOf("Cut (-20%)", "Maintain", "Bulk (+15%)", "Keto", "High Protein")
                    var selectedGoalIdx by remember { mutableStateOf(1) }

                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "DIET PLAN",
                        initialTitle = activeDietPlan.name,
                        initialTimeOrDate = "",
                        initialSubtitle = "",
                        isEditingExisting = false,
                        showTimePickerField = false,
                        showSubtitleField = false,
                        chipOptions = dietGoalChips,
                        selectedChipIndex = selectedGoalIdx,
                        onChipSelected = { selectedGoalIdx = it },
                        discreteSubFields = listOf(
                            DiscreteNumberSubField(
                                key = "kcal",
                                label = "Calories Target",
                                initialValue = initialKcal,
                                minimum = 800,
                                maximum = 5000,
                                step = 50,
                                unitSuffix = "kcal",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "protein",
                                label = "Protein Target",
                                initialValue = initialP,
                                minimum = 20,
                                maximum = 350,
                                step = 5,
                                unitSuffix = "g",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "carbs",
                                label = "Carbs Target",
                                initialValue = initialC,
                                minimum = 10,
                                maximum = 600,
                                step = 5,
                                unitSuffix = "g",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "fat",
                                label = "Fat Target",
                                initialValue = initialF,
                                minimum = 10,
                                maximum = 200,
                                step = 5,
                                unitSuffix = "g",
                                useSlider = true
                            )
                        ),
                        onSubFieldsChanged = { values ->
                            values["kcal"]?.let { kcalTarget = it }
                            values["protein"]?.let { proteinTarget = it }
                            values["carbs"]?.let { carbsTarget = it }
                            values["fat"]?.let { fatTarget = it }
                        },
                        onSave = { name, _, _ ->
                            val macrosCombined = "$proteinTarget/$carbsTarget/$fatTarget"
                            val effectiveName = if (name.isBlank()) dietGoalChips[selectedGoalIdx] else name
                            onSaveCustomDietPlan(effectiveName, kcalTarget.toString(), macrosCombined)
                        },
                        onCancel = onToggleDietTargetEditor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. MEAL LOGGING EDITOR
            if (showMealEditor) {
                item {
                    CatalogM3ListHeaderSample(title = "LOG MEAL ENTRY")
                }
                item {
                    WearActionButtonGroup(
                        options = MealType.entries.map { it.name.take(6) },
                        selectedIndex = selectedMealType.ordinal,
                        onOptionSelected = { idx -> onSelectMealType(MealType.entries[idx]) }
                    )
                }
                item {
                    var mealKcal by remember { mutableStateOf(450) }
                    var mealP by remember { mutableStateOf(30) }
                    var mealC by remember { mutableStateOf(45) }
                    var mealF by remember { mutableStateOf(14) }

                    val mealPortionChips = listOf("Snack", "Regular", "Large Feast")
                    var selectedPortionIdx by remember { mutableStateOf(1) }

                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = selectedMealType.name.uppercase(Locale.getDefault()),
                        initialTitle = "",
                        initialTimeOrDate = "",
                        initialSubtitle = "",
                        isEditingExisting = false,
                        showTimePickerField = false,
                        showSubtitleField = false,
                        chipOptions = mealPortionChips,
                        selectedChipIndex = selectedPortionIdx,
                        onChipSelected = { idx ->
                            selectedPortionIdx = idx
                            when (idx) {
                                0 -> { mealKcal = 250; mealP = 15; mealC = 25; mealF = 8 }
                                1 -> { mealKcal = 500; mealP = 35; mealC = 50; mealF = 15 }
                                2 -> { mealKcal = 900; mealP = 60; mealC = 95; mealF = 28 }
                            }
                        },
                        discreteSubFields = listOf(
                            DiscreteNumberSubField(
                                key = "meal_kcal",
                                label = "Calories",
                                initialValue = mealKcal,
                                minimum = 50,
                                maximum = 2500,
                                step = 25,
                                unitSuffix = "kcal",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "meal_p",
                                label = "Protein",
                                initialValue = mealP,
                                minimum = 0,
                                maximum = 150,
                                step = 2,
                                unitSuffix = "g",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "meal_c",
                                label = "Carbs",
                                initialValue = mealC,
                                minimum = 0,
                                maximum = 250,
                                step = 5,
                                unitSuffix = "g",
                                useSlider = true
                            ),
                            DiscreteNumberSubField(
                                key = "meal_f",
                                label = "Fat",
                                initialValue = mealF,
                                minimum = 0,
                                maximum = 100,
                                step = 2,
                                unitSuffix = "g",
                                useSlider = true
                            )
                        ),
                        onSubFieldsChanged = { values ->
                            values["meal_kcal"]?.let { mealKcal = it }
                            values["meal_p"]?.let { mealP = it }
                            values["meal_c"]?.let { mealC = it }
                            values["meal_f"]?.let { mealF = it }
                        },
                        onSave = { mealName, _, _ ->
                            val defaultName = "${selectedMealType.name.lowercase().replaceFirstChar { it.uppercase() }} (${mealPortionChips[selectedPortionIdx]})"
                            val effectiveName = if (mealName.isBlank()) defaultName else mealName
                            val macrosStr = "$mealP/$mealC/$mealF"
                            onSaveMeal(effectiveName, mealKcal.toString(), macrosStr)
                        },
                        onCancel = onToggleMealEditor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. FOOD LOGGING HISTORY
            item {
                CatalogM3ListHeaderSample(title = "FOOD LOG HISTORY (${foodLogs.size})")
            }

            if (foodLogs.isEmpty()) {
                item {
                    WearMetricCard(
                        title = "NO MEALS LOGGED",
                        value = "Tap + to Log Food",
                        subtitle = "Track calories, protein, carbs & fats",
                        icon = Icons.Default.LocalDining,
                        onClick = onToggleMealEditor
                    )
                }
            } else {
                items(foodLogs.size) { idx ->
                    val food = foodLogs[idx]
                    WearSwipeableActionCard(
                        onCardClick = {},
                        onEditClick = {},
                        onDeleteClick = { onDeleteFood(food.stamp.id) },
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
                                Text(
                                    text = "${food.name} · ${food.mealType.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${food.calories} kcal · P:${food.proteinGrams.toInt()}g C:${food.carbsGrams.toInt()}g F:${food.fatGrams.toInt()}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteFood(food.stamp.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${food.name}",
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
}

@Composable
private fun MacroMiniColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    progress: Float,
    accent: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 9.sp), color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp), fontWeight = FontWeight.SemiBold, maxLines = 1)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

// =============================================================================
// PAGE 3: PROFESSIONAL VITALS TELEMETRY, SENSORS & WEIGHT TREND GRAPH
// =============================================================================
@Composable
private fun BodyVitalsAndSensorsPage(
    spec: WearAdaptiveSpec,
    currentWeightKg: Double,
    weightTargetKg: Double,
    weightHistory: List<WeightLogPoint>,
    heartRateBpm: Double,
    heartRateAccuracy: String,
    steps: Int,
    sleepMinutes: Int,
    mood: String,
    energy: Int,
    isScanningVitals: Boolean,
    showWeightEditor: Boolean,
    onTriggerLiveSensorScan: () -> Unit,
    onLogStepsQuick: () -> Unit,
    onToggleWeightEditor: () -> Unit,
    onSaveWeightAndTarget: (String, String, String) -> Unit,
    onDeleteWeightPoint: (String) -> Unit,
    onJumpToTop: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    val infiniteTransition = rememberInfiniteTransition(label = "ecgPulseWave")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "pulseWaveAnim"
    )

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToTop,
                label = "Fast Ring",
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "VITALS",
                    badgeText = "${heartRateBpm.roundToInt()} BPM · ${"%.1f".format(Locale.ROOT, currentWeightKg)}KG"
                )
            }

            // 1. PROFESSIONAL OPTICAL HEART RATE & ECG WAVEFORM TELEMETRY CARD
            item {
                Card(
                    onClick = onTriggerLiveSensorScan,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF7A9A).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFFF7A9A), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("OPTICAL HEART RATE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF7A9A), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isScanningVitals) "Scanning Sensor..." else heartRateAccuracy,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (heartRateBpm > 0.0) {
                                    "${heartRateBpm.roundToInt()} BPM"
                                } else {
                                    "— BPM"
                                },
                                style = MaterialTheme.typography.titleMedium.tabularNums(),
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF7A9A)
                            )
                        }

                        // Live Animated Pulse Waveform Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            val path = Path()
                            val stepsCount = 60
                            val midY = size.height / 2f
                            for (i in 0..stepsCount) {
                                val x = (i.toFloat() / stepsCount) * size.width
                                val t = (i.toFloat() / stepsCount) * 4f * Math.PI.toFloat() + pulsePhase
                                val spike = if ((i % 15) == 7) -size.height * 0.42f else if ((i % 15) == 8) size.height * 0.30f else 0f
                                val y = midY + sin(t) * 3.dp.toPx() + spike
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFFF7A9A),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }

            // 2. WEIGHT TARGET & HISTORICAL SPARKLINE GRAPH CARD
            item {
                CatalogM3ListHeaderSample(title = "WEIGHT TARGET & TREND")
            }

            item {
                val weightDiff = currentWeightKg - weightTargetKg
                val diffLabel = if (weightDiff > 0.05) {
                    "${"%.1f".format(Locale.ROOT, weightDiff)}kg to goal"
                } else if (weightDiff < -0.05) {
                    "${"%.1f".format(Locale.ROOT, -weightDiff)}kg below target"
                } else {
                    "Target Reached!"
                }

                Card(
                    onClick = onToggleWeightEditor,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CURRENT / TARGET", style = MaterialTheme.typography.labelSmall, color = Color(0xFF79E2B0), fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${"%.1f".format(Locale.ROOT, currentWeightKg)} / ${"%.1f".format(Locale.ROOT, weightTargetKg)} kg",
                                    style = MaterialTheme.typography.titleSmall.tabularNums(),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(diffLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        // Weight Trend Sparkline & Target Dashed Line Canvas
                        val points = weightHistory.map { it.weightKg }
                        if (points.isNotEmpty()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .padding(vertical = 4.dp)
                            ) {
                                val allVals = points + weightTargetKg
                                val minW = (allVals.minOrNull() ?: 60.0) - 0.8
                                val maxW = (allVals.maxOrNull() ?: 80.0) + 0.8
                                val range = (maxW - minW).coerceAtLeast(1.0)

                                // Target horizontal guide line
                                val targetY = size.height - (((weightTargetKg - minW) / range).toFloat() * size.height)
                                drawLine(
                                    color = Color(0xFFFFD177).copy(alpha = 0.55f),
                                    start = Offset(0f, targetY.coerceIn(0f, size.height)),
                                    end = Offset(size.width, targetY.coerceIn(0f, size.height)),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Weight trend line & dots
                                val trendPath = Path()
                                points.forEachIndexed { idx, w ->
                                    val x = if (points.size == 1) size.width / 2f else (idx.toFloat() / (points.size - 1)) * size.width
                                    val y = (size.height - (((w - minW) / range).toFloat() * size.height)).coerceIn(2f, size.height - 2f)
                                    if (idx == 0) trendPath.moveTo(x, y) else trendPath.lineTo(x, y)
                                    drawCircle(
                                        color = Color(0xFF79E2B0),
                                        radius = 3.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }
                                drawPath(
                                    path = trendPath,
                                    color = Color(0xFF79E2B0),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        Text(
                            text = "Tap to log weight or adjust target (${weightHistory.size} entries)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showWeightEditor) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CURRENT WEIGHT (KG)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val currentKgInt = remember(currentWeightKg) { currentWeightKg.roundToInt() }
                        val targetKgInt = remember(weightTargetKg) { weightTargetKg.roundToInt() }
                        var currentWeightVal by remember { mutableStateOf(currentKgInt) }
                        var targetWeightVal by remember { mutableStateOf(targetKgInt) }

                        val moodChips = listOf("⚡ High Energy", "😊 Great", "😐 Balanced", "🥱 Tired", "🔥 Fasted High")
                        var selectedMoodIdx by remember { mutableStateOf(1) }

                        WearExpressiveUniversalEditorCard(
                            itemTypeTitle = "WEIGHT & VITALS",
                            initialTitle = "Daily Weigh-in",
                            initialTimeOrDate = "",
                            initialSubtitle = mood,
                            isEditingExisting = false,
                            showTimePickerField = false,
                            showSubtitleField = true,
                            subtitleFieldLabel = "Notes / Symptoms",
                            chipOptions = moodChips,
                            selectedChipIndex = selectedMoodIdx,
                            onChipSelected = { selectedMoodIdx = it },
                            discreteSubFields = listOf(
                                DiscreteNumberSubField(
                                    key = "current_wt",
                                    label = "Current Weight",
                                    initialValue = currentKgInt,
                                    minimum = 30,
                                    maximum = 200,
                                    step = 1,
                                    unitSuffix = "kg",
                                    useSlider = true
                                ),
                                DiscreteNumberSubField(
                                    key = "target_wt",
                                    label = "Goal Weight",
                                    initialValue = targetKgInt,
                                    minimum = 30,
                                    maximum = 200,
                                    step = 1,
                                    unitSuffix = "kg",
                                    useSlider = true
                                )
                            ),
                            onSubFieldsChanged = { values ->
                                values["current_wt"]?.let { currentWeightVal = it }
                                values["target_wt"]?.let { targetWeightVal = it }
                            },
                            onSave = { _, _, moodNote ->
                                val finalMood = if (moodNote.isNotBlank()) "$moodNote (${moodChips[selectedMoodIdx]})" else moodChips[selectedMoodIdx]
                                onSaveWeightAndTarget(
                                    currentWeightVal.toString(),
                                    targetWeightVal.toString(),
                                    finalMood
                                )
                            },
                            onCancel = onToggleWeightEditor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. STEPS, SLEEP & RECOVERY TELEMETRY
            item {
                CatalogM3ListHeaderSample(title = "ACTIVITY & RECOVERY")
            }

            item {
                WearMetricCard(
                    title = "STEP COUNTER",
                    value = "$steps steps",
                    subtitle = "Reported by the watch sensor",
                    icon = Icons.Default.DirectionsWalk,
                    onClick = onTriggerLiveSensorScan
                )
            }

            item {
                val sleepLabel = if (sleepMinutes > 0) {
                    "${sleepMinutes / 60}h ${sleepMinutes % 60}m"
                } else {
                    "No sleep data"
                }

                WearMetricCard(
                    title = "SLEEP & ENERGY",
                    value = if (sleepMinutes > 0) {
                        "$sleepLabel · Energy $energy/10"
                    } else {
                        "No sleep data"
                    },
                    subtitle = "Mood: ${mood.ifBlank { "Not recorded" }}",
                    icon = Icons.Rounded.Bedtime,
                    onClick = onToggleWeightEditor
                )
            }

            // 4. WEIGHT LOG HISTORY LIST
            item {
                CatalogM3ListHeaderSample(title = "WEIGHT HISTORY (${weightHistory.size})")
            }

            items(weightHistory.reversed().take(6).size) { idx ->
                val point = weightHistory.reversed()[idx]
                Card(
                    onClick = { onDeleteWeightPoint(point.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${"%.1f".format(Locale.ROOT, point.weightKg)} kg",
                            style = MaterialTheme.typography.labelMedium.tabularNums(),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = point.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
private fun ViewportConstrainedFastingConfirmation(
    label: String,
    isError: Boolean = false,
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
            .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f)
            )
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
