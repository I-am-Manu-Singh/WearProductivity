package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDashboardScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val zoneId = remember { ZoneId.systemDefault() }
    val todayStart = remember(today) { today.atStartOfDay(zoneId).toInstant().toEpochMilli() }
    val tomorrowStart = remember(today) { today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() }
    val nowMs = System.currentTimeMillis()

    // Filter date-sensitive records
    val activeTasks = remember(state.tasks) { state.tasks.filter { it.stamp.deletedAt == null } }
    val activeHabits = remember(state.habits) { state.habits.filter { it.stamp.deletedAt == null } }
    val activeEvents = remember(state.events) { state.events.filter { it.stamp.deletedAt == null } }
    val upcomingEvents = remember(activeEvents, nowMs) {
        activeEvents.filter { (it.scheduledAtEpochMs ?: 0L) >= nowMs }.sortedBy { it.scheduledAtEpochMs }
    }
    val todayExpenses = remember(state.expenses, todayStart, tomorrowStart) {
        state.expenses.filter { it.stamp.deletedAt == null && it.occurredAtEpochMs in todayStart until tomorrowStart }
    }
    val todayExpenseSum = remember(todayExpenses) { todayExpenses.sumOf { it.amount.coerceAtLeast(0.0) }.toInt() }

    val todayFoodLogs = remember(state.foodLogs, todayStart, tomorrowStart) {
        state.foodLogs.filter { it.stamp.deletedAt == null && it.loggedAtEpochMs in todayStart until tomorrowStart }
    }
    val todayCalories = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.calories.coerceAtLeast(0) } }
    val todayProtein = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.proteinGrams.coerceAtLeast(0.0) }.toInt() }

    val doneTasks = activeTasks.count { it.complete }
    val doneHabits = activeHabits.count { it.doneToday }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "LifeOS Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // 0. Connected Wear OS Watch Device Hub
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                var watchNodeName by remember { mutableStateOf<String?>("Checking Wear OS...") }
                var watchNodeId by remember { mutableStateOf<String?>(null) }
                var lastPingStatus by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    try {
                        val nodes = com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes.await()
                        val primaryNode = nodes.firstOrNull()
                        if (primaryNode != null) {
                            watchNodeName = primaryNode.displayName
                            watchNodeId = primaryNode.id
                        } else {
                            watchNodeName = "No Wear OS Watch Connected"
                        }
                    } catch (_: Exception) {
                        watchNodeName = "Wear OS Standalone"
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Watch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        watchNodeName ?: "Wear OS Watch",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (watchNodeId != null) "Connected & Syncing · Heart Rate: ${state.wellness.currentHeartRateBpm.toInt().takeIf { it > 0 }?.let { "$it bpm" } ?: "Active"} · Steps: ${state.wellness.steps}" else "Paired device standby",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (watchNodeId != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                com.google.android.gms.wearable.Wearable.getMessageClient(context).sendMessage(
                                                    watchNodeId!!,
                                                    "/lifeos/ping",
                                                    "ping".toByteArray(Charsets.UTF_8)
                                                ).await()
                                                lastPingStatus = "Watch Pinged!"
                                            } catch (_: Exception) {
                                                lastPingStatus = "Ping Failed"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ping Watch", style = MaterialTheme.typography.labelMedium)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                // Sync State Packet to Watch
                                                val packet = com.creativeblisslabs.wearproductivity.shared.SyncPacket(
                                                    deviceId = "phone-companion",
                                                    state = state
                                                )
                                                val jsonStr = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.encodeToString(
                                                    com.creativeblisslabs.wearproductivity.shared.SyncPacket.serializer(),
                                                    packet
                                                )
                                                val putDataReq = com.google.android.gms.wearable.PutDataMapRequest.create(
                                                    com.creativeblisslabs.wearproductivity.shared.SyncPaths.LIFE_STATE
                                                ).apply {
                                                    dataMap.putString("json", jsonStr)
                                                    dataMap.putLong("timestamp", System.currentTimeMillis())
                                                }.asPutDataRequest().setUrgent()
                                                com.google.android.gms.wearable.Wearable.getDataClient(context).putDataItem(putDataReq).await()
                                                lastPingStatus = "Synced to Watch!"
                                            } catch (_: Exception) {
                                                lastPingStatus = "Sync Failed"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Push Sync", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (lastPingStatus != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    lastPingStatus!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Focus Flow Quick Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.focus.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SelfImprovement,
                                    contentDescription = null,
                                    tint = if (state.focus.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (state.focus.active) "Focus Session Active" else "Focus & Deep Work",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (state.focus.active) {
                                    val leftSec = state.focus.remainingSeconds
                                    val min = leftSec / 60
                                    val sec = leftSec % 60
                                    val subjectName = state.focus.subject.ifBlank { "Deep Work" }
                                    "${String.format("%02d:%02d", min, sec)} remaining · $subjectName"
                                } else {
                                    "${state.focus.minutes} min session ready"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    if (state.focus.active) {
                                        repository.togglePauseFocusSession()
                                    } else {
                                        repository.startFocusSession(state.focus.minutes, state.focus.type, state.focus.subject)
                                    }
                                }
                            }
                        ) {
                            Text(if (state.focus.active) (if (state.focus.isPaused) "Resume" else "Pause") else "Start")
                        }
                    }
                }
            }

            // 2. Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Tasks",
                        value = "$doneTasks/${activeTasks.size}",
                        caption = if (activeTasks.isEmpty()) "No tasks" else "${activeTasks.size - doneTasks} left",
                        icon = Icons.Rounded.CheckCircle,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Habits",
                        value = "$doneHabits/${activeHabits.size}",
                        caption = if (activeHabits.isEmpty()) "None" else "${doneHabits * 100 / activeHabits.size.coerceAtLeast(1)}% done",
                        icon = Icons.Rounded.Repeat,
                        color = Color(0xFF386A20),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Daily Diet",
                        value = "$todayCalories kcal",
                        caption = "$todayProtein g protein",
                        icon = Icons.Rounded.Restaurant,
                        color = Color(0xFFBA1A1A),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Expenses",
                        value = "₹$todayExpenseSum",
                        caption = "${todayExpenses.size} logged today",
                        icon = Icons.Rounded.Payments,
                        color = Color(0xFF984061),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Wellness & Water Tracker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF006688))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hydration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "${state.wellness.waterGlasses * 250} ml / ${state.wellness.targetWaterGlasses * 250} ml",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006688)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = {
                                (state.wellness.waterGlasses.toFloat() / state.wellness.targetWaterGlasses.coerceAtLeast(1)).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF006688),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.setWaterGlasses((state.wellness.waterGlasses - 1).coerceAtLeast(0))
                                    }
                                }
                            ) {
                                Text("-1 Glass")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        repository.setWaterGlasses(state.wellness.waterGlasses + 1)
                                    }
                                }
                            ) {
                                Text("+1 Glass")
                            }
                        }
                    }
                }
            }

            // 4. Upcoming Agenda
            item {
                Text(
                    "Upcoming Agenda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (upcomingEvents.isEmpty()) {
                item {
                    Text(
                        "No upcoming events scheduled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(upcomingEvents.take(4).size) { index ->
                    val event = upcomingEvents[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                event.location?.takeIf { it.isNotBlank() }?.let { loc ->
                                    Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
