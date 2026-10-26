package com.creativeblisslabs.wearproductivity.wear.ui.audio

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.media.LrcLyricsParser
import com.creativeblisslabs.wearproductivity.wear.media.WearMediaLibraryService
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.animation.core.RepeatMode as RepeatGraphics
// =============================================================================
// AUDIO STUDIO DOMAIN MODELS & ENUMS
// =============================================================================
data class WearTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val folderName: String = "Music",
    val folderPath: String = "/sdcard/Music",
    val filePath: String = "",
    val contentUri: Uri? = null,
    val lyrics: String? = null,
    val lyricsSourceLabel: String = "Embedded",
    val formatExtension: String = "MP3",
    val sampleRateKhz: String = "44.1 kHz",
    val liked: Boolean = false
)

data class WearPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val accentHex: String = "mint",
    val trackIds: List<String>
)

data class CustomEqPreset(
    val id: String,
    val name: String,
    val bandGainsDb: List<Float> // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-12f..+12f)
)

data class OutputRouteDevice(
    val id: String,
    val name: String,
    val typeLabel: String,
    val isBluetooth: Boolean,
    val isPhoneRemote: Boolean = false,
    val deviceInfo: AudioDeviceInfo? = null
)

enum class RepeatMode(val label: String, val shortLabel: String) {
    OFF("Repeat Off", "Off"),
    ONE_FILE("Repeat Track", "Track"),
    FOLDER("Repeat Folder", "Folder"),
    QUEUE_ALL("Repeat Queue", "Queue")
}

enum class ShuffleScopeMode(val label: String, val shortLabel: String) {
    OFF("Shuffle Off", "Off"),
    QUEUE("Shuffle Queue", "Queue"),
    FOLDER("Shuffle Folder", "Folder")
}

enum class AudioLibraryTab(val label: String) {
    QUEUE("Queue"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders")
}

private enum class AudioModalOverlay {
    NONE,
    CURVED_VOLUME_AND_OUTPUT,
    QUICK_TRACK_OPTIONS,
    ADD_TO_PLAYLIST_PICKER,
    FOLDER_SCAN_MANAGER
}

private val eqBandFrequencies = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")

private fun formatPlaybackTime(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    return "${safeSeconds / 60}:${(safeSeconds % 60).toString().padStart(2, '0')}"
}


private fun DrawScope.drawScallopedProgressRing(
    progress: Float,
    phase: Float,
    trackColor: Color,
    activeColor: Color,
    strokeWidthPx: Float,
    lobes: Int = 10,
    waveAmplitudeRatio: Float = 0.06f
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxR = (minOf(size.width, size.height) / 2f) - (strokeWidthPx / 2f)
    val amp = maxR * waveAmplitudeRatio
    val baseRadius = maxR - amp
    val totalSteps = 180

    fun buildScallopPath(maxStep: Int): Path {
        val path = Path()
        for (i in 0..maxStep) {
            val fraction = i.toFloat() / totalSteps.toFloat()
            val angle = (-Math.PI / 2.0) + (fraction * 2.0 * Math.PI)
            val r = baseRadius + amp * kotlin.math.cos((lobes * angle) + phase).toFloat()
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    drawPath(
        path = buildScallopPath(totalSteps),
        color = trackColor,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val clamped = progress.coerceIn(0f, 1f)
    if (clamped > 0.005f) {
        val activeSteps = (totalSteps * clamped).toInt().coerceAtLeast(2)
        drawPath(
            path = buildScallopPath(activeSteps),
            color = activeColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
// =============================================================================
// PERSISTENCE MANAGER FOR LIBRARY SNAPSHOT, PLAYLISTS, FOLDERS, LYRICS & EQ
// =============================================================================
private object WearAudioStudioPrefs {
    private const val PREFS_NAME = "lifeos_wear_audio_studio_v3"
    private const val KEY_INCLUDED_FOLDERS = "included_scan_folders"
    private const val KEY_EXCLUDED_FOLDERS = "excluded_scan_folders"
    private const val KEY_PLAYLISTS = "custom_playlists_v3"
    private const val KEY_EQ_PRESETS = "custom_eq_presets_v3"
    private const val KEY_BURNED_LYRICS_PREFIX = "burned_lrc_"
    private const val KEY_LAST_TRACK_ID = "persisted_last_track_id"
    private const val KEY_LAST_ELAPSED_SECS = "persisted_last_elapsed_secs"
    private const val KEY_LAST_QUEUE_IDS = "persisted_last_queue_ids"
    private const val KEY_LIKED_TRACK_IDS = "persisted_liked_track_ids"
    private const val KEY_CACHED_LIBRARY_JSON = "persisted_cached_library_json_v1"

    fun loadIncludedFolders(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_INCLUDED_FOLDERS, null)
        if (!raw.isNullOrBlank()) {
            return raw.split("|").filter { it.isNotBlank() }
        }
        val root = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
        return listOf(
            "$root/Music",
            "$root/Download",
            "$root/Podcasts",
            "$root/Ringtones",
            "$root/Audiobooks",
            "/sdcard/Music",
            "/sdcard/Download"
        ).distinct()
    }

    fun loadExcludedFolders(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EXCLUDED_FOLDERS, null)
        if (!raw.isNullOrBlank()) {
            return raw.split("|").filter { it.isNotBlank() }
        }
        val root = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
        return listOf("$root/Android", "$root/Notifications")
    }

    fun saveIncludedFolders(context: Context, folders: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INCLUDED_FOLDERS, folders.distinct().joinToString("|"))
            .apply()
    }

    fun saveExcludedFolders(context: Context, folders: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EXCLUDED_FOLDERS, folders.distinct().joinToString("|"))
            .apply()
    }

    private const val KEY_LAST_TRACK_JSON = "persisted_last_track_json_v2"

    fun savePlaybackState(
        context: Context,
        track: WearTrack?,
        elapsedSecs: Int,
        queueIds: List<String>
    ) {
        if (track == null || track.id.isBlank() || track.id == "empty") return
        val trackJson = runCatching {
            JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("durationSeconds", track.durationSeconds)
                put("folderName", track.folderName)
                put("folderPath", track.folderPath)
                put("filePath", track.filePath)
                put("contentUri", track.contentUri?.toString() ?: "")
                put("formatExtension", track.formatExtension)
                put("sampleRateKhz", track.sampleRateKhz)
                put("liked", track.liked)
            }.toString()
        }.getOrDefault("")

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_TRACK_ID, track.id)
            .putString(KEY_LAST_TRACK_JSON, trackJson)
            .putInt(KEY_LAST_ELAPSED_SECS, elapsedSecs.coerceAtLeast(0))
            .putString(KEY_LAST_QUEUE_IDS, queueIds.take(150).joinToString(","))
            .apply()
    }

    fun loadPersistedTrackSnapshot(context: Context): WearTrack? {
        return runCatching {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_TRACK_JSON, null) ?: return null
            val o = JSONObject(raw)
            val id = o.optString("id")
            if (id.isBlank() || id == "empty") return null
            val uriStr = o.optString("contentUri")
            val cachedLyrics = loadBurnedLyricsCache(context, id)
            WearTrack(
                id = id,
                title = o.optString("title", "Audio Track"),
                artist = o.optString("artist", "Local Artist"),
                album = o.optString("album", "Watch Audio"),
                durationSeconds = o.optInt("durationSeconds", 180).coerceAtLeast(1),
                folderName = o.optString("folderName", "Music"),
                folderPath = o.optString("folderPath", "/sdcard/Music"),
                filePath = o.optString("filePath", ""),
                contentUri = if (uriStr.isNotBlank()) Uri.parse(uriStr) else null,
                lyrics = cachedLyrics,
                lyricsSourceLabel = if (!cachedLyrics.isNullOrBlank()) "Burned in File" else "None",
                formatExtension = o.optString("formatExtension", "MP3"),
                sampleRateKhz = o.optString("sampleRateKhz", "44.1 kHz"),
                liked = o.optBoolean("liked", false)
            )
        }.getOrNull()
    }

    fun loadPersistedPlaybackState(context: Context): Triple<String, Int, List<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trackId = prefs.getString(KEY_LAST_TRACK_ID, "") ?: ""
        val elapsed = prefs.getInt(KEY_LAST_ELAPSED_SECS, 0).coerceAtLeast(0)
        val qRaw = prefs.getString(KEY_LAST_QUEUE_IDS, "") ?: ""
        val qIds = if (qRaw.isNotBlank()) qRaw.split(",").filter { it.isNotBlank() } else emptyList()
        return Triple(trackId, elapsed, qIds)
    }

    fun saveLikedTrackIds(context: Context, likedIds: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIKED_TRACK_IDS, likedIds.joinToString(","))
            .apply()
    }

    fun loadLikedTrackIds(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIKED_TRACK_IDS, "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveCachedLibrary(context: Context, tracks: List<WearTrack>) {
        runCatching {
            val arr = JSONArray()
            tracks.take(250).forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("album", t.album)
                    put("durationSeconds", t.durationSeconds)
                    put("folderName", t.folderName)
                    put("folderPath", t.folderPath)
                    put("filePath", t.filePath)
                    put("contentUri", t.contentUri?.toString() ?: "")
                    put("formatExtension", t.formatExtension)
                    put("sampleRateKhz", t.sampleRateKhz)
                    put("liked", t.liked)
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_LIBRARY_JSON, arr.toString())
                .apply()
        }
    }

    fun loadCachedLibrary(context: Context): List<WearTrack> {
        return runCatching {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CACHED_LIBRARY_JSON, null) ?: return emptyList()
            val likedSet = loadLikedTrackIds(context)
            val arr = JSONArray(raw)
            val list = ArrayList<WearTrack>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                if (id.isBlank()) continue
                val uriStr = o.optString("contentUri")
                val cachedLyrics = loadBurnedLyricsCache(context, id)
                list.add(
                    WearTrack(
                        id = id,
                        title = o.optString("title", "Audio Track"),
                        artist = o.optString("artist", "Local Artist"),
                        album = o.optString("album", "Watch Audio"),
                        durationSeconds = o.optInt("durationSeconds", 180).coerceAtLeast(1),
                        folderName = o.optString("folderName", "Music"),
                        folderPath = o.optString("folderPath", "/sdcard/Music"),
                        filePath = o.optString("filePath", ""),
                        contentUri = if (uriStr.isNotBlank()) Uri.parse(uriStr) else null,
                        lyrics = cachedLyrics,
                        lyricsSourceLabel = if (!cachedLyrics.isNullOrBlank()) "Burned in File" else "None",
                        formatExtension = o.optString("formatExtension", "MP3"),
                        sampleRateKhz = o.optString("sampleRateKhz", "44.1 kHz"),
                        liked = o.optBoolean("liked", false) || id in likedSet
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun loadPlaylists(context: Context): List<WearPlaylist> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLAYLISTS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 5) {
                    WearPlaylist(
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
            WearPlaylist("pl-favourites", "Favourites", "Liked tracks", "coral", emptyList()),
            WearPlaylist("pl-workout", "Gym & Power", "High tempo sets", "mint", emptyList()),
            WearPlaylist("pl-focus", "Deep Coding", "Instrumental flow", "cobalt", emptyList()),
            WearPlaylist("pl-trek", "Himalayan Trails", "Acoustic & indie", "amber", emptyList())
        )
    }

    fun savePlaylists(context: Context, list: List<WearPlaylist>) {
        val encoded = list.joinToString(";;") { pl ->
            "${pl.id}|${pl.name.replace("|", "")}|${pl.description.replace("|", "")}|${pl.accentHex}|${pl.trackIds.joinToString(",")}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLAYLISTS, encoded)
            .apply()
    }

    fun loadEqPresets(context: Context): List<CustomEqPreset> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EQ_PRESETS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 3) {
                    val gains = p[2].split(",").mapNotNull { it.toFloatOrNull() }
                    if (gains.size == 5) CustomEqPreset(p[0], p[1], gains) else null
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            CustomEqPreset("eq-balanced", "Balanced", listOf(0f, 0f, 0f, 0f, 0f)),
            CustomEqPreset("eq-bass", "Bass Boost", listOf(6.5f, 4.5f, 0f, -1f, 1.5f)),
            CustomEqPreset("eq-vocals", "Vocal Clarity", listOf(-2f, 1.5f, 5.5f, 4f, 1f)),
            CustomEqPreset("eq-treble", "Treble Spark", listOf(-1f, 0f, 2f, 5f, 7f)),
            CustomEqPreset("eq-vshape", "Studio V-Curve", listOf(5f, 2f, -2.5f, 3f, 6f))
        )
    }

    fun saveEqPresets(context: Context, presets: List<CustomEqPreset>) {
        val encoded = presets.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.bandGainsDb.joinToString(",")}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EQ_PRESETS, encoded)
            .apply()
    }

    fun saveBurnedLyricsCache(context: Context, trackId: String, lrcContent: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_BURNED_LYRICS_PREFIX$trackId", lrcContent)
            .apply()
    }

    fun loadBurnedLyricsCache(context: Context, trackId: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_BURNED_LYRICS_PREFIX$trackId", null)
}

// =============================================================================
// REAL AUDIO DISCOVERY, ALBUM ARTWORK DECODER, LRCLIB CLIENT & LYRICS BURNER
// =============================================================================
private object WearAudioMediaEngine {

    fun hasAudioPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun requiredAudioPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    suspend fun scanAudioLibrary(
        context: Context,
        includedFolders: List<String>,
        excludedFolders: List<String>
    ): List<WearTrack> = withContext(Dispatchers.IO) {
        val discovered = linkedMapOf<String, WearTrack>()
        val audioExts = setOf("mp3", "wav", "m4a", "flac", "ogg", "aac", "opus", "amr")
        val likedIds = WearAudioStudioPrefs.loadLikedTrackIds(context)

        fun normalizePath(p: String): String =
            p.replaceFirst("^/sdcard".toRegex(), Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0")

        val normalizedExcluded = excludedFolders.map { normalizePath(it.trim()) }.filter { it.isNotBlank() }
        val normalizedIncluded = includedFolders.map { normalizePath(it.trim()) }.filter { it.isNotBlank() }

        fun isExcluded(path: String): Boolean {
            val norm = normalizePath(path)
            return normalizedExcluded.any { excl -> norm.startsWith(excl, ignoreCase = true) }
        }

        // 1. Query MediaStore.Audio.Media for indexed files
        runCatching {
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.RELATIVE_PATH
            )
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val relCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol).toString() else continue
                    val rawPath = (if (dataCol >= 0) cursor.getString(dataCol) else null).orEmpty()
                    val displayName = (if (nameCol >= 0) cursor.getString(nameCol) else null).orEmpty()
                    val relPath = (if (relCol >= 0) cursor.getString(relCol) else null).orEmpty()
                    val rootDir = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
                    val effectivePath = if (rawPath.isNotBlank()) rawPath else "$rootDir/${relPath.trim('/')}/$displayName"

                    if (isExcluded(effectivePath)) continue

                    val contentUri = ContentUris.withAppendedId(uri, id.toLongOrNull() ?: 0L)
                    val fileObj = File(effectivePath)
                    val parentFolder = fileObj.parentFile
                    val rawTitle = (if (titleCol >= 0) cursor.getString(titleCol) else null).orEmpty()
                    val rawArtist = (if (artistCol >= 0) cursor.getString(artistCol) else null).orEmpty()
                    val rawAlbum = (if (albumCol >= 0) cursor.getString(albumCol) else null).orEmpty()
                    val durMs = (if (durCol >= 0) cursor.getLong(durCol) else 0L).coerceAtLeast(1000L)
                    val ext = fileObj.extension.ifBlank { displayName.substringAfterLast('.', "MP3") }.uppercase(Locale.ROOT)

                    val burnedOrSidecar = readBurnedOrSidecarLyrics(context, id, effectivePath, null)

                    discovered[effectivePath.ifBlank { id }] = WearTrack(
                        id = id,
                        title = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.ifBlank { "Audio Track" },
                        artist = rawArtist.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Local Artist",
                        album = rawAlbum.takeIf { it.isNotBlank() && it != "<unknown>" } ?: (parentFolder?.name ?: "Watch Audio"),
                        durationSeconds = (durMs / 1000L).toInt().coerceAtLeast(1),
                        folderName = parentFolder?.name ?: "Music",
                        folderPath = parentFolder?.absolutePath ?: "$rootDir/Music",
                        filePath = effectivePath,
                        contentUri = contentUri,
                        lyrics = burnedOrSidecar.first,
                        lyricsSourceLabel = burnedOrSidecar.second,
                        formatExtension = ext,
                        sampleRateKhz = if (ext == "FLAC" || ext == "WAV") "48.0 kHz" else "44.1 kHz",
                        liked = id in likedIds
                    )
                }
            }
        }

        // 2. Direct File System Walk across Included Folders
        val allDirsToWalk = (normalizedIncluded + listOfNotNull(
            context.getExternalFilesDir(null)?.absolutePath,
            context.filesDir.absolutePath
        )).distinct()

        allDirsToWalk.forEach { folderPath ->
            if (isExcluded(folderPath)) return@forEach
            runCatching {
                val dir = File(folderPath)
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().maxDepth(4).forEach { f ->
                        if (f.isFile && f.extension.lowercase(Locale.ROOT) in audioExts && !isExcluded(f.absolutePath)) {
                            if (!discovered.containsKey(f.absolutePath)) {
                                val trackId = "file-${f.absolutePath.hashCode()}"
                                val burnedOrSidecar = readBurnedOrSidecarLyrics(context, trackId, f.absolutePath, null)
                                val ext = f.extension.uppercase(Locale.ROOT).ifBlank { "MP3" }
                                discovered[f.absolutePath] = WearTrack(
                                    id = trackId,
                                    title = f.nameWithoutExtension,
                                    artist = "Local Watch Track",
                                    album = f.parentFile?.name ?: "Folder",
                                    durationSeconds = 180,
                                    folderName = f.parentFile?.name ?: "Music",
                                    folderPath = f.parentFile?.absolutePath ?: folderPath,
                                    filePath = f.absolutePath,
                                    contentUri = Uri.fromFile(f),
                                    lyrics = burnedOrSidecar.first,
                                    lyricsSourceLabel = burnedOrSidecar.second,
                                    formatExtension = ext,
                                    sampleRateKhz = if (ext == "FLAC" || ext == "WAV") "48.0 kHz" else "44.1 kHz",
                                    liked = trackId in likedIds
                                )
                            }
                        }
                    }
                }
            }
        }

        val finalTracks = discovered.values.toList()
        if (finalTracks.isNotEmpty()) {
            WearAudioStudioPrefs.saveCachedLibrary(context, finalTracks)
        }
        finalTracks
    }

    private fun readBurnedOrSidecarLyrics(
        context: Context,
        trackId: String,
        filePath: String,
        embeddedLyrics: String?
    ): Pair<String?, String> {
        if (!embeddedLyrics.isNullOrBlank()) return embeddedLyrics to "Burned ID3 Tag"
        val cachedBurned = WearAudioStudioPrefs.loadBurnedLyricsCache(context, trackId)
        if (!cachedBurned.isNullOrBlank()) return cachedBurned to "Burned in File"
        if (filePath.isNotBlank()) {
            val audioFile = File(filePath)
            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val content = runCatching { lrcFile.readText() }.getOrNull()
                if (!content.isNullOrBlank()) return content to "Local .LRC File"
            }
        }
        return null to "None"
    }

    suspend fun fetchLyricsFromLrcLib(track: WearTrack): Pair<String?, String> = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = URLEncoder.encode(track.title.trim(), "UTF-8")
            val encodedArtist = URLEncoder.encode(track.artist.trim(), "UTF-8")
            val searchUrl = URL("https://lrclib.net/api/search?track_name=$encodedTitle&artist_name=$encodedArtist")
            val conn = (searchUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "LifeOS-WearOS-AudioStudio/3.0")
            }

            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj: JSONObject = arr.getJSONObject(i)
                    val synced = obj.optString("syncedLyrics", "")
                    if (synced.isNotBlank() && synced != "null") {
                        return@withContext synced to "LRCLIB Synced"
                    }
                }
                if (arr.length() > 0) {
                    val plain = arr.getJSONObject(0).optString("plainLyrics", "")
                    if (plain.isNotBlank() && plain != "null") {
                        val lines = plain.lines().filter { it.isNotBlank() }
                        val stepSec = (track.durationSeconds.coerceAtLeast(30).toFloat() / lines.size.coerceAtLeast(1))
                        val synthesizedLrc = lines.mapIndexed { idx, lineText ->
                            val totSec = (idx * stepSec).toInt()
                            val m = totSec / 60
                            val s = totSec % 60
                            "[%02d:%02d.00] %s".format(Locale.ROOT, m, s, lineText)
                        }.joinToString("\n")
                        return@withContext synthesizedLrc to "LRCLIB Auto-Timed"
                    }
                }
            }
        } catch (_: Exception) {}
        null to "Not Found"
    }

    suspend fun burnLyricsToAudioFile(
        context: Context,
        track: WearTrack,
        lrcText: String
    ): Boolean = withContext(Dispatchers.IO) {
        WearAudioStudioPrefs.saveBurnedLyricsCache(context, track.id, lrcText)
        var wroteDisk = false

        if (track.filePath.isNotBlank()) {
            val audioFile = File(track.filePath)
            runCatching {
                val sidecar = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
                sidecar.writeText(lrcText)
                wroteDisk = true
            }
        }
        runCatching {
            val vaultLyricsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "BurnedLyrics")
            vaultLyricsDir.mkdirs()
            File(vaultLyricsDir, "${track.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.lrc").writeText(lrcText)
            wroteDisk = true
        }
        wroteDisk
    }

    /**
     * Multi-stage Album Art loader:
     * 1. MediaMetadataRetriever via ContentUri FD or FilePath
     * 2. Android 10+ ContentResolver.loadThumbnail fallback
     * 3. Companion cover image in parent folder (cover.jpg, folder.png, etc.)
     */
    suspend fun loadTrackAlbumArt(
        context: Context,
        track: WearTrack,
        targetSizePx: Int = 256
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        fun decodeSampledBytes(bytes: ByteArray): ImageBitmap? {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
            var sample = 1
            while (opts.outWidth / sample > targetSizePx * 2 || opts.outHeight / sample > targetSizePx * 2) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)?.asImageBitmap()
        }

        // Stage 1: Open ParcelFileDescriptor via ContentResolver (most reliable on Android 11-15 / Wear OS)
        if (track.contentUri != null) {
            val fromFd = runCatching {
                context.contentResolver.openFileDescriptor(track.contentUri, "r")?.use { pfd ->
                    val mmr = MediaMetadataRetriever()
                    try {
                        mmr.setDataSource(pfd.fileDescriptor)
                        mmr.embeddedPicture?.let { decodeSampledBytes(it) }
                    } finally {
                        runCatching { mmr.release() }
                    }
                }
            }.getOrNull()
            if (fromFd != null) return@withContext fromFd
        }

        // Stage 2: Direct file path via FileInputStream FileDescriptor
        if (track.filePath.isNotBlank()) {
            val fromFileFd = runCatching {
                val file = File(track.filePath)
                if (file.exists() && file.canRead()) {
                    java.io.FileInputStream(file).use { fis ->
                        val mmr = MediaMetadataRetriever()
                        try {
                            mmr.setDataSource(fis.fd)
                            mmr.embeddedPicture?.let { decodeSampledBytes(it) }
                        } finally {
                            runCatching { mmr.release() }
                        }
                    }
                } else null
            }.getOrNull()
            if (fromFileFd != null) return@withContext fromFileFd
        }

        // Stage 3: Android 10+ ContentResolver.loadThumbnail (fetches system-cached album art)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && track.contentUri != null) {
            val thumb = runCatching {
                context.contentResolver.loadThumbnail(
                    track.contentUri,
                    AndroidSize(targetSizePx, targetSizePx),
                    null
                ).asImageBitmap()
            }.getOrNull()
            if (thumb != null) return@withContext thumb
        }

        // Stage 4: Companion cover image in parent folder
        if (track.filePath.isNotBlank()) {
            val coverBmp = runCatching {
                val parent = File(track.filePath).parentFile
                if (parent != null && parent.exists()) {
                    val coverFile = parent.listFiles()?.firstOrNull {
                        it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("jpg", "jpeg", "png", "webp")
                    }
                    coverFile?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
                } else null
            }.getOrNull()
            if (coverBmp != null) return@withContext coverBmp
        }

        null
    }

    fun getRealOutputRoutes(context: Context): List<OutputRouteDevice> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val list = mutableListOf<OutputRouteDevice>()
        runCatching {
            val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            devices.forEach { dev ->
                val isBt = dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                val name = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: when (dev.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Watch Speaker"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio"
                    else -> "Audio Output"
                }
                val typeLabel = when {
                    isBt -> "Bluetooth A2DP/BLE"
                    dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Internal Speaker"
                    else -> "Hardware Output"
                }
                if (list.none { it.name.equals(name, ignoreCase = true) }) {
                    list.add(OutputRouteDevice("dev-${dev.id}", name, typeLabel, isBt, false, dev))
                }
            }
        }
        if (list.none { !it.isBluetooth }) {
            list.add(OutputRouteDevice("watch-speaker", "This Watch Speaker", "Internal Speaker", false))
        }
        if (list.none { it.isPhoneRemote }) {
            list.add(OutputRouteDevice("phone-speaker", "Connected Phone Speaker", "DataLayer Remote Route", false, isPhoneRemote = true))
        }
        return list
    }

    fun switchHardwareAudioOutput(context: Context, route: OutputRouteDevice): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && route.deviceInfo != null) {
                audioManager.setCommunicationDevice(route.deviceInfo)
            } else {
                audioManager.isSpeakerphoneOn = !route.isBluetooth
            }
            true
        }.getOrDefault(false)
    }

    fun openBluetoothPairingSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

// =============================================================================
// MAIN COMPOSABLE: 3-PAGE WEAR OS AUDIO, LIBRARY, LYRICS & DSP EQUALIZER STUDIO
// =============================================================================
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearAudioScreen(state: LifeState, repository: LifeRepository) {
    val context = LocalContext.current
    var audioService by remember { mutableStateOf<WearMediaLibraryService?>(null) }

    DisposableEffect(context) {
        val intent = Intent(context, WearMediaLibraryService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        val connection = object : android.content.ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, binder: android.os.IBinder?) {
                val lb = binder as? WearMediaLibraryService.LocalBinder
                audioService = lb?.getService()
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                audioService = null
            }
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            runCatching { context.unbindService(connection) }
        }
    }

    val haptic = rememberLifeOSHapticProvider()
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var hasAudioPerm by remember { mutableStateOf(WearAudioMediaEngine.hasAudioPermission(context)) }
    var hasBtPerm by remember { mutableStateOf(WearAudioMediaEngine.hasBluetoothPermission(context)) }

    val includedFolders = remember {
        mutableStateListOf<String>().apply { addAll(WearAudioStudioPrefs.loadIncludedFolders(context)) }
    }
    val excludedFolders = remember {
        mutableStateListOf<String>().apply { addAll(WearAudioStudioPrefs.loadExcludedFolders(context)) }
    }

    val initialCachedLibrary = remember { WearAudioStudioPrefs.loadCachedLibrary(context) }
    val initialPersistedPlayback = remember { WearAudioStudioPrefs.loadPersistedPlaybackState(context) }
    val initialRestoredTrack = remember { WearAudioStudioPrefs.loadPersistedTrackSnapshot(context) }

    val allTracks = remember {
        mutableStateListOf<WearTrack>().apply {
            addAll(initialCachedLibrary)
            if (initialRestoredTrack != null && none { it.id == initialRestoredTrack.id }) {
                add(0, initialRestoredTrack)
            }
        }
    }
    val queue = remember {
        mutableStateListOf<WearTrack>().apply {
            val savedQ = initialPersistedPlayback.third.mapNotNull { id ->
                allTracks.firstOrNull { it.id == id }
            }
            addAll(savedQ.ifEmpty { allTracks })
            if (initialRestoredTrack != null && none { it.id == initialRestoredTrack.id }) {
                add(0, initialRestoredTrack)
            }
        }
    }
    val playlists = remember {
        mutableStateListOf<WearPlaylist>().apply { addAll(WearAudioStudioPrefs.loadPlaylists(context)) }
    }

    var restoredTrackSnapshot by remember { mutableStateOf(initialRestoredTrack) }
    var currentTrackId by remember {
        val savedId = initialPersistedPlayback.first.ifBlank { initialRestoredTrack?.id.orEmpty() }
        mutableStateOf(savedId.ifBlank { allTracks.firstOrNull()?.id.orEmpty() })
    }
    var isPlaying by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(initialPersistedPlayback.second) }
    var currentTrackAlbumBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val albumArtCache = remember { mutableStateMapOf<String, ImageBitmap>() }
    val equalizerRef = remember { mutableStateOf<Equalizer?>(null) }

    var repeatMode by remember { mutableStateOf(RepeatMode.OFF) }
    var shuffleScope by remember { mutableStateOf(ShuffleScopeMode.OFF) }
    var shuffleOrder by remember { mutableStateOf(emptyList<String>()) }
    var shufflePosition by remember { mutableIntStateOf(0) }

    val maxSystemVol = remember { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var volumeLevel by remember {
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        mutableFloatStateOf((cur.toFloat() / maxSystemVol.toFloat()).coerceIn(0f, 1f))
    }
    val outputRoutes = remember {
        mutableStateListOf<OutputRouteDevice>().apply { addAll(WearAudioMediaEngine.getRealOutputRoutes(context)) }
    }
    var selectedOutputRoute by remember {
        mutableStateOf(outputRoutes.firstOrNull() ?: OutputRouteDevice("watch-speaker", "This Watch Speaker", "Internal Speaker", false))
    }

    val eqPresets = remember {
        mutableStateListOf<CustomEqPreset>().apply { addAll(WearAudioStudioPrefs.loadEqPresets(context)) }
    }
    var selectedEqPresetId by remember { mutableStateOf(eqPresets.firstOrNull()?.id ?: "eq-balanced") }
    val activeEqBands = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }
    var showCustomEqCreator by remember { mutableStateOf(false) }

    var activeLibraryTab by remember { mutableStateOf(AudioLibraryTab.QUEUE) }
    var activeFilterTitle by remember { mutableStateOf<String?>(null) }
    var selectedAlbumForSquareDrilldown by remember { mutableStateOf<String?>(null) }
    var selectedArtistForDrilldown by remember { mutableStateOf<String?>(null) }
    val rootStoragePath = remember { Environment.getExternalStorageDirectory()?.absolutePath ?: "/sdcard" }
    var currentFolderBrowserPath by remember { mutableStateOf("$rootStoragePath/Music") }

    var showLyricsOverlay by remember { mutableStateOf(false) }
    var isFetchingLrcLib by remember { mutableStateOf(false) }
    var activeModalOverlay by remember { mutableStateOf(AudioModalOverlay.NONE) }
    var trackIdPendingPlaylistAdd by remember { mutableStateOf<String?>(null) }
    var showPlaylistEditor by remember { mutableStateOf(false) }
    var phoneMediaPlaying by remember { mutableStateOf(false) }

    var isRescanningLibrary by remember { mutableStateOf(false) }
    var scanProgressFraction by remember { mutableFloatStateOf(0f) }
    var scanStatusMessage by remember { mutableStateOf("Ready") }
    var feedbackOverlay by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    fun reconcileRestoredState(scanned: List<WearTrack>) {
        if (scanned.isEmpty()) return
        val (savedTrackId, savedElapsed, savedQueueIds) = WearAudioStudioPrefs.loadPersistedPlaybackState(context)

        // Match by exact ID first, or by file path / title if MediaStore reassigned ID
        fun findMatchingTrack(idOrKey: String): WearTrack? {
            return scanned.firstOrNull { it.id == idOrKey }
                ?: initialCachedLibrary.firstOrNull { it.id == idOrKey }?.let { old ->
                    scanned.firstOrNull {
                        (old.filePath.isNotBlank() && it.filePath.equals(old.filePath, ignoreCase = true)) ||
                                (it.title.equals(old.title, ignoreCase = true) && it.artist.equals(old.artist, ignoreCase = true))
                    }
                }
        }

        val reconstructedQueue = savedQueueIds.mapNotNull { findMatchingTrack(it) }
        queue.clear()
        queue.addAll(reconstructedQueue.ifEmpty { scanned })

        val activeSvcTrackId = audioService?.currentTrackId.orEmpty()
        val activeSvcUri = audioService?.currentTrackUri
        val matchedFromService = when {
            activeSvcTrackId.isNotBlank() -> findMatchingTrack(activeSvcTrackId)
            activeSvcUri != null -> scanned.firstOrNull { it.contentUri == activeSvcUri }
            else -> null
        }

        val matchedSaved = if (savedTrackId.isNotBlank()) findMatchingTrack(savedTrackId) else null

        when {
            matchedFromService != null -> {
                currentTrackId = matchedFromService.id
                if (queue.none { it.id == matchedFromService.id }) {
                    queue.add(0, matchedFromService)
                }
            }
            matchedSaved != null -> {
                currentTrackId = matchedSaved.id
                if (queue.none { it.id == matchedSaved.id }) {
                    queue.add(0, matchedSaved)
                }
                if (audioService?.isPlaying() != true && elapsedSeconds == 0) {
                    elapsedSeconds = savedElapsed.coerceAtLeast(0)
                }
            }
            currentTrackId.isBlank() || queue.none { it.id == currentTrackId } -> {
                currentTrackId = queue.firstOrNull()?.id ?: scanned.first().id
            }
        }
    }

    fun triggerLibraryRescan() {
        if (isRescanningLibrary) return
        isRescanningLibrary = true
        scanProgressFraction = 0.12f
        scanStatusMessage = "Querying MediaStore..."

        scope.launch {
            hasAudioPerm = WearAudioMediaEngine.hasAudioPermission(context)

            scanProgressFraction = 0.45f
            scanStatusMessage = "Scanning audio folders..."

            val scanned = WearAudioMediaEngine.scanAudioLibrary(context, includedFolders, excludedFolders)

            scanProgressFraction = 0.82f
            scanStatusMessage = "Restoring queue & tags..."

            if (scanned.isNotEmpty()) {
                allTracks.clear()
                allTracks.addAll(scanned)
                reconcileRestoredState(scanned)
            }

            outputRoutes.clear()
            outputRoutes.addAll(WearAudioMediaEngine.getRealOutputRoutes(context))

            scanProgressFraction = 1.0f
            scanStatusMessage = "Ready (${allTracks.size} tracks)"
            delay(350L)
            isRescanningLibrary = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasAudioPerm = grants[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                grants[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                WearAudioMediaEngine.hasAudioPermission(context)
        hasBtPerm = WearAudioMediaEngine.hasBluetoothPermission(context)
        triggerLibraryRescan()
        feedbackOverlay = (if (hasAudioPerm) "Audio Library Unlocked" else "Audio Permission Needed") to !hasAudioPerm
    }

    // 1. Trigger Library Scan on Launch
    LaunchedEffect(Unit) {
        if (!WearAudioMediaEngine.hasAudioPermission(context)) {
            permissionLauncher.launch(WearAudioMediaEngine.requiredAudioPermissions())
        } else {
            triggerLibraryRescan()
        }
    }

    val currentIndex = queue.indexOfFirst { it.id == currentTrackId }.let { if (it >= 0) it else 0 }
    val currentTrack = queue.firstOrNull { it.id == currentTrackId }
        ?: allTracks.firstOrNull { it.id == currentTrackId }
        ?: restoredTrackSnapshot?.takeIf { it.id == currentTrackId || currentTrackId.isBlank() }
        ?: queue.getOrNull(currentIndex)
        ?: allTracks.firstOrNull()
        ?: WearTrack("empty", "No Track Loaded", "Tap Library to Scan", "LifeOS Audio", 180)

    val progress = if (currentTrack.durationSeconds > 0) {
        (elapsedSeconds.toFloat() / currentTrack.durationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Sync with Background Service on Bind + Prepare Restored Track if Service is Idle
    LaunchedEffect(audioService, currentTrack.id, allTracks.size) {
        val svc = audioService ?: return@LaunchedEffect
        val svcPlaying = svc.isPlaying()
        isPlaying = svcPlaying

        // Match service's active track by ID, URI, or FilePath
        val svcTrackId = svc.currentTrackId
        val svcUri = svc.currentTrackUri
        val matchedFromService = allTracks.firstOrNull {
            (svcTrackId.isNotBlank() && it.id == svcTrackId) ||
                    (svcUri != null && (it.contentUri == svcUri || (it.filePath.isNotBlank() && svcUri.path == it.filePath)))
        }

        if (matchedFromService != null && matchedFromService.id != currentTrackId) {
            currentTrackId = matchedFromService.id
            restoredTrackSnapshot = matchedFromService
            if (queue.none { it.id == matchedFromService.id }) {
                queue.add(0, matchedFromService)
            }
        }

        val posMs = svc.getCurrentPosition()
        if (posMs > 0) {
            elapsedSeconds = posMs / 1000
        }

        val durMs = svc.getDuration()
        if (durMs > 1000 && currentTrack.id != "empty") {
            val realDurSec = durMs / 1000
            if (currentTrack.durationSeconds != realDurSec) {
                restoredTrackSnapshot = currentTrack.copy(durationSeconds = realDurSec)
                val qIdx = queue.indexOfFirst { it.id == currentTrack.id }
                if (qIdx >= 0) queue[qIdx] = queue[qIdx].copy(durationSeconds = realDurSec)
            }
        }

        // If service is idle after cold start, prepare the restored track at its saved elapsedSeconds
        if (!svcPlaying && !svc.isTrackPrepared() && currentTrack.id != "empty") {
            val uriToPrep = currentTrack.contentUri
                ?: currentTrack.filePath.takeIf { it.isNotBlank() }?.let { Uri.fromFile(File(it)) }
            if (uriToPrep != null) {
                svc.playTrack(
                    uri = uriToPrep,
                    title = currentTrack.title,
                    trackId = currentTrack.id,
                    startPositionMs = elapsedSeconds * 1000,
                    autoStart = false
                )
            }
        }
    }

    // 3. Decode Current Track Artwork + Fetch Missing Lyrics
    LaunchedEffect(currentTrack.id, currentTrack.contentUri, currentTrack.filePath) {
        if (currentTrack.id == "empty") return@LaunchedEffect

        val cachedByTrack = albumArtCache[currentTrack.id] ?: albumArtCache[currentTrack.album]
        if (cachedByTrack != null) {
            currentTrackAlbumBitmap = cachedByTrack
        } else {
            val loadedArt = WearAudioMediaEngine.loadTrackAlbumArt(context, currentTrack, targetSizePx = 320)
            currentTrackAlbumBitmap = loadedArt
            if (loadedArt != null) {
                albumArtCache[currentTrack.id] = loadedArt
                albumArtCache[currentTrack.album] = loadedArt
            }
        }

        if (currentTrack.lyrics.isNullOrBlank()) {
            isFetchingLrcLib = true
            val (fetchedLrc, sourceTag) = WearAudioMediaEngine.fetchLyricsFromLrcLib(currentTrack)
            isFetchingLrcLib = false
            if (!fetchedLrc.isNullOrBlank()) {
                val qIdx = queue.indexOfFirst { it.id == currentTrack.id }
                if (qIdx >= 0) {
                    queue[qIdx] = queue[qIdx].copy(lyrics = fetchedLrc, lyricsSourceLabel = sourceTag)
                }
                val aIdx = allTracks.indexOfFirst { it.id == currentTrack.id }
                if (aIdx >= 0) {
                    allTracks[aIdx] = allTracks[aIdx].copy(lyrics = fetchedLrc, lyricsSourceLabel = sourceTag)
                }
            }
        }
    }

    // 4. Background Album Artwork Preloader for Queue, Playlists, and Album Grid
    LaunchedEffect(allTracks.size, queue.size) {
        if (allTracks.isEmpty()) return@LaunchedEffect
        val prioritized = (queue + allTracks).distinctBy { it.id }
        for (track in prioritized) {
            if (!isActive) break
            if (albumArtCache.containsKey(track.id)) continue
            val bmp = WearAudioMediaEngine.loadTrackAlbumArt(context, track, targetSizePx = 144)
            if (bmp != null) {
                albumArtCache[track.id] = bmp
                if (!albumArtCache.containsKey(track.album)) {
                    albumArtCache[track.album] = bmp
                }
                if (track.id == currentTrack.id && currentTrackAlbumBitmap == null) {
                    currentTrackAlbumBitmap = bmp
                }
            }
        }
    }

    // Apply Equalizer Gains to Hardware DSP Equalizer attached to Service AudioSession
    fun applyHardwareEqualizerGains(gainsDb: List<Float>) {
        for (i in 0 until minOf(5, gainsDb.size)) {
            activeEqBands[i] = gainsDb[i].coerceIn(-12f, 12f)
        }
        runCatching {
            val sessionId = audioService?.getAudioSessionId() ?: 0
            if (equalizerRef.value == null && sessionId > 0) {
                equalizerRef.value = Equalizer(0, sessionId).apply { enabled = true }
            }
            val eq = equalizerRef.value ?: return
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            for (b in 0 until minOf(numBands, 5)) {
                val gainMb = (activeEqBands[b] * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(b.toShort(), gainMb)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (currentTrackId.isNotBlank() && currentTrackId != "empty") {
                WearAudioStudioPrefs.savePlaybackState(
                    context = context,
                    track = currentTrack,
                    elapsedSecs = elapsedSeconds,
                    queueIds = queue.map { it.id }
                )
            }
            runCatching { equalizerRef.value?.release() }
        }
    }

    fun updateSystemVolume(newFraction: Float) {
        val clamped = newFraction.coerceIn(0f, 1f)
        volumeLevel = clamped
        runCatching {
            val index = (clamped * maxSystemVol).roundToInt().coerceIn(0, maxSystemVol)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        }
    }

    fun rebuildShufflePool(anchorTrack: WearTrack, scopeMode: ShuffleScopeMode): List<String> {
        val pool = when (scopeMode) {
            ShuffleScopeMode.FOLDER -> queue.filter { it.folderPath.equals(anchorTrack.folderPath, ignoreCase = true) }
                .ifEmpty { queue }
            else -> queue
        }
        return listOf(anchorTrack.id) + pool.filter { it.id != anchorTrack.id }.shuffled().map { it.id }
    }

    fun togglePlayPause() {
        val svc = audioService
        if (svc != null) {
            val isNowPlaying = svc.togglePlayPause(
                fallbackUri = currentTrack.contentUri,
                fallbackTitle = currentTrack.title,
                fallbackTrackId = currentTrack.id,
                fallbackPositionMs = elapsedSeconds * 1000
            )
            isPlaying = isNowPlaying
        } else {
            isPlaying = !isPlaying
        }

        // Fixed: Pass 'currentTrack' (WearTrack) instead of 'currentTrackId' (String)
        if (currentTrackId.isNotBlank() && currentTrackId != "empty") {
            WearAudioStudioPrefs.savePlaybackState(
                context = context,
                track = currentTrack,
                elapsedSecs = elapsedSeconds,
                queueIds = queue.map { it.id }
            )
        }
        haptic.click()
    }
    fun selectTrack(trackId: String, startPlayback: Boolean = true) {
        val targetIdx = queue.indexOfFirst { it.id == trackId }
        val resolved: WearTrack = if (targetIdx < 0) {
            val fromAll = allTracks.firstOrNull { it.id == trackId }
            if (fromAll != null) {
                queue.add(0, fromAll)
                fromAll
            } else {
                feedbackOverlay = "Track Unavailable" to true
                haptic.click()
                return
            }
        } else {
            queue[targetIdx]
        }

        currentTrackId = resolved.id
        restoredTrackSnapshot = resolved
        elapsedSeconds = 0
        isPlaying = startPlayback
        currentTrackAlbumBitmap = albumArtCache[resolved.id] ?: albumArtCache[resolved.album]

        val uriToPlay = resolved.contentUri
            ?: resolved.filePath.takeIf { it.isNotBlank() }?.let { Uri.fromFile(File(it)) }

        if (uriToPlay != null && audioService != null) {
            audioService?.playTrack(
                uri = uriToPlay,
                title = resolved.title,
                trackId = resolved.id,
                startPositionMs = 0,
                autoStart = startPlayback
            )
        }

        if (shuffleScope != ShuffleScopeMode.OFF) {
            shuffleOrder = rebuildShufflePool(resolved, shuffleScope)
            shufflePosition = 0
        }

        WearAudioStudioPrefs.savePlaybackState(
            context = context,
            track = resolved,
            elapsedSecs = 0,
            queueIds = queue.map { it.id }
        )

        feedbackOverlay = "Playing ${resolved.title}" to false
        haptic.click()
    }

    fun seekToFraction(fraction: Float) {
        val targetSec = (fraction.coerceIn(0f, 1f) * currentTrack.durationSeconds).roundToInt().coerceIn(0, currentTrack.durationSeconds)
        elapsedSeconds = targetSec
        val svc = audioService
        if (svc != null) {
            if (!svc.isTrackPrepared() && currentTrack.contentUri != null) {
                svc.playTrack(
                    uri = currentTrack.contentUri,
                    title = currentTrack.title,
                    trackId = currentTrack.id,
                    startPositionMs = targetSec * 1000,
                    autoStart = isPlaying
                )
            } else {
                svc.seekTo(targetSec * 1000)
            }
        }
        if (currentTrackId.isNotBlank() && currentTrackId != "empty") {
            WearAudioStudioPrefs.savePlaybackState(context, currentTrack, targetSec, queue.map { it.id })
        }
    }

    fun movePrevious() {
        if (queue.isEmpty()) {
            feedbackOverlay = "Queue Empty" to true
            return
        }
        if (elapsedSeconds > 5) {
            seekToFraction(0f)
            feedbackOverlay = "Restarted Track" to false
            haptic.click()
            return
        }
        val scopedList = if (repeatMode == RepeatMode.FOLDER || shuffleScope == ShuffleScopeMode.FOLDER) {
            queue.filter { it.folderPath.equals(currentTrack.folderPath, ignoreCase = true) }.ifEmpty { queue }
        } else queue

        if (shuffleScope != ShuffleScopeMode.OFF && shuffleOrder.isNotEmpty()) {
            val previousPosition = shufflePosition - 1
            if (previousPosition >= 0) {
                shufflePosition = previousPosition
                selectTrack(shuffleOrder[shufflePosition], startPlayback = true)
            } else if (repeatMode == RepeatMode.QUEUE_ALL || repeatMode == RepeatMode.FOLDER) {
                shufflePosition = shuffleOrder.lastIndex
                selectTrack(shuffleOrder[shufflePosition], startPlayback = true)
            } else {
                seekToFraction(0f)
            }
        } else {
            val idxInScoped = scopedList.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
            if (idxInScoped > 0) {
                selectTrack(scopedList[idxInScoped - 1].id, startPlayback = true)
            } else if (repeatMode == RepeatMode.QUEUE_ALL || repeatMode == RepeatMode.FOLDER) {
                selectTrack(scopedList.last().id, startPlayback = true)
            } else {
                seekToFraction(0f)
            }
        }
    }

    fun moveNext(atTrackEnd: Boolean = false) {
        if (queue.isEmpty()) {
            isPlaying = false
            audioService?.pause()
            feedbackOverlay = "Queue Empty" to true
            return
        }
        if (atTrackEnd && repeatMode == RepeatMode.ONE_FILE) {
            seekToFraction(0f)
            audioService?.resume()
            isPlaying = true
            return
        }

        val scopedList = if (repeatMode == RepeatMode.FOLDER || shuffleScope == ShuffleScopeMode.FOLDER) {
            queue.filter { it.folderPath.equals(currentTrack.folderPath, ignoreCase = true) }.ifEmpty { queue }
        } else queue

        if (shuffleScope != ShuffleScopeMode.OFF) {
            if (shuffleOrder.isEmpty()) {
                shuffleOrder = rebuildShufflePool(currentTrack, shuffleScope)
                shufflePosition = 0
            }
            val nextPosition = shufflePosition + 1
            if (nextPosition >= shuffleOrder.size) {
                if (repeatMode == RepeatMode.QUEUE_ALL || repeatMode == RepeatMode.FOLDER) {
                    shufflePosition = 0
                } else {
                    isPlaying = false
                    audioService?.pause()
                    elapsedSeconds = currentTrack.durationSeconds
                    feedbackOverlay = "Playback Finished" to false
                    return
                }
            } else {
                shufflePosition = nextPosition
            }
            selectTrack(shuffleOrder[shufflePosition], startPlayback = true)
            return
        }

        val idxInScoped = scopedList.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
        val nextIdx = idxInScoped + 1
        if (nextIdx < scopedList.size) {
            selectTrack(scopedList[nextIdx].id, startPlayback = true)
        } else if (repeatMode == RepeatMode.QUEUE_ALL || repeatMode == RepeatMode.FOLDER) {
            selectTrack(scopedList.first().id, startPlayback = true)
        } else {
            isPlaying = false
            audioService?.pause()
            elapsedSeconds = currentTrack.durationSeconds
            feedbackOverlay = "Queue Finished" to false
        }
    }

    // 5. Real-Time Service Polling & Periodic Persistence Loop
    LaunchedEffect(audioService, currentTrackId) {
        var tickCount = 0
        while (isActive) {
            delay(250L)
            val svc = audioService
            if (svc != null) {
                val nowPlaying = svc.isPlaying()
                isPlaying = nowPlaying

                val posMs = svc.getCurrentPosition()
                if (nowPlaying || posMs > 0) {
                    elapsedSeconds = (posMs / 1000).coerceAtLeast(0)
                }

                val durMs = svc.getDuration()
                if (durMs > 1000) {
                    val durSec = durMs / 1000
                    val qIdx = queue.indexOfFirst { it.id == currentTrackId }
                    if (qIdx >= 0 && queue[qIdx].durationSeconds != durSec) {
                        queue[qIdx] = queue[qIdx].copy(durationSeconds = durSec)
                    }
                }

                tickCount++
                // Save progress every ~5 seconds while playing
                if (nowPlaying && tickCount % 20 == 0 && currentTrackId.isNotBlank() && currentTrackId != "empty") {
                    WearAudioStudioPrefs.savePlaybackState(context, currentTrack, elapsedSeconds, queue.map
                    { it.id })
                }
            }
        }
    }

    fun cycleShuffleScope() {
        shuffleScope = when (shuffleScope) {
            ShuffleScopeMode.OFF -> ShuffleScopeMode.QUEUE
            ShuffleScopeMode.QUEUE -> ShuffleScopeMode.FOLDER
            ShuffleScopeMode.FOLDER -> ShuffleScopeMode.OFF
        }
        if (shuffleScope != ShuffleScopeMode.OFF) {
            shuffleOrder = rebuildShufflePool(currentTrack, shuffleScope)
            shufflePosition = 0
        } else {
            shuffleOrder = emptyList()
            shufflePosition = 0
        }
        feedbackOverlay = shuffleScope.label to false
        haptic.click()
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE_FILE
            RepeatMode.ONE_FILE -> RepeatMode.FOLDER
            RepeatMode.FOLDER -> RepeatMode.QUEUE_ALL
            RepeatMode.QUEUE_ALL -> RepeatMode.OFF
        }
        feedbackOverlay = repeatMode.label to false
        haptic.click()
    }

    fun moveQueueItemOrder(trackId: String, moveUp: Boolean) {
        haptic.click()
        val idx = queue.indexOfFirst { it.id == trackId }
        if (idx < 0) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx in queue.indices) {
            val item = queue.removeAt(idx)
            queue.add(targetIdx, item)
            WearAudioStudioPrefs.savePlaybackState(context, currentTrack, elapsedSeconds, queue.map { it.id })
            feedbackOverlay = (if (moveUp) "Moved Up in Queue" else "Moved Down in Queue") to false
        }
    }

    fun addTrackToPlaylist(trackId: String, playlistId: String) {
        haptic.click()
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx >= 0) {
            val pl = playlists[idx]
            if (trackId !in pl.trackIds) {
                playlists[idx] = pl.copy(trackIds = pl.trackIds + trackId)
                WearAudioStudioPrefs.savePlaylists(context, playlists)
            }
            feedbackOverlay = "Added to ${pl.name}" to false
        }
        activeModalOverlay = AudioModalOverlay.NONE
        trackIdPendingPlaylistAdd = null
    }

    fun toggleLike(trackId: String) {
        val qIdx = queue.indexOfFirst { it.id == trackId }
        val aIdx = allTracks.indexOfFirst { it.id == trackId }
        var isNowLiked = false
        if (qIdx >= 0) {
            isNowLiked = !queue[qIdx].liked
            queue[qIdx] = queue[qIdx].copy(liked = isNowLiked)
        }
        if (aIdx >= 0) {
            if (qIdx < 0) isNowLiked = !allTracks[aIdx].liked
            allTracks[aIdx] = allTracks[aIdx].copy(liked = isNowLiked)
        }
        WearAudioStudioPrefs.saveLikedTrackIds(context, allTracks.filter { it.liked }.map { it.id }.toSet())
        WearAudioStudioPrefs.saveCachedLibrary(context, allTracks)

        val favIdx = playlists.indexOfFirst { it.id == "pl-favourites" }
        if (favIdx >= 0) {
            val currentFavs = playlists[favIdx].trackIds.toMutableList()
            if (isNowLiked && trackId !in currentFavs) currentFavs.add(trackId)
            else if (!isNowLiked) currentFavs.remove(trackId)
            playlists[favIdx] = playlists[favIdx].copy(trackIds = currentFavs)
            WearAudioStudioPrefs.savePlaylists(context, playlists)
        }
        feedbackOverlay = (if (isNowLiked) "Added to Favourites" else "Removed from Favourites") to false
        haptic.click()
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    val adaptiveSpec = com.creativeblisslabs.wearproductivity.wear.ui.components.core.LocalWearAdaptiveSpec.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (showLyricsOverlay) {
                M3FullScreenLyricsOverlay(
                    currentTrack = currentTrack,
                    elapsedSeconds = elapsedSeconds,
                    isFetchingLrcLib = isFetchingLrcLib,
                    onSeek = { fraction ->
                        seekToFraction(fraction)
                        audioService?.resume()
                        isPlaying = true
                        feedbackOverlay = "Seeked to ${formatPlaybackTime(elapsedSeconds)}" to false
                        haptic.click()
                    },
                    onFetchLrcLibNow = {
                        haptic.click()
                        scope.launch {
                            isFetchingLrcLib = true
                            val (fetched, src) = WearAudioMediaEngine.fetchLyricsFromLrcLib(currentTrack)
                            isFetchingLrcLib = false
                            if (!fetched.isNullOrBlank()) {
                                val qIdx = queue.indexOfFirst { it.id == currentTrack.id }
                                if (qIdx >= 0) queue[qIdx] = queue[qIdx].copy(lyrics = fetched, lyricsSourceLabel = src)
                                val aIdx = allTracks.indexOfFirst { it.id == currentTrack.id }
                                if (aIdx >= 0) allTracks[aIdx] = allTracks[aIdx].copy(lyrics = fetched, lyricsSourceLabel = src)
                                feedbackOverlay = "Fetched from $src" to false
                            } else {
                                feedbackOverlay = "LRCLIB: No Match Found" to true
                            }
                        }
                    },
                    onBurnLyricsToFile = { lrcToBurn ->
                        haptic.click()
                        scope.launch {
                            val ok = WearAudioMediaEngine.burnLyricsToAudioFile(context, currentTrack, lrcToBurn)
                            val qIdx = queue.indexOfFirst { it.id == currentTrack.id }
                            if (qIdx >= 0) queue[qIdx] = queue[qIdx].copy(lyrics = lrcToBurn, lyricsSourceLabel = "Burned in File")
                            feedbackOverlay = (if (ok) "Lyrics Burned to Audio File!" else "Saved to Tag Cache") to false
                        }
                    },
                    onClose = {
                        showLyricsOverlay = false
                        haptic.click()
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
                        0 -> M3ExpressiveCanvasPlayerPage(
                            spec = adaptiveSpec,
                            currentTrack = currentTrack,
                            albumArtBitmap = currentTrackAlbumBitmap ?: albumArtCache[currentTrack.id] ?: albumArtCache[currentTrack.album],
                            elapsedSeconds = elapsedSeconds,
                            isPlaying = isPlaying,
                            progress = progress,
                            repeatMode = repeatMode,
                            shuffleScope = shuffleScope,
                            selectedOutput = selectedOutputRoute.name,
                            onTogglePlay = ::togglePlayPause,
                            onPrevious = ::movePrevious,
                            onNext = { moveNext(atTrackEnd = false) },
                            onOpenCurvedVolumeAndOutput = {
                                haptic.click()
                                outputRoutes.clear()
                                outputRoutes.addAll(WearAudioMediaEngine.getRealOutputRoutes(context))
                                activeModalOverlay = AudioModalOverlay.CURVED_VOLUME_AND_OUTPUT
                            },
                            onOpenQuickOptions = {
                                haptic.click()
                                activeModalOverlay = AudioModalOverlay.QUICK_TRACK_OPTIONS
                            },
                            onOpenLyrics = {
                                haptic.click()
                                showLyricsOverlay = true
                            },
                            onOpenLibrary = {
                                activeModalOverlay = AudioModalOverlay.NONE
                                haptic.click()
                                scope.launch { pagerState.animateScrollToPage(1) }
                            }
                        )

                        1 -> M3AudioLibraryBrowserPage(
                            spec = adaptiveSpec,
                            hasAudioPerm = hasAudioPerm,
                            isRescanning = isRescanningLibrary,
                            scanProgressFraction = scanProgressFraction,
                            scanStatusMessage = scanStatusMessage,
                            allTracks = allTracks,
                            queue = queue,
                            currentTrack = currentTrack,
                            playlists = playlists,
                            albumArtCache = albumArtCache,
                            activeLibraryTab = activeLibraryTab,
                            activeFilterTitle = activeFilterTitle,
                            selectedAlbumDrilldown = selectedAlbumForSquareDrilldown,
                            selectedArtistDrilldown = selectedArtistForDrilldown,
                            currentFolderBrowserPath = currentFolderBrowserPath,
                            rootStoragePath = rootStoragePath,
                            repeatMode = repeatMode,
                            shuffleScope = shuffleScope,
                            showPlaylistEditor = showPlaylistEditor,
                            elapsedSeconds = elapsedSeconds,
                            onRequestAudioPermissions = {
                                haptic.click()
                                permissionLauncher.launch(WearAudioMediaEngine.requiredAudioPermissions())
                            },
                            onRescanLibrary = {
                                haptic.click()
                                triggerLibraryRescan()
                                feedbackOverlay = "Rescanning Audio Folders..." to false
                            },
                            onOpenFolderManagerModal = {
                                haptic.click()
                                activeModalOverlay = AudioModalOverlay.FOLDER_SCAN_MANAGER
                            },
                            onSelectLibraryTab = {
                                haptic.click()
                                activeLibraryTab = it
                                selectedAlbumForSquareDrilldown = null
                                selectedArtistForDrilldown = null
                            },
                            onResetFilter = {
                                queue.clear()
                                queue.addAll(allTracks)
                                activeFilterTitle = null
                                selectedAlbumForSquareDrilldown = null
                                selectedArtistForDrilldown = null
                                WearAudioStudioPrefs.savePlaybackState(context, currentTrack,
                                    elapsedSeconds, queue.map { it.id })
                                feedbackOverlay = "Showing All ${allTracks.size} Tracks" to false
                                haptic.click()
                            },
                            onCycleShuffleScope = ::cycleShuffleScope,
                            onCycleRepeatMode = ::cycleRepeatMode,
                            onOpenLyrics = {
                                haptic.click()
                                showLyricsOverlay = true
                            },
                            onSelectTrack = { id -> selectTrack(id, true) },
                            onMoveQueueTrack = ::moveQueueItemOrder,
                            onRemoveFromQueue = { id ->
                                haptic.click()
                                queue.removeAll { it.id == id }
                                WearAudioStudioPrefs.savePlaybackState(context, currentTrack,
                                    elapsedSeconds, queue.map { it.id })
                                feedbackOverlay = "Removed from Queue" to false
                            },
                            onOpenAddToPlaylist = { id ->
                                haptic.click()
                                trackIdPendingPlaylistAdd = id
                                activeModalOverlay = AudioModalOverlay.ADD_TO_PLAYLIST_PICKER
                            },
                            onLikeToggle = ::toggleLike,
                            onTogglePlaylistEditor = {
                                haptic.click()
                                showPlaylistEditor = !showPlaylistEditor
                            },
                            onCreatePlaylist = { name, desc, colorKey ->
                                val cleanName = name.trim().ifBlank { "Custom Mix" }
                                playlists.add(
                                    0,
                                    WearPlaylist(
                                        id = "pl-${System.currentTimeMillis()}",
                                        name = cleanName,
                                        description = desc.ifBlank { "${queue.size} tracks" },
                                        accentHex = colorKey,
                                        trackIds = queue.map { it.id }
                                    )
                                )
                                WearAudioStudioPrefs.savePlaylists(context, playlists)
                                showPlaylistEditor = false
                                feedbackOverlay = "Created '$cleanName'" to false
                                haptic.click()
                            },
                            onDeletePlaylist = { plId ->
                                haptic.click()
                                playlists.removeAll { it.id == plId && it.id != "pl-favourites" }
                                WearAudioStudioPrefs.savePlaylists(context, playlists)
                                feedbackOverlay = "Playlist Deleted" to false
                            },
                            onLoadPlaylist = { pl ->
                                val matching = if (pl.id == "pl-favourites") {
                                    allTracks.filter { it.liked }
                                } else {
                                    allTracks.filter { it.id in pl.trackIds }
                                }
                                queue.clear()
                                queue.addAll(matching.ifEmpty { allTracks })
                                activeFilterTitle = "Playlist: ${pl.name}"
                                activeLibraryTab = AudioLibraryTab.QUEUE
                                queue.firstOrNull()?.let { selectTrack(it.id, startPlayback = true) }
                            },
                            onSelectSquareAlbum = { albumTitle ->
                                haptic.click()
                                selectedAlbumForSquareDrilldown = albumTitle
                            },
                            onPlayEntireAlbum = { albumTitle ->
                                val matching = allTracks.filter { it.album.equals(albumTitle, ignoreCase = true) }
                                queue.clear()
                                queue.addAll(matching.ifEmpty { allTracks })
                                activeFilterTitle = "Album: $albumTitle"
                                selectedAlbumForSquareDrilldown = null
                                activeLibraryTab = AudioLibraryTab.QUEUE
                                queue.firstOrNull()?.let { selectTrack(it.id, startPlayback = true) }
                            },
                            onSelectArtist = { artistName ->
                                haptic.click()
                                selectedArtistForDrilldown = artistName
                            },
                            onPlayEntireArtist = { artistName ->
                                val matching = allTracks.filter { it.artist.equals(artistName, ignoreCase = true) }
                                queue.clear()
                                queue.addAll(matching.ifEmpty { allTracks })
                                activeFilterTitle = "Artist: $artistName"
                                selectedArtistForDrilldown = null
                                activeLibraryTab = AudioLibraryTab.QUEUE
                                queue.firstOrNull()?.let { selectTrack(it.id, startPlayback = true) }
                            },
                            onNavigateFolder = { path ->
                                haptic.click()
                                currentFolderBrowserPath = path
                            },
                            onPlayFolderQueue = { folderPath, shuffleFolder ->
                                val matching = allTracks.filter { it.folderPath.equals(folderPath, ignoreCase = true) }
                                    .ifEmpty { allTracks }
                                queue.clear()
                                queue.addAll(if (shuffleFolder) matching.shuffled() else matching)
                                if (shuffleFolder) shuffleScope = ShuffleScopeMode.FOLDER
                                activeFilterTitle = "Folder: ${File(folderPath).name}"
                                activeLibraryTab = AudioLibraryTab.QUEUE
                                queue.firstOrNull()?.let { selectTrack(it.id, startPlayback = true) }
                            },
                            onJumpToEqPage = {
                                haptic.click()
                                scope.launch { pagerState.animateScrollToPage(2) }
                            },
                            onReturnToPlayer = { scope.launch { pagerState.animateScrollToPage(0) } }
                        )

                        else -> M3EqualizerAndOutputStudioPage(
                            spec = adaptiveSpec,
                            eqPresets = eqPresets,
                            selectedEqPresetId = selectedEqPresetId,
                            activeBands = activeEqBands,
                            showCustomEqCreator = showCustomEqCreator,
                            outputRoutes = outputRoutes,
                            selectedOutputRoute = selectedOutputRoute,
                            volumeLevel = volumeLevel,
                            phoneMediaPlaying = phoneMediaPlaying,
                            onSelectEqPreset = { preset ->
                                haptic.click()
                                selectedEqPresetId = preset.id
                                applyHardwareEqualizerGains(preset.bandGainsDb)
                                feedbackOverlay = "EQ: ${preset.name}" to false
                            },
                            onAdjustBandGain = { bandIdx, deltaDb ->
                                haptic.click()
                                val updated = activeEqBands.toMutableList()
                                updated[bandIdx] = (updated[bandIdx] + deltaDb).coerceIn(-12f, 12f)
                                applyHardwareEqualizerGains(updated)
                            },
                            onToggleCustomEqCreator = {
                                haptic.click()
                                showCustomEqCreator = !showCustomEqCreator
                            },
                            onSaveCustomEqPreset = { presetName ->
                                val clean = presetName.trim().ifBlank { "Custom Studio" }
                                val created = CustomEqPreset(
                                    id = "eq-${System.currentTimeMillis()}",
                                    name = clean,
                                    bandGainsDb = activeEqBands.toList()
                                )
                                eqPresets.add(0, created)
                                selectedEqPresetId = created.id
                                WearAudioStudioPrefs.saveEqPresets(context, eqPresets)
                                showCustomEqCreator = false
                                feedbackOverlay = "Saved '$clean' EQ" to false
                                haptic.click()
                            },
                            onOpenCurvedVolumeOverlay = {
                                haptic.click()
                                activeModalOverlay = AudioModalOverlay.CURVED_VOLUME_AND_OUTPUT
                            },
                            onSelectOutputRoute = { route ->
                                haptic.click()
                                selectedOutputRoute = route
                                if (route.isPhoneRemote) {
                                    scope.launch(Dispatchers.IO) {
                                        runCatching {
                                            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                                            nodes.firstOrNull()?.let { node ->
                                                Wearable.getMessageClient(context)
                                                    .sendMessage(node.id, "/lifeos/audio_route_phone", "SPEAKER".toByteArray())
                                                    .await()
                                            }
                                        }
                                    }
                                } else {
                                    WearAudioMediaEngine.switchHardwareAudioOutput(context, route)
                                }
                                feedbackOverlay = "Output: ${route.name}" to false
                            },
                            onScanAndPairBluetooth = {
                                haptic.click()
                                if (!WearAudioMediaEngine.hasBluetoothPermission(context)) {
                                    permissionLauncher.launch(WearAudioMediaEngine.requiredAudioPermissions())
                                }
                                WearAudioMediaEngine.openBluetoothPairingSettings(context)
                            },
                            onPhoneRemoteCommand = { cmd ->
                                haptic.click()
                                if (cmd == "PLAY_PAUSE") phoneMediaPlaying = !phoneMediaPlaying
                                scope.launch(Dispatchers.IO) {
                                    runCatching {
                                        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                                        nodes.firstOrNull()?.let { node ->
                                            Wearable.getMessageClient(context)
                                                .sendMessage(node.id, "/lifeos/phone_media_control/$cmd", cmd.toByteArray())
                                                .await()
                                        }
                                    }
                                }
                                feedbackOverlay = "Phone: $cmd" to false
                            },
                            onReturnToPlayer = {
                                haptic.click()
                                scope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                    }
                }
            }

            // =========================================================================
            // FULL-SCREEN CURVED VOLUME CONTROLLER, TRACK OPTIONS & FOLDER SCAN MODALS
            // =========================================================================
            when (activeModalOverlay) {
                AudioModalOverlay.CURVED_VOLUME_AND_OUTPUT -> {
                    M3FullScreenCurvedVolumeAndOutputOverlay(
                        volumeLevel = volumeLevel,
                        selectedRoute = selectedOutputRoute,
                        outputRoutes = outputRoutes,
                        onVolumeChange = ::updateSystemVolume,
                        onSelectRoute = { route ->
                            haptic.click()
                            selectedOutputRoute = route
                            WearAudioMediaEngine.switchHardwareAudioOutput(context, route)
                            feedbackOverlay = "Route: ${route.name}" to false
                        },
                        onPairBluetoothDevice = {
                            haptic.click()
                            WearAudioMediaEngine.openBluetoothPairingSettings(context)
                        },
                        onDismiss = {
                            haptic.click()
                            activeModalOverlay = AudioModalOverlay.NONE
                        }
                    )
                }

                AudioModalOverlay.QUICK_TRACK_OPTIONS -> {
                    M3QuickTrackOptionsModal(
                        currentTrack = currentTrack,
                        repeatMode = repeatMode,
                        shuffleScope = shuffleScope,
                        onToggleLike = { toggleLike(currentTrack.id) },
                        onCycleShuffle = ::cycleShuffleScope,
                        onCycleRepeat = ::cycleRepeatMode,
                        onOpenLyrics = {
                            activeModalOverlay = AudioModalOverlay.NONE
                            showLyricsOverlay = true
                            haptic.click()
                        },
                        onOpenAddToPlaylist = {
                            trackIdPendingPlaylistAdd = currentTrack.id
                            activeModalOverlay = AudioModalOverlay.ADD_TO_PLAYLIST_PICKER
                            haptic.click()
                        },
                        onOpenEqualizer = {
                            activeModalOverlay = AudioModalOverlay.NONE
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onOpenLibrary = {
                            activeModalOverlay = AudioModalOverlay.NONE
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onDismiss = {
                            haptic.click()
                            activeModalOverlay = AudioModalOverlay.NONE
                        }
                    )
                }

                AudioModalOverlay.ADD_TO_PLAYLIST_PICKER -> {
                    M3AddToPlaylistModal(
                        playlists = playlists,
                        onSelectPlaylist = { pl ->
                            val targetId = trackIdPendingPlaylistAdd ?: currentTrack.id
                            addTrackToPlaylist(targetId, pl.id)
                        },
                        onDismiss = {
                            haptic.click()
                            activeModalOverlay = AudioModalOverlay.NONE
                        }
                    )
                }

                AudioModalOverlay.FOLDER_SCAN_MANAGER -> {
                    M3LibraryFolderScanManagerModal(
                        includedFolders = includedFolders,
                        excludedFolders = excludedFolders,
                        onAddIncludedFolder = { path ->
                            val clean = path.trim()
                            if (clean.isNotBlank() && clean !in includedFolders) {
                                includedFolders.add(clean)
                                WearAudioStudioPrefs.saveIncludedFolders(context, includedFolders)
                                triggerLibraryRescan()
                                feedbackOverlay = "Added Scan Folder" to false
                            }
                        },
                        onRemoveIncludedFolder = { path ->
                            if (includedFolders.size > 1) {
                                includedFolders.remove(path)
                                WearAudioStudioPrefs.saveIncludedFolders(context, includedFolders)
                                triggerLibraryRescan()
                            }
                        },
                        onAddExcludedFolder = { path ->
                            val clean = path.trim()
                            if (clean.isNotBlank() && clean !in excludedFolders) {
                                excludedFolders.add(clean)
                                WearAudioStudioPrefs.saveExcludedFolders(context, excludedFolders)
                                triggerLibraryRescan()
                                feedbackOverlay = "Folder Excluded" to false
                            }
                        },
                        onRemoveExcludedFolder = { path ->
                            excludedFolders.remove(path)
                            WearAudioStudioPrefs.saveExcludedFolders(context, excludedFolders)
                            triggerLibraryRescan()
                        },
                        onRescanNow = {
                            haptic.click()
                            triggerLibraryRescan()
                            activeModalOverlay = AudioModalOverlay.NONE
                            feedbackOverlay = "Library Rescanned" to false
                        },
                        onDismiss = {
                            haptic.click()
                            activeModalOverlay = AudioModalOverlay.NONE
                        }
                    )
                }

                AudioModalOverlay.NONE -> {}
            }

            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 3,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedAudioConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }
        }
    
}

@Composable
private fun MetadataChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = Color(0xFF79E2B0),
            fontWeight = FontWeight.Bold
        )
    }
}

// =============================================================================
// PAGE 0: FULL-SCREEN ALBUM ARTWORK BACKDROP & EXPRESSIVE TRANSPORT PLAYER
// =============================================================================
@Composable
private fun M3ExpressiveCanvasPlayerPage(
    spec: WearAdaptiveSpec,
    currentTrack: WearTrack,
    albumArtBitmap: ImageBitmap?,
    elapsedSeconds: Int,
    isPlaying: Boolean,
    progress: Float,
    repeatMode: RepeatMode,
    shuffleScope: ShuffleScopeMode,
    selectedOutput: String,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenCurvedVolumeAndOutput: () -> Unit,
    onOpenQuickOptions: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenLibrary: () -> Unit
    ) {
    val centerButtonSize = (spec.screenWidth * 0.35f).coerceIn(64.dp, 80.dp)
    val sideButtonWidth = (spec.screenWidth * 0.22f).coerceIn(44.dp, 54.dp)
    val sideButtonHeight = (spec.screenWidth * 0.27f).coerceIn(52.dp, 64.dp)
    val bottomPillWidth = (spec.screenWidth * 0.25f).coerceIn(48.dp, 60.dp)
    val bottomPillHeight = (spec.screenHeight * 0.15f).coerceIn(30.dp, 38.dp)

    val prevInteractionSource = remember { MutableInteractionSource() }
    val playInteractionSource = remember { MutableInteractionSource() }
    val nextInteractionSource = remember { MutableInteractionSource() }

    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()

    val buttonSpringSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    }
    val prevScale by animateFloatAsState(targetValue = if (isPrevPressed) 0.88f else 1.0f, animationSpec = buttonSpringSpec, label = "PrevScale")
    val nextScale by animateFloatAsState(targetValue = if (isNextPressed) 0.88f else 1.0f, animationSpec = buttonSpringSpec, label = "NextScale")
    val playPressScale by animateFloatAsState(targetValue = if (isPlayPressed) 0.85f else 1.0f, animationSpec = buttonSpringSpec, label = "PlayPressScale")
    val buttonToggleScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.90f,
        animationSpec = buttonSpringSpec,
        label = "PlayToggleScale"
    )

    ScreenScaffold(
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val artworkPrimary = MaterialTheme.colorScheme.primaryContainer
            val artworkTertiary = MaterialTheme.colorScheme.tertiaryContainer
            val surfaceDark = MaterialTheme.colorScheme.surfaceContainerLow

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(
                        bitmap = albumArtBitmap,
                        contentDescription = currentTrack.album,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.38f),
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        artworkPrimary.copy(alpha = if (isPlaying) 0.46f else 0.25f),
                                        artworkTertiary.copy(alpha = 0.22f),
                                        surfaceDark.copy(alpha = 0.95f),
                                        Color.Black
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            modifier = Modifier.size(spec.screenWidth * 0.62f),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            val outerTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
            val outerProgressColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.5.dp)
            ) {
                drawArc(
                    color = outerTrackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = outerProgressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = spec.screenHeight * 0.12f,
                        bottom = spec.screenHeight * 0.08f,
                        start = spec.screenWidth * 0.06f,
                        end = spec.screenWidth * 0.06f
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.12f) // Increased padding for screen curvature
                        .clickable(onClick = onOpenLyrics)
                ) {
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.titleSmall.compactLineHeight(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetadataChip(text = currentTrack.formatExtension)
                        MetadataChip(text = currentTrack.sampleRateKhz)
                        MetadataChip(text = if (!currentTrack.lyrics.isNullOrBlank()) "LRC Present" else "No LRC")
                    }
                }

                //play/pause toggle button

                PlayerControlBar(
                    isPlaying = isPlaying,
                    progress = elapsedSeconds.toFloat() / currentTrack.durationSeconds.toFloat().coerceAtLeast(1f),
                    onTogglePlay = { },
                    onPrevious = { },
                    onNext = { }
                )

                // =====================================================================
                // BOTTOM SLOT: Two-Row Curved Layout (Outer Ends + Centered Second Row)
                // =====================================================================
                val TranslucentPillColor = Color(0xFF382E32).copy(alpha = 0.85f)
                val pillBtnWidth = (spec.screenWidth * 0.20f).coerceIn(38.dp, 46.dp)
                val pillBtnHeight = (spec.screenHeight * 0.11f).coerceIn(25.dp, 30.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.12f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Row 1: Outer Ends (Library on Left, Options on Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenLibrary,
                            modifier = Modifier
                                .size(width = pillBtnWidth, height = pillBtnHeight)
                                .semantics { contentDescription = "Open Audio Library" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TranslucentPillColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = "${formatPlaybackTime(elapsedSeconds)} / ${formatPlaybackTime(currentTrack.durationSeconds)}",
                            style = MaterialTheme.typography.bodySmall.tabularNums(),
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                        Button(
                            onClick = onOpenQuickOptions,
                            modifier = Modifier
                                .size(width = pillBtnWidth, height = pillBtnHeight)
                                .semantics { contentDescription = "More Track Options" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TranslucentPillColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Row 2: Center (Output/Volume + Lyrics)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Output & Volume Button with Mini Scalloped Badge
                        Box(
                            modifier = Modifier.size(width = pillBtnWidth, height = pillBtnHeight),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Button(
                                onClick = onOpenCurvedVolumeAndOutput,
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TranslucentPillColor,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Headphones,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            val coralPinkAccent = Color(0xFFFF8A93)
                            val softPinkFill = Color(0xFFFFB2B8)
                            val darkIconTint = Color(0xFF470C14)
                            val miniScallopShape = remember { ScallopedPlayButtonShape(lobes = 8, waveAmplitudeRatio = 0.09f) }
                            Box(
                                modifier = Modifier
                                    .offset(x = 3.dp, y = (-3).dp)
                                    .size(14.dp)
                                    .clip(miniScallopShape)
                                    .background(softPinkFill),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(8.dp),
                                    tint = darkIconTint
                                )
                            }
                        }

                        // Lyrics Button (Second item in Row 2)
                        Button(
                            onClick = onOpenLyrics,
                            modifier = Modifier
                                .size(width = pillBtnWidth, height = pillBtnHeight)
                                .semantics { contentDescription = "Synced LRC Lyrics" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TranslucentPillColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Lyrics,
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

// =============================================================================
// PAGE 1: LIBRARY (QUEUE REORDER, PLAYLISTS, SQUARE ALBUMS, ARTISTS, HIERARCHICAL FOLDERS)
// =============================================================================
@Composable
private fun M3AudioLibraryBrowserPage(
    spec: WearAdaptiveSpec,
    hasAudioPerm: Boolean,
    isRescanning: Boolean,
    scanProgressFraction: Float,
    scanStatusMessage: String,
    allTracks: List<WearTrack>,
    queue: List<WearTrack>,
    currentTrack: WearTrack,
    playlists: List<WearPlaylist>,
    albumArtCache: Map<String, ImageBitmap>,
    activeLibraryTab: AudioLibraryTab,
    activeFilterTitle: String?,
    selectedAlbumDrilldown: String?,
    selectedArtistDrilldown: String?,
    currentFolderBrowserPath: String,
    rootStoragePath: String,
    repeatMode: RepeatMode,
    shuffleScope: ShuffleScopeMode,
    showPlaylistEditor: Boolean,
    elapsedSeconds: Int,
    onRequestAudioPermissions: () -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenFolderManagerModal: () -> Unit,
    onSelectLibraryTab: (AudioLibraryTab) -> Unit,
    onResetFilter: () -> Unit,
    onCycleShuffleScope: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onOpenLyrics: () -> Unit,
    onSelectTrack: (String) -> Unit,
    onMoveQueueTrack: (String, Boolean) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onOpenAddToPlaylist: (String) -> Unit,
    onLikeToggle: (String) -> Unit,
    onTogglePlaylistEditor: () -> Unit,
    onCreatePlaylist: (String, String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onLoadPlaylist: (WearPlaylist) -> Unit,
    onSelectSquareAlbum: (String?) -> Unit,
    onPlayEntireAlbum: (String) -> Unit,
    onSelectArtist: (String?) -> Unit,
    onPlayEntireArtist: (String) -> Unit,
    onNavigateFolder: (String) -> Unit,
    onPlayFolderQueue: (String, Boolean) -> Unit,
    onJumpToEqPage: () -> Unit,
    onReturnToPlayer: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize(),
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onReturnToPlayer,
                label = "Now Playing",
                icon = Icons.Rounded.KeyboardArrowUp
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
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
                    title = "AUDIO LIBRARY",
                    badgeText = if (isRescanning) "SCANNING..." else "${allTracks.size} TRACKS"
                )
            }

            if (isRescanning) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scanStatusMessage,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(scanProgressFraction * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { scanProgressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            colors = ProgressIndicatorDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hasAudioPerm) {
                        Button(
                            onClick = onRequestAudioPermissions,
                            modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Permit Audio", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    FilledTonalButton(
                        onClick = onOpenFolderManagerModal,
                        modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Folders", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = onRescanLibrary,
                        modifier = Modifier.weight(1f).heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(if (isRescanning) "Scanning" else "Rescan", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = "Shuf: ${shuffleScope.shortLabel}",
                        icon = Icons.Default.Shuffle,
                        selected = shuffleScope != ShuffleScopeMode.OFF,
                        onClick = onCycleShuffleScope
                    )
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = "Rep: ${repeatMode.shortLabel}",
                        icon = if (repeatMode == RepeatMode.ONE_FILE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        selected = repeatMode != RepeatMode.OFF,
                        onClick = onCycleRepeatMode
                    )
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = "Lyrics",
                        icon = Icons.Default.Lyrics,
                        selected = false,
                        onClick = onOpenLyrics
                    )
                }
            }

            item {
                WearActionButtonGroup(
                    options = AudioLibraryTab.entries.map { it.label },
                    selectedIndex = activeLibraryTab.ordinal,
                    onOptionSelected = { idx -> onSelectLibraryTab(AudioLibraryTab.entries[idx]) }
                )
            }

            if (activeFilterTitle != null) {
                item {
                    Card(
                        onClick = onResetFilter,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = activeFilterTitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.Close, contentDescription = "Reset Filter", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            when (activeLibraryTab) {
                AudioLibraryTab.QUEUE -> {
                    item {
                        CatalogM3ListHeaderSample(title = "ACTIVE QUEUE (${queue.size})")
                    }

                    items(queue.size) { index ->
                        val track = queue[index]
                        val isCurrent = track.id == currentTrack.id
                        val artBmp = albumArtCache[track.id] ?: albumArtCache[track.album]

                        WearSwipeableActionCard(
                            onCardClick = { onSelectTrack(track.id) },
                            onEditClick = { onOpenAddToPlaylist(track.id) },
                            onDeleteClick = { onRemoveFromQueue(track.id) },
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artBmp != null) {
                                        Image(bitmap = artBmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Rounded.Album, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${track.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} · ${formatPlaybackTime(track.durationSeconds)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (index > 0) {
                                        MiniCircleIconBtn(icon = Icons.Rounded.KeyboardArrowUp) {
                                            onMoveQueueTrack(track.id, true)
                                        }
                                    }
                                    if (index < queue.lastIndex) {
                                        MiniCircleIconBtn(icon = Icons.Rounded.KeyboardArrowDown) {
                                            onMoveQueueTrack(track.id, false)
                                        }
                                    }
                                    MiniCircleIconBtn(icon = Icons.Rounded.PlaylistAdd) {
                                        onOpenAddToPlaylist(track.id)
                                    }
                                    MiniCircleIconBtn(
                                        icon = if (track.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        tint = if (track.liked) Color(0xFFFF7A9A) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        onLikeToggle(track.id)
                                    }
                                }
                            }
                        }
                    }
                }

                AudioLibraryTab.PLAYLISTS -> {
                    item {
                        FilledTonalButton(
                            onClick = onTogglePlaylistEditor,
                            icon = {
                                Icon(
                                    if (showPlaylistEditor) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showPlaylistEditor) "Close Editor" else "New Custom Playlist")
                        }
                    }

                    if (showPlaylistEditor) {
                        item {
                            WearExpressiveUniversalEditorCard(
                                itemTypeTitle = "PLAYLIST",
                                initialTitle = "",
                                initialTimeOrDate = "mint",
                                initialSubtitle = "High-energy focus mix",
                                isEditingExisting = false,
                                showTimePickerField = true,
                                showSubtitleField = true,
                                timeFieldLabel = "Card Theme (mint/coral/cobalt)",
                                subtitleFieldLabel = "Description / Mood",
                                onSave = { name, colorHex, desc ->
                                    onCreatePlaylist(name, desc, colorHex.ifBlank { "mint" })
                                },
                                onCancel = onTogglePlaylistEditor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    items(playlists.size) { idx ->
                        val pl = playlists[idx]
                        val count = if (pl.id == "pl-favourites") allTracks.count { it.liked } else pl.trackIds.size
                        val firstTrackInPl = if (pl.id == "pl-favourites") {
                            allTracks.firstOrNull { it.liked }
                        } else {
                            allTracks.firstOrNull { it.id in pl.trackIds }
                        }
                        val plArtBmp = firstTrackInPl?.let { albumArtCache[it.id] ?: albumArtCache[it.album] }

                        WearSwipeableActionCard(
                            onCardClick = { onLoadPlaylist(pl) },
                            onEditClick = { onLoadPlaylist(pl) },
                            onDeleteClick = { onDeletePlaylist(pl.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (plArtBmp != null) {
                                            Image(bitmap = plArtBmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pl.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${pl.description} · $count tracks",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Playlist",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                AudioLibraryTab.ALBUMS -> {
                    if (selectedAlbumDrilldown != null) {
                        val albumTracks = allTracks.filter {
                            it.album.equals(selectedAlbumDrilldown, ignoreCase = true)
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { onSelectSquareAlbum(null) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Albums", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { onPlayEntireAlbum(selectedAlbumDrilldown) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Play All", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        item {
                            CatalogM3ListHeaderSample(title = "${selectedAlbumDrilldown.uppercase()} (${albumTracks.size})")
                        }
                        items(albumTracks.size) { idx ->
                            val t = albumTracks[idx]
                            val artBmp = albumArtCache[t.id] ?: albumArtCache[t.album]
                            TrackArtRowCard(
                                overline = "TRACK ${idx + 1}",
                                title = t.title,
                                subtitle = "${t.artist} · ${formatPlaybackTime(t.durationSeconds)}",
                                artBitmap = artBmp,
                                onClick = { onSelectTrack(t.id) }
                            )
                        }
                    } else {
                        val groupedAlbums = allTracks.groupBy { it.album }.entries.toList()
                        val albumRows = groupedAlbums.chunked(2)

                        item {
                            CatalogM3ListHeaderSample(title = "ALBUM ART GALLERY (${groupedAlbums.size})")
                        }

                        items(albumRows.size) { rowIdx ->
                            val rowPair = albumRows[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowPair.forEach { (albumName, tracksInAlbum) ->
                                    val artBmp = albumArtCache[albumName]
                                        ?: tracksInAlbum.firstNotNullOfOrNull { albumArtCache[it.id] }
                                    SquareAlbumArtCard(
                                        modifier = Modifier.weight(1f),
                                        albumTitle = albumName,
                                        artistName = tracksInAlbum.firstOrNull()?.artist ?: "Artist",
                                        trackCount = tracksInAlbum.size,
                                        artBitmap = artBmp,
                                        onClick = { onSelectSquareAlbum(albumName) }
                                    )
                                }
                                if (rowPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                AudioLibraryTab.ARTISTS -> {
                    if (selectedArtistDrilldown != null) {
                        val artistTracks = allTracks.filter {
                            it.artist.equals(selectedArtistDrilldown, ignoreCase = true)
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FilledTonalButton(onClick = { onSelectArtist(null) }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Artists", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(onClick = { onPlayEntireArtist(selectedArtistDrilldown) }) {
                                    Text("Play Artist", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        items(artistTracks.size) { idx ->
                            val t = artistTracks[idx]
                            val artBmp = albumArtCache[t.id] ?: albumArtCache[t.album]
                            TrackArtRowCard(
                                overline = t.album,
                                title = t.title,
                                subtitle = formatPlaybackTime(t.durationSeconds),
                                artBitmap = artBmp,
                                onClick = { onSelectTrack(t.id) }
                            )
                        }
                    } else {
                        val groupedArtists = allTracks.groupBy { it.artist }.entries.toList()
                        items(groupedArtists.size) { idx ->
                            val (artistName, artistTracks) = groupedArtists[idx]
                            val artistArtBmp = artistTracks.firstNotNullOfOrNull { albumArtCache[it.id] ?: albumArtCache[it.album] }

                            TrackArtRowCard(
                                overline = "ARTIST · ${artistTracks.size} TRACKS",
                                title = artistName,
                                subtitle = artistTracks.map { it.album }.distinct().joinToString(", "),
                                artBitmap = artistArtBmp,
                                fallbackIcon = Icons.Default.Person,
                                onClick = { onSelectArtist(artistName) }
                            )
                        }
                    }
                }

                AudioLibraryTab.FOLDERS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onOpenFolderManagerModal,
                                icon = {
                                    Icon(
                                        Icons.Rounded.CreateNewFolder,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Scan Config", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = onRescanLibrary,
                                icon = {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Rescan", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    item {
                        val segments = remember(currentFolderBrowserPath, rootStoragePath) {
                            val list = mutableListOf("Root" to rootStoragePath)
                            val rel = currentFolderBrowserPath.removePrefix(rootStoragePath).trim('/')
                            var acc = rootStoragePath
                            rel.split('/').filter { it.isNotBlank() }.forEach { seg ->
                                acc = "$acc/$seg"
                                list.add(seg to acc)
                            }
                            list
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            segments.forEachIndexed { i, (lbl, path) ->
                                val isLast = i == segments.lastIndex
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isLast) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { onNavigateFolder(path) }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = lbl.take(12),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!isLast) Text("›", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    val tracksInThisFolder = allTracks.filter {
                        it.folderPath.equals(currentFolderBrowserPath, ignoreCase = true)
                    }
                    if (tracksInThisFolder.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { onPlayFolderQueue(currentFolderBrowserPath, false) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(
                                        "Play Folder (${tracksInThisFolder.size})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onPlayFolderQueue(currentFolderBrowserPath, true) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(
                                        "Shuffle Folder",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    val allFolderPaths = allTracks.map { it.folderPath }.distinct()
                    items(allFolderPaths.size) { idx ->
                        val fPath = allFolderPaths[idx]
                        val fName = File(fPath).name.ifBlank { fPath }
                        val folderTracks = allTracks.filter { it.folderPath == fPath }
                        val folderArtBmp = folderTracks.firstNotNullOfOrNull { albumArtCache[it.id] ?: albumArtCache[it.album] }

                        TrackArtRowCard(
                            overline = "AUDIO FOLDER · ${folderTracks.size} TRACKS",
                            title = fName,
                            subtitle = fPath,
                            artBitmap = folderArtBmp,
                            fallbackIcon = Icons.Default.Folder,
                            onClick = {
                                onNavigateFolder(fPath)
                                onPlayFolderQueue(fPath, false)
                            }
                        )
                    }
                }
            }

            item {
                CatalogM3ChildButtonSample(
                    label = "5-Band Equalizer & Bluetooth Output",
                    onClick = onJumpToEqPage
                )
            }
        }
    }
}

@Composable
private fun TrackArtRowCard(
    overline: String,
    title: String,
    subtitle: String,
    artBitmap: ImageBitmap?,
    fallbackIcon: ImageVector = Icons.Rounded.MusicNote,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (artBitmap != null) {
                    Image(
                        bitmap = artBitmap,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SquareAlbumArtCard(
    modifier: Modifier = Modifier,
    albumTitle: String,
    artistName: String,
    trackCount: Int,
    artBitmap: ImageBitmap?,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (artBitmap != null) {
                Image(
                    bitmap = artBitmap,
                    contentDescription = albumTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text("$trackCount", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(albumTitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artistName, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// =============================================================================
// PAGE 2: REAL 5-BAND DSP EQUALIZER GRAPH, BLUETOOTH PAIRING & OUTPUT SWITCHER
// =============================================================================
@Composable
private fun M3EqualizerAndOutputStudioPage(
    spec: WearAdaptiveSpec,
    eqPresets: List<CustomEqPreset>,
    selectedEqPresetId: String,
    activeBands: List<Float>,
    showCustomEqCreator: Boolean,
    outputRoutes: List<OutputRouteDevice>,
    selectedOutputRoute: OutputRouteDevice,
    volumeLevel: Float,
    phoneMediaPlaying: Boolean,
    onSelectEqPreset: (CustomEqPreset) -> Unit,
    onAdjustBandGain: (Int, Float) -> Unit,
    onToggleCustomEqCreator: () -> Unit,
    onSaveCustomEqPreset: (String) -> Unit,
    onOpenCurvedVolumeOverlay: () -> Unit,
    onSelectOutputRoute: (OutputRouteDevice) -> Unit,
    onScanAndPairBluetooth: () -> Unit,
    onPhoneRemoteCommand: (String) -> Unit,
    onReturnToPlayer: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize(),
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onReturnToPlayer,
                label = "Player Ring",
                icon = Icons.Rounded.KeyboardArrowUp
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
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
                    title = "EQ & OUTPUT",
                    badgeText = selectedOutputRoute.name.take(12)
                )
            }

            item {
                Card(
                    onClick = onToggleCustomEqCreator,
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
                            Text(
                                text = "EQ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = eqPresets.firstOrNull { it.id == selectedEqPresetId }?.name ?: "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF79E2B0),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            val midY = size.height / 2f
                            drawLine(
                                color = Color.White.copy(alpha = 0.18f),
                                start = Offset(0f, midY),
                                end = Offset(size.width, midY),
                                strokeWidth = 1.dp.toPx()
                            )

                            val points = activeBands.mapIndexed { i, gainDb ->
                                val x = (i.toFloat() / (activeBands.size - 1).coerceAtLeast(1)) * (size.width - 16.dp.toPx()) + 8.dp.toPx()
                                val normalized = (gainDb / 12f).coerceIn(-1f, 1f)
                                val y = midY - (normalized * (size.height * 0.42f))
                                Offset(x, y)
                            }

                            val curvePath = Path()
                            val fillPath = Path()
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) {
                                    curvePath.moveTo(pt.x, pt.y)
                                    fillPath.moveTo(pt.x, size.height)
                                    fillPath.lineTo(pt.x, pt.y)
                                } else {
                                    val prev = points[idx - 1]
                                    val ctrlX = (prev.x + pt.x) / 2f
                                    curvePath.cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
                                    fillPath.cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
                                }
                            }
                            if (points.isNotEmpty()) {
                                fillPath.lineTo(points.last().x, size.height)
                                fillPath.close()
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF79E2B0).copy(alpha = 0.35f), Color.Transparent)
                                    )
                                )
                            }
                            drawPath(
                                path = curvePath,
                                color = Color(0xFF79E2B0),
                                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            points.forEach { pt ->
                                drawCircle(color = Color(0xFF79E2B0), radius = 3.5.dp.toPx(), center = pt)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            eqBandFrequencies.forEachIndexed { i, freqLbl ->
                                val g = activeBands.getOrElse(i) { 0f }
                                Text(
                                    text = "$freqLbl\n${if (g >= 0) "+${g.toInt()}" else "${g.toInt()}"}",
                                    style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 8.sp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    eqPresets.forEach { preset ->
                        val isSel = preset.id == selectedEqPresetId
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onSelectEqPreset(preset) }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            val chunkedBands = eqBandFrequencies.indices.chunked(3)

            items(chunkedBands.size) { rowIndex ->
                val rowBands = chunkedBands[rowIndex]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowBands.forEachIndexed { index, bandIdx ->
                        val freq = eqBandFrequencies[bandIdx]
                        val gain = activeBands.getOrElse(bandIdx) { 0f }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.width(64.dp)
                        ) {
                            IconButton(
                                onClick = { onAdjustBandGain(bandIdx, 1.5f) },
                                enabled = gain < 12f,
                                modifier = Modifier.size(IconButtonDefaults.SmallButtonSize),
                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Increase Band $freq"
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = freq,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (gain >= 0) "+" else ""}${String.format(Locale.ROOT, "%.1f", gain)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            IconButton(
                                onClick = { onAdjustBandGain(bandIdx, -1.5f) },
                                enabled = gain > -12f,
                                modifier = Modifier.size(IconButtonDefaults.SmallButtonSize),
                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Decrease Band $freq"
                                )
                            }
                        }

                        if (index < rowBands.lastIndex) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onToggleCustomEqCreator,
                    icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showCustomEqCreator) "Close Preset Saver" else "Save Custom EQ Preset")
                }
            }

            if (showCustomEqCreator) {
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "EQ PRESET",
                        initialTitle = "My Studio Curve",
                        initialTimeOrDate = "",
                        initialSubtitle = "",
                        isEditingExisting = false,
                        showTimePickerField = false,
                        showSubtitleField = false,
                        onSave = { name, _, _ -> onSaveCustomEqPreset(name) },
                        onCancel = onToggleCustomEqCreator,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                CatalogM3ListHeaderSample(title = "OUTPUT & BLUETOOTH ROUTING")
            }

            item {
                WearMetricCard(
                    title = "CURVED VOLUME DIAL",
                    value = "${(volumeLevel * 100f).roundToInt()}% Volume",
                    subtitle = "Tap to open full-screen curved arc controller",
                    icon = Icons.Rounded.VolumeUp,
                    onClick = onOpenCurvedVolumeOverlay
                )
            }

            items(outputRoutes.size) { idx ->
                val route = outputRoutes[idx]
                val isSelected = route.name.equals(selectedOutputRoute.name, ignoreCase = true)
                Card(
                    onClick = { onSelectOutputRoute(route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when {
                                    route.isBluetooth -> Icons.Rounded.BluetoothAudio
                                    route.isPhoneRemote -> Icons.Rounded.Smartphone
                                    else -> Icons.Rounded.Speaker
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(route.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(route.typeLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), maxLines = 1)
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active Route", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onScanAndPairBluetooth,
                    icon = { Icon(Icons.Rounded.BluetoothSearching, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan & Pair Bluetooth Audio")
                }
            }

            item {
                CatalogM3ListHeaderSample(title = "PHONE SPEAKER REMOTE")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onPhoneRemoteCommand("PREV") },
                        modifier = Modifier.size(IconButtonDefaults.SmallButtonSize),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Phone Prev"
                        )
                    }

                    IconButton(
                        onClick = { onPhoneRemoteCommand("PLAY_PAUSE") },
                        modifier = Modifier.size(IconButtonDefaults.LargeButtonSize),
                        colors = IconButtonDefaults.filledIconButtonColors()
                    ) {
                        Icon(
                            imageVector = if (phoneMediaPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Phone Play/Pause"
                        )
                    }

                    IconButton(
                        onClick = { onPhoneRemoteCommand("NEXT") },
                        modifier = Modifier.size(IconButtonDefaults.SmallButtonSize),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Phone Next"
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// FULL-SCREEN CURVED VOLUME CONTROLLER & OUTPUT ROUTING OVERLAY
// =============================================================================
@Composable
private fun M3FullScreenCurvedVolumeAndOutputOverlay(
    volumeLevel: Float,
    selectedRoute: OutputRouteDevice,
    outputRoutes: List<OutputRouteDevice>,
    onVolumeChange: (Float) -> Unit,
    onSelectRoute: (OutputRouteDevice) -> Unit,
    onPairBluetoothDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedVol by animateFloatAsState(
        targetValue = volumeLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "curvedVolArc"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = (-dragAmount.y / size.height.coerceAtLeast(1)).coerceIn(-0.15f, 0.15f)
                    onVolumeChange((volumeLevel + delta).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val trackCol = MaterialTheme.colorScheme.surfaceContainerHigh
        val activeCol = Color(0xFF79E2B0)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val strokePx = 10.dp.toPx()
            drawArc(
                color = trackCol,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeCol,
                startAngle = 135f,
                sweepAngle = 270f * animatedVol,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {


            Text(
                text = selectedRoute.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onVolumeChange((volumeLevel - 0.08f).coerceAtLeast(0f)) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Volume Down")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (volumeLevel <= 0.01f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = activeCol,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "${(volumeLevel * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.displaySmall.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                FilledTonalIconButton(
                    onClick = { onVolumeChange((volumeLevel + 0.08f).coerceAtMost(1f)) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Volume Up")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                outputRoutes.forEach { r ->
                    val isSel = r.name.equals(selectedRoute.name, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSelectRoute(r) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = r.name.take(10),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onPairBluetoothDevice)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+BT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), fontWeight = FontWeight.Bold)
                }
            }

            CompactButton(
                onClick = onDismiss,
                icon = { Icon(Icons.Default.Check, contentDescription = "Done") }
            ) {
                Text("Done")
            }
        }
    }
}

// =============================================================================
// IMMERSIVE DISTRACTION-FREE FULL-SCREEN LYRICS OVERLAY
// =============================================================================
@Composable
private fun M3FullScreenLyricsOverlay(
    currentTrack: WearTrack,
    elapsedSeconds: Int,
    isFetchingLrcLib: Boolean,
    onSeek: (Float) -> Unit,
    onFetchLrcLibNow: () -> Unit,
    onBurnLyricsToFile: (String) -> Unit,
    onClose: () -> Unit
) {
    val haptic = rememberLifeOSHapticProvider()
    val columnState = rememberTransformingLazyColumnState()

    val lyricsSource = remember(currentTrack.id, currentTrack.lyrics) {
        currentTrack.lyrics?.takeIf { it.isNotBlank() }
    }
    val lrcLines = remember(lyricsSource) {
        LrcLyricsParser.parseLrc(lyricsSource).sortedBy { it.timestampMs }
    }
    val currentTimeMs = elapsedSeconds.coerceAtLeast(0).toLong() * 1_000L
    val currentLineIndex = lrcLines.indexOfLast { it.timestampMs <= currentTimeMs }

    LaunchedEffect(currentTrack.id, currentLineIndex, lrcLines.size) {
        if (currentLineIndex >= 0) {
            columnState.animateScrollToItem(currentLineIndex.coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClose() },
                    onLongPress = {
                        haptic.click()
                        onFetchLrcLibNow()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black
                        )
                    )
                )
        )

        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = 72.dp,
                bottom = 72.dp,
                start = 18.dp,
                end = 18.dp
            ),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lrcLines.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFetchingLrcLib) "FETCHING FROM LRCLIB..." else "NO LYRICS FOUND",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentTrack.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = onFetchLrcLibNow,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(if (isFetchingLrcLib) "Searching..." else "Download Synced Lyrics", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                items(lrcLines.size) { idx ->
                    val line = lrcLines[idx]
                    val isCurrentLine = idx == currentLineIndex

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isCurrentLine) 1.08f else 0.92f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "immersiveLyricScale$idx"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isCurrentLine) Color.White else Color.White.copy(alpha = 0.45f),
                        label = "immersiveLyricColor$idx"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCurrentLine) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else Color.Transparent
                            )
                            .clickable {
                                haptic.click()
                                val durationMs = currentTrack.durationSeconds.coerceAtLeast(1).toFloat() * 1_000f
                                onSeek((line.timestampMs.toFloat() / durationMs).coerceIn(0f, 1f))
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium.compactLineHeight(),
                            fontWeight = if (isCurrentLine) FontWeight.Black else FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f), Color.Black)
                    )
                )
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!lyricsSource.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onBurnLyricsToFile(lyricsSource) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Burn LRC",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exit · ${currentTrack.lyricsSourceLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
// QUICK TRACK OPTIONS, ADD TO PLAYLIST & FOLDER SCAN/EXCLUDE MODALS
// =============================================================================
@Composable
private fun M3QuickTrackOptionsModal(
    currentTrack: WearTrack,
    repeatMode: RepeatMode,
    shuffleScope: ShuffleScopeMode,
    onToggleLike: () -> Unit,
    onCycleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize(),
        edgeButton = {
            WearAnchoredEdgeButton(onClick = onDismiss, label = "Close", icon = Icons.Default.Close)
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 14.dp,
                end = 14.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(title = currentTrack.title)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = shuffleScope.label,
                        icon = Icons.Default.Shuffle,
                        selected = shuffleScope != ShuffleScopeMode.OFF,
                        onClick = onCycleShuffle
                    )
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = repeatMode.label,
                        icon = Icons.Default.Repeat,
                        selected = repeatMode != RepeatMode.OFF,
                        onClick = onCycleRepeat
                    )
                }
            }
            item {
                FilledTonalButton(
                    onClick = onOpenAddToPlaylist,
                    icon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Custom Playlist")
                }
            }
            item {
                FilledTonalButton(
                    onClick = onOpenLyrics,
                    icon = { Icon(Icons.Default.Lyrics, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Synced Lyrics (Burned / LRCLIB)")
                }
            }
            item {
                FilledTonalButton(
                    onClick = onOpenEqualizer,
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("5-Band Equalizer & Graph")
                }
            }
            item {
                FilledTonalButton(
                    onClick = onToggleLike,
                    icon = { Icon(if (currentTrack.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentTrack.liked) "Liked in Favourites" else "Add to Favourites")
                }
            }
            item {
                FilledTonalButton(
                    onClick = onOpenLibrary,
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Full Library")
                }
            }
        }
    }
}

@Composable
private fun M3AddToPlaylistModal(
    playlists: List<WearPlaylist>,
    onSelectPlaylist: (WearPlaylist) -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize(),
        edgeButton = {
            WearAnchoredEdgeButton(onClick = onDismiss, label = "Cancel", icon = Icons.Default.Close)
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 14.dp,
                end = 14.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(title = "ADD TO PLAYLIST", badgeText = "${playlists.size} LISTS")
            }
            items(playlists.size) { idx ->
                val pl = playlists[idx]
                FilledTonalButton(
                    onClick = { onSelectPlaylist(pl) },
                    icon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${pl.name} (${pl.trackIds.size})")
                }
            }
        }
    }
}
@Composable
private fun M3LibraryFolderScanManagerModal(
    includedFolders: List<String>,
    excludedFolders: List<String>,
    onAddIncludedFolder: (String) -> Unit,
    onRemoveIncludedFolder: (String) -> Unit,
    onAddExcludedFolder: (String) -> Unit,
    onRemoveExcludedFolder: (String) -> Unit,
    onRescanNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    var showAddFolderPicker by remember { mutableStateOf(false) }
    var isAddingExcluded by remember { mutableStateOf(false) }

    val root = remember { Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0" }
    val candidateFolders = remember(root) {
        val base = listOf(
            "$root/Music",
            "$root/Download",
            "$root/Podcasts",
            "$root/Audiobooks",
            "$root/Ringtones",
            "$root/Notifications",
            "$root/Recordings",
            "$root/Snaptube",
            "$root/Telegram"
        )
        val discoveredTopLevel = runCatching {
            File(root).listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.map { it.absolutePath }.orEmpty()
        }.getOrDefault(emptyList())
        (base + discoveredTopLevel).distinct()
    }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        modifier = Modifier.fillMaxSize(),
        edgeButton = {
            WearAnchoredEdgeButton(onClick = onRescanNow, label = "Rescan Now", icon = Icons.Rounded.Refresh)
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 12.dp,
                end = 12.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.97f)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(title = "SCAN FOLDERS", badgeText = "${includedFolders.size} INC")
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = {
                            if (showAddFolderPicker && !isAddingExcluded) {
                                showAddFolderPicker = false
                            } else {
                                isAddingExcluded = false
                                showAddFolderPicker = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(if (showAddFolderPicker && !isAddingExcluded) "Close" else "+ Include", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = {
                            if (showAddFolderPicker && isAddingExcluded) {
                                showAddFolderPicker = false
                            } else {
                                isAddingExcluded = true
                                showAddFolderPicker = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(if (showAddFolderPicker && isAddingExcluded) "Close" else "+ Exclude", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (showAddFolderPicker) {
                item {
                    CatalogM3ListHeaderSample(
                        title = if (isAddingExcluded) "TAP DIRECTORY TO EXCLUDE" else "TAP DIRECTORY TO INCLUDE"
                    )
                }
                val availableToPick = candidateFolders.filter { path ->
                    if (isAddingExcluded) path !in excludedFolders else path !in includedFolders
                }
                items(availableToPick.size) { idx ->
                    val folderPath = availableToPick[idx]
                    Card(
                        onClick = {
                            if (isAddingExcluded) onAddExcludedFolder(folderPath) else onAddIncludedFolder(folderPath)
                            showAddFolderPicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = File(folderPath).name.ifBlank { folderPath },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = folderPath,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                CatalogM3ListHeaderSample(title = "INCLUDED FOLDERS")
            }
            items(includedFolders.size) { idx ->
                val path = includedFolders[idx]
                Card(
                    onClick = { onRemoveIncludedFolder(path) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(path, style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                CatalogM3ListHeaderSample(title = "EXCLUDED FOLDERS")
            }
            items(excludedFolders.size) { idx ->
                val path = excludedFolders[idx]
                Card(
                    onClick = { onRemoveExcludedFolder(path) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(path, style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.Delete, contentDescription = "Un-exclude", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun MiniCircleIconBtn(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = tint)
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED VIEWPORT UTILITIES
// =============================================================================
@Composable
private fun ViewportConstrainedAudioConfirmation(
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

@Composable
private fun ActionPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}