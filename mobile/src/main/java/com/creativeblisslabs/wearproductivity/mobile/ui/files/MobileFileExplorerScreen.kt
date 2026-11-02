package com.creativeblisslabs.wearproductivity.mobile.ui.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.util.UUID

@Composable
fun MobileFileExplorerScreen(repository: LifeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var sendProgress by remember { mutableFloatStateOf(0f) }

    var receivedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    fun refreshReceivedFiles() {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LifeOS")
        if (dir.exists()) {
            receivedFiles = dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            receivedFiles = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        refreshReceivedFiles()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                isSending = true
                sendProgress = 0.05f
                statusMessage = "Connecting to watch..."
                scope.launch(Dispatchers.IO) {
                    val transferId = UUID.randomUUID().toString().take(8)
                    var channel: ChannelClient.Channel? = null
                    val channelClient = Wearable.getChannelClient(context)
                    val ackDeferred = FileTransferAckRegistry.register(transferId)

                    try {
                        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                        val watchNode = nodes.firstOrNull()
                        if (watchNode != null) {
                            val fileName = getFileName(context, uri) ?: "shared_file"
                            val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1L
                            val channelPath = FileTransferProtocol.buildChannelPath(transferId, fileSize, fileName)

                            statusMessage = "Opening link to ${watchNode.displayName}..."
                            val openedChannel = channelClient.openChannel(watchNode.id, channelPath).await()
                            channel = openedChannel

                            val outputStream = channelClient.getOutputStream(openedChannel).await()
                            var totalBytesSent = 0L

                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val buffer = ByteArray(32768)
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytesSent += bytesRead
                                    if (fileSize > 0) {
                                        val pct = (totalBytesSent.toFloat() / fileSize.toFloat()).coerceIn(0f, 0.99f)
                                        sendProgress = pct
                                        statusMessage = "Sending $fileName (${(pct * 100).toInt()}%)"
                                    }
                                }
                                outputStream.flush()
                            }

                            statusMessage = "Verifying receipt on watch..."
                            // Await two-way verification from watch before closing
                            val ack = withTimeoutOrNull(60_000) { ackDeferred.await() }
                            if (ack == FileTransferAckState.SUCCESS) {
                                sendProgress = 1f
                                statusMessage = "Delivered & verified on watch: $fileName (Saved in Downloads/LifeOS)"
                            } else {
                                statusMessage = "Transfer complete, acknowledged with: ${ack ?: "unconfirmed"}"
                            }
                        } else {
                            statusMessage = "No paired watch found over Bluetooth / Wi-Fi"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        statusMessage = "Transfer failed: ${e.message}"
                    } finally {
                        FileTransferAckRegistry.unregister(transferId)
                        channel?.let {
                            try { channelClient.close(it).await() } catch (_: Exception) {}
                        }
                        isSending = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FILE TRANSFER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Send Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Send Files to Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Stream documents, audio, or PDFs directly to your watch over high-speed Bluetooth / Wi-Fi LAN.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                ExtendedFloatingActionButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        filePickerLauncher.launch(intent)
                    },
                    icon = { Icon(Icons.Default.SendToMobile, contentDescription = null) },
                    text = { Text("Select & Send File") },
                    expanded = !isSending
                )

                if (isSending) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { sendProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                statusMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Received from Watch Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Received from Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { refreshReceivedFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                Text(
                    "Files sent from watch are saved in Downloads/LifeOS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (receivedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No files received from watch yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(receivedFiles) { file ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openReceivedFile(context, file) },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(
                                            "${formatFileSize(file.length())} • ${formatDate(file.lastModified())}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

private fun openReceivedFile(context: Context, file: File) {
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

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).toInt() / 10.0} KB"
    val mb = kb / 1024.0
    return "${(mb * 10).toInt() / 10.0} MB"
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
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
