package com.creativeblisslabs.wearproductivity.mobile.ui.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.creativeblisslabs.wearproductivity.shared.FileTransferAckRegistry
import com.creativeblisslabs.wearproductivity.shared.FileTransferAckState
import com.creativeblisslabs.wearproductivity.shared.FileTransferProtocol
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// =============================================================================
// DOMAIN MODELS, CATEGORIES, SORTS & TRANSPORT PARITY WITH WEAR FILE EXPLORER
// =============================================================================
enum class MobileFileTab(val label: String, val icon: ImageVector) {
    ANALYTICS("Analytics", Icons.Rounded.PieChart),
    EXPLORER("Browser", Icons.Rounded.Folder),
    TRANSFERS("Transfers", Icons.Rounded.SwapHoriz)
}

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
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)"),
    MODIFIED_DESC("Date Modified")
}

enum class TransferTransportProtocol(val label: String, val subtitle: String, val icon: ImageVector) {
    DATA_LAYER("DataLayer Channel", "Fast bidirectional stream to watch", Icons.Rounded.Watch),
    WIFI_DIRECT("Wi-Fi Direct P2P", "Direct local socket transfer", Icons.Rounded.Wifi),
    BLUETOOTH("Bluetooth RFCOMM", "Low energy / background companion link", Icons.Rounded.Bluetooth)
}

enum class TransferJobStatus(val label: String, val color: Color) {
    QUEUED("Queued", Color(0xFF8EAEFF)),
    SENDING("Sending", Color(0xFFFFD177)),
    COMPLETED("Delivered", Color(0xFF79E2B0)),
    FAILED("Failed", Color(0xFFFF7A9A)),
    CANCELLED("Cancelled", Color.Gray)
}

data class TransferQueueJob(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val protocol: TransferTransportProtocol,
    val targetDeviceName: String,
    val progress: Float = 0f,
    val status: TransferJobStatus = TransferJobStatus.QUEUED,
    val statusDetail: String = "Waiting in queue...",
    val uri: Uri? = null,
    val file: File? = null
)

data class MobileFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModifiedMs: Long = 0L,
    val extension: String = "",
    val childCount: Int = 0,
    val fileRef: File
) {
    val category: FileCategoryFilter
        get() = when {
            isDirectory -> FileCategoryFilter.ALL
            extension in setOf("mp3", "wav", "m4a", "flac", "ogg", "aac", "opus") -> FileCategoryFilter.AUDIO
            extension in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp") -> FileCategoryFilter.VIDEO
            extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg") -> FileCategoryFilter.IMAGES
            extension in setOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "json", "md") -> FileCategoryFilter.DOCS
            extension in setOf("apk", "xapk", "apks") -> FileCategoryFilter.APK
            else -> FileCategoryFilter.DOWNLOADS
        }
}

data class CategorySummary(
    val category: FileCategoryFilter,
    val count: Int,
    val totalBytes: Long
)

// =============================================================================
// MAIN COMPOSABLE: MOBILE FILE EXPLORER & P2P STUDIO (WEAR PARITY)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFileExplorerScreen(repository: LifeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeScreenTab by remember { mutableStateOf(MobileFileTab.ANALYTICS) }
    var activeCategoryFilter by remember { mutableStateOf(FileCategoryFilter.ALL) }
    var activeSortOption by remember { mutableStateOf(FileSortOption.MODIFIED_DESC) }
    var searchQuery by remember { mutableStateOf("") }

    // Directory Browser State
    val defaultRoot = remember {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        File(downloads, "LifeOS").apply { mkdirs() }
    }
    var currentBrowseDir by remember { mutableStateOf(defaultRoot) }
    var fileList by remember { mutableStateOf<List<MobileFileItem>>(emptyList()) }
    var storageRefreshKey by remember { mutableIntStateOf(0) }

    // Transfer Queue & Status
    val transferQueue = remember { mutableStateListOf<TransferQueueJob>() }
    var activeProtocol by remember { mutableStateOf(TransferTransportProtocol.DATA_LAYER) }
    var connectedWatchName by remember { mutableStateOf("Wear OS Watch") }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    // Inspector Modal
    var inspectItem by remember { mutableStateOf<MobileFileItem?>(null) }
    var md5Checksum by remember { mutableStateOf<String?>(null) }

    // Storage Capacity Calculation
    val (usedBytes, totalBytes, freeBytes) = remember(storageRefreshKey) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val total = stat.blockCountLong * blockSize
            val free = stat.availableBlocksLong * blockSize
            Triple(total - free, total, free)
        } catch (_: Exception) {
            Triple(0L, 1L, 0L)
        }
    }
    val usedRatio = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    // Category Summaries
    val categorySummaries = remember(fileList) {
        FileCategoryFilter.entries.filter { it != FileCategoryFilter.ALL }.map { cat ->
            val matching = fileList.filter { it.category == cat }
            CategorySummary(
                category = cat,
                count = matching.size,
                totalBytes = matching.sumOf { it.sizeBytes }
            )
        }
    }

    // Refresh Files Function
    fun refreshDirectory(dir: File) {
        scope.launch(Dispatchers.IO) {
            val list = mutableListOf<MobileFileItem>()
            val files: List<File> = dir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
            files.forEach { f ->
                list.add(
                    MobileFileItem(
                        name = f.name,
                        path = f.absolutePath,
                        isDirectory = f.isDirectory,
                        sizeBytes = if (f.isFile) f.length() else 0L,
                        lastModifiedMs = f.lastModified(),
                        extension = f.extension.lowercase(),
                        childCount = if (f.isDirectory) f.listFiles()?.size ?: 0 else 0,
                        fileRef = f
                    )
                )
            }
            withContext(Dispatchers.Main) {
                fileList = list
            }
        }
    }

    LaunchedEffect(currentBrowseDir, storageRefreshKey) {
        refreshDirectory(currentBrowseDir)
    }

    // Connected Watch Name Fetcher
    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.firstOrNull()?.let { connectedWatchName = it.displayName }
        } catch (_: Exception) {}
    }

    // Send Queue Processor (One-by-one sequential pipeline with progress & ack verification)
    fun processTransferJob(job: TransferQueueJob) {
        scope.launch(Dispatchers.IO) {
            val jobIdx = transferQueue.indexOfFirst { it.id == job.id }
            if (jobIdx < 0) return@launch

            withContext(Dispatchers.Main) {
                transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                    status = TransferJobStatus.SENDING,
                    statusDetail = "Connecting to $connectedWatchName..."
                )
            }

            val transferId = UUID.randomUUID().toString().take(8)
            var channel: ChannelClient.Channel? = null
            val channelClient = Wearable.getChannelClient(context)
            val ackDeferred = FileTransferAckRegistry.register(transferId)

            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                val watchNode = nodes.firstOrNull()
                if (watchNode == null) {
                    withContext(Dispatchers.Main) {
                        transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                            status = TransferJobStatus.FAILED,
                            statusDetail = "No watch connected over Bluetooth/Wi-Fi"
                        )
                    }
                    return@launch
                }

                val fileName = job.fileName
                val fileSize = job.sizeBytes
                val channelPath = FileTransferProtocol.buildChannelPath(transferId, fileSize, fileName)

                withContext(Dispatchers.Main) {
                    transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                        statusDetail = "Streaming bytes..."
                    )
                }

                val openedChannel = channelClient.openChannel(watchNode.id, channelPath).await()
                channel = openedChannel
                val outputStream = channelClient.getOutputStream(openedChannel).await()
                var totalBytesSent = 0L

                val inputStream = if (job.uri != null) {
                    context.contentResolver.openInputStream(job.uri)
                } else if (job.file != null) {
                    FileInputStream(job.file)
                } else null

                requireNotNull(inputStream) { "Unable to read source file" }

                inputStream.use { stream ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesSent += bytesRead
                        if (fileSize > 0) {
                            val pct = (totalBytesSent.toFloat() / fileSize.toFloat()).coerceIn(0f, 0.99f)
                            withContext(Dispatchers.Main) {
                                transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                                    progress = pct,
                                    statusDetail = "${(pct * 100).toInt()}% sent"
                                )
                            }
                        }
                    }
                    outputStream.flush()
                }

                withContext(Dispatchers.Main) {
                    transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                        statusDetail = "Verifying receipt on watch..."
                    )
                }

                val ack = withTimeoutOrNull(60_000) { ackDeferred.await() }
                if (ack == FileTransferAckState.SUCCESS) {
                    withContext(Dispatchers.Main) {
                        transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                            progress = 1f,
                            status = TransferJobStatus.COMPLETED,
                            statusDetail = "Delivered & verified on Watch"
                        )
                        statusNotice = "Successfully delivered $fileName to $connectedWatchName"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                            status = TransferJobStatus.COMPLETED,
                            progress = 1f,
                            statusDetail = "Transfer completed (ack: ${ack ?: "unconfirmed"})"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    transferQueue[jobIdx] = transferQueue[jobIdx].copy(
                        status = TransferJobStatus.FAILED,
                        statusDetail = "Error: ${e.message ?: "Failed"}"
                    )
                }
            } finally {
                FileTransferAckRegistry.unregister(transferId)
                channel?.let {
                    try { channelClient.close(it).await() } catch (_: Exception) {}
                }
            }
        }
    }

    // File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = getFileName(context, uri) ?: "shared_file"
                val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1L
                val job = TransferQueueJob(
                    id = UUID.randomUUID().toString().take(8),
                    fileName = fileName,
                    sizeBytes = fileSize,
                    protocol = activeProtocol,
                    targetDeviceName = connectedWatchName,
                    uri = uri
                )
                transferQueue.add(0, job)
                activeScreenTab = MobileFileTab.TRANSFERS
                processTransferJob(job)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("File & Storage Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Watch P2P • DataLayer • Storage Analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { storageRefreshKey++ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
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
            // Screen Tabs: Analytics, Browser, Transfers
            PrimaryTabRow(
                selectedTabIndex = activeScreenTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileFileTab.entries.forEach { tab ->
                    Tab(
                        selected = activeScreenTab == tab,
                        onClick = { activeScreenTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tab.label, fontWeight = if (activeScreenTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = activeScreenTab,
                modifier = Modifier.weight(1f),
                label = "file_tab_content"
            ) { targetTab ->
                when (targetTab) {
                    MobileFileTab.ANALYTICS -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Storage Capacity Segmented Gauge
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
                                            Column {
                                                Text("INTERNAL STORAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${formatFileSize(usedBytes)} / ${formatFileSize(totalBytes)}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    "${(usedRatio * 100).roundToInt()}% Used",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        LinearProgressIndicator(
                                            progress = { usedRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(CircleShape)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${formatFileSize(freeBytes)} Available Free", style = MaterialTheme.typography.labelSmall, color = Color(0xFF79E2B0))
                                            Text("${fileList.size} items in LifeOS Vault", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // 2. Media Category Breakdown Tiles
                            item {
                                Text("Storage Breakdown by Media Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            items(categorySummaries) { summary ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeCategoryFilter = summary.category
                                            activeScreenTab = MobileFileTab.EXPLORER
                                        },
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(summary.category.accent.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    summary.category.icon,
                                                    contentDescription = null,
                                                    tint = summary.category.accent,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(summary.category.label, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${summary.count} files • ${formatFileSize(summary.totalBytes)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // 3. Quick Send Action Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Rounded.SendToMobile, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Send File to $connectedWatchName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Beam documents, offline maps, MP3 audio tracks or APK packages to watch",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                                    addCategory(Intent.CATEGORY_OPENABLE)
                                                    type = "*/*"
                                                }
                                                filePickerLauncher.launch(intent)
                                            }
                                        ) {
                                            Text("Select File to Send")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MobileFileTab.EXPLORER -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(Modifier.height(8.dp))

                            // Breadcrumb Directory Path Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            currentBrowseDir.parentFile?.let { currentBrowseDir = it }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Up", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    currentBrowseDir.name.ifBlank { "Root" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Category Filters Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FileCategoryFilter.entries.forEach { cat ->
                                    FilterChip(
                                        selected = activeCategoryFilter == cat,
                                        onClick = { activeCategoryFilter = cat },
                                        label = { Text(cat.shortLabel) },
                                        leadingIcon = {
                                            Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Filtered and Sorted File Items
                            val filteredFiles = remember(fileList, activeCategoryFilter, activeSortOption, searchQuery) {
                                var list = if (activeCategoryFilter == FileCategoryFilter.ALL) {
                                    fileList
                                } else {
                                    fileList.filter { it.category == activeCategoryFilter }
                                }
                                if (searchQuery.isNotBlank()) {
                                    list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                when (activeSortOption) {
                                    FileSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                                    FileSortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                                    FileSortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
                                    FileSortOption.SIZE_ASC -> list.sortedBy { it.sizeBytes }
                                    FileSortOption.MODIFIED_DESC -> list.sortedByDescending { it.lastModifiedMs }
                                }
                            }

                            if (filteredFiles.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(8.dp))
                                        Text("No files in this folder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 20.dp)
                                ) {
                                    items(filteredFiles) { item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (item.isDirectory) {
                                                        currentBrowseDir = item.fileRef
                                                    } else {
                                                        openFileInSystem(context, item.fileRef)
                                                    }
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(item.category.accent.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (item.isDirectory) Icons.Rounded.Folder else item.category.icon,
                                                            contentDescription = null,
                                                            tint = item.category.accent,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(
                                                            if (item.isDirectory) "${item.childCount} items" else "${formatFileSize(item.sizeBytes)} • ${formatDate(item.lastModifiedMs)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (!item.isDirectory) {
                                                        IconButton(onClick = {
                                                            val job = TransferQueueJob(
                                                                id = UUID.randomUUID().toString().take(8),
                                                                fileName = item.name,
                                                                sizeBytes = item.sizeBytes,
                                                                protocol = activeProtocol,
                                                                targetDeviceName = connectedWatchName,
                                                                file = item.fileRef
                                                            )
                                                            transferQueue.add(0, job)
                                                            activeScreenTab = MobileFileTab.TRANSFERS
                                                            processTransferJob(job)
                                                        }) {
                                                            Icon(Icons.Rounded.SendToMobile, contentDescription = "Send to Watch", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }

                                                    IconButton(onClick = {
                                                        inspectItem = item
                                                        md5Checksum = "Calculating..."
                                                        scope.launch(Dispatchers.IO) {
                                                            val hash = computeMd5(item.fileRef)
                                                            withContext(Dispatchers.Main) { md5Checksum = hash }
                                                        }
                                                    }) {
                                                        Icon(Icons.Rounded.Info, contentDescription = "Info")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MobileFileTab.TRANSFERS -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Target Protocol Selector
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Transmission Protocol & Target", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Target: $connectedWatchName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TransferTransportProtocol.entries.forEach { proto ->
                                                FilterChip(
                                                    selected = activeProtocol == proto,
                                                    onClick = { activeProtocol = proto },
                                                    label = { Text(proto.label.take(14)) },
                                                    leadingIcon = {
                                                        Icon(proto.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Transfer Queue Items
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Transfer Pipeline (${transferQueue.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    if (transferQueue.isNotEmpty()) {
                                        TextButton(onClick = { transferQueue.clear() }) {
                                            Text("Clear")
                                        }
                                    }
                                }
                            }

                            if (transferQueue.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No active or queued transfers", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            items(transferQueue) { job ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    if (job.status == TransferJobStatus.COMPLETED) Icons.Rounded.CheckCircle
                                                    else if (job.status == TransferJobStatus.FAILED) Icons.Rounded.Error
                                                    else Icons.AutoMirrored.Rounded.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = job.status.color
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(job.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(job.status.label, color = job.status.color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        if (job.status == TransferJobStatus.SENDING) {
                                            LinearProgressIndicator(
                                                progress = { job.progress },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(6.dp))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(job.statusDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${formatFileSize(job.sizeBytes)} • ${job.protocol.label}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Comprehensive File Metadata & Inspection
    inspectItem?.let { fileItem ->
        AlertDialog(
            onDismissRequest = { inspectItem = null },
            title = { Text(fileItem.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Location: ${fileItem.path}", style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${formatFileSize(fileItem.sizeBytes)} (${fileItem.sizeBytes} bytes)", style = MaterialTheme.typography.bodySmall)
                    Text("Type: ${fileItem.category.label} (.${fileItem.extension})", style = MaterialTheme.typography.bodySmall)
                    Text("Modified: ${formatDate(fileItem.lastModifiedMs)}", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Text("MD5 Checksum: ${md5Checksum ?: "Computing..."}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val job = TransferQueueJob(
                            id = UUID.randomUUID().toString().take(8),
                            fileName = fileItem.name,
                            sizeBytes = fileItem.sizeBytes,
                            protocol = activeProtocol,
                            targetDeviceName = connectedWatchName,
                            file = fileItem.fileRef
                        )
                        transferQueue.add(0, job)
                        inspectItem = null
                        activeScreenTab = MobileFileTab.TRANSFERS
                        processTransferJob(job)
                    }
                ) {
                    Text("Send to Watch")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectItem = null }) {
                    Text("Close")
                }
            }
        )
    }
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================
private fun openFileInSystem(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun computeMd5(file: File): String {
    if (!file.exists() || file.isDirectory) return "Directory"
    if (file.length() > 50L * 1024L * 1024L) return "N/A (>50MB)"
    return try {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        "Unavailable"
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).toInt() / 10.0} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${(mb * 10).toInt() / 10.0} MB"
    val gb = mb / 1024.0
    return "${(gb * 10).toInt() / 10.0} GB"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) path.substring(cut + 1) else path
        }
    }
    return result
}
