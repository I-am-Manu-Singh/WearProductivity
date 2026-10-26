package com.creativeblisslabs.wearproductivity.mobile.ui.files

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MobileFileExplorerScreen(repository: LifeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                isSending = true
                statusMessage = "Sending file to watch..."
                scope.launch(Dispatchers.IO) {
                    try {
                        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                        val watchNode = nodes.firstOrNull()
                        if (watchNode != null) {
                            val fileName = getFileName(context, uri) ?: "shared_file"
                            val channelClient = Wearable.getChannelClient(context)
                            val channel = channelClient.openChannel(watchNode.id, "/file_transfer/$fileName").await()
                            val outputStream = channelClient.getOutputStream(channel).await()

                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }
                            }
                            outputStream.flush()
                            outputStream.close()
                            channelClient.close(channel)
                            statusMessage = "File sent successfully to /sdcard/Download/$fileName"
                        } else {
                            statusMessage = "No paired watch found"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        statusMessage = "Failed to send file"
                    } finally {
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Send Files to Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select an audio file or document from your phone storage to stream directly to the watch's local storage.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

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
                    CircularProgressIndicator()
                }

                statusMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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
