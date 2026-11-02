package com.creativeblisslabs.wearproductivity.mobile.ui.focus

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.FocusType
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFocusScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val focus = state.focus

    var selectedDuration by remember { mutableIntStateOf(focus.minutes.coerceAtLeast(25)) }
    var selectedType by remember { mutableStateOf(FocusType.Pomodoro) }
    var customTopic by remember { mutableStateOf("") }
    var showPresetDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val remainingSec = focus.remainingSeconds
    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val progress = if (focus.totalSeconds > 0) {
        ((focus.totalSeconds - focus.remainingSeconds).toFloat() / focus.totalSeconds).coerceIn(0f, 1f)
    } else 0f

    val activeHistory = remember(state.focusHistory) {
        state.focusHistory.filter { it.stamp.deletedAt == null }.sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deep Work & Focus", fontWeight = FontWeight.Bold) },
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
            // 1. Primary Timer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (focus.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.SelfImprovement, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (focus.active) (focus.subject.ifBlank { focus.type.name }) else "Pomodoro Timer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!focus.active) {
                                TextButton(onClick = { showPresetDialog = true }) {
                                    Text("Modes")
                                }
                            }
                        }

                        // Big Clock Readout
                        Text(
                            text = if (focus.active) String.format("%02d:%02d", mins, secs) else "${selectedDuration}:00",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            fontWeight = FontWeight.Black,
                            color = if (focus.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )

                        LinearProgressIndicator(
                            progress = { if (focus.active) progress else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Text(
                            text = if (focus.active) {
                                if (focus.isPaused) "Paused" else "In Progress"
                            } else "Target: $selectedDuration minutes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Transport Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (focus.active) {
                                FilledTonalButton(
                                    onClick = { scope.launch { repository.togglePauseFocusSession() } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (focus.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (focus.isPaused) "Resume" else "Pause")
                                }
                                Button(
                                    onClick = { scope.launch { repository.stopFocusSession() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stop")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val topic = customTopic.ifBlank { "Deep Work" }
                                            repository.startFocusSession(
                                                minutes = selectedDuration,
                                                type = selectedType,
                                                subject = topic
                                            )
                                            val triggerEpochMs = System.currentTimeMillis() + (selectedDuration * 60_000L)
                                            com.creativeblisslabs.wearproductivity.shared.alarms.SystemAlarmScheduler.scheduleAlarm(
                                                context = context,
                                                triggerAtMillis = triggerEpochMs,
                                                receiverClass = com.creativeblisslabs.wearproductivity.mobile.platform.MobileReminderReceiver::class.java,
                                                alarmId = 9902,
                                                title = "Focus Complete!",
                                                message = "$topic (${selectedDuration}m) completed. Time for a break!"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Focus Session")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Duration Steppers (when idle)
            if (!focus.active) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Session Length", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 25, 45, 60, 90).forEach { dur ->
                                    val isSel = selectedDuration == dur
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedDuration = dur },
                                        label = { Text("${dur}m") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Focus Session History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Past Sessions (${activeHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (activeHistory.isEmpty()) {
                item {
                    Text(
                        "No completed focus sessions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(activeHistory.take(15)) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(session.subject.ifBlank { "Deep Work" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
                                Text(
                                    if (session.completed) "Completed · $dateStr" else "Ended · $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${session.minutesFocused} min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Select Focus Type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        FocusType.Pomodoro to 25,
                        FocusType.DeepWork to 50,
                        FocusType.ShortBreak to 5,
                        FocusType.LongBreak to 15,
                        FocusType.Custom to 30
                    ).forEach { (type, defMins) ->
                        TextButton(
                            onClick = {
                                selectedType = type
                                selectedDuration = defMins
                                showPresetDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(type.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${defMins}m", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPresetDialog = false }) { Text("Close") } }
        )
    }
}
