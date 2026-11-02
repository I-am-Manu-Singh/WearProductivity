package com.creativeblisslabs.wearproductivity.wear.ui.files

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.documentfile.provider.DocumentFile
import android.graphics.BitmapFactory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.content.ContentUris
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size as AndroidSize
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// =============================================================================
// FILE EXPLORER MODELS, CATEGORIES, SORTS & TRANSFER QUEUE TYPES
// =============================================================================
enum class FileCategoryFilter(val label: String, val shortLabel: String, val accent: Color, val icon: ImageVector) {
    ALL("All Files", "All", Color(0xFF8EAEFF), Icons.Rounded.Folder),
    AUDIO("Audio Tracks", "Audio", Color(0xFF79E2B0), Icons.Rounded.AudioFile),
    VIDEO("Video Clips", "Video", Color(0xFFFF9A7A), Icons.Rounded.Movie),
    IMAGES("Photos & Art", "Images", Color(0xFFBBA5FF), Icons.Rounded.Image),
    DOCS("Documents", "Docs", Color(0xFFFFD177), Icons.Rounded.Description),
    APK("APK Packages", "APKs", Color(0xFFFF7A9A), Icons.Rounded.Android),
    DOWNLOADS("Downloads", "DLs", Color(0xFF68B6FF), Icons.Rounded.Download)
}

enum class FileSortOption(val label: String) {
    NAME_ASC("A–Z"),
    NAME_DESC("Z–A"),
    SIZE_DESC("Size ↓"),
    SIZE_ASC("Size ↑"),
    MODIFIED_DESC("Modified"),
    ADDED_DESC("Added")
}

enum class TransferTransportProtocol(val label: String, val subtitle: String, val icon: ImageVector) {
    DATA_LAYER("DataLayer", "Wearable ChannelClient stream", Icons.Rounded.Watch),
    WIFI("Wi-Fi Direct", "High-speed local LAN socket", Icons.Rounded.Wifi),
    BLUETOOTH("Bluetooth", "OPP / RFCOMM companion link", Icons.Rounded.Bluetooth),
    MOBILE_CLOUD("Mobile Data", "Cloud relay sync to phone", Icons.Rounded.CloudUpload)
}

enum class TransferJobStatus(val label: String, val color: Color) {
    QUEUED("Queued", Color(0xFF8EAEFF)),
    SENDING("Sending", Color(0xFFFFD177)),
    COMPLETED("Sent", Color(0xFF79E2B0)),
    FAILED("Failed", Color(0xFFFF7A9A)),
    CANCELLED("Cancelled", Color.Gray)
}

data class TransferQueueJob(
    val id: String,
    val fileItem: WearFileItem,
    val protocol: TransferTransportProtocol,
    val targetDeviceName: String,
    val progress: Float = 0f,
    val transferredBytes: Long = 0L,
    val speedKbps: Int = 0,
    val status: TransferJobStatus = TransferJobStatus.QUEUED,
    val statusDetail: String = "Waiting in queue..."
)

data class ClipboardFileOp(
    val sourceFile: WearFileItem,
    val isMoveOperation: Boolean
)

data class StorageCategorySummary(
    val category: FileCategoryFilter,
    val count: Int,
    val totalBytes: Long
)
data class WearFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModifiedMs: Long = 0L,
    val createdMs: Long = 0L,
    val extension: String = "",
    val childCount: Int = 0,
    val mimeType: String = "application/octet-stream",
    val canRead: Boolean = true,
    val canWrite: Boolean = true,
    val isHidden: Boolean = false,
    val contentUri: Uri? = null,
    val relativePath: String? = null,
    val dateAddedMs: Long = 0L
) {
    val category: FileCategoryFilter
        get() = when {
            isDirectory -> FileCategoryFilter.ALL
            extension in setOf("mp3", "wav", "m4a", "flac", "ogg", "aac", "opus") ->
                FileCategoryFilter.AUDIO
            extension in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp") ->
                FileCategoryFilter.VIDEO
            extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic") ->
                FileCategoryFilter.IMAGES
            extension in setOf("apk", "aab") ->
                FileCategoryFilter.APK
            extension in setOf(
                "pdf", "txt", "md", "json", "xml", "csv",
                "log", "doc", "docx", "kt", "py"
            ) -> FileCategoryFilter.DOCS
            else -> FileCategoryFilter.ALL
        }

    val isInDownloads: Boolean
        get() = relativePath
            ?.replace('\\', '/')
            ?.startsWith("Download/", ignoreCase = true)
            ?: path.contains("/Download/", ignoreCase = true)

    val formattedSize: String
        get() = if (isDirectory) "$childCount items" else formatByteCount(sizeBytes)

    val formattedModifiedDate: String
        get() = if (lastModifiedMs <= 0L) "--"
        else SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
            .format(Date(lastModifiedMs))

    val formattedCreatedDate: String
        get() {
            val timestamp = dateAddedMs.takeIf { it > 0L } ?: createdMs
            return if (timestamp <= 0L) "--"
            else SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date(timestamp))
        }

    val permissionsBadge: String
        get() = buildString {
            append(if (isDirectory) "d" else "-")
            append(if (canRead) "r" else "-")
            append(if (canWrite) "w" else "-")
        }

    val icon: ImageVector
        get() = if (isDirectory) Icons.Rounded.Folder else category.icon

    val accentColor: Color
        get() = if (isDirectory) Color(0xFF8EAEFF) else category.accent
}

private fun formatByteCount(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    val kb = safe / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.ROOT, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.ROOT, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.ROOT, "%.0f KB", kb)
        else -> "$safe B"
    }
}

private data class FileExplorerAdaptiveSpec(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLargeScreen: Boolean,
    val outerHorizontalMargin: Dp,
    val topBottomSlotHorizontalMargin: Dp,
    val topSlotPadding: Dp,
    val bottomEdgeButtonPadding: Dp
)

// =============================================================================
// HYBRID FILE SYSTEM + MEDIASTORE ENGINE (SOLVES EMPTY FOLDERS ON WEAR OS)
// =============================================================================
private object WearStorageEngine {

    fun hasMediaOrStoragePermissions(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            return true
        }

        val required = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        return required.any {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }


    fun hasAllFilesManageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    fun requiredRuntimePermissions(): Array<String> =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    fun requestAllFilesAccessIntent(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }.onFailure {
                runCatching {
                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                }
            }
        }
    }

    /**
     * Reads a directory using both direct Java File API and Android MediaStore fallback so
     * files inside /sdcard/Music, /sdcard/Download, /sdcard/Pictures, etc. always populate even
     * under Wear OS Scoped Storage rules.
     */
    suspend fun listDirectoryHybrid(
        context: Context,
        dirPath: String,
        rootPath: String,
        mediaItems: List<WearFileItem>
    ): List<WearFileItem> = withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, WearFileItem>()
        val currentDir = File(dirPath)

        // App vault and directories the app can directly access.
        runCatching {
            currentDir.listFiles()?.forEach { file ->
                val item = buildWearFileItem(file)
                result[item.path] = item
            }
        }

        // Match MediaStore records by RELATIVE_PATH, not by DATA.
        val currentRelativePath = dirPath
            .removePrefix(rootPath)
            .trim('/')
            .let { if (it.isBlank()) "" else "$it/" }

        mediaItems.forEach { item ->
            val relative = item.relativePath ?: return@forEach
            val normalized = relative.replace('\\', '/')

            if (normalized.equals(currentRelativePath, ignoreCase = true)) {
                result[item.path] = item
            } else if (
                normalized.startsWith(currentRelativePath, ignoreCase = true)
            ) {
                val remainder = normalized.removePrefix(currentRelativePath)
                val childName = remainder.substringBefore('/').trim()

                if (childName.isNotBlank()) {
                    val childPath = File(dirPath, childName).absolutePath

                    if (!result.containsKey(childPath)) {
                        result[childPath] = WearFileItem(
                            name = childName,
                            path = childPath,
                            isDirectory = true,
                            mimeType = "inode/directory",
                            canRead = true,
                            canWrite = false
                        )
                    }
                }
            }
        }

        // Show the app vault only at shared-storage root.
        val vault = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "LifeOS_Vault"
        )

        if (
            dirPath == rootPath &&
            vault.isDirectory &&
            !result.containsKey(vault.absolutePath)
        ) {
            result[vault.absolutePath] = buildWearFileItem(vault).copy(
                name = "LifeOS Vault (App Storage)"
            )
        }

        result.values.toList()
    }

    /**
     * Global scan across storage + MediaStore for category filtering (Audio, Video, Images, Docs, APKs, Downloads)
     * and storage analytics breakdown.
     */
    suspend fun scanAllCategorizedFiles(
        context: Context,
        rootPath: String
    ): List<WearFileItem> = withContext(Dispatchers.IO) {
        val allItems = LinkedHashMap<String, WearFileItem>()

        // Shared storage: MediaStore is the discovery source.
        queryMediaStoreFiles(context).forEach { item ->
            allItems[item.path] = item
        }

        // App-specific vault: direct File access is appropriate here.
        val vault = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "LifeOS_Vault"
        )

        if (vault.isDirectory) {
            runCatching {
                vault.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val item = buildWearFileItem(file)
                        allItems[item.path] = item
                    }
            }
        }

        allItems.values.toList()
    }
    private fun queryMediaStoreFiles(context: Context): List<WearFileItem> {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")

        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            add(MediaStore.Files.FileColumns.DATE_ADDED)
            add(MediaStore.Files.FileColumns.MIME_TYPE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        val result = LinkedHashMap<String, WearFileItem>()
        val pageSize = 200
        var offset = 0
        var hasMore = true

        while (hasMore) {
            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
            }

            val pageCount = runCatching {
                resolver.query(
                    collection,
                    projection,
                    queryArgs,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val modifiedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val addedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                    val relativePathCol =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                        } else {
                            -1
                        }

                    var count = 0

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol)?.takeIf { it.isNotBlank() }
                            ?: continue

                        val itemUri = ContentUris.withAppendedId(collection, id)
                        val extension = name.substringAfterLast('.', "")
                            .lowercase(Locale.ROOT)

                        val size = if (sizeCol >= 0) {
                            cursor.getLong(sizeCol).coerceAtLeast(0L)
                        } else 0L

                        val modifiedMs = if (modifiedCol >= 0) {
                            cursor.getLong(modifiedCol).coerceAtLeast(0L) * 1000L
                        } else 0L

                        val addedMs = if (addedCol >= 0) {
                            cursor.getLong(addedCol).coerceAtLeast(0L) * 1000L
                        } else 0L

                        val mime = if (mimeCol >= 0) {
                            cursor.getString(mimeCol)
                        } else null

                        val relativePath = if (relativePathCol >= 0) {
                            cursor.getString(relativePathCol)
                        } else null

                        val uriString = itemUri.toString()

                        result[uriString] = WearFileItem(
                            name = name,
                            path = uriString,
                            isDirectory = false,
                            sizeBytes = size,
                            lastModifiedMs = modifiedMs,
                            createdMs = 0L,
                            dateAddedMs = addedMs,
                            extension = extension,
                            mimeType = mime ?: resolveMimeType(extension),
                            canRead = true,
                            canWrite = false,
                            isHidden = name.startsWith("."),
                            contentUri = itemUri,
                            relativePath = relativePath
                        )

                        count++
                    }

                    count
                } ?: 0
            }.getOrDefault(0)

            offset += pageSize
            hasMore = pageCount == pageSize
        }

        return result.values.toList()
    }
    fun buildWearFileItem(f: File): WearFileItem {
        val isDir = f.isDirectory
        val ext = if (isDir) "" else f.extension.lowercase(Locale.ROOT)
        val modMs = runCatching { f.lastModified() }.getOrDefault(0L)
        val createdMs = runCatching {
            val attrs = Files.readAttributes(f.toPath(), BasicFileAttributes::class.java)
            attrs.creationTime().toMillis()
        }.getOrDefault(modMs)
        val childCnt = if (isDir) (runCatching { f.list()?.size ?: 0 }.getOrDefault(0)) else 0

        return WearFileItem(
            name = f.name.ifBlank { f.absolutePath },
            path = f.absolutePath,
            isDirectory = isDir,
            sizeBytes = if (isDir) 0L else runCatching { f.length() }.getOrDefault(0L),
            lastModifiedMs = modMs,
            createdMs = createdMs,
            extension = ext,
            childCount = childCnt,
            mimeType = if (isDir) "inode/directory" else resolveMimeType(ext),
            canRead = runCatching { f.canRead() }.getOrDefault(true),
            canWrite = runCatching { f.canWrite() }.getOrDefault(false),
            isHidden = f.name.startsWith(".")
        )
    }

    private fun resolveMimeType(ext: String): String {
        if (ext.isBlank()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase(Locale.ROOT))
            ?: when (ext.lowercase(Locale.ROOT)) {
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "flac" -> "audio/flac"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "pdf" -> "application/pdf"
                "txt", "log", "md", "kt", "py" -> "text/plain"
                "json" -> "application/json"
                "apk" -> "application/vnd.android.package-archive"
                else -> "application/octet-stream"
            }
    }

    suspend fun computeMd5Checksum(
        context: Context,
        item: WearFileItem
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            if (item.sizeBytes > 50L * 1024L * 1024L) {
                return@withContext "N/A (>50MB)"
            }

            val input = item.contentUri?.let { uri ->
                context.contentResolver.openInputStream(uri)
            } ?: FileInputStream(File(item.path))

            requireNotNull(input) {
                "Unable to open file"
            }

            val digest = MessageDigest.getInstance("MD5")

            input.use { stream ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count == -1) break
                    digest.update(buffer, 0, count)
                }
            }

            digest.digest().joinToString("") {
                "%02x".format(it)
            }.uppercase(Locale.ROOT).take(16)
        }.getOrDefault("UNAVAILABLE")
    }

    suspend fun loadThumbnailBitmap(
        context: Context,
        item: WearFileItem,
        targetPx: Int = 96
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = item.contentUri

            when (item.category) {
                FileCategoryFilter.IMAGES,
                FileCategoryFilter.VIDEO -> {
                    if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver
                            .loadThumbnail(uri, AndroidSize(targetPx, targetPx), null)
                            .asImageBitmap()
                    } else if (item.category == FileCategoryFilter.IMAGES) {
                        BitmapFactory.decodeFile(item.path)?.asImageBitmap()
                    } else {
                        null
                    }
                }

                FileCategoryFilter.AUDIO -> {
                    val retriever = MediaMetadataRetriever()

                    try {
                        if (uri != null) {
                            retriever.setDataSource(context, uri)
                        } else {
                            retriever.setDataSource(item.path)
                        }

                        retriever.embeddedPicture
                            ?.let { bytes ->
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    ?.asImageBitmap()
                            }
                    } finally {
                        retriever.release()
                    }
                }

                else -> null
            }
        }.getOrNull()
    }
}

// =============================================================================
// BASELINE VIEWPORT CONTAINER
// =============================================================================
@Composable
private fun FileExplorerBaselineViewport(
    baselineWatchSize: Dp = 416.dp,
    content: @Composable (FileExplorerAdaptiveSpec) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment =
            Alignment.Center
    ) {
        val isPhonePreview = this.maxWidth > 280.dp || this.maxHeight > 280.dp
        val viewportWidth = if (isPhonePreview) baselineWatchSize else this.maxWidth
        val viewportHeight = if (isPhonePreview) baselineWatchSize else this.maxHeight

        val spec = remember(viewportWidth, viewportHeight) {
            FileExplorerAdaptiveSpec(
                screenWidth = viewportWidth,
                screenHeight = viewportHeight,
                isLargeScreen = viewportWidth >= 225.dp,
                outerHorizontalMargin = viewportWidth * 0.052f,
                topBottomSlotHorizontalMargin = viewportWidth * 0.14f,
                topSlotPadding = viewportHeight * 0.11f,
                bottomEdgeButtonPadding = viewportHeight * 0.22f
            )
        }

        if (isPhonePreview) {
            Box(
                modifier = Modifier
                    .size(baselineWatchSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
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
                    .background(MaterialTheme.colorScheme.background),
                ) {
                content(spec)
            }
        }
    }
}
private fun isValidFileName(raw: String): Boolean {
    val name = raw.trim()

    return name.isNotEmpty() &&
            name != "." &&
            name != ".." &&
            name.none {
                it == '/' ||
                        it == '\\' ||
                        Character.isISOControl(it)
            }
}
// =============================================================================
// MAIN COMPOSABLE: 3-PAGE WEAR OS FILE EXPLORER, ANALYTICS & TRANSFER STUDIO
// =============================================================================

@Composable
fun WearFileExplorerScreen(state: LifeState, repository: LifeRepository) {
    val context = LocalContext.current
    val haptic = rememberLifeOSHapticProvider()
    val scope = rememberCoroutineScope()

    val rootPath = remember {
        Environment.getExternalStorageDirectory()?.absolutePath ?: "/sdcard"
    }
    var currentDirectoryPath by remember { mutableStateOf(rootPath) }

    // Permissions State & Runtime Permission Launcher
    var hasReadPermission by remember {
        mutableStateOf(WearStorageEngine.hasMediaOrStoragePermissions(context))
    }
    var hasAllFilesAccess by remember {
        mutableStateOf(WearStorageEngine.hasAllFilesManageAccess())
    }

    val directoryFiles = remember { mutableStateListOf<WearFileItem>() }
    val globalCategorizedFiles = remember { mutableStateListOf<WearFileItem>() }
    var isScanning by remember { mutableStateOf(false) }

    var activeCategoryFilter by remember { mutableStateOf(FileCategoryFilter.ALL) }
    var activeSortOption by remember { mutableStateOf(FileSortOption.NAME_ASC) }

    // File Operations & Modals State
    var selectedFileForInspector by remember { mutableStateOf<WearFileItem?>(null) }
    var activePreviewFile by remember { mutableStateOf<WearFileItem?>(null) }
    var renamingFileItem by remember { mutableStateOf<WearFileItem?>(null) }
    var showCreateFolderEditor by remember { mutableStateOf(false) }
    var clipboardOp by remember { mutableStateOf<ClipboardFileOp?>(null) }

    // Authentic Sequential One-by-One Transfer Queue State
    val transferQueue = remember { mutableStateListOf<TransferQueueJob>() }
    var selectedProtocol by remember { mutableStateOf(TransferTransportProtocol.DATA_LAYER) }
    var connectedNodeName by remember { mutableStateOf("Companion Phone") }
    var isQueueProcessorRunning by remember { mutableStateOf(false) }

    var feedbackOverlay by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var storageRefreshKey by remember {
        mutableIntStateOf(0)
    }
    // Storage StatFs Telemetry
    val storageStats = remember(rootPath, storageRefreshKey) {
        runCatching {
            val stat = StatFs(rootPath)

            val total = stat.totalBytes.coerceAtLeast(1L)
            val free = stat.availableBytes.coerceIn(0L, total)
            val used = total - free

            Triple(
                used,
                total,
                (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            )
        }.getOrElse {
            Triple(0L, 0L, 0f)
        }
    }
    fun refreshFilesAndAnalytics() {
        if (isScanning) return

        isScanning = true

        scope.launch {
            try {
                hasReadPermission =
                    WearStorageEngine.hasMediaOrStoragePermissions(context)

                hasAllFilesAccess =
                    WearStorageEngine.hasAllFilesManageAccess()

                val allList = WearStorageEngine.scanAllCategorizedFiles(
                    context = context,
                    rootPath = rootPath
                )

                val dirList = WearStorageEngine.listDirectoryHybrid(
                    context = context,
                    dirPath = currentDirectoryPath,
                    rootPath = rootPath,
                    mediaItems = allList
                )

                globalCategorizedFiles.clear()
                globalCategorizedFiles.addAll(allList)

                directoryFiles.clear()
                directoryFiles.addAll(dirList)

                storageRefreshKey++
            } finally {
                isScanning = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasReadPermission =
            WearStorageEngine.hasMediaOrStoragePermissions(context)

        hasAllFilesAccess =
            WearStorageEngine.hasAllFilesManageAccess()

        refreshFilesAndAnalytics()

        feedbackOverlay = (
                if (hasReadPermission) {
                    "Storage Access Granted"
                } else {
                    "Some Storage Permissions Missing"
                }
                ) to !hasReadPermission
    }

    // Auto-request permissions on first entry if not yet granted
    LaunchedEffect(Unit) {
        if (!WearStorageEngine.hasMediaOrStoragePermissions(context)) {
            permissionLauncher.launch(WearStorageEngine.requiredRuntimePermissions())
        }
        // Check connected Wear DataLayer nodes
        runCatching {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.firstOrNull()?.displayName?.let { connectedNodeName = it }
        }
    }

    LaunchedEffect(currentDirectoryPath) {
        if (globalCategorizedFiles.isNotEmpty()) {
            isScanning = true

            try {
                val dirList = WearStorageEngine.listDirectoryHybrid(
                    context = context,
                    dirPath = currentDirectoryPath,
                    rootPath = rootPath,
                    mediaItems = globalCategorizedFiles.toList()
                )

                directoryFiles.clear()
                directoryFiles.addAll(dirList)
            } finally {
                isScanning = false
            }
        }
    }

    // =========================================================================
    // AUTHENTIC SEQUENTIAL ONE-BY-ONE FILE TRANSFER QUEUE PROCESSOR
    // =========================================================================
    LaunchedEffect(Unit) {
        snapshotFlow {
            transferQueue.any {
                it.status == TransferJobStatus.QUEUED
            }
        }.collect { hasQueuedJobs ->

            if (!hasQueuedJobs || isQueueProcessorRunning) {
                return@collect
            }

            isQueueProcessorRunning = true

            try {
                while (isActive) {
                    val jobIdx =
                        transferQueue.indexOfFirst { it.status == TransferJobStatus.QUEUED }
                    if (jobIdx < 0) break

                    val job = transferQueue[jobIdx]
                    val srcFile = File(job.fileItem.path)
                    val fileItem = job.fileItem
                    val totalBytes = fileItem.sizeBytes.coerceAtLeast(1L)
                    transferQueue[jobIdx] = job.copy(
                        status = TransferJobStatus.SENDING,
                        progress = 0.02f,
                        statusDetail = "Opening ${job.protocol.label} link..."
                    )

                    val startTimeMs = System.currentTimeMillis()
                    var realChannelDelivered = false

                    // Attempt real Wearable ChannelClient / DataClient transfer first
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                            val targetNode = nodes.firstOrNull()
                            if (targetNode != null && fileItem.canRead) {
                                withContext(Dispatchers.Main.immediate) {
                                    connectedNodeName = targetNode.displayName
                                }
                                when (job.protocol) {
                                    TransferTransportProtocol.DATA_LAYER, TransferTransportProtocol.BLUETOOTH -> {
                                        val channelClient = Wearable.getChannelClient(context)
                                        val channel = channelClient.openChannel(
                                            targetNode.id,
                                            "/lifeos/file_transfer/${job.fileItem.name}"
                                        ).await()
                                        val outputStream =
                                            channelClient.getOutputStream(channel).await()
                                        val inputStream = fileItem.contentUri?.let { uri ->
                                            context.contentResolver.openInputStream(uri)
                                        } ?: run {
                                            val file = File(fileItem.path)
                                            require(file.isFile && file.canRead()) {
                                                "File is unavailable or unreadable"
                                            }
                                            FileInputStream(file)
                                        }

                                        requireNotNull(inputStream) {
                                            "Unable to open file content"
                                        }

                                        inputStream.use { stream ->
                                            val buffer = ByteArray(32 * 1024)
                                            var read: Int
                                            var sent = 0L

                                            while (stream.read(buffer).also { read = it } != -1) {
                                                currentCoroutineContext().ensureActive()

                                                outputStream.write(buffer, 0, read)
                                                sent += read

                                                val elapsedSec = (
                                                        (System.currentTimeMillis() - startTimeMs) / 1000f
                                                        ).coerceAtLeast(0.1f)

                                                val kbps = (
                                                        (sent / 1024f) / elapsedSec
                                                        ).roundToInt().coerceAtLeast(0)

                                                val progress = (
                                                        sent.toFloat() / totalBytes.toFloat()
                                                        ).coerceIn(0f, 0.99f)

                                                withContext(Dispatchers.Main.immediate) {
                                                    val index = transferQueue.indexOfFirst {
                                                        it.id == job.id
                                                    }

                                                    if (index >= 0 &&
                                                        transferQueue[index].status != TransferJobStatus.CANCELLED
                                                    ) {
                                                        transferQueue[index] = transferQueue[index].copy(
                                                            progress = progress,
                                                            transferredBytes = sent,
                                                            speedKbps = kbps,
                                                            statusDetail = "${formatByteCount(sent)} / ${formatByteCount(totalBytes)}"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        channelClient.close(channel).await()
                                        realChannelDelivered = true
                                    }

                                    TransferTransportProtocol.WIFI,
                                    TransferTransportProtocol.MOBILE_CLOUD -> {
                                        throw UnsupportedOperationException(
                                            "${job.protocol.label} file transfer is not implemented"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // If no physical companion node was open or using Wi-Fi/Mobile packet framing, stream chunked progress
                    if (!realChannelDelivered) {
                        val failedIdx = transferQueue.indexOfFirst {
                            it.id == job.id
                        }

                        if (
                            failedIdx >= 0 &&
                            transferQueue[failedIdx].status != TransferJobStatus.CANCELLED
                        ) {
                            transferQueue[failedIdx] = transferQueue[failedIdx].copy(
                                status = TransferJobStatus.FAILED,
                                progress = 0f,
                                statusDetail = "No real file payload was transferred"
                            )
                        }

                        continue
                    }

                    val finalIdx = transferQueue.indexOfFirst { it.id == job.id }
                    if (finalIdx >= 0 && transferQueue[finalIdx].status != TransferJobStatus.CANCELLED) {
                        transferQueue[finalIdx] = transferQueue[finalIdx].copy(
                            progress = 1f,
                            transferredBytes = totalBytes,
                            status = TransferJobStatus.COMPLETED,
                            statusDetail = "Delivered via ${job.protocol.label} to ${transferQueue[finalIdx].targetDeviceName}"
                        )
                        haptic.click()
                        feedbackOverlay = "Sent ${job.fileItem.name}" to false
                    }
                }
            } finally {
                isQueueProcessorRunning = false
            }
        }
    }

    fun enqueueFileTransfer(item: WearFileItem, protocol: TransferTransportProtocol = selectedProtocol) {
        haptic.click()
        if (item.isDirectory) {
            // Enqueue all files inside directory one by one
            val children = File(item.path).listFiles()?.filter { it.isFile }.orEmpty()
            if (children.isEmpty()) {
                feedbackOverlay = "Folder has no direct files" to true
                return
            }
            children.forEach { f ->
                val childItem = WearStorageEngine.buildWearFileItem(f)
                transferQueue.add(
                    TransferQueueJob(
                        id = "tx-${System.currentTimeMillis()}-${f.name.hashCode()}",
                        fileItem = childItem,
                        protocol = protocol,
                        targetDeviceName = connectedNodeName
                    )
                )
            }
            feedbackOverlay = "Queued ${children.size} files (${protocol.label})" to false
        } else {
            transferQueue.add(
                TransferQueueJob(
                    id = "tx-${System.currentTimeMillis()}-${item.name.hashCode()}",
                    fileItem = item,
                    protocol = protocol,
                    targetDeviceName = connectedNodeName
                )
            )
            feedbackOverlay = "Queued ${item.name} (${protocol.label})" to false
        }
    }

    fun launchSystemBluetoothOrWifiShare(item: WearFileItem) {
        haptic.click()
        runCatching {
            val uri = item.contentUri ?: Uri.fromFile(File(item.path))
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share ${item.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            enqueueFileTransfer(item, TransferTransportProtocol.BLUETOOTH)
        }
    }

    // =========================================================================
    // REAL FILE SYSTEM OPERATIONS: RENAME, COPY, MOVE (PASTE), DELETE, NEW FOLDER
    // =========================================================================
    fun executeRename(item: WearFileItem, newNameInput: String) {
        val cleanName = newNameInput.trim()

        if (!isValidFileName(cleanName)) {
            feedbackOverlay = "Invalid File Name" to true
            return
        }

        scope.launch(Dispatchers.IO) {
            val success = runCatching {
                val src = File(item.path)
                val parent = src.parentFile
                    ?: error("Invalid source directory")

                require(src.exists() && parent.isDirectory)

                val dest = File(parent, cleanName)

                if (dest.canonicalFile == src.canonicalFile) {
                    true
                } else {
                    require(!dest.exists()) {
                        "A file with this name already exists"
                    }

                    src.renameTo(dest)
                }
            }.getOrDefault(false)

            withContext(Dispatchers.Main) {
                if (success) {
                    renamingFileItem = null
                    selectedFileForInspector = null
                }

                refreshFilesAndAnalytics()

                feedbackOverlay = (
                        if (success) "Renamed to $cleanName"
                        else "Rename Failed"
                        ) to !success

                haptic.click()
            }
        }
    }

    fun executePasteIntoCurrentDir() {
        val op = clipboardOp ?: return
        scope.launch(Dispatchers.IO) {
            val src = File(op.sourceFile.path)
            val targetDir = File(currentDirectoryPath)
            var dest = File(targetDir, src.name)
            if (dest.absolutePath == src.absolutePath) {
                dest = File(targetDir, "Copy_${src.name}")
            }
            val ok = runCatching {
                require(src.exists() && src.canRead())
                require(targetDir.isDirectory && targetDir.canWrite())

                val sourcePath = src.canonicalPath
                val targetPath = targetDir.canonicalPath

                require(
                    !src.isDirectory ||
                            !targetPath.startsWith(sourcePath + File.separator)
                ) {
                    "Cannot copy a folder into itself"
                }

                require(!dest.exists()) {
                    "Destination already exists"
                }

                val copied = if (src.isDirectory) {
                    src.copyRecursively(
                        target = dest,
                        overwrite = false
                    )
                } else {
                    src.copyTo(
                        target = dest,
                        overwrite = false
                    )
                    true
                }

                require(copied) {
                    "Copy failed"
                }

                if (op.isMoveOperation) {
                    require(src.deleteRecursively()) {
                        "Copied, but original could not be deleted"
                    }
                }

                true
            }.getOrDefault(false)

            withContext(Dispatchers.Main) {
                if (op.isMoveOperation && ok) clipboardOp = null
                refreshFilesAndAnalytics()
                feedbackOverlay = (if (ok) {
                    if (op.isMoveOperation) "Moved ${src.name}" else "Copied ${src.name}"
                } else "Write Restricted Here") to !ok
                haptic.click()
            }
        }
    }

    fun executeDelete(item: WearFileItem) {
        scope.launch(Dispatchers.IO) {
            val f = File(item.path)
            val deleted = runCatching {
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (selectedFileForInspector?.path == item.path) selectedFileForInspector = null
                refreshFilesAndAnalytics()
                feedbackOverlay = (if (deleted) "Deleted ${item.name}" else "Delete Restricted") to !deleted
                haptic.click()
            }
        }
    }

    fun executeCreateFolder(folderName: String) {
        val clean = folderName.trim()

        if (!isValidFileName(clean)) {
            feedbackOverlay = "Invalid Folder Name" to true
            return
        }
        scope.launch(Dispatchers.IO) {
            val newDir = File(currentDirectoryPath, clean)
            val created = runCatching {
                val parent = File(currentDirectoryPath).canonicalFile
                val target = File(parent, clean).canonicalFile

                target.parentFile == parent &&
                        parent.isDirectory &&
                        !target.exists() &&
                        target.mkdir()
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                showCreateFolderEditor = false
                refreshFilesAndAnalytics()
                feedbackOverlay = (if (created) "Folder '$clean' Created" else "Cannot Create Here") to !created
                haptic.click()
            }
        }
    }

    // =========================================================================
    // FILTERING & SORTING PIPELINE
    // =========================================================================
    fun WearFileItem.matchesCategoryFilter(
        filter: FileCategoryFilter
    ): Boolean {
        if (isDirectory) return false

        return when (filter) {
            FileCategoryFilter.ALL -> true
            FileCategoryFilter.DOWNLOADS -> isInDownloads
            else -> category == filter
        }
    }

    val sourceItemsForDisplay = remember(
        directoryFiles.toList(),
        globalCategorizedFiles.toList(),
        activeCategoryFilter
    ) {
        if (activeCategoryFilter == FileCategoryFilter.ALL) {
            directoryFiles.toList()
        } else {
            (directoryFiles + globalCategorizedFiles)
                .filter { it.matchesCategoryFilter(activeCategoryFilter) }
                .distinctBy { it.path }
        }
    }

    val sortedDisplayedFiles = remember(sourceItemsForDisplay, activeSortOption) {
        val comparator = when (activeSortOption) {
            FileSortOption.NAME_ASC -> compareBy<WearFileItem> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
            FileSortOption.NAME_DESC -> compareBy<WearFileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase(Locale.ROOT) }
            FileSortOption.SIZE_DESC -> compareBy<WearFileItem> { !it.isDirectory }.thenByDescending { it.sizeBytes }
            FileSortOption.SIZE_ASC -> compareBy<WearFileItem> { !it.isDirectory }.thenBy { it.sizeBytes }
            FileSortOption.MODIFIED_DESC -> compareBy<WearFileItem> { !it.isDirectory }.thenByDescending { it.lastModifiedMs }
            FileSortOption.ADDED_DESC ->
                compareBy<WearFileItem> { !it.isDirectory }
                    .thenByDescending {
                        it.dateAddedMs.takeIf { timestamp -> timestamp > 0L }
                            ?: it.createdMs
                    }
        }
        sourceItemsForDisplay.sortedWith(comparator)
    }

    // Category Analytics Breakdown
    val categorySummaries = remember(
        globalCategorizedFiles.toList(),
        directoryFiles.toList()
    ) {
        val pool = (globalCategorizedFiles + directoryFiles)
            .filter { !it.isDirectory }
            .distinctBy { it.path }

        FileCategoryFilter.entries
            .filter { it != FileCategoryFilter.ALL }
            .map { category ->
                val matching = pool.filter {
                    it.matchesCategoryFilter(category)
                }

                StorageCategorySummary(
                    category = category,
                    count = matching.size,
                    totalBytes = matching.sumOf { it.sizeBytes }
                )
            }
    }
    // Breadcrumb Segments Computation
    val breadcrumbSegments = remember(currentDirectoryPath, rootPath) {
        val list = mutableListOf("Root" to rootPath)
        if (currentDirectoryPath.startsWith(rootPath) && currentDirectoryPath != rootPath) {
            val rel = currentDirectoryPath.removePrefix(rootPath).trim('/')
            var accum = rootPath
            rel.split('/').filter { it.isNotBlank() }.forEach { part ->
                accum = "$accum/$part"
                list.add(part to accum)
            }
        } else if (currentDirectoryPath != rootPath) {
            list.add(File(currentDirectoryPath).name.ifBlank { "App" } to currentDirectoryPath)
        }
        list
    }

    // 3-Page Vertical Pager:
    // Page 0 = Storage Analytics Cards, Category Breakdown & Permissions Deck
    // Page 1 = Compact Chip Breadcrumbs, Filter/Sort Bars, Thumbnail File Cards & Clipboard
    // Page 2 = Multi-Transport Sequential Transfer Queue (Wi-Fi, Bluetooth, DataLayer, Mobile Data)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    val storagePrefs = remember(context) {
        context.getSharedPreferences(
            "wear_file_explorer_storage",
            Context.MODE_PRIVATE
        )
    }

    var selectedTreeUri by remember {
        mutableStateOf(
            storagePrefs.getString("selected_tree_uri", null)
                ?.let(Uri::parse)
        )
    }

    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val requestedFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            val persistedFlags = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    requestedFlags
                )
                requestedFlags
            }.getOrElse {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                }.getOrDefault(0)
            }

            if (persistedFlags != 0) {
                selectedTreeUri = uri
                storagePrefs.edit()
                    .putString("selected_tree_uri", uri.toString())
                    .apply()

                feedbackOverlay = "Folder access granted" to false
            } else {
                feedbackOverlay = "Unable to persist folder access" to true
            }
        }
    }
    FileExplorerBaselineViewport { adaptiveSpec ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            ) {
            VerticalPager(
                state = pagerState,
                flingBehavior = pagerFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    // =========================================================================
                    // PAGE 0: STORAGE ANALYTICS CARDS, MEDIA BREAKDOWN & PERMISSIONS
                    // =========================================================================
                    0 -> StorageAnalyticsAndPermissionsPage(
                        spec = adaptiveSpec,
                        usedBytes = storageStats.first,
                        totalBytes = storageStats.second,
                        usedRatio = storageStats.third,
                        hasReadPermission = hasReadPermission,
                        hasAllFilesAccess = hasAllFilesAccess,
                        categorySummaries = categorySummaries,
                        activeCategoryFilter = activeCategoryFilter,
                        onRequestRuntimePermissions = {
                            haptic.click()
                            permissionLauncher.launch(WearStorageEngine.requiredRuntimePermissions())
                        },
                        onRequestAllFilesAccess = {
                            haptic.click()
                            WearStorageEngine.requestAllFilesAccessIntent(context)
                        },
                        onSelectCategoryToBrowse = { cat ->
                            haptic.click()
                            activeCategoryFilter = cat
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onOpenAppVaultDirectory = {
                            haptic.click()
                            val vault = File(context.getExternalFilesDir(null) ?: context.filesDir, "LifeOS_Vault")
                            vault.mkdirs()
                            currentDirectoryPath = vault.absolutePath
                            activeCategoryFilter = FileCategoryFilter.ALL
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onOpenDocumentTree = {
                            haptic.click()
                            treePicker.launch(null)
                        },
                        onJumpToBrowser = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )

                    // =========================================================================
                    // PAGE 1: FILE EXPLORER, COMPACT BREADCRUMB CHIPS, SORT/FILTER & THUMBNAILS
                    // =========================================================================
                    1 -> FileBrowserAndOperationsPage(
                        spec = adaptiveSpec,
                        breadcrumbSegments = breadcrumbSegments,
                        files = sortedDisplayedFiles,
                        isScanning = isScanning,
                        hasReadPermission = hasReadPermission,
                        activeCategoryFilter = activeCategoryFilter,
                        activeSortOption = activeSortOption,
                        clipboardOp = clipboardOp,
                        showCreateFolderEditor = showCreateFolderEditor,
                        renamingFileItem = renamingFileItem,
                        activeQueueCount = transferQueue.count { it.status == TransferJobStatus.QUEUED || it.status == TransferJobStatus.SENDING },
                        onNavigateToPath = { path ->
                            haptic.click()
                            activeCategoryFilter = FileCategoryFilter.ALL
                            currentDirectoryPath = path
                        },
                        onNavigateUp = {
                            haptic.click()
                            if (activeCategoryFilter != FileCategoryFilter.ALL) {
                                activeCategoryFilter = FileCategoryFilter.ALL
                            } else {
                                val parent = File(currentDirectoryPath).parentFile
                                if (parent != null && currentDirectoryPath != rootPath && parent.canRead()) {
                                    currentDirectoryPath = parent.absolutePath
                                } else {
                                    currentDirectoryPath = rootPath
                                }
                            }
                        },
                        onSelectCategory = { cat ->
                            haptic.click()
                            activeCategoryFilter = cat
                        },
                        onSelectSort = { sort ->
                            haptic.click()
                            activeSortOption = sort
                        },
                        onRequestPermissions = {
                            haptic.click()
                            permissionLauncher.launch(WearStorageEngine.requiredRuntimePermissions())
                            if (!WearStorageEngine.hasAllFilesManageAccess()) {
                                WearStorageEngine.requestAllFilesAccessIntent(context)
                            }
                        },
                        onRefresh = {
                            haptic.click()
                            refreshFilesAndAnalytics()
                            feedbackOverlay = "Refreshed Storage" to false
                        },
                        onToggleCreateFolder = {
                            haptic.click()
                            showCreateFolderEditor = !showCreateFolderEditor
                        },
                        onCreateFolder = ::executeCreateFolder,
                        onPasteClipboard = ::executePasteIntoCurrentDir,
                        onClearClipboard = {
                            haptic.click()
                            clipboardOp = null
                        },
                        onConfirmRename = ::executeRename,
                        onCancelRename = {
                            haptic.click()
                            renamingFileItem = null
                        },
                        onOpenItem = { item ->
                            haptic.click()
                            if (item.isDirectory) {
                                activeCategoryFilter = FileCategoryFilter.ALL
                                currentDirectoryPath = item.path
                            } else {
                                activePreviewFile = item
                            }
                        },
                        onInspectItem = { item ->
                            haptic.click()
                            selectedFileForInspector = item
                        },
                        onQuickSendItem = { item ->
                            enqueueFileTransfer(item, selectedProtocol)
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onDeleteItem = ::executeDelete,
                        onJumpToAnalytics = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onJumpToTransferQueue = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    // =========================================================================
                    // PAGE 2: MULTI-TRANSPORT SEQUENTIAL TRANSFER QUEUE & PROGRESS RING
                    // =========================================================================
                    else -> TransferQueueAndTransportStudioPage(
                        spec = adaptiveSpec,
                        selectedProtocol = selectedProtocol,
                        connectedNodeName = connectedNodeName,
                        queue = transferQueue,
                        onSelectProtocol = { proto ->
                            haptic.click()
                            selectedProtocol = proto
                            feedbackOverlay = "Transport: ${proto.label}" to false
                        },
                        onCancelOrRemoveJob = { jobId ->
                            haptic.click()
                            val idx = transferQueue.indexOfFirst { it.id == jobId }
                            if (idx >= 0) {
                                if (transferQueue[idx].status == TransferJobStatus.SENDING) {
                                    transferQueue[idx] = transferQueue[idx].copy(
                                        status = TransferJobStatus.CANCELLED,
                                        statusDetail = "Cancelled by user"
                                    )
                                } else {
                                    transferQueue.removeAt(idx)
                                }
                            }
                        },
                        onRetryFailedJobs = {
                            haptic.click()
                            for (i in transferQueue.indices) {
                                if (transferQueue[i].status == TransferJobStatus.FAILED || transferQueue[i].status == TransferJobStatus.CANCELLED) {
                                    transferQueue[i] = transferQueue[i].copy(
                                        status = TransferJobStatus.QUEUED,
                                        progress = 0f,
                                        statusDetail = "Re-queued"
                                    )
                                }
                            }
                        },
                        onClearCompleted = {
                            haptic.click()
                            transferQueue.removeAll { it.status == TransferJobStatus.COMPLETED || it.status == TransferJobStatus.CANCELLED }
                        },
                        onBackToBrowser = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
            }

            // =========================================================================
            // FULL-SCREEN REAL MEDIA & DOCUMENT PREVIEW OVERLAY
            // =========================================================================
            activePreviewFile?.let { fileToPreview ->
                M3RealFilePreviewOverlay(
                    item = fileToPreview,
                    onOpenInspector = {
                        haptic.click()
                        selectedFileForInspector = fileToPreview
                        activePreviewFile = null
                    },
                    onSendFile = {
                        enqueueFileTransfer(fileToPreview, selectedProtocol)
                        activePreviewFile = null
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onClose = {
                        haptic.click()
                        activePreviewFile = null
                    }
                )
            }

            // =========================================================================
            // COMPREHENSIVE FILE INSPECTOR & OPERATIONS MODAL (Rename, Copy, Move, Share, Info)
            // =========================================================================
            selectedFileForInspector?.let { inspectedItem ->
                M3ComprehensiveFileInspectorModal(
                    item = inspectedItem,
                    selectedProtocol = selectedProtocol,
                    onOpenPreview = {
                        haptic.click()
                        activePreviewFile = inspectedItem
                        selectedFileForInspector = null
                    },
                    onStartRename = {
                        haptic.click()
                        renamingFileItem = inspectedItem
                        selectedFileForInspector = null
                    },
                    onCopyFile = {
                        haptic.click()
                        clipboardOp = ClipboardFileOp(inspectedItem, isMoveOperation = false)
                        selectedFileForInspector = null
                        feedbackOverlay = "Copied ${inspectedItem.name}" to false
                    },
                    onCutMoveFile = {
                        haptic.click()
                        clipboardOp = ClipboardFileOp(inspectedItem, isMoveOperation = true)
                        selectedFileForInspector = null
                        feedbackOverlay = "Cut ${inspectedItem.name} (Navigate & Paste)" to false
                    },
                    onEnqueueTransfer = { proto ->
                        selectedProtocol = proto
                        enqueueFileTransfer(inspectedItem, proto)
                        selectedFileForInspector = null
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    onSystemShare = {
                        launchSystemBluetoothOrWifiShare(inspectedItem)
                        selectedFileForInspector = null
                    },
                    onDeleteFile = {
                        executeDelete(inspectedItem)
                    },
                    onDismiss = {
                        haptic.click()
                        selectedFileForInspector = null
                    }
                )
            }

            // Right Bezel 3-Page Vertical Pager Indicator
            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 3,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            // In-Viewport Constrained Curved Confirmation Overlay
            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedFileExplorerConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }
        }
    }
}
// =============================================================================
// PAGE 0: STORAGE ANALYTICS CARDS, CATEGORY FILTER CHIPS & PERMISSIONS
// =============================================================================
@Composable
private fun StorageAnalyticsAndPermissionsPage(
    spec: FileExplorerAdaptiveSpec,
    usedBytes: Long,
    totalBytes: Long,
    usedRatio: Float,
    hasReadPermission: Boolean,
    hasAllFilesAccess: Boolean,
    categorySummaries: List<StorageCategorySummary>,
    activeCategoryFilter: FileCategoryFilter,
    onRequestRuntimePermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onSelectCategoryToBrowse: (FileCategoryFilter) -> Unit,
    onOpenAppVaultDirectory: () -> Unit,
    onOpenDocumentTree: () -> Unit,
    onJumpToBrowser: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToBrowser,
                label = "Browse Files",
                icon = Icons.Rounded.FolderOpen
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
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement
            = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "STORAGE STUDIO",
                    badgeText = "${(usedRatio * 100).roundToInt()}% USED"
                )
            }

            // 1. PERMISSION GRANT BANNER (If Media or All-Files Access is missing)
            if (!hasReadPermission || !hasAllFilesAccess) {
                item {
                    Card(
                        onClick = if (!hasReadPermission) onRequestRuntimePermissions else onRequestAllFilesAccess,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (!hasReadPermission) "GRANT MEDIA PERMISSIONS" else "ENABLE ALL FILES ACCESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = "Tap to allow Wear OS to read all files inside Music, Downloads, Docs & APK folders.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // 2. WATCH STORAGE CAPACITY & SEGMENTED BAR CARD
            item {
                Card(
                    onClick = onJumpToBrowser,
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
                                Text("INTERNAL STORAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${formatByteCount(usedBytes)} / ${formatByteCount(totalBytes)}",
                                    style = MaterialTheme.typography.titleSmall.tabularNums(),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "${formatByteCount(totalBytes - usedBytes)} Free",
                                style = MaterialTheme.typography.labelSmall.tabularNums(),
                                color = Color(0xFF79E2B0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { usedRatio },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. CATEGORY ANALYTICS CARDS (Audio, Video, Images, Docs, APKs, Downloads)
            item {
                CatalogM3ListHeaderSample(title = "MEDIA & FILE ANALYTICS")
            }

            items(categorySummaries.size) { idx ->
                val summary = categorySummaries[idx]
                val cat = summary.category
                Card(
                    onClick = { onSelectCategoryToBrowse(cat) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeCategoryFilter == cat) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
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
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(cat.accent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${summary.count} files · ${formatByteCount(summary.totalBytes)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Filter ${cat.label}",
                            modifier = Modifier.size(16.dp),
                            tint = cat.accent
                        )
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onOpenAppVaultDirectory,
                    icon = { Icon(Icons.Rounded.FolderSpecial, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open LifeOS Sandbox Vault")
                }
                FilledTonalButton(
                    onClick = onOpenDocumentTree,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Grant Folder Access")
                }
            }
        }
    }
}

// =============================================================================
// PAGE 1: FILE BROWSER, COMPACT CHIP BREADCRUMBS, SORT/FILTER & THUMBNAIL CARDS
// =============================================================================
@Composable
private fun FileBrowserAndOperationsPage(
    spec: FileExplorerAdaptiveSpec,
    breadcrumbSegments: List<Pair<String, String>>,
    files: List<WearFileItem>,
    isScanning: Boolean,
    hasReadPermission: Boolean,
    activeCategoryFilter: FileCategoryFilter,
    activeSortOption: FileSortOption,
    clipboardOp: ClipboardFileOp?,
    showCreateFolderEditor: Boolean,
    renamingFileItem: WearFileItem?,
    activeQueueCount: Int,
    onNavigateToPath: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSelectCategory: (FileCategoryFilter) -> Unit,
    onSelectSort: (FileSortOption) -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onToggleCreateFolder: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onClearClipboard: () -> Unit,
    onConfirmRename: (WearFileItem, String) -> Unit,
    onCancelRename: () -> Unit,
    onOpenItem: (WearFileItem) -> Unit,
    onInspectItem: (WearFileItem) -> Unit,
    onQuickSendItem: (WearFileItem) -> Unit,
    onDeleteItem: (WearFileItem) -> Unit,
    onJumpToAnalytics: () -> Unit,
    onJumpToTransferQueue: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = if (activeQueueCount > 0) onJumpToTransferQueue else onRefresh,
                label = if (activeQueueCount > 0) "Queue ($activeQueueCount)" else "Refresh",
                icon = if (activeQueueCount > 0) Icons.Rounded.CloudUpload else Icons.Default.Refresh
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
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement
            = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = if (activeCategoryFilter == FileCategoryFilter.ALL) "FILE EXPLORER" else activeCategoryFilter.label.uppercase(Locale.ROOT),
                    badgeText = "${files.size} ITEMS"
                )
            }

            // 1. COMPACT SPACE-SAVING HORIZONTAL CHIP BREADCRUMB BAR + NEW FOLDER / ANALYTICS BUTTONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Up / Back Pill
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onNavigateUp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Up", modifier = Modifier.size(14.dp))
                    }

                    // Compact Scrollable Breadcrumb Chips
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        breadcrumbSegments.forEachIndexed { idx, (label, path) ->
                            val isLast = idx == breadcrumbSegments.lastIndex
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isLast) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .clickable { onNavigateToPath(path) }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = label.take(12),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = if (isLast) FontWeight.Black else FontWeight.Medium,
                                    color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (!isLast) {
                                Text("›", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // New Folder Mini Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onToggleCreateFolder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 2. ACTIVE CLIPBOARD PASTE BAR (When a File is Copied or Cut for Moving)
            if (clipboardOp != null) {
                item {
                    Card(
                        onClick = onPasteClipboard,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (clipboardOp.isMoveOperation) "MOVE HERE: ${clipboardOp.sourceFile.name}"
                                    else "PASTE COPY: ${clipboardOp.sourceFile.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to paste into current folder",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(15.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cancel Clipboard",
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable(onClick = onClearClipboard)
                                )
                            }
                        }
                    }
                }
            }

            // 3. INLINE RENAME OR NEW FOLDER UNIVERSAL EDITOR
            if (renamingFileItem != null) {
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "RENAME",
                        initialTitle = renamingFileItem.name,
                        initialTimeOrDate = renamingFileItem.formattedSize,
                        initialSubtitle = renamingFileItem.path,
                        isEditingExisting = true,
                        showTimePickerField = false,
                        showSubtitleField = false,
                        onSave = { newName, _, _ -> onConfirmRename(renamingFileItem, newName) },
                        onCancel = onCancelRename,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (showCreateFolderEditor) {
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "FOLDER",
                        initialTitle = "New_Folder",
                        initialTimeOrDate = "",
                        initialSubtitle = "",
                        isEditingExisting = false,
                        showTimePickerField = false,
                        showSubtitleField = false,
                        onSave = { folderName, _, _ -> onCreateFolder(folderName) },
                        onCancel = onToggleCreateFolder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. COMPACT HORIZONTAL CATEGORY FILTER CHIPS (All, Audio, Video, Images, Docs, APKs, DLs)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileCategoryFilter.entries.forEach { cat ->
                        val selected = activeCategoryFilter == cat
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) cat.accent
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { onSelectCategory(cat) }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = if (selected) MaterialTheme.colorScheme.background else cat.accent
                            )
                            Text(
                                text = cat.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 5. COMPACT HORIZONTAL SORT BAR (A–Z, Z–A, Size ↓, Size ↑, Modified, Created)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileSortOption.entries.forEach { sortOpt ->
                        val selected = activeSortOption == sortOpt
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .clickable { onSelectSort(sortOpt) }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sortOpt.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. FILE & FOLDER ITEM CARDS WITH REAL THUMBNAILS & METADATA CHIPS
            if (files.isEmpty()) {
                item {
                    AppCard(
                        onClick = onRequestPermissions,
                        appName = { Text("Storage") },
                        title = { Text(if (isScanning) "Scanning..." else "No Files Shown") },
                        appImage = { Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (!hasReadPermission) "Tap to grant Wear OS file read permissions"
                            else "Folder is empty or tap to grant All-Files access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(files.size) { idx ->
                    val item = files[idx]
                    M3ThumbnailFileItemCard(
                        item = item,
                        onOpen = { onOpenItem(item) },
                        onInspect = { onInspectItem(item) },
                        onQuickSend = { onQuickSendItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onJumpToAnalytics,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Analytics", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = onJumpToTransferQueue,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Send Queue", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// =============================================================================
// FILE & FOLDER CARD WITH ASYNC THUMBNAIL DECODING & INFO CHIPS
// =============================================================================
@Composable
private fun TransformingLazyColumnItemScope.M3ThumbnailFileItemCard(
    item: WearFileItem,
    onOpen: () -> Unit,
    onInspect: () -> Unit,
    onQuickSend: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(item.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.path) {
        if (!item.isDirectory && item.category in setOf(FileCategoryFilter.IMAGES, FileCategoryFilter.VIDEO, FileCategoryFilter.AUDIO)) {
            thumbnailBitmap = WearStorageEngine.loadThumbnailBitmap(context, item, targetPx = 72)
        }
    }

    WearSwipeableActionCard(
        onCardClick = onOpen,
        onEditClick = onInspect,
        onDeleteClick = onDelete,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Thumbnail or Category Vector Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.accentColor.copy(alpha = 0.20f))
                        .border(0.8.dp, item.accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = thumbnailBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = item.accentColor
                        )
                    }
                }

                // File Name & Info Chips Row
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Compact File Metadata Chips (Size / Child Count, Extension, Date, Perms)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FileMetaMiniChip(text = item.formattedSize, color = item.accentColor)
                        if (!item.isDirectory && item.extension.isNotBlank()) {
                            FileMetaMiniChip(
                                text = item.extension.uppercase(Locale.ROOT).take(4),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        FileMetaMiniChip(
                            text = item.formattedModifiedDate.substringBefore(' '),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Action Buttons (Info Inspector / Send)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (!item.isDirectory) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(onClick = onQuickSend),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Upload,
                                contentDescription = "Queue Send",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onInspect),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "File Info & Actions",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileMetaMiniChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 8.sp),
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

// =============================================================================
// PAGE 2: MULTI-TRANSPORT SEQUENTIAL TRANSFER QUEUE (ONE-BY-ONE WITH PROGRESS)
// =============================================================================
@Composable
private fun TransferQueueAndTransportStudioPage(
    spec: FileExplorerAdaptiveSpec,
    selectedProtocol: TransferTransportProtocol,
    connectedNodeName: String,
    queue: List<TransferQueueJob>,
    onSelectProtocol: (TransferTransportProtocol) -> Unit,
    onCancelOrRemoveJob: (String) -> Unit,
    onRetryFailedJobs: () -> Unit,
    onClearCompleted: () -> Unit,
    onBackToBrowser: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    val activeJob = queue.firstOrNull { it.status == TransferJobStatus.SENDING }
    val completedCount = queue.count { it.status == TransferJobStatus.COMPLETED }
    val overallQueueProgress = if (queue.isEmpty()) 0f else {
        queue.sumOf { it.progress.toDouble() }.toFloat() / queue.size.toFloat()
    }
    val animatedBoundaryProg by animateFloatAsState(
        targetValue = overallQueueProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "transferQueueBoundaryProg"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            scrollState = columnState,
            timeText = { TimeText() },
            edgeButton = {
                WearAnchoredEdgeButton(
                    onClick = onBackToBrowser,
                    label = "File Browser",
                    icon = Icons.Rounded.Folder
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
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    WearExpressiveHeader(
                        title = "TRANSFER QUEUE",
                        badgeText = "$completedCount/${queue.size} SENT"
                    )
                }


                item {
                    WearActionButtonGroup(
                        options = TransferTransportProtocol.entries.map { it.label.take(7) },
                        selectedIndex = selectedProtocol.ordinal,
                        onOptionSelected = { idx -> onSelectProtocol(TransferTransportProtocol.entries[idx]) }
                    )
                }

                item {
                    WearMetricCard(
                        title = "TARGET DEVICE · ${selectedProtocol.label.uppercase()}",
                        value = connectedNodeName,
                        subtitle = selectedProtocol.subtitle,
                        icon = selectedProtocol.icon,
                        onClick = {}
                    )
                }

                // 2. ACTIVE ONE-BY-ONE STREAMING TRANSFER TELEMETRY CARD
                if (activeJob != null) {
                    item {
                        Card(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SENDING: ${activeJob.fileItem.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(activeJob.progress * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium.tabularNums(),
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = activeJob.statusDetail,
                                    style = MaterialTheme.typography.bodySmall.tabularNums().copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                LinearProgressIndicator(
                                    progress = { activeJob.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 3. SEQUENTIAL QUEUE ITEMS LIST
                item {
                    CatalogM3ListHeaderSample(title = "QUEUE ITEMS (${queue.size})")
                }

                if (queue.isEmpty()) {
                    item {
                        WearMetricCard(
                            title = "QUEUE IS EMPTY",
                            value = "Tap Upload on Any File",
                            subtitle = "Files send sequentially one-by-one",
                            icon = Icons.Rounded.CloudDone,
                            onClick = onBackToBrowser
                        )
                    }
                } else {
                    items(queue.size) { idx ->
                        val job = queue[idx]
                        Card(
                            onClick = { onCancelOrRemoveJob(job.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${idx + 1}. ${job.fileItem.name}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${job.protocol.label} · ${job.statusDetail}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    FileMetaMiniChip(text = job.status.label, color = job.status.color)
                                }
                                LinearProgressIndicator(
                                    progress = { job.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp),
                                    colors = ProgressIndicatorDefaults.colors(
                                        indicatorColor = job.status.color,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onRetryFailedJobs,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("Retry All", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = onClearCompleted,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("Clear Done", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Outer Boundary Progress Ring when Queue is Actively Transferring
        if (queue.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            ) {
                drawArc(
                    color = Color(0xFF79E2B0),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedBoundaryProg,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

// =============================================================================
// REAL FILE PREVIEW OVERLAY (IMAGES, AUDIO/VIDEO PLAYER, TEXT/JSON/DOCS, APKS)
// =============================================================================
@Composable
private fun M3RealFilePreviewOverlay(
    item: WearFileItem,
    onOpenInspector: () -> Unit,
    onSendFile: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val columnState = rememberTransformingLazyColumnState()

    var decodedBitmap by remember(item.path) { mutableStateOf<ImageBitmap?>(null) }
    var textPreviewContent by remember(item.path) { mutableStateOf<String?>(null) }

    // Built-in MediaPlayer for Audio & Video Preview
    var isPlayingMedia by remember { mutableStateOf(false) }
    var mediaPositionMs by remember { mutableIntStateOf(0) }
    var mediaDurationMs by remember { mutableIntStateOf(1) }
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    var isMediaPrepared by remember(item.path) {
        mutableStateOf(false)
    }

    DisposableEffect(item.path) {
        var player: MediaPlayer? = null

        if (
            item.category == FileCategoryFilter.AUDIO ||
            item.category == FileCategoryFilter.VIDEO
        ) {
            runCatching {
                MediaPlayer().also { mp ->
                    player = mp

                    mp.setOnPreparedListener { prepared ->
                        mediaDurationMs = prepared.duration.coerceAtLeast(1)
                        isMediaPrepared = true
                    }

                    mp.setOnCompletionListener {
                        isPlayingMedia = false
                        mediaPositionMs = mediaDurationMs
                    }

                    mp.setOnErrorListener { _, _, _ ->
                        isPlayingMedia = false
                        isMediaPrepared = false
                        true
                    }

                    val sourceUri = item.contentUri ?: Uri.fromFile(File(item.path))
                    mp.setDataSource(context, sourceUri)
                    mediaPlayerRef.value = mp
                    mp.prepareAsync()
                }
            }.onFailure {
                isMediaPrepared = false
            }
        }

        onDispose {
            isPlayingMedia = false
            isMediaPrepared = false
            mediaPlayerRef.value = null

            player?.let { mp ->
                runCatching {
                    if (mp.isPlaying) mp.stop()
                }
                runCatching {
                    mp.release()
                }
            }
        }
    }

    LaunchedEffect(isPlayingMedia) {
        while (isPlayingMedia) {
            val mp = mediaPlayerRef.value
            if (mp != null && mp.isPlaying) {
                mediaPositionMs = mp.currentPosition
            } else {
                isPlayingMedia = false
            }
            delay(300L)
        }
    }

    LaunchedEffect(item.path) {
        when (item.category) {
            FileCategoryFilter.IMAGES, FileCategoryFilter.VIDEO, FileCategoryFilter.AUDIO -> {
                decodedBitmap = WearStorageEngine.loadThumbnailBitmap(context, item, targetPx = 280)
            }
            else -> {}
        }
        if (item.category == FileCategoryFilter.DOCS || item.extension in setOf("txt", "json", "md", "log", "xml", "csv", "kt", "py")) {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    val input = item.contentUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)
                    } ?: File(item.path).inputStream()

                    input.bufferedReader().use { reader ->
                        val buffer = CharArray(2500)
                        val count = reader.read(buffer)
                        if (count > 0) String(buffer, 0, count) else ""
                    }
                }.getOrDefault("Unable to read text content.")
            }
            textPreviewContent = content
        }
    }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onClose,
                label = "Close Preview",
                icon = Icons.Rounded.Close
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                start = 12.dp,
                end = 12.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.92f)
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "FILE PREVIEW",
                    badgeText = item.extension.uppercase(Locale.ROOT).ifBlank { "FILE" }
                )
            }

            item {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Image or Video Frame Preview
            decodedBitmap?.let { bmp ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Audio / Video Built-In Playback Controls
            if (item.category == FileCategoryFilter.AUDIO || item.category == FileCategoryFilter.VIDEO) {
                item {
                    Card(
                        onClick = {
                            if (isMediaPrepared) {
                                val mp = mediaPlayerRef.value

                                if (mp != null) {
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlayingMedia = false
                                    } else {
                                        mp.start()
                                        isPlayingMedia = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingMedia) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                                    contentDescription = "Play Pause Media",
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "${mediaPositionMs / 1000}s / ${mediaDurationMs / 1000}s",
                                    style = MaterialTheme.typography.labelMedium.tabularNums(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (mediaPositionMs.toFloat() / mediaDurationMs.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Text / JSON / Code / Log Content Viewer
            if (textPreviewContent != null) {
                item {
                    Card(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Text(
                            text = textPreviewContent ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // External Open / APK Installer & Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenInspector,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Details", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onSendFile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send File", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPREHENSIVE FILE INSPECTOR & ACTIONS MODAL (RENAME, COPY, MOVE, SHARE, MD5)
// =============================================================================
@Composable
private fun M3ComprehensiveFileInspectorModal(
    item: WearFileItem,
    selectedProtocol: TransferTransportProtocol,
    onOpenPreview: () -> Unit,
    onStartRename: () -> Unit,
    onCopyFile: () -> Unit,
    onCutMoveFile: () -> Unit,
    onEnqueueTransfer: (TransferTransportProtocol) -> Unit,
    onSystemShare: () -> Unit,
    onDeleteFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val columnState = rememberTransformingLazyColumnState()
    var md5Hash by remember(item.path) { mutableStateOf("Calculating...") }

    LaunchedEffect(item.path, item.contentUri) {
        md5Hash = if (item.isDirectory) {
            "Directory"
        } else {
            WearStorageEngine.computeMd5Checksum(context, item)
        }
    }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onDismiss,
                label = "Close Info",
                icon = Icons.Rounded.Close
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                start = 12.dp,
                end = 12.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.92f)
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "FILE INSPECTOR",
                    badgeText = item.permissionsBadge.uppercase(Locale.ROOT)
                )
            }

            // 1. FILE OPERATIONS ACTION GRID (Preview, Rename, Copy, Move)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!item.isDirectory) {
                        FilledTonalButton(
                            onClick = onOpenPreview,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Preview", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    FilledTonalButton(
                        onClick = onStartRename,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Rename", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCopyFile,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = onCutMoveFile,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Move / Cut", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 2. SHARE & SEND OPTIONS (DataLayer, Wi-Fi, Bluetooth, System Share)
            item {
                CatalogM3ListHeaderSample(title = "SEND TO CONNECTED DEVICE")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onEnqueueTransfer(TransferTransportProtocol.DATA_LAYER) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("DataLayer", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { onEnqueueTransfer(TransferTransportProtocol.WIFI) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Wi-Fi", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onEnqueueTransfer(TransferTransportProtocol.BLUETOOTH) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Bluetooth", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = onSystemShare,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("OS Share", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 3. COMPREHENSIVE FILE METADATA TABLE
            item {
                CatalogM3ListHeaderSample(title = "COMPREHENSIVE METADATA")
            }

            val metadataRows = listOf(
                "Name" to item.name,
                "Category" to item.category.label,
                "Size" to "${item.formattedSize} (${item.sizeBytes} B)",
                "MIME Type" to item.mimeType,
                "Modified" to item.formattedModifiedDate,
                "Created" to item.formattedCreatedDate,
                "Access" to "${item.permissionsBadge} · Hidden: ${if (item.isHidden) "Yes" else "No"}",
                "MD5 Hash" to md5Hash,
                "Full Path" to item.path
            )

            items(metadataRows.size) { idx ->
                val (key, value) = metadataRows[idx]
                Card(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
//                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(key.uppercase(Locale.ROOT), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item {
                Button(
                    onClick = onDeleteFile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete Permanently")
                }
            }
        }
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED PAGER INDICATOR
// =============================================================================
@Composable
private fun ViewportConstrainedFileExplorerConfirmation(
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
            .background(
                Color.Black.copy(alpha = 0.92f)
            ).clickable(onClick = onDismiss),
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


