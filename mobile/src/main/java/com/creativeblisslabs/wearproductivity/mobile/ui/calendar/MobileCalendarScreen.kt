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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

data class MobileSyncedGoogleEvent(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val timeLabel: String,
    val location: String,
    val isAllDay: Boolean
)

enum class MobileCalendarTab(val label: String) {
    DAY_AGENDA("Day Agenda"),
    MONTH_VIEW("Month View"),
    COUNTDOWNS_ALARMS("Countdowns & Alarms")
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

    suspend fun queryDeviceCalendar(): List<MobileSyncedGoogleEvent> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MobileSyncedGoogleEvent>()
        try {
            val nowMs = System.currentTimeMillis()
            val endMs = nowMs + (60L * 24L * 3600L * 1000L)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, nowMs - (14L * 24L * 3600L * 1000L))
            ContentUris.appendId(builder, endMs)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION
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
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

                val zone = ZoneId.systemDefault()
                val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

                while (cursor.moveToNext() && result.size < 150) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx).toString() else "${result.size}"
                    val title = (if (titleIdx >= 0) cursor.getString(titleIdx) else null)?.ifBlank { "Calendar Event" } ?: "Calendar Event"
                    val beginMs = if (beginIdx >= 0) cursor.getLong(beginIdx) else nowMs
                    val endMsVal = if (endIdx >= 0) cursor.getLong(endIdx) else beginMs
                    val isAllDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                    val loc = (if (locIdx >= 0) cursor.getString(locIdx) else null).orEmpty()

                    val zdtStart = Instant.ofEpochMilli(beginMs).atZone(zone)
                    val zdtEnd = Instant.ofEpochMilli(endMsVal).atZone(zone)
                    val timeLabel = if (isAllDay) {
                        "All Day"
                    } else {
                        "${zdtStart.toLocalTime().format(timeFmt)} - ${zdtEnd.toLocalTime().format(timeFmt)}"
                    }

                    result.add(
                        MobileSyncedGoogleEvent(
                            id = "gcal-$id-$beginMs",
                            title = title,
                            startDate = zdtStart.toLocalDate(),
                            endDate = zdtEnd.toLocalDate(),
                            timeLabel = timeLabel,
                            location = loc.ifBlank { "Calendar" },
                            isAllDay = isAllDay
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        result
    }

    fun syncCalendarEvents() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            scope.launch {
                isSyncingGCal = true
                val list = queryDeviceCalendar()
                syncedEvents.clear()
                syncedEvents.addAll(list)
                isSyncingGCal = false
                gCalSyncMessage = "Synced ${list.size} events"
            }
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
                        Text("Planner & Calendar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
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
            // Tab row
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileCalendarTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = { Text(tab.label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) }
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
                MobileCalendarTab.DAY_AGENDA -> {
                    DayAgendaView(
                        selectedDate = selectedDate,
                        today = today,
                        tasks = allTasks.filter { taskMatchesDate(it.dueLabel, selectedDate, today) },
                        events = allEvents.filter { eventMatchesDate(it.timeRange, selectedDate, today) },
                        syncedEvents = syncedEvents.filter { it.startDate == selectedDate },
                        onSelectDate = { selectedDate = it },
                        onToggleTask = { task ->
                            scope.launch { repository.toggleTask(task.stamp.id) }
                        },
                        onDeleteEvent = { id ->
                            scope.launch { repository.deleteEvent(id) }
                        }
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
                        today = today,
                        onToggleAlarm = { id ->
                            scope.launch { repository.toggleAlarm(id) }
                        },
                        onDeleteAlarm = { id ->
                            scope.launch { repository.deleteAlarm(id) }
                        },
                        onDeleteImportantDate = { id ->
                            scope.launch { repository.deleteImportantDate(id) }
                        }
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

@Composable
private fun DayAgendaView(
    selectedDate: LocalDate,
    today: LocalDate,
    tasks: List<LifeTask>,
    events: List<com.creativeblisslabs.wearproductivity.shared.CalendarEvent>,
    syncedEvents: List<MobileSyncedGoogleEvent>,
    onSelectDate: (LocalDate) -> Unit,
    onToggleTask: (LifeTask) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
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

        // Tasks for Selected Date
        item {
            Text("Action Items & Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (tasks.isEmpty()) {
            item {
                Text("No tasks due on this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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
                                "${task.project.ifBlank { "Task" }} • Priority P${task.priority}",
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

@Composable
private fun CountdownsAndAlarmsView(
    importantDates: List<ImportantDate>,
    alarms: List<LifeAlarm>,
    today: LocalDate,
    onToggleAlarm: (String) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onDeleteImportantDate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    shape = RoundedCornerShape(12.dp)
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
                            IconButton(onClick = { onDeleteAlarm(alarm.stamp.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Alarm", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Birthdays & Milestones (${importantDates.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
