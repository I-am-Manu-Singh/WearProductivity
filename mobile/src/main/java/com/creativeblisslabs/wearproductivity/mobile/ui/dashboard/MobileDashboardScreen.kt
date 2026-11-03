package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.RoutineTask
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Complication Slots matching WearDashboardScreen
private enum class MobileComplicationSlot(val key: String, val label: String) {
    TOP("face_top_complication", "Top Slot"),
    UPPER_LEFT("face_upper_left_complication", "Upper Left"),
    UPPER_RIGHT("face_upper_right_complication", "Upper Right"),
    BOTTOM_LEFT("face_bottom_left_complication", "Bottom Left"),
    BOTTOM_RIGHT("face_bottom_right_complication", "Bottom Right"),
    BOTTOM("face_bottom_complication", "Bottom Slot"),
    RING("face_ring_complication", "Outer Ring")
}

// Complication Data Types matching WearDashboardScreen
private enum class MobileComplicationDataType(
    val id: String,
    val shortLabel: String,
    val fullName: String,
    val icon: ImageVector,
    val accent: Color
) {
    FAST("FAST", "FAST", "Intermittent Fasting", Icons.Rounded.Restaurant, Color(0xFFFF9A7A)),
    WATER("WATER", "H2O", "Hydration Goal", Icons.Rounded.WaterDrop, Color(0xFF68B6FF)),
    HABITS("HABITS", "HAB", "Daily Habits", Icons.Rounded.AutoAwesome, Color(0xFF79E2B0)),
    TASKS("TASKS", "TSK", "Open Tasks", Icons.Rounded.TaskAlt, Color(0xFF8EAEFF)),
    FOCUS("FOCUS", "FOC", "Focus Pomodoro", Icons.Rounded.Timer, Color(0xFFBBA5FF)),
    HEART("HEART", "BPM", "Heart Rate", Icons.Rounded.Favorite, Color(0xFFFF7A9A)),
    STEPS("STEPS", "STP", "Daily Steps", Icons.Rounded.DirectionsWalk, Color(0xFF79E2B0)),
    SLEEP("SLEEP", "SLP", "Sleep Recovery", Icons.Rounded.Bedtime, Color(0xFFA6B8FF)),
    CALORIES("CALORIES", "KCAL", "Calories Burned", Icons.Rounded.LocalFireDepartment, Color(0xFFFFB76B)),
    EXPENSES("EXPENSES", "EXP", "Expenses", Icons.Rounded.AccountBalanceWallet, Color(0xFFFFD177)),
    ALARM("ALARM", "ALM", "Next Alarm", Icons.Rounded.Alarm, Color(0xFFFFD177)),
    EVENTS("EVENTS", "EVT", "Calendar Events", Icons.Rounded.Event, Color(0xFF8EAEFF)),
    ROUTINES("ROUTINES", "RTN", "Daily Routines", Icons.Rounded.Schedule, Color(0xFFBBA5FF)),
    RUNS("RUNS", "RUN", "Run Workouts", Icons.AutoMirrored.Rounded.DirectionsRun, Color(0xFF79E2B0)),
    BATTERY("BATTERY", "BAT", "Watch Battery", Icons.Rounded.BatteryChargingFull, Color(0xFF79E2B0)),
    WEATHER("WEATHER", "WTH", "Local Weather", Icons.Rounded.WbSunny, Color(0xFFFFD177)),
    DATE("DATE", "DAY", "Current Date", Icons.Rounded.CalendarToday, Color(0xFF8EAEFF)),
    NONE("NONE", "OFF", "Hidden / None", Icons.Rounded.Close, Color.Gray);

    companion object {
        fun fromId(id: String): MobileComplicationDataType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TASKS
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDashboardScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    // Active Routines & Workouts
    val activeRoutines = remember(state.routines) {
        state.routines.filter { it.stamp.deletedAt == null }.sortedBy { it.orderIndex }
    }
    val doneRoutines = activeRoutines.count { it.doneToday }

    val activeRuns = remember(state.runs) { state.runs.filter { it.stamp.deletedAt == null } }
    val doneRuns = activeRuns.count { it.completed }

    val activeAlarms = remember(state.alarms) { state.alarms.filter { it.stamp.deletedAt == null } }
    val enabledAlarms = activeAlarms.count { it.enabled }
    val nextAlarm = remember(activeAlarms) { activeAlarms.firstOrNull { it.enabled } }

    val upcomingDates = remember(state.importantDates) {
        state.importantDates.filter { it.stamp.deletedAt == null }
            .sortedBy { it.daysUntil() }
    }

    // Wear OS Connection & Complications State
    var watchNodeName by remember { mutableStateOf<String?>("Checking Wear OS...") }
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    var lastRemoteActionStatus by remember { mutableStateOf<String?>(null) }

    // Complication Configuration State (Synced to Wear)
    var faceTopSlot by remember { mutableStateOf("FAST") }
    var faceUpperLeftSlot by remember { mutableStateOf("HEART") }
    var faceUpperRightSlot by remember { mutableStateOf("FOCUS") }
    var faceBottomLeftSlot by remember { mutableStateOf("STEPS") }
    var faceBottomRightSlot by remember { mutableStateOf("CALORIES") }
    var faceBottomSlot by remember { mutableStateOf("ALARM") }
    var faceRingSlot by remember { mutableStateOf("TASKS") }

    // Watch Face Customization States (Synced to Wear)
    var faceStyle by remember { mutableStateOf("MODULAR") }
    var faceFont by remember { mutableStateOf("GOOGLE_SANS") }
    var facePalette by remember { mutableStateOf("COBALT") }
    var faceShowAmPm by remember { mutableStateOf(true) }
    var faceShowDate by remember { mutableStateOf(true) }
    var faceShowStatusIcons by remember { mutableStateOf(true) }
    var faceShowMediaPill by remember { mutableStateOf(true) }

    var selectedSlotForPicker by remember { mutableStateOf<MobileComplicationSlot?>(null) }

    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
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

    fun pushComplicationUpdate(slot: MobileComplicationSlot, type: MobileComplicationDataType) {
        when (slot) {
            MobileComplicationSlot.TOP -> faceTopSlot = type.id
            MobileComplicationSlot.UPPER_LEFT -> faceUpperLeftSlot = type.id
            MobileComplicationSlot.UPPER_RIGHT -> faceUpperRightSlot = type.id
            MobileComplicationSlot.BOTTOM_LEFT -> faceBottomLeftSlot = type.id
            MobileComplicationSlot.BOTTOM_RIGHT -> faceBottomRightSlot = type.id
            MobileComplicationSlot.BOTTOM -> faceBottomSlot = type.id
            MobileComplicationSlot.RING -> faceRingSlot = type.id
        }

        if (watchNodeId != null) {
            scope.launch {
                try {
                    val payload = "${slot.key}:${type.id}".toByteArray(Charsets.UTF_8)
                    Wearable.getMessageClient(context).sendMessage(
                        watchNodeId!!,
                        "/lifeos/complication_update",
                        payload
                    ).await()
                    lastRemoteActionStatus = "Updated ${slot.label} -> ${type.shortLabel}"
                } catch (_: Exception) {
                    lastRemoteActionStatus = "Failed to sync complication"
                }
            }
        }
    }

    fun pushWearPrefUpdate(type: String, key: String, value: String, description: String) {
        if (watchNodeId != null) {
            scope.launch {
                try {
                    val payload = "$type:$key:$value".toByteArray(Charsets.UTF_8)
                    Wearable.getMessageClient(context).sendMessage(
                        watchNodeId!!,
                        "/lifeos/wear_preference_update",
                        payload
                    ).await()
                    lastRemoteActionStatus = "Synced: $description"
                } catch (_: Exception) {
                    lastRemoteActionStatus = "Failed to sync preference"
                }
            }
        }
    }

    fun sendRemoteCommand(path: String, label: String) {
        if (watchNodeId == null) {
            lastRemoteActionStatus = "Watch not connected"
            return
        }
        scope.launch {
            try {
                Wearable.getMessageClient(context).sendMessage(
                    watchNodeId!!,
                    path,
                    label.toByteArray(Charsets.UTF_8)
                ).await()
                lastRemoteActionStatus = "$label sent to Watch"
            } catch (_: Exception) {
                lastRemoteActionStatus = "$label failed"
            }
        }
    }

    fun dispatchMediaKey(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

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
            // 0. Wear OS Watch Device Hub & Remote Diagnostics Bar
            item {
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
                                        .size(44.dp)
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
                                        if (watchNodeId != null) {
                                            "Connected · HR: ${state.wellness.currentHeartRateBpm.toInt().takeIf { it > 0 }?.let { "$it BPM" } ?: "Normal"} · Steps: ${state.wellness.steps}"
                                        } else {
                                            "Paired device standby"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (watchNodeId != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "LIVE SYNC",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Dual-Direction Actions Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { sendRemoteCommand("/lifeos/ping", "Ping") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { sendRemoteCommand("/lifeos/find_watch", "Find Watch") },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Rounded.RingVolume, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Find Watch", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val packet = com.creativeblisslabs.wearproductivity.shared.SyncPacket(
                                                deviceId = "phone-companion",
                                                state = state
                                            )
                                            val jsonStr = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.encodeToString(
                                                com.creativeblisslabs.wearproductivity.shared.SyncPacket.serializer(),
                                                packet
                                            )
                                            val putDataReq = PutDataMapRequest.create(
                                                com.creativeblisslabs.wearproductivity.shared.SyncPaths.LIFE_STATE
                                            ).apply {
                                                dataMap.putString("json", jsonStr)
                                                dataMap.putLong("timestamp", System.currentTimeMillis())
                                            }.asPutDataRequest().setUrgent()
                                            Wearable.getDataClient(context).putDataItem(putDataReq).await()
                                            lastRemoteActionStatus = "State synced to Watch!"
                                        } catch (_: Exception) {
                                            lastRemoteActionStatus = "State push failed"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (lastRemoteActionStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                lastRemoteActionStatus!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // 1. Watch Face Studio & Complications Remote Manager
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.DashboardCustomize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Watch Face Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Remote Complications",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Circular Watch Face Interactive Visual Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Watch Bezel Outer Ring
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedSlotForPicker = MobileComplicationSlot.RING },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Row 1: TOP Slot
                                    val topType = MobileComplicationDataType.fromId(faceTopSlot)
                                    ComplicationSlotPill(
                                        slot = MobileComplicationSlot.TOP,
                                        type = topType,
                                        onClick = { selectedSlotForPicker = MobileComplicationSlot.TOP }
                                    )

                                    // Row 2: UPPER_LEFT & UPPER_RIGHT Slots
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val ulType = MobileComplicationDataType.fromId(faceUpperLeftSlot)
                                        ComplicationSlotPill(
                                            slot = MobileComplicationSlot.UPPER_LEFT,
                                            type = ulType,
                                            onClick = { selectedSlotForPicker = MobileComplicationSlot.UPPER_LEFT }
                                        )

                                        // Central Digital Time Readout
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "10:09",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = (-0.5).sp
                                            )
                                            val ringType = MobileComplicationDataType.fromId(faceRingSlot)
                                            Text(
                                                "Ring: ${ringType.shortLabel}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = ringType.accent
                                            )
                                        }

                                        val urType = MobileComplicationDataType.fromId(faceUpperRightSlot)
                                        ComplicationSlotPill(
                                            slot = MobileComplicationSlot.UPPER_RIGHT,
                                            type = urType,
                                            onClick = { selectedSlotForPicker = MobileComplicationSlot.UPPER_RIGHT }
                                        )
                                    }

                                    // Row 3: BOTTOM_LEFT & BOTTOM_RIGHT Slots
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val blType = MobileComplicationDataType.fromId(faceBottomLeftSlot)
                                        ComplicationSlotPill(
                                            slot = MobileComplicationSlot.BOTTOM_LEFT,
                                            type = blType,
                                            onClick = { selectedSlotForPicker = MobileComplicationSlot.BOTTOM_LEFT }
                                        )

                                        val brType = MobileComplicationDataType.fromId(faceBottomRightSlot)
                                        ComplicationSlotPill(
                                            slot = MobileComplicationSlot.BOTTOM_RIGHT,
                                            type = brType,
                                            onClick = { selectedSlotForPicker = MobileComplicationSlot.BOTTOM_RIGHT }
                                        )
                                    }

                                    // Row 4: BOTTOM Slot
                                    val bottomType = MobileComplicationDataType.fromId(faceBottomSlot)
                                    ComplicationSlotPill(
                                        slot = MobileComplicationSlot.BOTTOM,
                                        type = bottomType,
                                        onClick = { selectedSlotForPicker = MobileComplicationSlot.BOTTOM }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Tap any slot or bezel ring above to change complication telemetry streamed to the watch face.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Watch Face Style Selector Chips
                        Text(
                            "WATCH FACE STYLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val styles = listOf(
                            "MODULAR" to "Modular 6-Slot",
                            "ANALOG_CHRONO" to "Analog Chrono",
                            "DIGITAL_BIG" to "Digital Big",
                            "PIXEL_CONCENTRIC" to "Pixel Concentric",
                            "SAMSUNG_SPORT" to "Galaxy Sport"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(styles) { (styleId, styleName) ->
                                val isSelected = faceStyle == styleId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        faceStyle = styleId
                                        pushWearPrefUpdate("string", "watch_face_type", styleId, "Style: $styleName")
                                    },
                                    label = { Text(styleName, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Accent Color Palette Selector
                        Text(
                            "ACCENT THEME PALETTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val palettes = listOf(
                            Triple("COBALT", "Cobalt", Color(0xFF8EAEFF)),
                            Triple("MINT", "Mint", Color(0xFF79E2B0)),
                            Triple("CORAL", "Coral", Color(0xFFFF9A7A)),
                            Triple("AMBER", "Amber", Color(0xFFFFD177)),
                            Triple("LAVENDER", "Lavender", Color(0xFFBBA5FF)),
                            Triple("MONOCHROME", "Mono", Color(0xFFE2E2E9))
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(palettes) { (pId, pName, pColor) ->
                                val isSelected = facePalette == pId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        facePalette = pId
                                        pushWearPrefUpdate("string", "watch_face_accent_color", pId, "Theme: $pName")
                                    },
                                    label = { Text(pName, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(pColor)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Typography Selector
                        Text(
                            "TYPOGRAPHY FONT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val fonts = listOf(
                            "GOOGLE_SANS" to "Google Sans",
                            "ROBOTO_MONO" to "Monospace",
                            "CLASSIC_SERIF" to "Serif",
                            "SANS_SERIF" to "Clean Sans"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(fonts) { (fId, fName) ->
                                val isSelected = faceFont == fId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        faceFont = fId
                                        pushWearPrefUpdate("string", "watch_face_font", fId, "Font: $fName")
                                    },
                                    label = { Text(fName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Element Visibility Switches
                        Text(
                            "ELEMENTS & TOGGLES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AM / PM Indicator", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = faceShowAmPm,
                                onCheckedChange = {
                                    faceShowAmPm = it
                                    pushWearPrefUpdate("bool", "watch_face_show_am_pm", it.toString(), "AM/PM: $it")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date Display", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = faceShowDate,
                                onCheckedChange = {
                                    faceShowDate = it
                                    pushWearPrefUpdate("bool", "watch_face_show_date_text", it.toString(), "Date: $it")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Battery & Weather Badges", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = faceShowStatusIcons,
                                onCheckedChange = {
                                    faceShowStatusIcons = it
                                    pushWearPrefUpdate("bool", "watch_face_show_status_icons", it.toString(), "Badges: $it")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Media Pill", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = faceShowMediaPill,
                                onCheckedChange = {
                                    faceShowMediaPill = it
                                    pushWearPrefUpdate("bool", "watch_face_show_active_media_pill", it.toString(), "Media Pill: $it")
                                }
                            )
                        }

                        // Complication Picker Bottom Sheet
                        if (selectedSlotForPicker != null) {
                            ModalBottomSheet(
                                onDismissRequest = { selectedSlotForPicker = null }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        "Configure ${selectedSlotForPicker?.label}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Choose data type to push to watch face",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(MobileComplicationDataType.entries) { itemType ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        pushComplicationUpdate(selectedSlotForPicker!!, itemType)
                                                        selectedSlotForPicker = null
                                                    },
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(itemType.accent.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(itemType.icon, contentDescription = null, tint = itemType.accent, modifier = Modifier.size(20.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(itemType.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                                        Text("Indicator tag: [${itemType.shortLabel}]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Intermittent Fasting Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.fast.active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Restaurant,
                                    contentDescription = null,
                                    tint = if (state.fast.active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (state.fast.active) "Fasting Active (${state.fast.phase})" else "Intermittent Fasting",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        if (state.fast.active) {
                                            repository.stopFast()
                                        } else {
                                            repository.startFast("prog-16-8", 16 * 3600L)
                                        }
                                    }
                                }
                            ) {
                                Text(if (state.fast.active) "End Fast" else "Start 16:8")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        val progressClamped = state.fast.progress.coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressClamped },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Elapsed: ${(progressClamped * 100).roundToInt()}% · Plan: ${state.fast.plan.ifBlank { "16:8 Protocol" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (state.fast.active) "Phase: ${state.fast.phase}" else "Idle",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Biometrics, Sleep & Wellness Telemetry Card
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
                                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFFF5252))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Biometrics & Wellness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${state.wellness.currentHeartRateBpm.toInt().takeIf { it > 0 }?.let { "$it BPM" } ?: "-- BPM"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF5252)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sleep Telemetry
                            val sleepHours = state.wellness.sleepLastNightMinutes / 60
                            val sleepMins = state.wellness.sleepLastNightMinutes % 60
                            WellnessStatBox(
                                title = "Sleep",
                                value = if (state.wellness.sleepLastNightMinutes > 0) "${sleepHours}h ${sleepMins}m" else "7h 30m",
                                subtitle = "Recovery score: High",
                                icon = Icons.Rounded.Bedtime,
                                color = Color(0xFF7986CB),
                                modifier = Modifier.weight(1f)
                            )

                            // Weight Telemetry
                            WellnessStatBox(
                                title = "Body Weight",
                                value = "${state.wellness.bodyWeight ?: 75.0} kg",
                                subtitle = "Target: 72.0 kg",
                                icon = Icons.Rounded.MonitorWeight,
                                color = Color(0xFF4DB6AC),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Energy & Mood Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Energy Level: ${state.wellness.energy}/10", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Calm", "Energized", "Tired").forEach { moodOpt ->
                                    FilterChip(
                                        selected = state.wellness.mood.equals(moodOpt, ignoreCase = true),
                                        onClick = {
                                            scope.launch {
                                                repository.updateWellness(mood = moodOpt)
                                            }
                                        },
                                        label = { Text(moodOpt, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Focus Flow Quick Card
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

            // 5. Daily Routines Card (Morning / Afternoon / Evening Blocks)
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
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Daily Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "$doneRoutines/${activeRoutines.size} completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeRoutines.isEmpty()) {
                            Text(
                                "No routine tasks configured. Add morning & evening routines in Routines tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activeRoutines.take(4).forEach { routine ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                scope.launch {
                                                    repository.toggleRoutine(routine.stamp.id)
                                                }
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = routine.doneToday,
                                            onCheckedChange = {
                                                scope.launch {
                                                    repository.toggleRoutine(routine.stamp.id)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                routine.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${routine.startTime} - ${routine.endTime} · Phase: ${routine.phase}",
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

            // 6. Alarms & Workout Plans Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Alarms Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
                                Switch(
                                    checked = nextAlarm?.enabled ?: false,
                                    onCheckedChange = {
                                        nextAlarm?.let { alm ->
                                            scope.launch { repository.toggleAlarm(alm.stamp.id) }
                                        }
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                nextAlarm?.time ?: "--:--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                nextAlarm?.label?.ifBlank { "Next Alarm" } ?: "No active alarm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$enabledAlarms/${activeAlarms.size} active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Workout Plans Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                                IconButton(
                                    onClick = {
                                        sendRemoteCommand("/lifeos/workout/start_run", "Start Run")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Workout", tint = Color(0xFF4CAF50))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$doneRuns/${activeRuns.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Run Plans Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Push to Watch",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 7. Companion Media Playback Controller
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Companion Audio Player", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Synced with Wear OS Playback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
                            }
                            FilledIconButton(
                                onClick = { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
                            }
                        }
                    }
                }
            }

            // 8. Metrics Grid (Tasks, Habits, Calories, Expenses)
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

            // 9. Hydration Tracker
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

            // 10. Important Dates & Milestone Countdowns
            if (upcomingDates.isNotEmpty()) {
                item {
                    Text(
                        "Milestones & Countdowns",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(upcomingDates.take(5)) { dateItem ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.width(160.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "${dateItem.daysUntil()}d left",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        dateItem.title.ifBlank { dateItem.personName },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        dateItem.dateIso,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 11. Upcoming Agenda
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
private fun ComplicationSlotPill(
    slot: MobileComplicationSlot,
    type: MobileComplicationDataType,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = type.accent.copy(alpha = 0.22f),
        border = androidx.compose.foundation.BorderStroke(1.dp, type.accent.copy(alpha = 0.6f)),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(type.icon, contentDescription = null, tint = type.accent, modifier = Modifier.size(12.dp))
            Text(
                type.shortLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = type.accent
            )
        }
    }
}

@Composable
private fun WellnessStatBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
