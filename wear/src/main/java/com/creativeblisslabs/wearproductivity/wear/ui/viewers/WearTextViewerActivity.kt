package com.creativeblisslabs.wearproductivity.wear.ui.viewers

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import java.io.File

class WearTextViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra("EXTRA_FILE_PATH")
        if (filePath.isNullOrBlank()) {
            Toast.makeText(this, "No text file path provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(this, "Cannot read file: ${file.name}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val textContent = try {
            // Read up to 256KB to keep memory safe on Wear OS
            val bytes = file.inputStream().use { it.readNBytes(256 * 1024) }
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }

        setContent {
            AppScaffold {
                TextViewerContent(
                    fileName = file.name,
                    content = textContent,
                    fileSizeBytes = file.length(),
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun TextViewerContent(
    fileName: String,
    content: String,
    fileSizeBytes: Long,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fileName.take(12),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
            }
        }

        Text(
            text = "${(fileSizeBytes / 1024).coerceAtLeast(1)} KB",
            color = Color.Gray,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = content,
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

