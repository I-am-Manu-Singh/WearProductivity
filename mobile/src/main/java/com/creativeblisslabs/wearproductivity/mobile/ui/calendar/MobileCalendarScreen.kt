package com.creativeblisslabs.wearproductivity.mobile.ui.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.creativeblisslabs.wearproductivity.shared.DateCategory
import com.creativeblisslabs.wearproductivity.shared.ImportantDate
import com.creativeblisslabs.wearproductivity.shared.LifeAlarm
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeTask
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// =============================================================================
// SHARED DATA MODELS FOR PLANNER RICH METADATA & PARITY WITH WEAR
// =============================================================================
enum class MobilePlannerItemKind(val label: String, val badge: String, val defaultColor: Color) {
    TASK("Task", "TASK", Color(0xFF79E2B0)),
    EVENT("Event", "EVENT", Color(0xFF8EAEFF)),
    VIDEO_CALL("Video Call", "VIDEO", Color(0xFFBBA5FF)),
    AUDIO_CALL("Audio Call", "CALL", Color(0xFF68B6FF)),
    BIRTHDAY("Birthday", "BDAY", Color(0xFFFF9A7A)),
    ALARM("Alarm", "ALARM", Color(0xFFFFD177))
}

enum class MobilePlannerRecurrenceMode(val label: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKDAYS("M-F"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class MobileKanbanColumnStage(val label: String, val shortLabel: String, val color: Color) {
    BACKLOG("Backlog", "Bklog", Color(0xFF9AA0A6)),
    TODO("To Do", "ToDo", Color(0xFF8EAEFF)),
    IN_PROGRESS("In Progress", "Doing", Color(0xFFFFB76B)),
    DONE("Completed", "Done", Color(0xFF79E2B0))
}

enum class MobileTaskBoardMode(val label: String) {
    KANBAN("Kanban Board"),
    EISENHOWER("Eisenhower 2x2")
}

enum class MobileCalendarHorizonView(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class MobileSubtaskChecklistEntry(
    val id: String,
    val title: String,
    val done: Boolean = false
)

data class MobilePlannerItemRichMeta(
    val itemId: String,
    val kind: MobilePlannerItemKind = MobilePlannerItemKind.TASK,
    val colorKey: String = "mint",
    val iconKey: String = "task",
    val tagLabel: String = "Work",
    val listGroupName: String = "VIPs",
    val priorityLevel: Int = 1, // 1=Q1 Urgent/Imp, 2=Q2 Schedule, 3=Q3 Delegate, 4=Q4 Eliminate
    val kanbanStage: MobileKanbanColumnStage = MobileKanbanColumnStage.TODO,
    val estimatedMinutes: Int = 30,
    val manualProgressPct: Int = 0,
    val recurrence: MobilePlannerRecurrenceMode = MobilePlannerRecurrenceMode.ONCE,
    val repeatDaysMask: Int = 0,
    val bulletNotes: List<String> = emptyList(),
    val subtasks: List<MobileSubtaskChecklistEntry> = emptyList(),
    val callPlatformOrLocation: String = "Google Meet",
    val snoozedUntilEpochMs: Long = 0L,
    val stoppedTodayIso: String = ""
)

private object MobileUnifiedPlannerPrefs {
    private const val PREFS = "lifeos_unified_planner_prefs_v3"
    private const val KEY_META = "item_rich_meta_v3"
    private const val KEY_LIST_NAMES = "custom_birthday_lists_v3"

    fun loadCustomLists(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LIST_NAMES, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split("|").filter { it.isNotBlank() }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf("Family", "Friends", "VIPs", "Work Team", "Anniversaries")
    }

    fun saveCustomLists(context: Context, lists: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIST_NAMES, lists.distinct().joinToString("|"))
            .apply()
    }

    fun loadMetaMap(context: Context): Map<String, MobilePlannerItemRichMeta> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_META, null)
            ?: return emptyMap()
        return raw.split(";;").mapNotNull { entry ->
            val p = entry.split("|")
            if (p.size >= 14) {
                val subtasks = p[11].split("~").mapNotNull { st ->
                    val sp = st.split("^")
                    if (sp.size >= 3) MobileSubtaskChecklistEntry(sp[0], sp[1], sp[2].toBoolean()) else null
                }
                val bullets = p[10].split("~").filter { it.isNotBlank() }
                val meta = MobilePlannerItemRichMeta(
                    itemId = p[0],
                    kind = runCatching { MobilePlannerItemKind.valueOf(p[1]) }.getOrDefault(MobilePlannerItemKind.TASK),
                    colorKey = p[2],
                    iconKey = p[3],
                    tagLabel = p[4],
                    listGroupName = p[5],
                    priorityLevel = p[6].toIntOrNull()?.coerceIn(1, 4) ?: 1,
                    kanbanStage = runCatching { MobileKanbanColumnStage.valueOf(p[7]) }.getOrDefault(MobileKanbanColumnStage.TODO),
                    estimatedMinutes = p[8].toIntOrNull() ?: 30,
                    manualProgressPct = p[9].toIntOrNull()?.coerceIn(0, 100) ?: 0,
                    bulletNotes = bullets,
                    subtasks = subtasks,
                    callPlatformOrLocation = p[12],
                    recurrence = runCatching { MobilePlannerRecurrenceMode.valueOf(p[13]) }.getOrDefault(MobilePlannerRecurrenceMode.ONCE),
                    snoozedUntilEpochMs = p.getOrNull(14)?.toLongOrNull() ?: 0L,
                    stoppedTodayIso = p.getOrNull(15).orEmpty(),
                    repeatDaysMask = p.getOrNull(16)?.toIntOrNull() ?: 0
                )
                meta.itemId to meta
            } else null
        }.toMap()
    }

    fun saveMetaMap(context: Context, map: Map<String, MobilePlannerItemRichMeta>) {
        val encoded = map.values.joinToString(";;") { m ->
            val safeBullets = m.bulletNotes.joinToString("~") { it.replace("|", "").replace("~", "") }
            val safeSubtasks = m.subtasks.joinToString("~") { "${it.id}^${it.title.replace("^", "").replace("|", "")}^${it.done}" }
            "${m.itemId}|${m.kind.name}|${m.colorKey}|${m.iconKey}|${m.tagLabel.replace("|", "")}|${m.listGroupName.replace("|", "")}|${m.priorityLevel}|${m.kanbanStage.name}|${m.estimatedMinutes}|${m.manualProgressPct}|$safeBullets|$safeSubtasks|${m.callPlatformOrLocation.replace("|", "")}|${m.recurrence.name}|${m.snoozedUntilEpochMs}|${m.stoppedTodayIso}|${m.repeatDaysMask}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_META, encoded).apply()
    }
}

val mobilePlannerPalette = mapOf(
    "mint" to Color(0xFF79E2B0),
    "cobalt" to Color(0xFF8EAEFF),
    "coral" to Color(0xFFFF9A7A),
    "amber" to Color(0xFFFFD177),
    "lavender" to Color(0xFFBBA5FF),
    "rose" to Color(0xFFFF7A9A)
)

data class MobileSyncedGoogleEvent(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val timeLabel: String,
    val location: String,
    val isAllDay: Boolean
)

enum class MobileCalendarTab(val label: String, val icon: ImageVector) {
    SPOTLIGHT("Spotlight", Icons.Rounded.Lightbulb),
    DAY_AGENDA("Day Agenda", Icons.Rounded.Schedule),
    KANBAN_MATRIX("Boards", Icons.Rounded.ViewKanban),
    MONTH_VIEW("Month", Icons.Rounded.CalendarMonth),
    COUNTDOWNS_ALARMS("Countdowns", Icons.Rounded.Celebration)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileCalendarScreen(repository: LifeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by repository.state.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentTab by remember { mutableStateOf(MobileCalendarTab.DAY_AGENDA) }

    val today = remember { LocalDate.now() }
    val allTasks = state.tasks.filter { it.stamp.deletedAt == null }
    val allEvents = state.events.filter { it.stamp.deletedAt == null }
    val allImportantDates = state.importantDates.filter { it.stamp.deletedAt == null }
    val allAlarms = state.alarms.filter { it.stamp.deletedAt == null }

    val syncedEvents = remember { mutableStateListOf<MobileSyncedGoogleEvent>() }
    var isSyncingGCal by remember { mutableStateOf(false) }
    var gCalSyncMessage by remember { mutableStateOf<String?>(null) }

    // Rich Meta mapping
    val metaMap = remember {
        mutableStateMapOf<String, MobilePlannerItemRichMeta>().apply {
            putAll(MobileUnifiedPlannerPrefs.loadMetaMap(context))
        }
    }

    fun getMeta(itemId: String): MobilePlannerItemRichMeta =
        metaMap[itemId] ?: MobilePlannerItemRichMeta(itemId = itemId)

    fun updateMeta(meta: MobilePlannerItemRichMeta) {
        metaMap[meta.itemId] = meta
        MobileUnifiedPlannerPrefs.saveMetaMap(context, metaMap)
    }

    var customLists by remember {
        mutableStateOf(MobileUnifiedPlannerPrefs.loadCustomLists(context))
    }
    var selectedCustomList by remember { mutableStateOf(customLists.firstOrNull() ?: "VIPs") }

    // Wear OS Connected Watch Info
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    var watchSyncStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            watchNodeId = nodes.firstOrNull()?.id
        } catch (_: Exception) {}
    }

    fun sendWatchNotification(title: String, message: String) {
        if (watchNodeId == null) {
            watchSyncStatus = "Watch standby (Not connected)"
            return
        }
        scope.launch {
            try {
                val payload = "$title: $message".toByteArray(Charsets.UTF_8)
                Wearable.getMessageClient(context).sendMessage(
                    watchNodeId!!,
                    "/lifeos/find_watch",
                    payload
                ).await()
                watchSyncStatus = "Alert pushed to Watch"
            } catch (_: Exception) {
                watchSyncStatus = "Watch alert failed"
            }
        }
    }

    suspend fun queryDeviceCalendar(): List<MobileSyncedGoogleEvent> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MobileSyncedGoogleEvent>()
        try {
            val nowMs = System.currentTimeMillis()
            val endMs = nowMs + (60L * 24L * 3600L * 1000L)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, nowMs)
            ContentUris.appendId(builder, endMs)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.ALL_DAY
            )

            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                val zone = ZoneId.systemDefault()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx).toString()
                    val title = cursor.getString(titleIdx) ?: "Untitled Event"
                    val begin = cursor.getLong(beginIdx)
                    val end = cursor.getLong(endIdx)
                    val loc = cursor.getString(locIdx) ?: ""
                    val isAllDay = cursor.getInt(allDayIdx) == 1

                    val startDate = Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
                    val endDate = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
                    val startTime = Instant.ofEpochMilli(begin).atZone(zone).toLocalTime()
                    val endTime = Instant.ofEpochMilli(end).atZone(zone).toLocalTime()

                    val timeLabel = if (isAllDay) "All Day" else "${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"

                    result.add(
                        MobileSyncedGoogleEvent(
                            id = id,
                            title = title,
                            startDate = startDate,
                            endDate = endDate,
                            timeLabel = timeLabel,
                            location = loc,
                            isAllDay = isAllDay
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        result
    }

    fun syncCalendarEvents() {
        scope.launch {
            isSyncingGCal = true
            val list = queryDeviceCalendar()
            syncedEvents.clear()
            syncedEvents.addAll(list)
            isSyncingGCal = false
            gCalSyncMessage = "Synced ${list.size} calendar events"
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            syncCalendarEvents()
        } else {
            gCalSyncMessage = "Calendar permission denied"
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            syncCalendarEvents()
        }
    }

    // Quick add dialog state
    var showAddEventDialog by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }
    var newEventTime by remember { mutableStateOf("10:00") }
    var newEventLocation by remember { mutableStateOf("") }

    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var newAlarmLabel by remember { mutableStateOf("Wake Up") }
    var newAlarmTime by remember { mutableStateOf("07:00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Planner & Calendar Hub", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) syncCalendarEvents() else calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Device Calendar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentTab == MobileCalendarTab.COUNTDOWNS_ALARMS) {
                        showAddAlarmDialog = true
                    } else {
                        showAddEventDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Tab Row for all 5 Parity Views
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 12.dp
            ) {
                MobileCalendarTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(tab.label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = gCalSyncMessage != null) {
                gCalSyncMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { gCalSyncMessage = null }) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                MobileCalendarTab.SPOTLIGHT -> {
                    PlannerSpotlightView(
                        today = today,
                        tasks = allTasks,
                        events = allEvents,
                        alarms = allAlarms,
                        getMeta = ::getMeta,
                        onCompleteTask = { task -> scope.launch { repository.toggleTask(task.stamp.id) } },
                        onToggleAlarm = { id -> scope.launch { repository.toggleAlarm(id) } },
                        onAlertWatch = { title, msg -> sendWatchNotification(title, msg) },
                        onJumpToAgenda = { currentTab = MobileCalendarTab.DAY_AGENDA }
                    )
                }

                MobileCalendarTab.DAY_AGENDA -> {
                    DayAgendaView(
                        selectedDate = selectedDate,
                        today = today,
                        tasks = allTasks.filter { taskMatchesDate(it.dueLabel, selectedDate, today) },
                        events = allEvents.filter { eventMatchesDate(it.timeRange, selectedDate, today) },
                        syncedEvents = syncedEvents.filter { it.startDate == selectedDate },
                        getMeta = ::getMeta,
                        onUpdateMeta = ::updateMeta,
                        onSelectDate = { selectedDate = it },
                        onToggleTask = { task -> scope.launch { repository.toggleTask(task.stamp.id) } },
                        onDeleteEvent = { id -> scope.launch { repository.deleteEvent(id) } }
                    )
                }

                MobileCalendarTab.KANBAN_MATRIX -> {
                    KanbanAndEisenhowerView(
                        tasks = allTasks,
                        getMeta = ::getMeta,
                        onUpdateMeta = ::updateMeta,
                        onToggleTask = { task -> scope.launch { repository.toggleTask(task.stamp.id) } }
                    )
                }

                MobileCalendarTab.MONTH_VIEW -> {
                    MonthGridView(
                        displayedYearMonth = displayedYearMonth,
                        selectedDate = selectedDate,
                        today = today,
                        tasks = allTasks,
                        events = allEvents,
                        syncedEvents = syncedEvents,
                        alarms = allAlarms,
                        birthdays = allImportantDates,
                        onChangeMonth = { delta ->
                            displayedYearMonth = displayedYearMonth.plusMonths(delta.toLong())
                            selectedDate = displayedYearMonth.atDay(1)
                        },
                        onSelectDate = {
                            selectedDate = it
                            currentTab = MobileCalendarTab.DAY_AGENDA
                        }
                    )
                }

                MobileCalendarTab.COUNTDOWNS_ALARMS -> {
                    CountdownsAndAlarmsView(
                        importantDates = allImportantDates,
                        alarms = allAlarms,
                        customLists = customLists,
                        selectedList = selectedCustomList,
                        onSelectList = { selectedCustomList = it },
                        onAddCustomList = { newList ->
                            val updated = (customLists + newList).distinct()
                            customLists = updated
                            MobileUnifiedPlannerPrefs.saveCustomLists(context, updated)
                        },
                        today = today,
                        onToggleAlarm = { id -> scope.launch { repository.toggleAlarm(id) } },
                        onDeleteAlarm = { id -> scope.launch { repository.deleteAlarm(id) } },
                        onDeleteImportantDate = { id -> scope.launch { repository.deleteImportantDate(id) } },
                        onPingWatchAlarm = { id -> sendWatchNotification("Alarm Trigger", "Scheduled Alarm ping from phone companion") }
                    )
                }
            }
        }
    }

    if (showAddEventDialog) {
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("New Calendar Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newEventTitle,
                        onValueChange = { newEventTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newEventTime,
                        onValueChange = { newEventTime = it },
                        label = { Text("Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newEventLocation,
                        onValueChange = { newEventLocation = it },
                        label = { Text("Location / Meet link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newEventTitle.isNotBlank()) {
                        val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val timeRange = "$dateStr at $newEventTime"
                        scope.launch {
                            repository.quickEvent(newEventTitle.trim(), timeRange, newEventLocation.trim(), "15m")
                        }
                        newEventTitle = ""
                        newEventLocation = ""
                        showAddEventDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAddAlarmDialog = false },
            title = { Text("New Alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newAlarmLabel,
                        onValueChange = { newAlarmLabel = it },
                        label = { Text("Alarm Label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAlarmTime,
                        onValueChange = { newAlarmTime = it },
                        label = { Text("Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newAlarmTime.isNotBlank()) {
                        val validTime = runCatching {
                            LocalTime.parse(newAlarmTime.take(5), DateTimeFormatter.ofPattern("H:m"))
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                        }.getOrDefault(newAlarmTime.take(5).ifBlank { "07:00" })
                        scope.launch {
                            repository.quickAlarm(validTime, newAlarmLabel.trim().ifBlank { "Alarm" }, "Daily")
                        }
                        showAddAlarmDialog = false
                    }
                }) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAlarmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// 1. SPOTLIGHT DECK VIEW (Matching Wear PlannerSpotlightBoundaryPage)
// =============================================================================
@Composable
private fun PlannerSpotlightView(
    today: LocalDate,
    tasks: List<LifeTask>,
    events: List<com.creativeblisslabs.wearproductivity.shared.CalendarEvent>,
    alarms: List<LifeAlarm>,
    getMeta: (String) -> MobilePlannerItemRichMeta,
    onCompleteTask: (LifeTask) -> Unit,
    onToggleAlarm: (String) -> Unit,
    onAlertWatch: (String, String) -> Unit,
    onJumpToAgenda: () -> Unit
) {
    val activeTasks = remember(tasks) { tasks.filter { !it.complete } }
    val spotlightTask = remember(activeTasks) { activeTasks.firstOrNull { it.priority == 1 } ?: activeTasks.firstOrNull() }
    val nextAlarm = remember(alarms) { alarms.firstOrNull { it.enabled } }
    val nextEvent = remember(events) { events.firstOrNull() }

    val taskDone = tasks.count { it.complete }
    val taskTotal = tasks.size.coerceAtLeast(1)
    val dayProgress = (taskDone.toFloat() / taskTotal.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spotlight Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "SPOTLIGHT MISSION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            "${(dayProgress * 100).toInt()}% Day Completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (spotlightTask != null) {
                        val meta = getMeta(spotlightTask.stamp.id)
                        Text(
                            spotlightTask.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Priority: P${spotlightTask.priority} · Estimated: ${meta.estimatedMinutes}m · Stage: ${meta.kanbanStage.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onCompleteTask(spotlightTask) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Done")
                            }

                            FilledTonalButton(
                                onClick = { onAlertWatch("Spotlight Mission", spotlightTask.title) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Push to Watch")
                            }
                        }
                    } else {
                        Text(
                            "All daily tasks cleared!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Great job staying on top of your schedule today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Next Telemetry Cards (Alarm & Upcoming Event)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Next Alarm Telemetry
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            if (nextAlarm != null) {
                                Switch(
                                    checked = nextAlarm.enabled,
                                    onCheckedChange = { onToggleAlarm(nextAlarm.stamp.id) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(nextAlarm?.time ?: "--:--", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(nextAlarm?.label?.ifBlank { "Next Alarm" } ?: "No active alarm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Next Event Telemetry
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Rounded.Event, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(nextEvent?.title ?: "No Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(nextEvent?.timeRange ?: "Free schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onJumpToAgenda,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Full Daily Agenda")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =============================================================================
// 2. KANBAN BOARD & EISENHOWER 2x2 MATRIX VIEW (Matching Wear Screen)
// =============================================================================
@Composable
private fun KanbanAndEisenhowerView(
    tasks: List<LifeTask>,
    getMeta: (String) -> MobilePlannerItemRichMeta,
    onUpdateMeta: (MobilePlannerItemRichMeta) -> Unit,
    onToggleTask: (LifeTask) -> Unit
) {
    var boardMode by remember { mutableStateOf(MobileTaskBoardMode.KANBAN) }
    var selectedStage by remember { mutableStateOf(MobileKanbanColumnStage.TODO) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector: Kanban Board vs Eisenhower 2x2
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = boardMode == MobileTaskBoardMode.KANBAN,
                onClick = { boardMode = MobileTaskBoardMode.KANBAN },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Kanban Board")
            }
            SegmentedButton(
                selected = boardMode == MobileTaskBoardMode.EISENHOWER,
                onClick = { boardMode = MobileTaskBoardMode.EISENHOWER },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Eisenhower 2x2")
            }
        }

        if (boardMode == MobileTaskBoardMode.KANBAN) {
            // Stage Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MobileKanbanColumnStage.entries.forEach { stage ->
                    val count = tasks.count { getMeta(it.stamp.id).kanbanStage == stage }
                    val isSelected = selectedStage == stage
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedStage = stage },
                        color = if (isSelected) stage.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(1.5.dp, stage.color) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stage.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) stage.color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Tasks in Selected Kanban Stage
            val stageTasks = tasks.filter { getMeta(it.stamp.id).kanbanStage == selectedStage }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stageTasks.isEmpty()) {
                    item {
                        Text(
                            "No tasks in ${selectedStage.label}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(stageTasks) { task ->
                        val meta = getMeta(task.stamp.id)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            val nextIndex = (selectedStage.ordinal + 1) % MobileKanbanColumnStage.entries.size
                                            onUpdateMeta(meta.copy(kanbanStage = MobileKanbanColumnStage.entries[nextIndex]))
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Move to next stage")
                                    }
                                }

                                Text(
                                    "${task.project.ifBlank { "Task" }} · Estimated: ${meta.estimatedMinutes}m · Subtasks: ${meta.subtasks.count { it.done }}/${meta.subtasks.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Eisenhower 2x2 Matrix View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val quadrants = listOf(
                    1 to ("Q1: Urgent & Important (Do First)" to Color(0xFFFF7A9A)),
                    2 to ("Q2: Important & Not Urgent (Schedule)" to Color(0xFF8EAEFF)),
                    3 to ("Q3: Urgent & Not Important (Delegate)" to Color(0xFFFFB76B)),
                    4 to ("Q4: Not Urgent & Not Important (Eliminate)" to Color(0xFF9AA0A6))
                )

                quadrants.forEach { (quadrantId, pair) ->
                    val (label, accentColor) = pair
                    val qTasks = tasks.filter { getMeta(it.stamp.id).priorityLevel == quadrantId }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(accentColor, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("(${qTasks.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (qTasks.isEmpty()) {
                                    Text("Empty quadrant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    qTasks.take(3).forEach { task ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = task.complete,
                                                onCheckedChange = { onToggleTask(task) }
                                            )
                                            Text(
                                                task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textDecoration = if (task.complete) TextDecoration.LineThrough else TextDecoration.None
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
}

// =============================================================================
// 3. DAY AGENDA VIEW (With Subtask Checklist & Rich Color/Tag Editing)
// =============================================================================
@Composable
private fun DayAgendaView(
    selectedDate: LocalDate,
    today: LocalDate,
    tasks: List<LifeTask>,
    events: List<com.creativeblisslabs.wearproductivity.shared.CalendarEvent>,
    syncedEvents: List<MobileSyncedGoogleEvent>,
    getMeta: (String) -> MobilePlannerItemRichMeta,
    onUpdateMeta: (MobilePlannerItemRichMeta) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleTask: (LifeTask) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    var expandedTaskId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Horizontal date selector strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (offset in -3L..7L) {
                    val date = selectedDate.plusDays(offset)
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isToday && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { onSelectDate(date) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    date.format(DateTimeFormatter.ofPattern("EEE")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${tasks.count { it.complete }}/${tasks.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF79E2B0))
                        Text("Tasks Done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Divider(modifier = Modifier.height(40.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${events.size + syncedEvents.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF8EAEFF))
                        Text("Events & Calls", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Google Calendar Synced Events
        if (syncedEvents.isNotEmpty()) {
            item {
                Text("Google Calendar Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(syncedEvents) { gEv ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8EAEFF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF4285F4))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(gEv.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text("${gEv.timeLabel} • ${gEv.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Planned Events & Meetings
        item {
            Text("Planner Events & Calls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (events.isEmpty()) {
            item {
                Text("No scheduled meetings or events for this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(events) { ev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFBBA5FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF7C4DFF))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text("${ev.timeRange} • ${ev.location.ifBlank { "Meeting" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteEvent(ev.stamp.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Action Items & Tasks with Subtask Checklist Inspection
        item {
            Text("Action Items & Subtasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (tasks.isEmpty()) {
            item {
                Text("No tasks due on this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(tasks) { task ->
                val meta = getMeta(task.stamp.id)
                val isExpanded = expandedTaskId == task.stamp.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedTaskId = if (isExpanded) null else task.stamp.id },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = task.complete,
                                onCheckedChange = { onToggleTask(task) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (task.complete) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    "${task.project.ifBlank { "Task" }} • P${meta.priorityLevel} · ${meta.kanbanStage.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { expandedTaskId = if (isExpanded) null else task.stamp.id }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle subtasks"
                                )
                            }
                        }

                        // Subtasks & Rich Metadata Drawer
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Checklist & Subtasks (${meta.subtasks.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            if (meta.subtasks.isEmpty()) {
                                Text("No subtasks defined. Tap + to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                meta.subtasks.forEachIndexed { idx, st ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = st.done,
                                            onCheckedChange = { newDone ->
                                                val updatedList = meta.subtasks.toMutableList()
                                                updatedList[idx] = st.copy(done = newDone)
                                                onUpdateMeta(meta.copy(subtasks = updatedList))
                                            }
                                        )
                                        Text(st.title, style = MaterialTheme.typography.bodySmall)
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

// =============================================================================
// 4. MONTH GRID VIEW
// =============================================================================
@Composable
private fun MonthGridView(
    displayedYearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    tasks: List<LifeTask>,
    events: List<com.creativeblisslabs.wearproductivity.shared.CalendarEvent>,
    syncedEvents: List<MobileSyncedGoogleEvent>,
    alarms: List<LifeAlarm>,
    birthdays: List<ImportantDate>,
    onChangeMonth: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstOfMonth = displayedYearMonth.atDay(1)
    val startOffset = (firstOfMonth.dayOfWeek.value % 7) // Sun=0 .. Sat=6
    val daysInMonth = displayedYearMonth.lengthOfMonth()
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onChangeMonth(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                displayedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { onChangeMonth(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }

        // Weekday header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar Month Matrix
        for (week in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (dow in 0..6) {
                    val cellIndex = week * 7 + dow
                    val dayNum = cellIndex - startOffset + 1
                    if (dayNum in 1..daysInMonth) {
                        val cellDate = displayedYearMonth.atDay(dayNum)
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today
                        val hasTask = tasks.any { taskMatchesDate(it.dueLabel, cellDate, today) }
                        val hasEvent = events.any { eventMatchesDate(it.timeRange, cellDate, today) } || syncedEvents.any { it.startDate == cellDate }
                        val hasAlarm = alarms.any { it.enabled && (it.anchorDateIso == cellDate.toString() || it.repeat.contains("Daily", true)) }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 1.dp else 0.dp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDate(cellDate) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (hasTask) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF79E2B0)))
                                if (hasEvent) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF8EAEFF)))
                                if (hasAlarm) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFFD177)))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. COUNTDOWNS & ALARMS STUDIO VIEW (With Custom VIP & Family Lists)
// =============================================================================
@Composable
private fun CountdownsAndAlarmsView(
    importantDates: List<ImportantDate>,
    alarms: List<LifeAlarm>,
    customLists: List<String>,
    selectedList: String,
    onSelectList: (String) -> Unit,
    onAddCustomList: (String) -> Unit,
    today: LocalDate,
    onToggleAlarm: (String) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onDeleteImportantDate: (String) -> Unit,
    onPingWatchAlarm: (String) -> Unit
) {
    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Alarms Section
        item {
            Text("Active Alarms (${alarms.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (alarms.isEmpty()) {
            item {
                Text("No alarms configured. Tap + to create one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(alarms) { alarm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(alarm.time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("${alarm.label} • ${alarm.repeat}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = alarm.enabled,
                                onCheckedChange = { onToggleAlarm(alarm.stamp.id) }
                            )
                            IconButton(onClick = { onPingWatchAlarm(alarm.stamp.id) }) {
                                Icon(Icons.Rounded.RingVolume, contentDescription = "Ring Watch", tint = Color(0xFFFFB300))
                            }
                            IconButton(onClick = { onDeleteAlarm(alarm.stamp.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Alarm", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Custom Lists Filter Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Birthdays & Lists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showNewListDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New List", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(customLists) { listName ->
                    FilterChip(
                        selected = selectedList == listName,
                        onClick = { onSelectList(listName) },
                        label = { Text(listName) }
                    )
                }
            }
        }

        if (importantDates.isEmpty()) {
            item {
                Text("No countdowns or birthdays logged.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(importantDates) { d ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (d.category == DateCategory.Birthday) Color(0xFFFF9A7A).copy(alpha = 0.2f) else Color(0xFFFF7A9A).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (d.category == DateCategory.Birthday) Icons.Default.Cake else Icons.Default.Star, contentDescription = null, tint = Color(0xFFFF7A9A))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.title, fontWeight = FontWeight.Bold)
                            Text("${d.personName.ifBlank { d.category.name }} • In ${d.daysUntil()} days (${d.dateIso})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteImportantDate(d.stamp.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("Create Custom List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newListName.isNotBlank()) {
                        onAddCustomList(newListName.trim())
                        newListName = ""
                        showNewListDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun taskMatchesDate(dueLabel: String, date: LocalDate, today: LocalDate): Boolean {
    val norm = dueLabel.trim()
    val target = when {
        norm.equals("Today", ignoreCase = true) -> today
        norm.equals("Tomorrow", ignoreCase = true) -> today.plusDays(1)
        else -> runCatching { LocalDate.parse(norm) }.getOrNull()
    }
    return target == date
}

private fun eventMatchesDate(timeRange: String, date: LocalDate, today: LocalDate): Boolean {
    val iso = Regex("""\b\d{4}-\d{2}-\d{2}\b""").find(timeRange)?.value
    val target = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    return target == date
}
