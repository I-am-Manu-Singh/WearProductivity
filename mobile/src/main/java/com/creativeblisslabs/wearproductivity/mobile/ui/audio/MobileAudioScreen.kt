package com.creativeblisslabs.wearproductivity.mobile.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AudioTrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationLabel: String,
    val isBinaural: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileAudioScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var currentTrack by remember { mutableStateOf("Deep Focus Binaural 432Hz") }
    var currentArtist by remember { mutableStateOf("LifeOS Acoustic Studio") }
    var currentProgress by remember { mutableFloatStateOf(0.35f) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    val presetTracks = remember {
        listOf(
            AudioTrackItem("1", "Deep Focus Binaural 432Hz", "LifeOS Studio", "45:00", true),
            AudioTrackItem("2", "Ambient Rain & Theta Waves", "Nature Soundscapes", "60:00", true),
            AudioTrackItem("3", "Workout High Energy BPM", "Cardio Beats", "32:15"),
            AudioTrackItem("4", "Evening Wind Down & Alpha", "Mindfulness Lab", "25:00", true),
            AudioTrackItem("5", "Pure White Noise Sound Machine", "Focus Generator", "90:00")
        )
    }

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
                    statusNotice = "Sent '$action' to Watch"
                } ?: run {
                    statusNotice = "No Watch Connected"
                }
            } catch (_: Exception) {
                statusNotice = "Failed to reach Watch"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio & Sound Studio", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Now Playing Main Player
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(currentTrack, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(currentArtist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)

                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = currentProgress,
                            onValueChange = { currentProgress = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("12:15", style = MaterialTheme.typography.labelSmall)
                            Text("45:00", style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // Media Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { sendWearCommand("previous") }) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                            }
                            FilledIconButton(
                                onClick = {
                                    isPlaying = !isPlaying
                                    sendWearCommand("play_pause")
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(onClick = { sendWearCommand("next") }) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                            }
                        }

                        statusNotice?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 2. Watch Route / Broadcast Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Wear OS Audio Streaming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Route music and ambient focus audio to watch speaker or Bluetooth earphones", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { sendWearCommand("route_watch") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play on Watch")
                            }
                            FilledTonalButton(
                                onClick = { sendWearCommand("route_phone") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play on Phone")
                            }
                        }
                    }
                }
            }

            // 3. Audio Playlist & Ambient Tracks
            item {
                Text("Soundscapes & Playlists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(presetTracks) { track ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (track.isBinaural) Color(0xFF6750A4).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (track.isBinaural) Icons.Rounded.Headphones else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = if (track.isBinaural) Color(0xFF6750A4) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("${track.artist} · ${track.durationLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(onClick = {
                            currentTrack = track.title
                            currentArtist = track.artist
                            isPlaying = true
                            sendWearCommand("play")
                        }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                        }
                    }
                }
            }
        }
    }
}
