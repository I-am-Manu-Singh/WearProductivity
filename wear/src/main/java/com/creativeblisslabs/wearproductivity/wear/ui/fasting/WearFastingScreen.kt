package com.creativeblisslabs.wearproductivity.wear.ui.fasting

import android.content.Context
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
import com.creativeblisslabs.wearproductivity.wear.ui.focus.WearAdaptiveSpec
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val DEFAULT_FASTING_GOAL_MINUTES = 16 * 60
private const val DEFAULT_WATER_TARGET_ML = 12 * 250
private const val WATER_SERVING_ML = 250

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

data class DietPlanPreset(
    val id: String,
    val name: String,
    val targetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int

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
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

// =============================================================================
// REAL-TIME PCM AMBIENT SOUND SYNTHESIZER ENGINE (MINDFUL FASTING & CRAVING CALM)
// =============================================================================
private class FastingSoundEngine(private val scope: CoroutineScope) {
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null

    fun updatePlayback(shouldPlay: Boolean, mode: FastingSoundMode, volume: Float = 0.22f) {
        stop()
        if (!shouldPlay || mode == FastingSoundMode.OFF) return

        audioJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = runCatching {
                AudioTrack.Builder()
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
            }.getOrNull() ?: return@launch

            audioTrack = track
            runCatching {
                track.setVolume(volume.coerceIn(0.05f, 1f))
                track.play()
            }

            val samples = ShortArray(1024)
            var phase1 = 0.0
            var phase2 = 0.0
            var lfoPhase = 0.0
            var filterOut = 0.0

            try {
                while (isActive) {
                    for (i in samples.indices) {
                        val sampleVal = when (mode) {
                            FastingSoundMode.ZEN_432 -> {
                                phase1 += (2.0 * Math.PI * 216.0) / sampleRate
                                phase2 += (2.0 * Math.PI * 220.0) / sampleRate
                                if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                                if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                                (sin(phase1) + sin(phase2)) * 0.20
                            }
                            FastingSoundMode.RAIN -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                filterOut += 0.08 * (white - filterOut)
                                val drop = if (Random.nextInt(1000) > 996) Random.nextDouble(-0.5, 0.5) else 0.0
                                (filterOut * 0.65) + drop
                            }
                            FastingSoundMode.WAVES -> {
                                lfoPhase += (2.0 * Math.PI * 0.12) / sampleRate
                                if (lfoPhase > 2.0 * Math.PI) lfoPhase -= 2.0 * Math.PI
                                val swell = 0.25 + 0.75 * ((sin(lfoPhase) + 1.0) * 0.5)
                                val white = Random.nextDouble(-1.0, 1.0)
                                filterOut += 0.03 * (white - filterOut)
                                filterOut * swell * 0.75
                            }
                            FastingSoundMode.WHITE -> {
                                Random.nextDouble(-1.0, 1.0) * 0.30
                            }
                            FastingSoundMode.OFF -> 0.0
                        }
                        samples[i] = (sampleVal.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(samples, 0, samples.size)
                }
            } catch (_: Exception) {
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun stop() {
        audioJob?.cancel()
        audioJob = null
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
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
            FastingProgramPreset("prog-16-8", "16:8 Leangains", 16, 8, "Metabolic fat burn"),
            FastingProgramPreset("prog-18-6", "18:6 Warrior", 18, 6, "Autophagy boost"),
            FastingProgramPreset("prog-20-4", "20:4 OMAD Prep", 20, 4, "Deep ketosis"),
            FastingProgramPreset("prog-14-10", "14:10 Circadian", 14, 10, "Gentle rhythm")
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
}
// =============================================================================
// MAIN ENTRY SCREEN
// =============================================================================
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)

// =============================================================================
// PAGE 0: FULL-SCREEN BOUNDARY CIRCULAR PROGRESS & TRANSPORT CONTROLS CANVAS
// =============================================================================

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


// =============================================================================
// PAGE 2: DIET LOGGING, MACRO CONSUMPTION TARGETS & CUSTOM DIET PLANS
// =============================================================================


// =============================================================================
// PAGE 3: PROFESSIONAL VITALS TELEMETRY, SENSORS & WEIGHT TREND GRAPH
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

private fun TextStyle.compactLineHeight(): TextStyle = this.copy(
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )