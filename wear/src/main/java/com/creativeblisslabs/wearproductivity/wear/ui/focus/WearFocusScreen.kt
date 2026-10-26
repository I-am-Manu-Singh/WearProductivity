package com.creativeblisslabs.wearproductivity.wear.ui.focus

import androidx.compose.material.icons.automirrored.rounded.*
import android.content.Context
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
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import com.creativeblisslabs.wearproductivity.shared.FocusHistory
import com.creativeblisslabs.wearproductivity.shared.FocusSession
import com.creativeblisslabs.wearproductivity.shared.FocusType
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.SyncStamp
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

fun FocusType.label(): String = when (this) {
    FocusType.Pomodoro -> "Pomodoro"
    FocusType.ShortBreak -> "Short Break"
    FocusType.LongBreak -> "Long Break"
    FocusType.DeepWork -> "Deep Work"
    FocusType.Custom -> "Custom"
}

fun formatHoursMins(totalMinutes: Int): String {
    val hrs = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hrs > 0) {
        if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
    } else {
        "${mins}m"
    }
}

private fun formatTimerClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.ROOT, "%02d:%02d", mins, secs)
}

data class WearAdaptiveSpecDeleted(

data class FocusTopicPreset(
    val id: String,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val type: FocusType = FocusType.Custom

enum class AmbientSoundMode(val label: String) {
    OFF("Off"),
    WHITE("White"),
    RAIN("Rain"),
    WAVES("Waves"),
    BINAURAL("40Hz")
}

private enum class DurationPickerTarget {
    NONE, FOCUS, BREAK
}

// =============================================================================
// REAL-TIME PCM AMBIENT FOCUS SOUND SYNTHESIZER ENGINE
// =============================================================================


private class FocusAmbientSoundEngine(private val externalScope: CoroutineScope) {
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var currentVolumeLevel: Float = 0.25f

    fun updatePlayback(shouldPlay: Boolean, mode: AmbientSoundMode, volume: Float = currentVolumeLevel) {
        stop()
        currentVolumeLevel = volume
        if (!shouldPlay || mode == AmbientSoundMode.OFF) return

        // Changed 'scope' to 'externalScope' to avoid compiler shadowing issues
        audioJob = externalScope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            // Fix 1: Declared and extracted 'track' properly outside runCatching scope
            val track: AudioTrack = runCatching {
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
                track.setVolume(currentVolumeLevel.coerceIn(0.05f, 1f))
                track.play()
            }

            val samples = ShortArray(1024)
            var phase1 = 0.0
            var phase2 = 0.0
            var lfoPhase = 0.0
            var lastOut = 0.0

            try {
                while (isActive) {
                    for (i in samples.indices) {
                        val rawSample = when (mode) {
                            AmbientSoundMode.WHITE -> {
                                (Random.nextDouble(-1.0, 1.0)) * 0.35
                            }
                            AmbientSoundMode.RAIN -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.08 * (white - lastOut)))
                                val droplet = if (Random.nextInt(1000) > 996) Random.nextDouble(-0.6, 0.6) else 0.0
                                (lastOut * 0.65) + droplet
                            }
                            AmbientSoundMode.WAVES -> {
                                lfoPhase += (2.0 * Math.PI * 0.14) / sampleRate
                                if (lfoPhase > 2.0 * Math.PI) lfoPhase -= 2.0 * Math.PI
                                val swell = 0.25 + 0.75 * ((sin(lfoPhase) + 1.0) * 0.5)
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.03 * (white - lastOut)))
                                lastOut * swell * 0.8
                            }
                            AmbientSoundMode.BINAURAL -> {
                                phase1 += (2.0 * Math.PI * 200.0) / sampleRate
                                phase2 += (2.0 * Math.PI * 240.0) / sampleRate
                                if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                                if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                                ((sin(phase1) + sin(phase2)) * 0.22)
                            }
                            AmbientSoundMode.OFF -> 0.0
                        }
                        samples[i] = (rawSample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
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

    fun setVolume(volume: Float) {
        currentVolumeLevel = volume
        runCatching {
            audioTrack?.setVolume(volume.coerceIn(0.05f, 1f))
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
// PERSISTENCE HELPERS FOR CUSTOM TOPICS & SESSIONS
// =============================================================================
private object FocusTopicPrefs {
    private const val PREFS_NAME = "lifeos_focus_topics_prefs"
    private const val KEY_TOPICS = "custom_focus_topics_v1"
    private const val KEY_SOUND_MODE = "focus_sound_mode"
    private const val KEY_SOUND_MUTED = "focus_sound_muted"

    fun loadTopics(context: Context, seedTopics: List<String>): List<FocusTopicPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TOPICS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 4) {
                    FocusTopicPreset(
                        id = parts[0],
                        name = parts[1],
                        focusMinutes = parts[2].toIntOrNull()?.coerceIn(1, 240) ?: 25,
                        breakMinutes = parts[3].toIntOrNull()?.coerceIn(1, 90) ?: 5
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        val defaults = listOf(
            FocusTopicPreset("topic-coding", "Coding", 45, 10, FocusType.DeepWork),
            FocusTopicPreset("topic-deep", "Deep Work", 50, 10, FocusType.DeepWork),
            FocusTopicPreset("topic-reading", "Reading", 25, 5, FocusType.Pomodoro),
            FocusTopicPreset("topic-music", "Music Practice", 30, 5, FocusType.Custom)
        )
        val extraFromRepo = seedTopics
            .filter { seed -> defaults.none { it.name.equals(seed, ignoreCase = true) } }
            .mapIndexed { idx, name ->
                FocusTopicPreset("topic-repo-$idx", name, 25, 5, FocusType.Pomodoro)
            }
        return defaults + extraFromRepo
    }

    fun saveTopics(context: Context, topics: List<FocusTopicPreset>) {
        val serialized = topics.joinToString(";;") { "${it.id}|${it.name.replace("|", "")}|${it.focusMinutes}|${it.breakMinutes}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOPICS, serialized)
            .apply()
    }

    fun loadSoundMode(context: Context): AmbientSoundMode {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SOUND_MODE, AmbientSoundMode.OFF.name) ?: AmbientSoundMode.OFF.name
        return runCatching { AmbientSoundMode.valueOf(name) }.getOrDefault(AmbientSoundMode.OFF)
    }

    fun saveSoundMode(context: Context, mode: AmbientSoundMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOUND_MODE, mode.name)
            .apply()
    }

    fun loadMuted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_MUTED, false)

    fun saveMuted(context: Context, muted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SOUND_MUTED, muted)
            .apply()
    }
}

@Composable
fun WearBaselineViewport(
    baselineWatchSize: Dp = 416.dp,
    content: @Composable (WearAdaptiveSpec) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val isPhonePreview = this.maxWidth > 280.dp || this.maxHeight > 280.dp
        val viewportWidth = if (isPhonePreview) baselineWatchSize else this.maxWidth
        val viewportHeight = if (isPhonePreview) baselineWatchSize else this.maxHeight

        val spec = remember(viewportWidth, viewportHeight) {
            WearAdaptiveSpec(
                screenWidth = viewportWidth,
                screenHeight = viewportHeight,
                isLargeScreen = viewportWidth >= 225.dp,
                outerHorizontalMargin = viewportWidth * 0.052f,
                topBottomSlotHorizontalMargin = viewportWidth * 0.14f,
                topSlotPadding = viewportHeight * 0.11f,
                bottomEdgeButtonPadding = viewportHeight * 0.22f
        }

        if (isPhonePreview) {
            Box(
                modifier = Modifier
                    .size(baselineWatchSize)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            ) {
                content(spec)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                content(spec)
            }
        }
    }
}

// =============================================================================
// MAIN ENTRY SCREEN
// =============================================================================

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearFocusScreen(state: LifeState, repository: LifeRepository) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberLifeOSHapticProvider()
    val liveState by repository.state.collectAsState()
    val focus = liveState.focus

    // Custom Topics & User Custom Timers State
    val customTopics = remember {
        mutableStateListOf<FocusTopicPreset>().apply {
            addAll(FocusTopicPrefs.loadTopics(context, liveState.settings.focusTopics))
        }
    }
    var selectedTopicId by remember {
        mutableStateOf(customTopics.firstOrNull()?.id ?: "topic-coding")
    }
    val activeTopic = customTopics.firstOrNull { it.id == selectedTopicId }
        ?: customTopics.firstOrNull()
        ?: FocusTopicPreset("default", "Focus", 25, 5, FocusType.Pomodoro)

    var selectedMinutes by remember(focus.minutes) {
        mutableIntStateOf(focus.minutes.coerceIn(1, 240))
    }
    var customFocusMinutes by remember { mutableIntStateOf(activeTopic.focusMinutes) }
    var breakMinutes by remember { mutableIntStateOf(activeTopic.breakMinutes) }
    var selectedType by remember { mutableStateOf(FocusType.Pomodoro) }
    var pickerTarget by remember { mutableStateOf(DurationPickerTarget.NONE) }

    // Topic / Session Universal Editor State
    var showTopicEditor by remember { mutableStateOf(false) }
    var editingTopicId by remember { mutableStateOf<String?>(null) }
    var draftTopicName by remember { mutableStateOf("") }
    var draftTopicFocusMins by remember { mutableStateOf("25") }
    var draftTopicBreakMins by remember { mutableStateOf("5") }

    // Locally Deleted History IDs (for instant clean deletion in UI)
    val deletedHistoryIds = remember { mutableStateListOf<String>() }
    val localManualHistory = remember { mutableStateListOf<FocusHistory>() }

    // Ambient Sound Engine & Mute State
    val soundEngine = remember { FocusAmbientSoundEngine(coroutineScope) }

    var ambientMode by remember { mutableStateOf(FocusTopicPrefs.loadSoundMode(context)) }
    var isSoundMuted by remember { mutableStateOf(FocusTopicPrefs.loadMuted(context)) }

    // Add Volume State Tracker
    var currentVolume by remember { mutableFloatStateOf(0.25f) }

    var sessionNotice by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Manage Real-Time Ambient Sound Synthesis Lifecycle (Added currentVolume dependency)
    LaunchedEffect(focus.active, focus.isPaused, ambientMode, isSoundMuted, currentVolume) {
        val shouldPlayAudio = focus.active && !focus.isPaused && !isSoundMuted && ambientMode != AmbientSoundMode.OFF
        soundEngine.updatePlayback(shouldPlay = shouldPlayAudio, mode = ambientMode, volume = currentVolume)
    }

    DisposableEffect(Unit) {
        onDispose {
            soundEngine.stop()
        }
    }

    // Active Focus 1-Second Countdown Ticker
    LaunchedEffect(focus.active, focus.isPaused, focus.remainingSeconds) {
        if (focus.active && !focus.isPaused && focus.remainingSeconds > 0) {
            delay(1_000L)
            if (focus.remainingSeconds <= 1) {
                haptic.click()
                sessionNotice = "Session Complete!" to false
            }
            repository.tickFocusSecond(1)
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    fun selectTopicPreset(preset: FocusTopicPreset) {
        haptic.click()
        selectedTopicId = preset.id
        customFocusMinutes = preset.focusMinutes.coerceIn(1, 240)
        breakMinutes = preset.breakMinutes.coerceIn(1, 90)
        selectedMinutes = customFocusMinutes
        selectedType = preset.type
        sessionNotice = "${preset.name}: ${preset.focusMinutes}m / ${preset.breakMinutes}m" to false
    }

    fun applyTypeSelection(type: FocusType) {
        haptic.click()
        selectedType = type
        selectedMinutes = when (type) {
            FocusType.Pomodoro -> 25
            FocusType.ShortBreak -> breakMinutes
            FocusType.LongBreak -> 15
            FocusType.DeepWork -> 50
            FocusType.Custom -> customFocusMinutes.coerceIn(1, 240)
        }
    }

    fun startSessionWithParams(minutes: Int, type: FocusType, topicName: String) {
        haptic.click()
        sessionNotice = null
        val safeDuration = minutes.coerceIn(1, 240)
        coroutineScope.launch {
            repository.addFocusTopic(topicName)
            repository.startFocusSession(
                minutes = safeDuration,
                type = type,
                subject = topicName
            sessionNotice = "Started $topicName (${safeDuration}m)" to false
        }
    }

    fun startCurrentSession() {
        val duration = when (selectedType) {
            FocusType.ShortBreak -> breakMinutes.coerceIn(1, 90)
            FocusType.Custom -> customFocusMinutes.coerceIn(1, 240)
            else -> selectedMinutes.coerceIn(1, 240)
        }
        val subjectLabel = if (selectedType == FocusType.ShortBreak || selectedType == FocusType.LongBreak) {
            "Break · ${activeTopic.name}"
        } else {
            activeTopic.name
        }
        startSessionWithParams(duration, selectedType, subjectLabel)
    }

    fun switchBetweenFocusAndBreak() {
        haptic.click()
        coroutineScope.launch {
            val isBreakNow = focus.active && (focus.type == FocusType.ShortBreak || focus.type == FocusType.LongBreak)
            if (isBreakNow) {
                val workMins = customFocusMinutes.coerceIn(1, 240)
                repository.startFocusSession(
                    minutes = workMins,
                    type = FocusType.Custom,
                    subject = activeTopic.name
                )
                sessionNotice = "Focus: ${activeTopic.name} (${workMins}m)" to false
            } else {
                val brkMins = breakMinutes.coerceIn(1, 90)
                repository.startFocusSession(
                    minutes = brkMins,
                    type = FocusType.ShortBreak,
                    subject = "Break · ${activeTopic.name}"
                )
                sessionNotice = "Break Started (${brkMins}m)" to false
            }
        }
    }

    fun previousOrRestartSession() {
        haptic.click()
        coroutineScope.launch {
            val elapsed = focus.totalSeconds - focus.remainingSeconds
            if (focus.active && elapsed > 5) {
                val repeatMins = (focus.totalSeconds / 60).coerceAtLeast(1)
                repository.startFocusSession(
                    minutes = repeatMins,
                    type = focus.type,
                    subject = focus.subject.ifBlank { activeTopic.name }
                )
                sessionNotice = "Restarted ${focus.subject.ifBlank { focus.type.label() }}" to false
            } else {
                switchBetweenFocusAndBreak()
            }
        }
    }

    fun stopAndLogCurrentSession() {
        haptic.click()
        val elapsedMinutes = ((focus.totalSeconds - focus.remainingSeconds) / 60).coerceAtLeast(1)
        val wasFocusWork = focus.type != FocusType.ShortBreak && focus.type != FocusType.LongBreak
        if (focus.active && wasFocusWork && elapsedMinutes >= 1) {
            localManualHistory.add(
                0,
                FocusHistory(
                    stamp = SyncStamp("focus-local-${System.currentTimeMillis()}"),
                    subject = focus.subject.ifBlank { activeTopic.name },
                    minutesFocused = elapsedMinutes,
                    completed = focus.remainingSeconds <= 60
                )
        }
        sessionNotice = "Session Logged ($elapsedMinutes min)" to false
        coroutineScope.launch { repository.stopFocusSession() }
    }

    fun toggleMuteSound() {
        haptic.click()
        val nextMuted = !isSoundMuted
        isSoundMuted = nextMuted
        if (!nextMuted && ambientMode == AmbientSoundMode.OFF) {
            ambientMode = AmbientSoundMode.RAIN
            FocusTopicPrefs.saveSoundMode(context, ambientMode)
        }
        FocusTopicPrefs.saveMuted(context, nextMuted)
        sessionNotice = (if (nextMuted) "Focus Sound Muted" else "Sound: ${ambientMode.label}") to false
    }

    fun openNewTopicEditor() {
        haptic.click()
        editingTopicId = null
        draftTopicName = ""
        draftTopicFocusMins = customFocusMinutes.toString()
        draftTopicBreakMins = breakMinutes.toString()
        showTopicEditor = true
    }

    fun openEditTopicEditor(preset: FocusTopicPreset) {
        haptic.click()
        editingTopicId = preset.id
        draftTopicName = preset.name
        draftTopicFocusMins = preset.focusMinutes.toString()
        draftTopicBreakMins = preset.breakMinutes.toString()
        showTopicEditor = true
    }

    fun saveTopicPreset(nameInput: String, focusMinsInput: String, breakMinsInput: String) {
        val cleanName = nameInput.trim().take(36)
        if (cleanName.isBlank()) {
            sessionNotice = "Enter Topic Name" to true
            haptic.click()
            return
        }
        val parsedFocus = focusMinsInput.trim().filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 240) ?: 25
        val parsedBreak = breakMinsInput.trim().filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 90) ?: 5
        val editId = editingTopicId

        if (editId != null) {
            val idx = customTopics.indexOfFirst { it.id == editId }
            if (idx >= 0) {
                val updated = customTopics[idx].copy(
                    name = cleanName,
                    focusMinutes = parsedFocus,
                    breakMinutes = parsedBreak
                )
                customTopics[idx] = updated
                selectTopicPreset(updated)
            }
            sessionNotice = "Updated $cleanName" to false
        } else {
            val newPreset = FocusTopicPreset(
                id = "topic-${System.currentTimeMillis()}",
                name = cleanName,
                focusMinutes = parsedFocus,
                breakMinutes = parsedBreak,
                type = FocusType.Custom
            customTopics.add(0, newPreset)
            selectTopicPreset(newPreset)
            coroutineScope.launch { repository.addFocusTopic(cleanName) }
            sessionNotice = "Added $cleanName" to false
        }

        FocusTopicPrefs.saveTopics(context, customTopics)
        showTopicEditor = false
        editingTopicId = null
        haptic.click()
    }

    fun deleteTopicPreset(id: String) {
        if (customTopics.size <= 1) {
            sessionNotice = "Keep at least 1 topic" to true
            return
        }
        customTopics.removeAll { it.id == id }
        FocusTopicPrefs.saveTopics(context, customTopics)
        if (selectedTopicId == id) {
            customTopics.firstOrNull()?.let { selectTopicPreset(it) }
        }
        showTopicEditor = false
        editingTopicId = null
        sessionNotice = "Topic Deleted" to false
        haptic.click()
    }

    val combinedHistory = remember(liveState.focusHistory, localManualHistory.size, deletedHistoryIds.size) {
        (localManualHistory + liveState.focusHistory)
            .distinctBy { it.stamp.id }
            .filterNot { it.stamp.id in deletedHistoryIds }
    }

    val adaptiveSpec = com.creativeblisslabs.wearproductivity.wear.ui.components.core.LocalWearAdaptiveSpec.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (pickerTarget != DurationPickerTarget.NONE) {
                DurationPicker(
                    title = if (pickerTarget == DurationPickerTarget.FOCUS) "FOCUS DURATION" else "BREAK DURATION",
                    initialMinutes = if (pickerTarget == DurationPickerTarget.FOCUS) customFocusMinutes else breakMinutes,
                    onConfirm = { newTotalMinutes ->
                        haptic.click()
                        if (pickerTarget == DurationPickerTarget.FOCUS) {
                            customFocusMinutes = newTotalMinutes.coerceIn(1, 240)
                            selectedType = FocusType.Custom
                            selectedMinutes = customFocusMinutes
                            val idx = customTopics.indexOfFirst { it.id == selectedTopicId }
                            if (idx >= 0) {
                                customTopics[idx] = customTopics[idx].copy(focusMinutes = customFocusMinutes)
                                FocusTopicPrefs.saveTopics(context, customTopics)
                            }
                            sessionNotice = "${activeTopic.name}: ${formatHoursMins(customFocusMinutes)}" to false
                        } else {
                            breakMinutes = newTotalMinutes.coerceIn(1, 90)
                            if (selectedType == FocusType.ShortBreak) {
                                selectedMinutes = breakMinutes
                            }
                            val idx = customTopics.indexOfFirst { it.id == selectedTopicId }
                            if (idx >= 0) {
                                customTopics[idx] = customTopics[idx].copy(breakMinutes = breakMinutes)
                                FocusTopicPrefs.saveTopics(context, customTopics)
                            }
                            sessionNotice = "Break Set: ${formatHoursMins(breakMinutes)}" to false
                        }
                        pickerTarget = DurationPickerTarget.NONE
                    },
                    onCancel = {
                        haptic.click()
                        pickerTarget = DurationPickerTarget.NONE
                    }
                )
            } else {
                VerticalPager(
                    state = pagerState,
                    flingBehavior = pagerFlingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> ActiveFocusBoundaryCanvasPage(
                            spec = adaptiveSpec,
                            focus = focus,
                            activeTopic = activeTopic,
                            selectedType = selectedType,
                            selectedMinutes = if (selectedType == FocusType.Custom) customFocusMinutes else selectedMinutes,
                            breakMinutes = breakMinutes,
                            ambientMode = ambientMode,
                            isSoundMuted = isSoundMuted,
                            onTogglePlayPause = {
                                if (focus.active) {
                                    haptic.click()
                                    coroutineScope.launch { repository.togglePauseFocusSession() }
                                } else {
                                    startCurrentSession()
                                }
                            },
                            onPreviousOrRestart = ::previousOrRestartSession,
                            onNextFocusOrBreak = ::switchBetweenFocusAndBreak,
                            onStopSession = ::stopAndLogCurrentSession,
                            onExtendSession = {
                                haptic.click()
                                if (focus.active) {
                                    coroutineScope.launch { repository.extendFocusSession(5) }
                                    sessionNotice = "+5m Added" to false
                                } else {
                                    customFocusMinutes = (customFocusMinutes + 5).coerceAtMost(240)
                                    selectedMinutes = customFocusMinutes
                                    selectedType = FocusType.Custom
                                    sessionNotice = "Timer: ${formatHoursMins(customFocusMinutes)}" to false
                                }
                            },
                            onToggleMute = ::toggleMuteSound,
                            onCycleTopic = {
                                val currIdx = customTopics.indexOfFirst { it.id == selectedTopicId }.coerceAtLeast(0)
                                val nextTopic = customTopics[(currIdx + 1) % customTopics.size]
                                selectTopicPreset(nextTopic)
                            },
                            onOpenCustomPicker = {
                                haptic.click()
                                pickerTarget = DurationPickerTarget.FOCUS
                            },
                            onOpenDashboard = {
                                haptic.click()
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onSelectAmbientMode = { mode ->
                                haptic.click()
                                ambientMode = mode
                                isSoundMuted = (mode == AmbientSoundMode.OFF)
                                FocusTopicPrefs.saveSoundMode(context, mode)
                                FocusTopicPrefs.saveMuted(context, isSoundMuted)
                                sessionNotice = "Sound: ${mode.label}" to false
                            }
                        )

                        1 -> FocusDashboardTransformingPage(
                            spec = adaptiveSpec,
                            state = liveState,
                            focus = focus,
                            activeTopic = activeTopic,
                            customTopics = customTopics,
                            history = combinedHistory,
                            selectedType = selectedType,
                            selectedMinutes = if (selectedType == FocusType.Custom) customFocusMinutes else selectedMinutes,
                            customFocusMinutes = customFocusMinutes,
                            breakMinutes = breakMinutes,
                            ambientMode = ambientMode,
                            isSoundMuted = isSoundMuted,
                            showTopicEditor = showTopicEditor,
                            isEditingTopic = editingTopicId != null,
                            draftTopicName = draftTopicName,
                            draftTopicFocusMins = draftTopicFocusMins,
                            draftTopicBreakMins = draftTopicBreakMins,
                            onSelectType = ::applyTypeSelection,
                            onSelectTopic = ::selectTopicPreset,
                            onStartTopicSession = { preset ->
                                selectTopicPreset(preset)
                                startSessionWithParams(preset.focusMinutes, FocusType.Custom, preset.name)
                            },
                            onOpenNewTopicEditor = ::openNewTopicEditor,
                            onOpenEditTopicEditor = ::openEditTopicEditor,
                            onSaveTopic = ::saveTopicPreset,
                            onCancelTopicEditor = {
                                haptic.click()
                                showTopicEditor = false
                                editingTopicId = null
                            },
                            onDeleteTopic = {
                                editingTopicId?.let { deleteTopicPreset(it) }
                            },
                            onSelectAmbientMode = { mode ->
                                haptic.click()
                                ambientMode = mode
                                isSoundMuted = (mode == AmbientSoundMode.OFF)
                                FocusTopicPrefs.saveSoundMode(context, mode)
                                FocusTopicPrefs.saveMuted(context, isSoundMuted)
                                sessionNotice = "Sound: ${mode.label}" to false
                            },
                            onToggleMute = ::toggleMuteSound,
                            onOpenCustomFocusPicker = {
                                haptic.click()
                                selectedType = FocusType.Custom
                                selectedMinutes = customFocusMinutes
                                pickerTarget = DurationPickerTarget.FOCUS
                            },
                            onOpenBreakPicker = {
                                haptic.click()
                                pickerTarget = DurationPickerTarget.BREAK
                            },
                            onStartSession = ::startCurrentSession,
                            onPauseResumeSession = {
                                haptic.click()
                                coroutineScope.launch { repository.togglePauseFocusSession() }
                            },
                            onStopSession = ::stopAndLogCurrentSession,
                            onPreviousOrRestart = ::previousOrRestartSession,
                            onNextFocusOrBreak = ::switchBetweenFocusAndBreak,
                            onExtendSession = {
                                haptic.click()
                                coroutineScope.launch { repository.extendFocusSession(5) }
                                sessionNotice = "+5m Added" to false
                            },
                            onRepeatHistoryItem = { item ->
                                startSessionWithParams(item.minutesFocused.coerceAtLeast(5), FocusType.Custom, item.subject)
                            },
                            onDeleteHistoryItem = { item ->
                                haptic.click()
                                deletedHistoryIds.add(item.stamp.id)
                                sessionNotice = "History Entry Removed" to false
                            },
                            onJumpToCanvas = {
                                haptic.click()
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                    }
                }

                VerticalPagerIndicator(
                    currentPage = pagerState.currentPage,
                    pageCount = 2,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = adaptiveSpec.screenWidth * 0.02f)
                )
            }

            // In-Viewport Constrained Curved Confirmation Overlay
            sessionNotice?.let { (msg, isError) ->
                ViewportConstrainedFocusConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { sessionNotice = null }
                )
            }
        }
    
}

// =============================================================================
// PAGE 0: FULL-SCREEN BOUNDARY CIRCULAR PROGRESS & EXPRESSIVE TRANSPORT CANVAS
// =============================================================================
@androidx.annotation.OptIn(ProtoLayoutExperimental::class)
@Composable
private fun ActiveFocusBoundaryCanvasPage(
    spec: WearAdaptiveSpec,
    focus: FocusSession,
    activeTopic: FocusTopicPreset,
    selectedType: FocusType,
    selectedMinutes: Int,
    breakMinutes: Int,
    ambientMode: AmbientSoundMode,
    isSoundMuted: Boolean,
    onTogglePlayPause: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextFocusOrBreak: () -> Unit,
    onStopSession: () -> Unit,
    onExtendSession: () -> Unit,
    onToggleMute: () -> Unit,
    onCycleTopic: () -> Unit,
    onOpenCustomPicker: () -> Unit,
    onOpenDashboard: () -> Unit,
    onSelectAmbientMode: (AmbientSoundMode) -> Unit
    ) {
    val isActive = focus.active
    val isPaused = focus.isPaused
    val isBreakActive = isActive && (focus.type == FocusType.ShortBreak || focus.type == FocusType.LongBreak)

    val totalSessionSeconds = focus.totalSeconds.coerceAtLeast(focus.remainingSeconds).coerceAtLeast(1)
    val safeRemainingSeconds = focus.remainingSeconds.coerceAtLeast(0)
    val remainingProgress = if (isActive) {
        (safeRemainingSeconds.toFloat() / totalSessionSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        (selectedMinutes.toFloat() / 90f).coerceIn(0.12f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = remainingProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "boundaryFocusProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "focusWavePhase")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "wavePhaseAnim"
    )

    val primaryAccent = when {
        isBreakActive -> Color(0xFF79E2B0)
        isActive && isPaused -> Color(0xFFFFD177)
        else -> MaterialTheme.colorScheme.primary
    }

    val centerButtonSize = (spec.screenWidth * 0.32f).coerceIn(58.dp, 74.dp)
    val sideButtonWidth = (spec.screenWidth * 0.21f).coerceIn(42.dp, 52.dp)
    val sideButtonHeight = (spec.screenWidth * 0.25f).coerceIn(48.dp, 60.dp)
    val bottomPillHeight = (spec.screenHeight * 0.14f).coerceIn(30.dp, 36.dp)
    val localCoroutineScope = rememberCoroutineScope()

// 2. Pass that explicit variable into your Engine constructor
    val soundEngine = remember { FocusAmbientSoundEngine(localCoroutineScope) }
    var currentVolume by remember { mutableFloatStateOf(0.25f) }
    val showLabel = true // Set your visibility condition here

// Calculate display integer (e.g., 0.25f * 10 = 2.5 -> rounds to 3)
    val currentLevel = (currentVolume * 10f).roundToInt()
    ScreenScaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // 1. FULL-SCREEN AMBIENT RADIAL CONTAINER CROPPED INSIDE BEZEL PROGRESS RING
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isActive && !isPaused) 0.28f else 0.14f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBreakActive) Icons.Rounded.SelfImprovement else Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(spec.screenWidth * 0.56f),
                    tint = primaryAccent.copy(alpha = 0.07f)
                )
            }

            // 2. FULL-SCREEN BOUNDARY CIRCULAR PROGRESS ARC
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
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
            }

            // 3. FOREGROUND M3 EXPRESSIVE CONTROLS & TELEMETRY
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
                // Top Slot: Topic Pill + Countdown Clock + Phase Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                            .clickable(onClick = onCycleTopic)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBreakActive) Icons.Rounded.Coffee else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = primaryAccent
                        )
                        Text(
                            text = if (isActive) focus.subject.ifBlank { activeTopic.name } else activeTopic.name,
                            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = if (isActive) formatTimerClock(safeRemainingSeconds) else formatTimerClock(selectedMinutes * 60),
                        style = MaterialTheme.typography.displayMedium.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.clickable(onClick = onOpenCustomPicker)
                    )

                    Text(
                        text = when {
                            isActive && isPaused -> "PAUSED · TAP TO RESUME"
                            isBreakActive -> "BREAK · NEXT: ${activeTopic.focusMinutes}M FOCUS"
                            isActive -> "${focus.type.label().uppercase()} · ${breakMinutes}M BREAK"
                            else -> "${selectedType.label().uppercase()} · TAP TIME TO EDIT"
                        },
                        style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Slot: Prev (Restart/Switch) | Center Play/Pause + Wavy Ring | Next (Focus <-> Break)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Button: Restart or Switch Phase
                    Button(
                        onClick = onPreviousOrRestart,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Restart or Previous Phase" },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Center Play / Pause Button with Expressive Wavy Scallop Ring
                    Box(
                        modifier = Modifier.size(centerButtonSize + 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFocusWavyRing(
                                progress = animatedProgress,
                                phase = if (isActive && !isPaused) wavePhase else 0f,
                                trackColor = primaryAccent.copy(alpha = 0.28f),
                                activeColor = primaryAccent,
                                strokeWidthPx = 3.5.dp.toPx(),
                                waveAmplitudePx = if (isActive && !isPaused) 2.dp.toPx() else 1.dp.toPx(),
                                lobes = 12
                            )
                        }

                        Button(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(centerButtonSize)
                                .semantics {
                                    contentDescription = when {
                                        !isActive -> "Start Focus"
                                        isPaused -> "Resume Focus"
                                        else -> "Pause Focus"
                                    }
                                },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isActive && !isPaused) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Right Button: Next (Switch between Focus and Break)
                    Button(
                        onClick = onNextFocusOrBreak,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = if (isBreakActive) "Switch to Focus" else "Switch to Break" },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                    WearActionButtonGroup(
                        options = AmbientSoundMode.entries.map { it.label },
                        selectedIndex = if (isSoundMuted) 0 else ambientMode.ordinal,
                        onOptionSelected = { idx ->
                            onSelectAmbientMode(AmbientSoundMode.entries[idx])
                        }
                    )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {



                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val newLevel = (currentLevel - 1).coerceAtLeast(0)
                                val newVolume = newLevel / 10f
                                currentVolume = newVolume
                                soundEngine.setVolume(newVolume)
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "Vol : $currentLevel",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val newLevel = (currentLevel + 1).coerceAtMost(10)
                                val newVolume = newLevel / 10f
                                currentVolume = newVolume
                                soundEngine.setVolume(newVolume)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                // Bottom Slot: Mute/Sound Toggle | +5m Extend | Stop or Deck Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.06f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Focus Sounds Pill
                    val soundActive = !isSoundMuted && ambientMode != AmbientSoundMode.OFF
                    Button(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .height(bottomPillHeight)
                            .semantics { contentDescription = if (soundActive) "Mute Focus Sound" else "Enable Focus Sound" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (soundActive) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (soundActive) MaterialTheme.colorScheme.onSecondaryContainer
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
                                imageVector = if (soundActive) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // +5 Min Extend Pill
                    Button(
                        onClick = onExtendSession,
                        modifier = Modifier
                            .height(bottomPillHeight)
                            .semantics { contentDescription = "Add 5 Minutes" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "+5m",
                                style = MaterialTheme.typography.labelSmall.tabularNums(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Stop (when active) or Open Topics/Settings Deck (when idle)
                    if (isActive) {
                        Button(
                            onClick = onStopSession,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = "Stop Focus Session" },
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
                            onClick = onOpenDashboard,
                            modifier = Modifier
                                .height(bottomPillHeight)
                                .semantics { contentDescription = "Open Focus Topics and Analytics" },
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

private fun DrawScope.drawFocusWavyRing(
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
// PAGE 1: FOCUS DASHBOARD, ACTIVE SESSION CONTROLLER, CUSTOM TOPICS & ANALYTICS
// =============================================================================
@Composable
private fun FocusDashboardTransformingPage(
    spec: WearAdaptiveSpec,
    state: LifeState,
    focus: FocusSession,
    activeTopic: FocusTopicPreset,
    customTopics: List<FocusTopicPreset>,
    history: List<FocusHistory>,
    selectedType: FocusType,
    selectedMinutes: Int,
    customFocusMinutes: Int,
    breakMinutes: Int,
    ambientMode: AmbientSoundMode,
    isSoundMuted: Boolean,
    showTopicEditor: Boolean,
    isEditingTopic: Boolean,
    draftTopicName: String,
    draftTopicFocusMins: String,
    draftTopicBreakMins: String,
    onSelectType: (FocusType) -> Unit,
    onSelectTopic: (FocusTopicPreset) -> Unit,
    onStartTopicSession: (FocusTopicPreset) -> Unit,
    onOpenNewTopicEditor: () -> Unit,
    onOpenEditTopicEditor: (FocusTopicPreset) -> Unit,
    onSaveTopic: (String, String, String) -> Unit,
    onCancelTopicEditor: () -> Unit,
    onDeleteTopic: () -> Unit,
    onSelectAmbientMode: (AmbientSoundMode) -> Unit,
    onToggleMute: () -> Unit,
    onOpenCustomFocusPicker: () -> Unit,
    onOpenBreakPicker: () -> Unit,
    onStartSession: () -> Unit,
    onPauseResumeSession: () -> Unit,
    onStopSession: () -> Unit,
    onPreviousOrRestart: () -> Unit,
    onNextFocusOrBreak: () -> Unit,
    onExtendSession: () -> Unit,
    onRepeatHistoryItem: (FocusHistory) -> Unit,
    onDeleteHistoryItem: (FocusHistory) -> Unit,
    onJumpToCanvas: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val isActive = focus.active
    val isBreakActive = isActive && (focus.type == FocusType.ShortBreak || focus.type == FocusType.LongBreak)
    val totalFocusedMinutes = history.sumOf { it.minutesFocused }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = if (isActive) onJumpToCanvas else onStartSession,
                label = if (isActive) "Timer Ring" else "Start ${activeTopic.name}",
                icon = if (isActive) Icons.Rounded.KeyboardArrowUp else Icons.Default.PlayArrow
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
                    title = "FOCUS STUDIO",
                    icon = Icons.Filled.Timer,
                    badgeText = if (isActive) formatTimerClock(focus.remainingSeconds) else formatHoursMins(selectedMinutes),
                    modifier = Modifier.fillMaxWidth()
                )
            }



            item {
                FilledTonalButton(
                    onClick = if (showTopicEditor) onCancelTopicEditor else onOpenNewTopicEditor,
                    icon = { Icon(if (showTopicEditor) Icons.Default.Close else Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showTopicEditor) "Close Topic Editor" else "New Topic & Custom Timer")
                }
            }

            if (showTopicEditor) {
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "TOPIC TIMER",
                        initialTitle = draftTopicName,
                        initialTimeOrDate = draftTopicFocusMins,
                        initialSubtitle = draftTopicBreakMins,
                        isEditingExisting = isEditingTopic,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Focus Minutes (1-240)",
                        subtitleFieldLabel = "Break Minutes (1-90)",
                        onSave = { title, focusMins, brkMins ->
                            onSaveTopic(title, focusMins, brkMins)
                        },
                        onCancel = onCancelTopicEditor,
                        onDelete = if (isEditingTopic) onDeleteTopic else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(customTopics.size) { idx ->
                val preset = customTopics[idx]
                val isSelected = preset.id == activeTopic.id
                val topicHistoryMins = history
                    .filter { it.subject.contains(preset.name, ignoreCase = true) }
                    .sumOf { it.minutesFocused }

                M3FocusTopicRow(
                    preset = preset,
                    isSelected = isSelected,
                    loggedMinutes = topicHistoryMins,
                    onSelect = { onSelectTopic(preset) },
                    onStartNow = { onStartTopicSession(preset) },
                    onEdit = { onOpenEditTopicEditor(preset) }
                )
            }

            // =========================================================================
            // 4. QUICK DURATION & MODE CUSTOMIZATION
            // =========================================================================
            item {
                CatalogM3ListHeaderSample(title = "TIMER DURATIONS")
            }

            item {
                WearActionButtonGroup(
                    options = listOf("Pomo 25m", "Short ${breakMinutes}m", "Long 15m", "Deep 50m"),
                    selectedIndex = when (selectedType) {
                        FocusType.Pomodoro -> 0
                        FocusType.ShortBreak -> 1
                        FocusType.LongBreak -> 2
                        FocusType.DeepWork -> 3
                        else -> 0
                    },
                    onOptionSelected = { idx ->
                        onSelectType(
                            when (idx) {
                                0 -> FocusType.Pomodoro
                                1 -> FocusType.ShortBreak
                                2 -> FocusType.LongBreak
                                else -> FocusType.DeepWork
                            }
                        )
                    }
                )
            }

            item {
                WearMetricCard(
                    title = "FOCUS DURATION (${activeTopic.name.uppercase()})",
                    value = formatHoursMins(customFocusMinutes),
                    subtitle = "Tap to open dial duration picker",
                    icon = Icons.Default.Tune,
                    onClick = onOpenCustomFocusPicker
                )
            }

            item {
                WearMetricCard(
                    title = "BREAK DURATION",
                    value = formatHoursMins(breakMinutes),
                    subtitle = "Tap to adjust recovery break",
                    icon = Icons.Rounded.Coffee,
                    onClick = onOpenBreakPicker
                )
            }



            // =========================================================================
            // 6. RECENT FOCUS HISTORY LOGS (TAP TO REPEAT, SWIPE/ICON TO DELETE)
            // =========================================================================
            item {
                CatalogM3ListHeaderSample(title = "SESSION HISTORY (${history.size})")
            }

            if (history.isEmpty()) {
                item {
                    WearMetricCard(
                        title = "NO SESSIONS YET",
                        value = "Start a Focus Timer",
                        subtitle = "Completed sessions are logged automatically",
                        icon = Icons.Rounded.History,
                        onClick = onStartSession
                    )
                }
            } else {
                items(history.take(15).size) { idx ->
                    val record = history[idx]
                    WearSwipeableActionCard(
                        onCardClick = { onRepeatHistoryItem(record) },
                        onEditClick = { onRepeatHistoryItem(record) },
                        onDeleteClick = { onDeleteHistoryItem(record) },
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
                                    text = record.subject.ifBlank { "Focus Session" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${record.minutesFocused} min · ${if (record.completed) "Completed" else "Logged"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalIconButton(
                                    onClick = { onRepeatHistoryItem(record) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay,
                                        contentDescription = "Repeat Session",
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteHistoryItem(record) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete Log",
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
}

@Composable
private fun TransformingLazyColumnItemScope.M3FocusTopicRow(
    preset: FocusTopicPreset,
    isSelected: Boolean,
    loggedMinutes: Int,
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
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${preset.focusMinutes}m focus · ${preset.breakMinutes}m break · ${loggedMinutes}m logged",
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
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Topic",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED VIEWPORT UTILITIES
// =============================================================================
@Composable
private fun ViewportConstrainedFocusConfirmation(
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

@Composable
private fun VerticalPagerIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
            .padding(vertical = 5.dp, horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotHeight by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 4.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "pagerDotHeight"
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dotHeight)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
        }
    }
}

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