package com.creativeblisslabs.wearproductivity.mobile.ui.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.creativeblisslabs.wearproductivity.mobile.media.LrcLyricsParser
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.media.LocalMediaScanner
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// =============================================================================
// DOMAIN MODELS & ENUMS MATCHING WEAR AUDIO STUDIO
// =============================================================================
data class AudioTrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "LifeOS Audio",
    val durationSeconds: Int = 180,
    val folderPath: String = "Music",
    val contentUri: Uri? = null,
    val lyrics: String? = null,
    val lyricsSourceLabel: String = "Embedded",
    val liked: Boolean = false,
    val isBinaural: Boolean = false
) {
    val durationFormatted: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return "$mins:${secs.toString().padStart(2, '0')}"
        }
}

data class AudioPlaylistItem(
    val id: String,
    val name: String,
    val description: String,
    val accentHex: String = "mint",
    val trackIds: List<String>
)

data class EqPreset(
    val id: String,
    val name: String,
    val bandGainsDb: List<Float> // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-12f..+12f)
)

data class AudioRouteDevice(
    val id: String,
    val name: String,
    val typeLabel: String,
    val isBluetooth: Boolean,
    val isWatchRemote: Boolean = false
)

enum class AudioScreenTab(val label: String) {
    NOW_PLAYING("Player"),
    LIBRARY("Library"),
    EQUALIZER_OUTPUT("EQ & Route"),
    LYRICS("Lyrics Studio")
}

enum class LibrarySubTab(val label: String) {
    QUEUE("Queue"),
    ALL_TRACKS("Tracks"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders")
}

enum class RepeatMode(val label: String, val shortLabel: String) {
    OFF("Repeat Off", "Off"),
    ONE_TRACK("Repeat Track", "Track"),
    FOLDER("Repeat Folder", "Folder"),
    QUEUE_ALL("Repeat Queue", "Queue")
}

enum class ShuffleMode(val label: String, val shortLabel: String) {
    OFF("Shuffle Off", "Off"),
    QUEUE("Shuffle Queue", "Queue"),
    FOLDER("Shuffle Folder", "Folder")
}

// =============================================================================
// PERSISTENCE MANAGER FOR PLAYLISTS, EQ, FAVORITES, LYRICS & RECENT STATE
// =============================================================================
private object MobileAudioStudioPrefs {
    private const val PREFS_NAME = "lifeos_mobile_audio_studio_v3"
    private const val KEY_PLAYLISTS = "mobile_custom_playlists_v3"
    private const val KEY_EQ_PRESETS = "mobile_custom_eq_presets_v3"
    private const val KEY_LIKED_IDS = "mobile_liked_track_ids"
    private const val KEY_BURNED_LYRICS_PREFIX = "mobile_lrc_"

    fun loadPlaylists(context: Context): List<AudioPlaylistItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLAYLISTS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 5) {
                    AudioPlaylistItem(
                        id = p[0],
                        name = p[1],
                        description = p[2],
                        accentHex = p[3],
                        trackIds = p[4].split(",").filter { it.isNotBlank() }
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            AudioPlaylistItem("pl-fav", "Favourites", "Liked & starred tracks", "coral", emptyList()),
            AudioPlaylistItem("pl-workout", "Gym & Cardio Power", "High tempo rhythm sets", "mint", emptyList()),
            AudioPlaylistItem("pl-focus", "Deep Work Flow", "Alpha waves & binaural instrumentals", "cobalt", emptyList()),
            AudioPlaylistItem("pl-ambient", "Sleep & Wind Down", "Rain, ocean & theta frequency", "amber", emptyList())
        )
    }

    fun savePlaylists(context: Context, list: List<AudioPlaylistItem>) {
        val encoded = list.joinToString(";;") { pl ->
            "${pl.id}|${pl.name.replace("|", "")}|${pl.description.replace("|", "")}|${pl.accentHex}|${pl.trackIds.joinToString(",")}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLAYLISTS, encoded)
            .apply()
    }

    fun loadEqPresets(context: Context): List<EqPreset> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EQ_PRESETS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 3) {
                    val gains = p[2].split(",").mapNotNull { it.toFloatOrNull() }
                    if (gains.size == 5) EqPreset(p[0], p[1], gains) else null
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            EqPreset("eq-flat", "Flat Balanced", listOf(0f, 0f, 0f, 0f, 0f)),
            EqPreset("eq-bass", "Bass Heavy", listOf(7.0f, 5.0f, 0f, -1f, 1.5f)),
            EqPreset("eq-vocals", "Vocal Clarity", listOf(-2f, 1.5f, 6.0f, 4.0f, 1f)),
            EqPreset("eq-treble", "Treble Sparkle", listOf(-1f, 0f, 2f, 5.5f, 7.5f)),
            EqPreset("eq-acoustic", "Warm Acoustic", listOf(3.5f, 2.0f, 1.0f, 2.5f, 4.0f))
        )
    }

    fun saveEqPresets(context: Context, presets: List<EqPreset>) {
        val encoded = presets.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.bandGainsDb.joinToString(",")}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EQ_PRESETS, encoded)
            .apply()
    }

    fun loadLikedIds(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIKED_IDS, "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveLikedIds(context: Context, likedIds: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIKED_IDS, likedIds.joinToString(","))
            .apply()
    }

    fun saveCachedLyrics(context: Context, trackId: String, lrcText: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_BURNED_LYRICS_PREFIX$trackId", lrcText)
            .apply()
    }

    fun loadCachedLyrics(context: Context, trackId: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_BURNED_LYRICS_PREFIX$trackId", null)
    }
}

// =============================================================================
// MAIN COMPOSABLE: MOBILE AUDIO STUDIO (100% WEAR FEATURE PARITY)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileAudioScreen(repository: LifeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(AudioScreenTab.NOW_PLAYING) }
    var activeLibSubTab by remember { mutableStateOf(LibrarySubTab.QUEUE) }

    // Audio Playback State
    var isPlaying by remember { mutableStateOf(false) }
    var currentTrack by remember {
        mutableStateOf(
            AudioTrackItem(
                id = "preset-1",
                title = "Deep Focus Binaural 432Hz",
                artist = "LifeOS Acoustic Studio",
                album = "Neuro-Acoustics Vol 1",
                durationSeconds = 2700,
                folderPath = "/storage/emulated/0/Music",
                isBinaural = true
            )
        )
    }
    var elapsedSeconds by remember { mutableIntStateOf(345) }
    var repeatMode by remember { mutableStateOf(RepeatMode.OFF) }
    var shuffleMode by remember { mutableStateOf(ShuffleMode.OFF) }
    var volumeLevel by remember { mutableFloatStateOf(0.70f) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var isFetchingLyrics by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf<String?>(null) }

    // Dialog state
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<AudioTrackItem?>(null) }

    // Permissions State
    val audioPermissionGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        audioPermissionGranted.value = isGranted
    }

    // Playlists & EQ Presets
    var playlists by remember { mutableStateOf(MobileAudioStudioPrefs.loadPlaylists(context)) }
    var eqPresets by remember { mutableStateOf(MobileAudioStudioPrefs.loadEqPresets(context)) }
    var selectedEqPresetId by remember { mutableStateOf(eqPresets.first().id) }
    var activeBands by remember { mutableStateOf(eqPresets.first().bandGainsDb) }
    var likedTrackIds by remember { mutableStateOf(MobileAudioStudioPrefs.loadLikedIds(context)) }

    // Audio Tracks Library (Discovered + Ambient Presets)
    val libraryTracks = remember { mutableStateListOf<AudioTrackItem>() }
    val queueTracks = remember { mutableStateListOf<AudioTrackItem>() }

    // Load Default Presets + Local Media Scanner
    fun refreshTracks() {
        scope.launch(Dispatchers.IO) {
            val list = mutableListOf<AudioTrackItem>()
            // Built-in curated studio tracks
            list.add(AudioTrackItem("preset-1", "Deep Focus Binaural 432Hz", "LifeOS Acoustic Studio", "Alpha Flow", 2700, "/storage/Music", isBinaural = true))
            list.add(AudioTrackItem("preset-2", "Ambient Rainfall & Theta Waves", "Nature Soundscapes", "Atmospheric Labs", 3600, "/storage/Music", isBinaural = true))
            list.add(AudioTrackItem("preset-3", "Workout High Energy BPM", "Cardio Beats", "Peak Performance", 1935, "/storage/Workout"))
            list.add(AudioTrackItem("preset-4", "Evening Wind Down & Delta Waves", "Mindfulness Lab", "Rest Recovery", 1500, "/storage/Music", isBinaural = true))
            list.add(AudioTrackItem("preset-5", "Pure White Noise Machine", "Focus Generator", "Sound Masking", 5400, "/storage/WhiteNoise"))

            if (audioPermissionGranted.value) {
                val scanned = LocalMediaScanner.scanLocalAudio(context)
                scanned.forEach { disc ->
                    list.add(
                        AudioTrackItem(
                            id = disc.id,
                            title = disc.title,
                            artist = disc.artist,
                            album = disc.album,
                            durationSeconds = (disc.durationMs / 1000).toInt(),
                            folderPath = disc.folderPath,
                            contentUri = disc.contentUri,
                            liked = disc.id in likedTrackIds
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                libraryTracks.clear()
                libraryTracks.addAll(list)
                if (queueTracks.isEmpty()) {
                    queueTracks.addAll(list.take(10))
                }
            }
        }
    }

    LaunchedEffect(audioPermissionGranted.value) {
        refreshTracks()
    }

    // Connected Routes
    val availableRoutes = remember {
        val list = mutableListOf<AudioRouteDevice>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val devs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devs) {
                val isBt = dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                val name = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: when (dev.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Headset"
                    else -> "Audio Output"
                }
                val typeLabel = if (isBt) "Bluetooth Wireless" else "Internal Speaker"
                if (list.none { it.name.equals(name, ignoreCase = true) }) {
                    list.add(AudioRouteDevice("dev-${dev.id}", name, typeLabel, isBt))
                }
            }
        }
        if (list.none { !it.isBluetooth }) {
            list.add(AudioRouteDevice("phone-speaker", "Phone Speaker", "Internal Device Speaker", false))
        }
        list.add(AudioRouteDevice("wear-remote", "Connected Wear OS Watch", "DataLayer Remote Streaming", false, isWatchRemote = true))
        list
    }
    var selectedRoute by remember { mutableStateOf(availableRoutes.first()) }

    // Wear Command Sender
    fun sendWearCommand(action: String) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                nodes.firstOrNull()?.let { node ->
                    Wearable.getMessageClient(context).sendMessage(
                        node.id,
                        "/lifeos/phone_media_control/$action",
                        action.toByteArray()
                    ).await()
                    statusNotice = "Watch: '$action' synced"
                } ?: run {
                    statusNotice = "No Watch Connected"
                }
            } catch (_: Exception) {
                statusNotice = "Failed to sync with Watch"
            }
        }
    }

    // LRCLIB Synced Lyrics Fetcher
    fun fetchLyrics(track: AudioTrackItem) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isFetchingLyrics = true }
            try {
                val encodedTitle = URLEncoder.encode(track.title.trim(), "UTF-8")
                val encodedArtist = URLEncoder.encode(track.artist.trim(), "UTF-8")
                val searchUrl = URL("https://lrclib.net/api/search?track_name=$encodedTitle&artist_name=$encodedArtist")
                val conn = (searchUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "LifeOS-Companion-AudioStudio/3.0")
                }

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(body)
                    var foundLyrics: String? = null
                    var sourceTag = "LRCLIB"
                    for (i in 0 until arr.length()) {
                        val obj: JSONObject = arr.getJSONObject(i)
                        val synced = obj.optString("syncedLyrics", "")
                        if (synced.isNotBlank() && synced != "null") {
                            foundLyrics = synced
                            sourceTag = "LRCLIB Synced"
                            break
                        }
                    }
                    if (foundLyrics == null && arr.length() > 0) {
                        val plain = arr.getJSONObject(0).optString("plainLyrics", "")
                        if (plain.isNotBlank() && plain != "null") {
                            foundLyrics = plain
                            sourceTag = "LRCLIB Plain"
                        }
                    }

                    if (!foundLyrics.isNullOrBlank()) {
                        MobileAudioStudioPrefs.saveCachedLyrics(context, track.id, foundLyrics)
                        withContext(Dispatchers.Main) {
                            currentTrack = currentTrack.copy(lyrics = foundLyrics, lyricsSourceLabel = sourceTag)
                            statusNotice = "Fetched synced lyrics from LRCLIB"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            statusNotice = "No lyrics found on LRCLIB"
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    statusNotice = "Lyrics search failed"
                }
            } finally {
                withContext(Dispatchers.Main) { isFetchingLyrics = false }
            }
        }
    }

    fun playTrack(track: AudioTrackItem) {
        currentTrack = track
        isPlaying = true
        elapsedSeconds = 0
        sendWearCommand("play")
        val cached = MobileAudioStudioPrefs.loadCachedLyrics(context, track.id)
        if (!cached.isNullOrBlank()) {
            currentTrack = currentTrack.copy(lyrics = cached, lyricsSourceLabel = "Saved Cache")
        } else {
            fetchLyrics(track)
        }
    }

    fun toggleLike(trackId: String) {
        val next = likedTrackIds.toMutableSet()
        if (trackId in next) next.remove(trackId) else next.add(trackId)
        likedTrackIds = next
        MobileAudioStudioPrefs.saveLikedIds(context, next)
        if (currentTrack.id == trackId) {
            currentTrack = currentTrack.copy(liked = trackId in next)
        }
        val idx = libraryTracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            libraryTracks[idx] = libraryTracks[idx].copy(liked = trackId in next)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Audio & Sound Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (selectedRoute.isWatchRemote) "Streaming to Watch Speaker" else "Phone Companion Audio Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sendWearCommand("sync_state") }) {
                        Icon(Icons.Rounded.Sync, contentDescription = "Sync Watch")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Tabs: Player, Library, EQ & Route, Lyrics Studio
            PrimaryTabRow(
                selectedTabIndex = activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                AudioScreenTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.label, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            AnimatedContent(
                targetState = activeTab,
                modifier = Modifier.weight(1f),
                label = "audio_tab_content"
            ) { targetTab ->
                when (targetTab) {
                    AudioScreenTab.NOW_PLAYING -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Hero Player Canvas & Controls
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Visualizer Wave Disk
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        )
                                                    )
                                                )
                                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (currentTrack.isBinaural) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(54.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    currentTrack.title,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${currentTrack.artist} • ${currentTrack.album}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = { toggleLike(currentTrack.id) }) {
                                                Icon(
                                                    imageVector = if (currentTrack.liked || currentTrack.id in likedTrackIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                    contentDescription = "Like",
                                                    tint = if (currentTrack.liked || currentTrack.id in likedTrackIds) Color(0xFFFF5252) else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        // Progress Slider
                                        val progress = if (currentTrack.durationSeconds > 0) {
                                            (elapsedSeconds.toFloat() / currentTrack.durationSeconds).coerceIn(0f, 1f)
                                        } else 0f

                                        Slider(
                                            value = progress,
                                            onValueChange = { frac ->
                                                elapsedSeconds = (frac * currentTrack.durationSeconds).toInt()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val elMins = elapsedSeconds / 60
                                            val elSecs = elapsedSeconds % 60
                                            Text("$elMins:${elSecs.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall)
                                            Text(currentTrack.durationFormatted, style = MaterialTheme.typography.labelSmall)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        // Transport Controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = {
                                                shuffleMode = when (shuffleMode) {
                                                    ShuffleMode.OFF -> ShuffleMode.QUEUE
                                                    ShuffleMode.QUEUE -> ShuffleMode.FOLDER
                                                    ShuffleMode.FOLDER -> ShuffleMode.OFF
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Rounded.Shuffle,
                                                    contentDescription = "Shuffle",
                                                    tint = if (shuffleMode != ShuffleMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(onClick = {
                                                sendWearCommand("previous")
                                                val prevIdx = (queueTracks.indexOfFirst { it.id == currentTrack.id } - 1).coerceAtLeast(0)
                                                if (queueTracks.isNotEmpty()) currentTrack = queueTracks[prevIdx]
                                            }) {
                                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(34.dp))
                                            }

                                            FilledIconButton(
                                                onClick = {
                                                    isPlaying = !isPlaying
                                                    sendWearCommand(if (isPlaying) "play" else "pause")
                                                },
                                                modifier = Modifier.size(62.dp)
                                            ) {
                                                Icon(
                                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                    contentDescription = "Play/Pause",
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }

                                            IconButton(onClick = {
                                                sendWearCommand("next")
                                                val nextIdx = (queueTracks.indexOfFirst { it.id == currentTrack.id } + 1).coerceAtMost(queueTracks.lastIndex)
                                                if (queueTracks.isNotEmpty()) currentTrack = queueTracks[nextIdx]
                                            }) {
                                                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(34.dp))
                                            }

                                            IconButton(onClick = {
                                                repeatMode = when (repeatMode) {
                                                    RepeatMode.OFF -> RepeatMode.ONE_TRACK
                                                    RepeatMode.ONE_TRACK -> RepeatMode.QUEUE_ALL
                                                    RepeatMode.QUEUE_ALL -> RepeatMode.FOLDER
                                                    RepeatMode.FOLDER -> RepeatMode.OFF
                                                }
                                            }) {
                                                Icon(
                                                    if (repeatMode == RepeatMode.ONE_TRACK) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                                    contentDescription = "Repeat",
                                                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        statusNotice?.let {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }

                            // 2. Output Routing Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (selectedRoute.isWatchRemote) Icons.Rounded.Watch else Icons.Rounded.Speaker,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Output Route", fontWeight = FontWeight.Bold)
                                            }
                                            Text(selectedRoute.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            availableRoutes.forEach { route ->
                                                FilterChip(
                                                    selected = selectedRoute.id == route.id,
                                                    onClick = {
                                                        selectedRoute = route
                                                        sendWearCommand(if (route.isWatchRemote) "route_watch" else "route_phone")
                                                    },
                                                    label = { Text(route.name.take(16)) },
                                                    leadingIcon = {
                                                        Icon(
                                                            if (route.isWatchRemote) Icons.Rounded.Watch else if (route.isBluetooth) Icons.Rounded.Bluetooth else Icons.Rounded.Smartphone,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Quick Actions & Lyrics Jump
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            trackToAddToPlaylist = currentTrack
                                            showAddToPlaylistDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add to Playlist", fontSize = 12.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = { activeTab = AudioScreenTab.LYRICS },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("View Lyrics", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    AudioScreenTab.LIBRARY -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Library Sub Tabs: Queue, Tracks, Playlists, Albums, Artists, Folders
                            ScrollableTabRow(
                                selectedTabIndex = activeLibSubTab.ordinal,
                                edgePadding = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LibrarySubTab.entries.forEach { subTab ->
                                    Tab(
                                        selected = activeLibSubTab == subTab,
                                        onClick = { activeLibSubTab = subTab },
                                        text = { Text(subTab.label) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Storage Permission Banner
                            if (!audioPermissionGranted.value) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Enable audio storage permissions to discover device MP3s",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    Manifest.permission.READ_MEDIA_AUDIO
                                                } else {
                                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                                }
                                                permLauncher.launch(perm)
                                            }
                                        ) {
                                            Text("Grant", fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                when (activeLibSubTab) {
                                    LibrarySubTab.QUEUE -> {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Current Queue (${queueTracks.size})", fontWeight = FontWeight.Bold)
                                                TextButton(onClick = { queueTracks.clear() }) {
                                                    Text("Clear")
                                                }
                                            }
                                        }
                                        items(queueTracks) { track ->
                                            AudioTrackCard(
                                                track = track,
                                                isCurrent = track.id == currentTrack.id,
                                                isPlaying = isPlaying && track.id == currentTrack.id,
                                                onPlay = { playTrack(track) },
                                                onToggleLike = { toggleLike(track.id) },
                                                onAddToPlaylist = {
                                                    trackToAddToPlaylist = track
                                                    showAddToPlaylistDialog = true
                                                }
                                            )
                                        }
                                    }

                                    LibrarySubTab.ALL_TRACKS -> {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("All Tracks (${libraryTracks.size})", fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { refreshTracks() }) {
                                                    Icon(Icons.Rounded.Refresh, contentDescription = "Rescan")
                                                }
                                            }
                                        }
                                        items(libraryTracks) { track ->
                                            AudioTrackCard(
                                                track = track,
                                                isCurrent = track.id == currentTrack.id,
                                                isPlaying = isPlaying && track.id == currentTrack.id,
                                                onPlay = { playTrack(track) },
                                                onToggleLike = { toggleLike(track.id) },
                                                onAddToPlaylist = {
                                                    trackToAddToPlaylist = track
                                                    showAddToPlaylistDialog = true
                                                }
                                            )
                                        }
                                    }

                                    LibrarySubTab.PLAYLISTS -> {
                                        item {
                                            Button(
                                                onClick = { showCreatePlaylistDialog = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Create New Playlist")
                                            }
                                        }
                                        items(playlists) { pl ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(pl.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                        Text("${pl.description} • ${pl.trackIds.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    IconButton(onClick = {
                                                        val plTracks = libraryTracks.filter { it.id in pl.trackIds }
                                                        if (plTracks.isNotEmpty()) {
                                                            queueTracks.clear()
                                                            queueTracks.addAll(plTracks)
                                                            playTrack(plTracks.first())
                                                        }
                                                    }) {
                                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Playlist", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    LibrarySubTab.ALBUMS -> {
                                        val albums = libraryTracks.groupBy { it.album }
                                        items(albums.keys.toList()) { albumName ->
                                            val tracks = albums[albumName] ?: emptyList()
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(albumName, fontWeight = FontWeight.Bold)
                                                        Text("${tracks.firstOrNull()?.artist ?: "Various"} • ${tracks.size} tracks", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    IconButton(onClick = {
                                                        queueTracks.clear()
                                                        queueTracks.addAll(tracks)
                                                        playTrack(tracks.first())
                                                    }) {
                                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Album")
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    LibrarySubTab.ARTISTS -> {
                                        val artists = libraryTracks.groupBy { it.artist }
                                        items(artists.keys.toList()) { artistName ->
                                            val tracks = artists[artistName] ?: emptyList()
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(artistName, fontWeight = FontWeight.Bold)
                                                        Text("${tracks.size} songs", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    IconButton(onClick = {
                                                        queueTracks.clear()
                                                        queueTracks.addAll(tracks)
                                                        playTrack(tracks.first())
                                                    }) {
                                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Artist")
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    LibrarySubTab.FOLDERS -> {
                                        val folders = libraryTracks.groupBy { it.folderPath }
                                        items(folders.keys.toList()) { folderPath ->
                                            val tracks = folders[folderPath] ?: emptyList()
                                            val folderName = if (folderPath.contains("/")) folderPath.substringAfterLast("/") else folderPath
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(folderName, fontWeight = FontWeight.Bold)
                                                        Text(folderPath, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    IconButton(onClick = {
                                                        queueTracks.clear()
                                                        queueTracks.addAll(tracks)
                                                        playTrack(tracks.first())
                                                    }) {
                                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Folder")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AudioScreenTab.EQUALIZER_OUTPUT -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Equalizer Graph & Canvas Visualizer
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("5-Band DSP Graphic Equalizer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                eqPresets.firstOrNull { it.id == selectedEqPresetId }?.name ?: "Custom",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        // Interactive Canvas Curve
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                        ) {
                                            val midY = size.height / 2f
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.3f),
                                                start = Offset(0f, midY),
                                                end = Offset(size.width, midY),
                                                strokeWidth = 1.dp.toPx()
                                            )

                                            val points = activeBands.mapIndexed { i, gain ->
                                                val x = (i.toFloat() / (activeBands.size - 1).coerceAtLeast(1)) * (size.width - 24.dp.toPx()) + 12.dp.toPx()
                                                val normalized = (gain / 12f).coerceIn(-1f, 1f)
                                                val y = midY - (normalized * (size.height * 0.42f))
                                                Offset(x, y)
                                            }

                                            val path = Path().apply {
                                                points.forEachIndexed { i, pt ->
                                                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                                }
                                            }

                                            drawPath(
                                                path = path,
                                                color = Color(0xFF6750A4),
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )

                                            points.forEach { pt ->
                                                drawCircle(
                                                    color = Color(0xFF6750A4),
                                                    radius = 5.dp.toPx(),
                                                    center = pt
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 5 Frequency Band Sliders (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
                                        val freqLabels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            activeBands.forEachIndexed { idx, gain ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(freqLabels.getOrElse(idx) { "" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    Text("${gain.toInt()}dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Slider(
                                                        value = gain,
                                                        onValueChange = { newVal ->
                                                            val updated = activeBands.toMutableList()
                                                            updated[idx] = newVal
                                                            activeBands = updated
                                                            selectedEqPresetId = "custom"
                                                        },
                                                        valueRange = -12f..12f,
                                                        modifier = Modifier
                                                            .height(140.dp)
                                                            .padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. EQ Preset Chips
                            item {
                                Text("Preset Equalizer Modes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    eqPresets.forEach { preset ->
                                        FilterChip(
                                            selected = selectedEqPresetId == preset.id,
                                            onClick = {
                                                selectedEqPresetId = preset.id
                                                activeBands = preset.bandGainsDb
                                                sendWearCommand("eq_${preset.name.lowercase().replace(" ", "_")}")
                                            },
                                            label = { Text(preset.name) }
                                        )
                                    }
                                }
                            }

                            // 3. Audio Volume Stepper
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Output Volume Level", fontWeight = FontWeight.Bold)
                                            }
                                            Text("${(volumeLevel * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = volumeLevel,
                                            onValueChange = {
                                                volumeLevel = it
                                                sendWearCommand("volume_${(it * 100).toInt()}")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AudioScreenTab.LYRICS -> {
                        val parsedLines = remember(currentTrack.lyrics) {
                            LrcLyricsParser.parseLrc(currentTrack.lyrics)
                        }
                        val currentTimeMs = elapsedSeconds.toLong() * 1000L
                        val activeLineIdx = parsedLines.indexOfLast { it.timestampMs <= currentTimeMs }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(currentTrack.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(
                                            if (parsedLines.isEmpty()) "No lyrics loaded" else "Source: ${currentTrack.lyricsSourceLabel} (${parsedLines.size} lines)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { fetchLyrics(currentTrack) },
                                        enabled = !isFetchingLyrics
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isFetchingLyrics) "Fetching..." else "Fetch LRCLIB", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (parsedLines.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(8.dp))
                                        Text("No lyrics available for this song", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Tap 'Fetch LRCLIB' to download synchronized timecoded lyrics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(parsedLines.size) { idx ->
                                        val line = parsedLines[idx]
                                        val isActive = idx == activeLineIdx
                                        Text(
                                            text = line.text,
                                            style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (line.timestampMs > 0) {
                                                        elapsedSeconds = (line.timestampMs / 1000).toInt()
                                                    }
                                                }
                                                .padding(vertical = 4.dp)
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

    // Modal: Create Playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Audio Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            val newPl = AudioPlaylistItem(
                                id = "pl-${System.currentTimeMillis()}",
                                name = newPlaylistName.trim(),
                                description = "Custom mobile playlist",
                                trackIds = emptyList()
                            )
                            val updated = playlists + newPl
                            playlists = updated
                            MobileAudioStudioPrefs.savePlaylists(context, updated)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add to Playlist
    if (showAddToPlaylistDialog && trackToAddToPlaylist != null) {
        val targetTrack = trackToAddToPlaylist!!
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a playlist for '${targetTrack.title}':", style = MaterialTheme.typography.bodyMedium)
                    playlists.forEach { pl ->
                        FilledTonalButton(
                            onClick = {
                                val nextTrackIds = (pl.trackIds + targetTrack.id).distinct()
                                val updatedPl = pl.copy(trackIds = nextTrackIds)
                                val updatedList = playlists.map { if (it.id == pl.id) updatedPl else it }
                                playlists = updatedList
                                MobileAudioStudioPrefs.savePlaylists(context, updatedList)
                                showAddToPlaylistDialog = false
                                statusNotice = "Added to ${pl.name}"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(pl.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================
@Composable
private fun AudioTrackCard(
    track: AudioTrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlay() }
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (track.isBinaural) Color(0xFF6750A4).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.isBinaural) Icons.Rounded.Headphones else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = if (track.isBinaural) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        track.title,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${track.artist} • ${track.durationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (track.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (track.liked) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add to playlist", modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play"
                    )
                }
            }
        }
    }
}
