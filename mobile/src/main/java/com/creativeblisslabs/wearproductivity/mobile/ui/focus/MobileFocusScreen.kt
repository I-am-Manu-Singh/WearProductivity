package com.creativeblisslabs.wearproductivity.mobile.ui.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.FocusHistory
import com.creativeblisslabs.wearproductivity.shared.FocusSession
import com.creativeblisslabs.wearproductivity.shared.FocusType
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

// =============================================================================
// SHARED FOCUS TOPIC PRESET & AMBIENT SOUND GENERATOR (100% PARITY WITH WEAR)
// =============================================================================
data class MobileFocusTopicPreset(
    val id: String,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val type: FocusType = FocusType.Custom
)

enum class MobileAmbientSoundMode(val label: String, val icon: ImageVector) {
    OFF("Off", Icons.AutoMirrored.Rounded.VolumeOff),
    WHITE("White Noise", Icons.Rounded.Air),
    RAIN("Rainfall", Icons.Rounded.WaterDrop),
    WAVES("Ocean Waves", Icons.Rounded.Waves),
    BINAURAL("40Hz Gamma", Icons.Rounded.Hearing)
}

private object MobileFocusTopicPrefs {
    private const val PREFS_NAME = "lifeos_focus_topics_prefs"
    private const val KEY_TOPICS = "custom_focus_topics_v1"
    private const val KEY_SOUND_MODE = "focus_sound_mode"
    private const val KEY_SOUND_MUTED = "focus_sound_muted"

    fun loadTopics(context: Context, seedTopics: List<String>): List<MobileFocusTopicPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TOPICS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 4) {
                    val type = parts.getOrNull(4)?.let { v -> runCatching { FocusType.valueOf(v) }.getOrNull() } ?: FocusType.Custom
                    MobileFocusTopicPreset(
                        id = parts[0],
                        name = parts[1],
                        focusMinutes = parts[2].toIntOrNull()?.coerceIn(1, 240) ?: 25,
                        breakMinutes = parts[3].toIntOrNull()?.coerceIn(1, 90) ?: 5,
                        type = type
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }

        val defaults = listOf(
            MobileFocusTopicPreset("topic-coding", "Coding & Dev", 45, 10, FocusType.DeepWork),
            MobileFocusTopicPreset("topic-deep", "Deep Work", 50, 10, FocusType.DeepWork),
            MobileFocusTopicPreset("topic-reading", "Reading / Study", 25, 5, FocusType.Pomodoro),
            MobileFocusTopicPreset("topic-writing", "Writing", 30, 5, FocusType.Custom),
            MobileFocusTopicPreset("topic-admin", "Admin & Planning", 20, 5, FocusType.Pomodoro)
        )

        val extraFromRepo = seedTopics
            .filter { seed -> defaults.none { it.name.equals(seed.trim(), ignoreCase = true) } }
            .distinctBy { it.trim().lowercase(Locale.ROOT) }
            .mapIndexed { idx, name ->
                MobileFocusTopicPreset(
                    id = "topic-repo-$idx",
                    name = name.trim(),
                    focusMinutes = 25,
                    breakMinutes = 5,
                    type = FocusType.Pomodoro
                )
            }

        return defaults + extraFromRepo
    }

    fun saveTopics(context: Context, topics: List<MobileFocusTopicPreset>) {
        val serialized = topics.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "").replace(";;", "")}|${it.focusMinutes}|${it.breakMinutes}|${it.type.name}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TOPICS, serialized).apply()
    }

    fun loadSoundMode(context: Context): MobileAmbientSoundMode {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SOUND_MODE, MobileAmbientSoundMode.OFF.name)
        return runCatching { MobileAmbientSoundMode.valueOf(name ?: MobileAmbientSoundMode.OFF.name) }.getOrDefault(MobileAmbientSoundMode.OFF)
    }

    fun saveSoundMode(context: Context, mode: MobileAmbientSoundMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_SOUND_MODE, mode.name).apply()
    }
}

// Real-Time PCM Ambient Audio Synthesizer (Zero-Asset procedural audio engine)
private class MobileFocusAmbientSoundEngine(private val externalScope: CoroutineScope) {
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var currentVolumeLevel: Float = 0.35f

    fun updatePlayback(shouldPlay: Boolean, mode: MobileAmbientSoundMode, volume: Float = currentVolumeLevel) {
        stop()
        currentVolumeLevel = volume
        if (!shouldPlay || mode == MobileAmbientSoundMode.OFF) return

        audioJob = externalScope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

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
                            MobileAmbientSoundMode.WHITE -> {
                                Random.nextDouble(-1.0, 1.0) * 0.35
                            }
                            MobileAmbientSoundMode.RAIN -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.08 * (white - lastOut)))
                                val droplet = if (Random.nextInt(1000) > 996) Random.nextDouble(-0.6, 0.6) else 0.0
                                (lastOut * 0.65) + droplet
                            }
                            MobileAmbientSoundMode.WAVES -> {
                                lfoPhase += (2.0 * Math.PI * 0.14) / sampleRate
                                if (lfoPhase > 2.0 * Math.PI) lfoPhase -= 2.0 * Math.PI
                                val swell = 0.25 + 0.75 * ((sin(lfoPhase) + 1.0) * 0.5)
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.03 * (white - lastOut)))
                                lastOut * swell * 0.8
                            }
                            MobileAmbientSoundMode.BINAURAL -> {
                                phase1 += (2.0 * Math.PI * 200.0) / sampleRate
                                phase2 += (2.0 * Math.PI * 240.0) / sampleRate
                                if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                                if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                                (sin(phase1) + sin(phase2)) * 0.22
                            }
                            MobileAmbientSoundMode.OFF -> 0.0
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

    fun stop() {
        audioJob?.cancel()
        audioJob = null
        audioTrack?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        audioTrack = null
    }
}

// 3 Navigation Tabs Matching Wear OS Focus Flow
enum class MobileFocusTab(val label: String, val icon: ImageVector) {
    ACTIVE_TIMER("Active Timer", Icons.Rounded.Timer),
    TOPICS_AMBIENT("Topics & Sound", Icons.Rounded.GraphicEq),
    HISTORY_ANALYTICS("History & Score", Icons.Rounded.Insights)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFocusScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = state.focus

    var currentTab by remember { mutableStateOf(MobileFocusTab.ACTIVE_TIMER) }

    // Custom Topics State
    var topicPresets by remember {
        mutableStateOf(MobileFocusTopicPrefs.loadTopics(context, state.settings.focusTopics))
    }
    var activeTopic by remember {
        mutableStateOf(topicPresets.firstOrNull { it.name.equals(focus.subject, ignoreCase = true) } ?: topicPresets.first())
    }

    // Ambient Sound Generator Engine
    val soundEngine = remember { MobileFocusAmbientSoundEngine(scope) }
    var ambientSoundMode by remember { mutableStateOf(MobileFocusTopicPrefs.loadSoundMode(context)) }
    var isSoundMuted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { soundEngine.stop() }
    }

    // Reactively trigger synthesized ambient sound when focus is active
    LaunchedEffect(focus.active, focus.isPaused, ambientSoundMode, isSoundMuted) {
        val shouldPlay = focus.active && !focus.isPaused && !isSoundMuted && ambientSoundMode != MobileAmbientSoundMode.OFF
        soundEngine.updatePlayback(shouldPlay, ambientSoundMode)
    }

    // Wear OS Connected Watch info
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    var watchSyncStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            watchNodeId = nodes.firstOrNull()?.id
        } catch (_: Exception) {}
    }

    fun pushWatchFocusNotification(title: String, message: String) {
        if (watchNodeId == null) {
            watchSyncStatus = "Watch standby (Not connected)"
            return
        }
        scope.launch {
            try {
                val payload = "$title: $message".toByteArray(Charsets.UTF_8)
                Wearable.getMessageClient(context).sendMessage(
                    watchNodeId!!,
                    "/lifeos/find_watch",
                    payload
                ).await()
                watchSyncStatus = "Focus signal sent to Watch"
            } catch (_: Exception) {
                watchSyncStatus = "Watch alert failed"
            }
        }
    }

    // Focus Session Readouts
    val remainingSec = focus.remainingSeconds
    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val progress = if (focus.totalSeconds > 0) {
        ((focus.totalSeconds - focus.remainingSeconds).toFloat() / focus.totalSeconds).coerceIn(0f, 1f)
    } else 0f

    val activeHistory = remember(state.focusHistory) {
        state.focusHistory.filter { it.stamp.deletedAt == null }.sortedByDescending { it.timestamp }
    }

    // Add Topic Dialog state
    var showCreateTopicDialog by remember { mutableStateOf(false) }
    var newTopicName by remember { mutableStateOf("") }
    var newTopicFocusMins by remember { mutableStateOf("30") }
    var newTopicBreakMins by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Focus & Flow Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (focus.active) "${activeTopic.name} · ${if (focus.isPaused) "Paused" else "In Session"}" else "Pomodoro & Deep Work Hub",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pushWatchFocusNotification("Focus Session", "${activeTopic.name} - ${activeTopic.focusMinutes}m focus")
                    }) {
                        Icon(Icons.Rounded.Watch, contentDescription = "Sync to Watch")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileFocusTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(tab.label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = watchSyncStatus != null) {
                watchSyncStatus?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { watchSyncStatus = null }) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                MobileFocusTab.ACTIVE_TIMER -> {
                    ActiveFocusTimerPageView(
                        focus = focus,
                        activeTopic = activeTopic,
                        progress = progress,
                        mins = mins,
                        secs = secs,
                        ambientSoundMode = ambientSoundMode,
                        isSoundMuted = isSoundMuted,
                        onTogglePlayPause = {
                            scope.launch {
                                if (focus.active) {
                                    repository.togglePauseFocusSession()
                                } else {
                                    repository.startFocusSession(
                                        minutes = activeTopic.focusMinutes,
                                        type = activeTopic.type,
                                        subject = activeTopic.name
                                    )
                                }
                            }
                        },
                        onExtend15m = { scope.launch { repository.extendFocusSession(15) } },
                        onStopSession = { scope.launch { repository.stopFocusSession() } },
                        onToggleSoundMute = { isSoundMuted = !isSoundMuted },
                        onSelectTopic = { activeTopic = it },
                        topicPresets = topicPresets,
                        onPushWatchAlert = { title, msg -> pushWatchFocusNotification(title, msg) }
                    )
                }

                MobileFocusTab.TOPICS_AMBIENT -> {
                    TopicsAndAmbientSoundPageView(
                        topics = topicPresets,
                        activeTopic = activeTopic,
                        ambientSoundMode = ambientSoundMode,
                        isSoundMuted = isSoundMuted,
                        onSelectTopic = { activeTopic = it },
                        onSelectSoundMode = { mode ->
                            ambientSoundMode = mode
                            MobileFocusTopicPrefs.saveSoundMode(context, mode)
                        },
                        onToggleMute = { isSoundMuted = !isSoundMuted },
                        onOpenCreateTopic = { showCreateTopicDialog = true }
                    )
                }

                MobileFocusTab.HISTORY_ANALYTICS -> {
                    FocusHistoryAndScorePageView(
                        state = state,
                        activeHistory = activeHistory
                    )
                }
            }
        }
    }

    if (showCreateTopicDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTopicDialog = false },
            title = { Text("Create Focus Topic Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTopicName,
                        onValueChange = { newTopicName = it },
                        label = { Text("Topic Name (e.g. Research)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTopicFocusMins,
                        onValueChange = { newTopicFocusMins = it },
                        label = { Text("Focus Duration (Minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTopicBreakMins,
                        onValueChange = { newTopicBreakMins = it },
                        label = { Text("Break Duration (Minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newTopicName.isNotBlank()) {
                        val fMins = newTopicFocusMins.toIntOrNull()?.coerceIn(5, 180) ?: 25
                        val bMins = newTopicBreakMins.toIntOrNull()?.coerceIn(1, 60) ?: 5
                        val newTopic = MobileFocusTopicPreset(
                            id = "topic-custom-${System.currentTimeMillis()}",
                            name = newTopicName.trim(),
                            focusMinutes = fMins,
                            breakMinutes = bMins,
                            type = FocusType.Custom
                        )
                        val updated = topicPresets + newTopic
                        topicPresets = updated
                        activeTopic = newTopic
                        MobileFocusTopicPrefs.saveTopics(context, updated)
                        showCreateTopicDialog = false
                        newTopicName = ""
                    }
                }) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTopicDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// 1. ACTIVE FOCUS TIMER VIEW (Matching ActiveFocusBoundaryCanvasPage on Wear)
// =============================================================================
@Composable
private fun ActiveFocusTimerPageView(
    focus: FocusSession,
    activeTopic: MobileFocusTopicPreset,
    progress: Float,
    mins: Int,
    secs: Int,
    ambientSoundMode: MobileAmbientSoundMode,
    isSoundMuted: Boolean,
    onTogglePlayPause: () -> Unit,
    onExtend15m: () -> Unit,
    onStopSession: () -> Unit,
    onToggleSoundMute: () -> Unit,
    onSelectTopic: (MobileFocusTopicPreset) -> Unit,
    topicPresets: List<MobileFocusTopicPreset>,
    onPushWatchAlert: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Focus Boundary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (focus.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                if (focus.active) "SESSION RUNNING" else "READY TO FOCUS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleSoundMute) {
                                Icon(
                                    imageVector = if (!isSoundMuted && ambientSoundMode != MobileAmbientSoundMode.OFF) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                    contentDescription = "Toggle Audio"
                                )
                            }
                            Text(
                                ambientSoundMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        activeTopic.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (focus.active) String.format("%02d:%02d", mins, secs) else "${activeTopic.focusMinutes}:00",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Transport Actions: Play/Pause, +15m, Stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (focus.active) {
                            OutlinedButton(
                                onClick = onExtend15m,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+15m")
                            }
                        }

                        Button(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(
                                imageVector = if (focus.active && !focus.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (focus.active) (if (focus.isPaused) "Resume" else "Pause") else "Start Session")
                        }

                        if (focus.active) {
                            FilledTonalButton(
                                onClick = onStopSession,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }

        // Quick Topic Presets Horizontal Selector
        item {
            Text("Focus Topic Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topicPresets) { preset ->
                    val isSelected = activeTopic.id == preset.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTopic(preset) },
                        label = { Text("${preset.name} (${preset.focusMinutes}m)") },
                        leadingIcon = {
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        // Remote Push to Watch Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Companion Watch Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Mirror active focus countdown and ambient vibration pulses to Wear OS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = {
                            onPushWatchAlert("Focus Session", "Started ${activeTopic.name} for ${activeTopic.focusMinutes} minutes")
                        }
                    ) {
                        Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push")
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. TOPICS & AMBIENT SOUND STUDIO VIEW (Matching Wear OS Focus Sound Synthesizer)
// =============================================================================
@Composable
private fun TopicsAndAmbientSoundPageView(
    topics: List<MobileFocusTopicPreset>,
    activeTopic: MobileFocusTopicPreset,
    ambientSoundMode: MobileAmbientSoundMode,
    isSoundMuted: Boolean,
    onSelectTopic: (MobileFocusTopicPreset) -> Unit,
    onSelectSoundMode: (MobileAmbientSoundMode) -> Unit,
    onToggleMute: () -> Unit,
    onOpenCreateTopic: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Procedural Audio Synthesizer Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Real-Time PCM Sound Synthesizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (!isSoundMuted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = "Mute"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Procedural ambient noise generated in real-time via low-latency AudioTrack (Zero asset overhead).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    MobileAmbientSoundMode.entries.forEach { mode ->
                        val isSelected = ambientSoundMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSoundMode(mode) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(mode.icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(mode.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Focus Presets Library
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Focus Disciplines & Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenCreateTopic) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Preset")
                }
            }
        }

        items(topics) { preset ->
            val isCurrent = activeTopic.id == preset.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTopic(preset) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(preset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${preset.focusMinutes}m Focus · ${preset.breakMinutes}m Break · Type: ${preset.type.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. HISTORY & SCORE ANALYTICS VIEW
// =============================================================================
@Composable
private fun FocusHistoryAndScorePageView(
    state: LifeState,
    activeHistory: List<FocusHistory>
) {
    val totalMins = activeHistory.sumOf { it.minutesFocused }
    val completedSessions = activeHistory.count { it.completed }
    val focusGoalMins = state.settings.focusGoalMinutes.coerceAtLeast(60)
    val goalPct = (totalMins.toFloat() / focusGoalMins.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DAILY DEEP WORK TARGET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("${totalMins}m / ${focusGoalMins}m", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("${(goalPct * 100).toInt()}% Met", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { goalPct },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "$completedSessions focus sessions completed · Best streak preserved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text("Past Focus Sessions (${activeHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (activeHistory.isEmpty()) {
            item {
                Text("No completed focus sessions recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(activeHistory.take(20)) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(session.subject.ifBlank { "Deep Work" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
                            Text(
                                if (session.completed) "Completed · $dateStr" else "Ended Early · $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${session.minutesFocused} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
