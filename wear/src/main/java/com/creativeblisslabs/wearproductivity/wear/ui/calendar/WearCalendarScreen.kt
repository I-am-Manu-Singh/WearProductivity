package com.creativeblisslabs.wearproductivity.wear.ui.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.CalendarContract
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.creativeblisslabs.wearproductivity.shared.DateCategory
import com.creativeblisslabs.wearproductivity.shared.ImportantDate
import com.creativeblisslabs.wearproductivity.shared.LifeAlarm
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.LifeTask
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// =============================================================================
// UNIFIED PLANNER DOMAIN MODELS & ENUMS
// =============================================================================
enum class UnifiedPlannerSection {
    SPOTLIGHT,
    CALENDAR_MONTH,
    AGENDA_TASKS,
    KANBAN_EISENHOWER,
    BIRTHDAYS_ALARMS
}

enum class PlannerItemKind(val label: String, val badge: String, val defaultColor: Color) {
    TASK("Task", "TASK", Color(0xFF79E2B0)),
    EVENT("Event", "EVENT", Color(0xFF8EAEFF)),
    VIDEO_CALL("Video Call", "VIDEO", Color(0xFFBBA5FF)),
    AUDIO_CALL("Audio Call", "CALL", Color(0xFF68B6FF)),
    BIRTHDAY("Birthday", "BDAY", Color(0xFFFF9A7A)),
    IMPORTANT_DATE("Countdown", "DATE", Color(0xFFFF7A9A)),
    ALARM("Alarm", "ALARM", Color(0xFFFFD177))
}

enum class PlannerRecurrenceMode(val label: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKDAYS("M-F"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class KanbanColumnStage(val label: String, val shortLabel: String, val color: Color) {
    BACKLOG("Backlog", "Bklog", Color(0xFF9AA0A6)),
    TODO("To Do", "ToDo", Color(0xFF8EAEFF)),
    IN_PROGRESS("In Progress", "Doing", Color(0xFFFFB76B)),
    DONE("Completed", "Done", Color(0xFF79E2B0))
}

enum class TaskBoardMode(val label: String) {
    KANBAN("Kanban Board"),
    EISENHOWER("Eisenhower 2x2")
}

enum class CalendarHorizonView(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class SubtaskChecklistEntry(
    val id: String,
    val title: String,
    val done: Boolean = false
)

data class PlannerItemRichMeta(
    val itemId: String,
    val kind: PlannerItemKind = PlannerItemKind.TASK,
    val colorKey: String = "mint",
    val iconKey: String = "task",
    val tagLabel: String = "Work",
    val listGroupName: String = "VIPs",
    val priorityLevel: Int = 1, // 1=Q1 Urgent/Imp, 2=Q2 Schedule, 3=Q3 Delegate, 4=Q4 Eliminate
    val kanbanStage: KanbanColumnStage = KanbanColumnStage.TODO,
    val estimatedMinutes: Int = 30,
    val manualProgressPct: Int = 0,
    val recurrence: PlannerRecurrenceMode = PlannerRecurrenceMode.ONCE,
    val repeatDaysMask: Int = 0b0111110, // Mon..Fri
    val bulletNotes: List<String> = emptyList(),
    val subtasks: List<SubtaskChecklistEntry> = emptyList(),
    val callPlatformOrLocation: String = "Google Meet",
    val snoozedUntilEpochMs: Long = 0L,
    val stoppedTodayIso: String = ""
)

data class SyncedGoogleCalendarEvent(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val timeLabel: String,
    val location: String
)

private val plannerPalette = mapOf(
    "mint" to Color(0xFF79E2B0),
    "cobalt" to Color(0xFF8EAEFF),
    "coral" to Color(0xFFFF9A7A),
    "amber" to Color(0xFFFFD177),
    "lavender" to Color(0xFFBBA5FF),
    "rose" to Color(0xFFFF7A9A)
)

private val plannerIconKeys = listOf("task", "event", "video", "call", "cake", "alarm", "star", "code")

private fun resolvePlannerColor(key: String, fallback: Color = Color(0xFF79E2B0)): Color =
    plannerPalette[key.lowercase(Locale.ROOT)] ?: fallback

private fun resolvePlannerIcon(key: String, kind: PlannerItemKind): ImageVector = when (key) {
    "video" -> Icons.Rounded.Videocam
    "call" -> Icons.Rounded.Call
    "cake" -> Icons.Rounded.Cake
    "alarm" -> Icons.Rounded.Alarm
    "star" -> Icons.Rounded.Star
    "code" -> Icons.Rounded.Code
    "event" -> Icons.Rounded.Event
    "task" -> Icons.Rounded.TaskAlt
    else -> when (kind) {
        PlannerItemKind.TASK -> Icons.Rounded.TaskAlt
        PlannerItemKind.EVENT -> Icons.Rounded.Event
        PlannerItemKind.VIDEO_CALL -> Icons.Rounded.Videocam
        PlannerItemKind.AUDIO_CALL -> Icons.Rounded.Call
        PlannerItemKind.BIRTHDAY -> Icons.Rounded.Cake
        PlannerItemKind.IMPORTANT_DATE -> Icons.Rounded.Celebration
        PlannerItemKind.ALARM -> Icons.Rounded.Alarm
    }
}

// =============================================================================
// REAL ANDROID CALENDAR CONTRACT SYNC & NOTIFICATION ENGINE
// =============================================================================
private object GoogleCalendarWatchBridge {
    private const val CHANNEL_ID = "lifeos_unified_planner_channel"

    suspend fun queryDeviceCalendarEvents(context: Context): List<SyncedGoogleCalendarEvent> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SyncedGoogleCalendarEvent>()
            runCatching {
                val nowMs = System.currentTimeMillis()
                val endMs = nowMs + (30L * 24L * 3600L * 1000L)
                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, nowMs - (7L * 24L * 3600L * 1000L))
                ContentUris.appendId(builder, endMs)

                val projection = arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
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
                    val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                    while (cursor.moveToNext() && results.size < 25) {
                        val id = if (idIdx >= 0) cursor.getLong(idIdx).toString() else "${results.size}"
                        val title = (if (titleIdx >= 0) cursor.getString(titleIdx) else null)?.ifBlank { "Calendar Event" } ?: "Calendar Event"
                        val beginMs = if (beginIdx >= 0) cursor.getLong(beginIdx) else nowMs
                        val loc = (if (locIdx >= 0) cursor.getString(locIdx) else null).orEmpty()
                        val zdt = Instant.ofEpochMilli(beginMs).atZone(ZoneId.systemDefault())
                        results.add(
                            SyncedGoogleCalendarEvent(
                                id = "gcal-$id-$beginMs",
                                title = title,
                                startDate = zdt.toLocalDate(),
                                timeLabel = zdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                                location = loc.ifBlank { "Google Calendar" }
                            )
                        )
                    }
                }
            }
            results
        }

    fun triggerWatchNotification(context: Context, title: String, body: String) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Planner & Alarm Reminders", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        }
    }
}

// =============================================================================
// PERSISTENT PREFERENCES FOR SUBTASKS, KANBAN, CALLS & COUNTDOWN LISTS
// =============================================================================
private object UnifiedPlannerPrefs {
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

    fun loadMetaMap(context: Context): Map<String, PlannerItemRichMeta> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_META, null)
            ?: return emptyMap()
        return raw.split(";;").mapNotNull { entry ->
            val p = entry.split("|")
            if (p.size >= 14) {
                val subtasks = p[11].split("~").mapNotNull { st ->
                    val sp = st.split("^")
                    if (sp.size >= 3) SubtaskChecklistEntry(sp[0], sp[1], sp[2].toBoolean()) else null
                }
                val bullets = p[10].split("~").filter { it.isNotBlank() }
                val meta = PlannerItemRichMeta(
                    itemId = p[0],
                    kind = runCatching { PlannerItemKind.valueOf(p[1]) }.getOrDefault(PlannerItemKind.TASK),
                    colorKey = p[2],
                    iconKey = p[3],
                    tagLabel = p[4],
                    listGroupName = p[5],
                    priorityLevel = p[6].toIntOrNull()?.coerceIn(1, 4) ?: 1,
                    kanbanStage = runCatching { KanbanColumnStage.valueOf(p[7]) }.getOrDefault(KanbanColumnStage.TODO),
                    estimatedMinutes = p[8].toIntOrNull() ?: 30,
                    manualProgressPct = p[9].toIntOrNull()?.coerceIn(0, 100) ?: 0,
                    bulletNotes = bullets,
                    subtasks = subtasks,
                    callPlatformOrLocation = p[12],
                    recurrence = runCatching { PlannerRecurrenceMode.valueOf(p[13]) }.getOrDefault(PlannerRecurrenceMode.ONCE),
                    snoozedUntilEpochMs = p.getOrNull(14)?.toLongOrNull() ?: 0L,
                    stoppedTodayIso = p.getOrNull(15).orEmpty()
                )
                meta.itemId to meta
            } else null
        }.toMap()
    }

    fun saveMetaMap(context: Context, map: Map<String, PlannerItemRichMeta>) {
        val encoded = map.values.joinToString(";;") { m ->
            val safeBullets = m.bulletNotes.joinToString("~") { it.replace("|", "").replace("~", "") }
            val safeSubtasks = m.subtasks.joinToString("~") { "${it.id}^${it.title.replace("^", "").replace("|", "")}^${it.done}" }
            "${m.itemId}|${m.kind.name}|${m.colorKey}|${m.iconKey}|${m.tagLabel.replace("|", "")}|${m.listGroupName.replace("|", "")}|${m.priorityLevel}|${m.kanbanStage.name}|${m.estimatedMinutes}|${m.manualProgressPct}|$safeBullets|$safeSubtasks|${m.callPlatformOrLocation.replace("|", "")}|${m.recurrence.name}|${m.snoozedUntilEpochMs}|${m.stoppedTodayIso}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_META, encoded).apply()
    }
}

// =============================================================================
// MAIN UNIFIED ENTRY POINT: WEAR CALENDAR + TASKS + EVENTS + ALARMS STUDIO
// =============================================================================
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearCalendarScreen(
    state: LifeState,
    repository: LifeRepository,
    initialSection: UnifiedPlannerSection = UnifiedPlannerSection.SPOTLIGHT
) {
    val context = LocalContext.current.applicationContext
    val haptic = rememberLifeOSHapticProvider()
    val scope = rememberCoroutineScope()
    val liveState by repository.state.collectAsState()

    var clockNow by remember { mutableStateOf(LocalDateTime.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var displayedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var horizonView by remember { mutableStateOf(CalendarHorizonView.DAILY) }
    var activeFilterKind by remember { mutableStateOf<PlannerItemKind?>(null) } // null = All

    // Rich Item Metadata & Custom Countdown Lists
    val metaMap = remember {
        mutableStateMapOf<String, PlannerItemRichMeta>().apply {
            putAll(UnifiedPlannerPrefs.loadMetaMap(context))
        }
    }
    val customCountdownLists = remember {
        mutableStateListOf<String>().apply {
            addAll(UnifiedPlannerPrefs.loadCustomLists(context))
        }
    }
    var selectedCountdownList by remember { mutableStateOf("All") }

    fun persistMeta() {
        UnifiedPlannerPrefs.saveMetaMap(context, metaMap.toMap())
    }

    // Synced Google Calendar Events
    val googleCalendarEvents = remember { mutableStateListOf<SyncedGoogleCalendarEvent>() }
    var lastGCalSyncStatus by remember { mutableStateOf("Synced") }

    fun syncWithGoogleCalendar() {
        scope.launch {
            val queried = GoogleCalendarWatchBridge.queryDeviceCalendarEvents(context)
            googleCalendarEvents.clear()
            googleCalendarEvents.addAll(queried)
            lastGCalSyncStatus = if (queried.isNotEmpty()) "${queried.size} GCal Events" else "GCal Active"
        }
    }

    LaunchedEffect(Unit) {
        syncWithGoogleCalendar()
        while (true) {
            delay(30_000L)
            clockNow = LocalDateTime.now()
        }
    }

    // Helper to resolve or seed Rich Metadata for any Task, Event, Date, or Alarm
    fun metaForTask(task: LifeTask): PlannerItemRichMeta = metaMap.getOrPut(task.stamp.id) {
        val prio = task.priority.coerceIn(1, 4)
        PlannerItemRichMeta(
            itemId = task.stamp.id,
            kind = PlannerItemKind.TASK,
            colorKey = when (prio) { 1 -> "coral"; 2 -> "mint"; 3 -> "cobalt"; else -> "amber" },
            iconKey = "task",
            tagLabel = task.project.ifBlank { "Work" },
            priorityLevel = prio,
            kanbanStage = if (task.complete) KanbanColumnStage.DONE else KanbanColumnStage.TODO,
            estimatedMinutes = 45,
            manualProgressPct = if (task.complete) 100 else 0,
            bulletNotes = listOf("Deliver milestone cleanly", "Verify edge cases on watch"),
            subtasks = listOf(
                SubtaskChecklistEntry("${task.stamp.id}-s1", "Prepare spec & architecture", task.complete),
                SubtaskChecklistEntry("${task.stamp.id}-s2", "Implement & test core flow", task.complete),
                SubtaskChecklistEntry("${task.stamp.id}-s3", "Final review & sign-off", task.complete)
            )
        )
    }

    fun metaForEvent(id: String, title: String, location: String): PlannerItemRichMeta = metaMap.getOrPut(id) {
        val isVideo = title.contains("video", true) || title.contains("meet", true) || title.contains("zoom", true) || location.contains("meet", true)
        val isCall = title.contains("call", true) || title.contains("phone", true) || title.contains("sync", true)
        val kind = when {
            isVideo -> PlannerItemKind.VIDEO_CALL
            isCall -> PlannerItemKind.AUDIO_CALL
            else -> PlannerItemKind.EVENT
        }
        PlannerItemRichMeta(
            itemId = id,
            kind = kind,
            colorKey = when (kind) {
                PlannerItemKind.VIDEO_CALL -> "lavender"
                PlannerItemKind.AUDIO_CALL -> "cobalt"
                else -> "cobalt"
            },
            iconKey = if (isVideo) "video" else if (isCall) "call" else "event",
            tagLabel = if (isVideo || isCall) "Call" else "Agenda",
            estimatedMinutes = 30,
            bulletNotes = listOf("Location: ${location.ifBlank { "Google Meet" }}", "Join 2 mins early"),
            subtasks = listOf(
                SubtaskChecklistEntry("$id-e1", "Review agenda notes", false),
                SubtaskChecklistEntry("$id-e2", "Complete meeting action items", false)
            ),
            callPlatformOrLocation = location.ifBlank { "Google Meet" }
        )
    }

    fun metaForImportantDate(date: ImportantDate): PlannerItemRichMeta = metaMap.getOrPut(date.stamp.id) {
        val isBday = date.category == DateCategory.Birthday || date.title.contains("birthday", true)
        PlannerItemRichMeta(
            itemId = date.stamp.id,
            kind = if (isBday) PlannerItemKind.BIRTHDAY else PlannerItemKind.IMPORTANT_DATE,
            colorKey = if (isBday) "coral" else "rose",
            iconKey = "cake",
            tagLabel = date.category.name,
            listGroupName = if (date.personName.isNotBlank()) "VIPs" else "Family",
            recurrence = PlannerRecurrenceMode.YEARLY,
            bulletNotes = listOfNotNull(
                date.personName.takeIf { it.isNotBlank() }?.let { "Person: $it" },
                date.notes.takeIf { it.isNotBlank() } ?: "Send wishes & gift reminder"
            )
        )
    }

    // Compute Task / Event Liquid Fill Progress (0f..1f)
    fun itemFillProgress(itemId: String, isCompleteFlag: Boolean): Float {
        if (isCompleteFlag) return 1f
        val meta = metaMap[itemId] ?: return 0f
        if (meta.subtasks.isNotEmpty()) {
            val doneCount = meta.subtasks.count { it.done }
            return (doneCount.toFloat() / meta.subtasks.size.toFloat()).coerceIn(0f, 1f)
        }
        return (meta.manualProgressPct / 100f).coerceIn(0f, 1f)
    }

    // Active Entities from Repository
    val today = clockNow.toLocalDate()
    val allTasks = liveState.tasks.filter { it.stamp.deletedAt == null }
    val allEvents = liveState.events.filter { it.stamp.deletedAt == null }
    val allImportantDates = liveState.importantDates.filter { it.stamp.deletedAt == null }
    val allAlarms = liveState.alarms.filter { it.stamp.deletedAt == null }

    // Alarm Ringing Detection
    val currentClockStr = clockNow.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val ringingAlarm = allAlarms.firstOrNull { it.enabled && it.time == currentClockStr }

    LaunchedEffect(ringingAlarm?.stamp?.id) {
        if (ringingAlarm != null) {
            GoogleCalendarWatchBridge.triggerWatchNotification(context, "Alarm Ringing", ringingAlarm.label)
            repeat(15) { cycle ->
                haptic.click()
                delay((1_000L - (cycle * 35L)).coerceAtLeast(300L))
            }
        }
    }

    // Filtered items for Selected Date / Horizon
    val tasksForSelectedDate = remember(allTasks, selectedDate, horizonView, metaMap.toMap()) {
        allTasks.filter { t ->
            val meta = metaForTask(t)
            if (meta.stoppedTodayIso == selectedDate.toString()) return@filter false
            when (horizonView) {
                CalendarHorizonView.DAILY -> taskMatchesDate(t.dueLabel, meta, selectedDate, today)
                CalendarHorizonView.WEEKLY -> {
                    val startW = selectedDate.minusDays(3)
                    val endW = selectedDate.plusDays(3)
                    (0L..6L).any { offset -> taskMatchesDate(t.dueLabel, meta, startW.plusDays(offset), today) }
                }
                CalendarHorizonView.MONTHLY -> true
            }
        }
    }

    val eventsForSelectedDate = remember(allEvents, googleCalendarEvents.toList(), selectedDate, horizonView, metaMap.toMap()) {
        allEvents.filter { e ->
            val meta = metaForEvent(e.stamp.id, e.title, e.location)
            if (meta.stoppedTodayIso == selectedDate.toString()) return@filter false
            when (horizonView) {
                CalendarHorizonView.DAILY -> eventMatchesDate(e.timeRange, meta, selectedDate, today)
                CalendarHorizonView.WEEKLY -> {
                    val startW = selectedDate.minusDays(3)
                    (0L..6L).any { offset -> eventMatchesDate(e.timeRange, meta, startW.plusDays(offset), today) }
                }
                CalendarHorizonView.MONTHLY -> true
            }
        }
    }

    // Overall Progress & Spotlight Selection
    val totalActionable = (tasksForSelectedDate.size + eventsForSelectedDate.size).coerceAtLeast(1)
    val totalCompletedScore = tasksForSelectedDate.sumOf { itemFillProgress(it.stamp.id, it.complete).toDouble() } +
            eventsForSelectedDate.sumOf { itemFillProgress(it.stamp.id, false).toDouble() }
    val overallDayProgress = (totalCompletedScore.toFloat() / totalActionable.toFloat()).coerceIn(0f, 1f)

    val nextEnabledAlarm = allAlarms.filter { it.enabled }.minByOrNull { it.time }
    val spotlightTask = tasksForSelectedDate.firstOrNull { !it.complete } ?: tasksForSelectedDate.firstOrNull()
    val spotlightEvent = eventsForSelectedDate.firstOrNull()

    // Universal Editor & Modal States
    var showUniversalEditor by remember { mutableStateOf(false) }
    var editorKind by remember { mutableStateOf(PlannerItemKind.TASK) }
    var editingEntityId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftTimeOrDate by remember { mutableStateOf("09:00") }
    var draftSubtitleAndMeta by remember { mutableStateOf("Work · 45m · P1") }
    var draftColorKey by remember { mutableStateOf("mint") }
    var draftIconKey by remember { mutableStateOf("task") }
    var draftRecurrence by remember { mutableStateOf(PlannerRecurrenceMode.ONCE) }
    var draftPriority by remember { mutableIntStateOf(1) }
    var draftWeekdays by remember {
        mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
    }

    // Expanded Task/Event ID for inline Subtasks & Bullet Notes
    var expandedItemId by remember { mutableStateOf<String?>(null) }
    var showAddSubtaskForId by remember { mutableStateOf<String?>(null) }

    // Snooze & Stop Modal State
    var showSnoozeStopModalForId by remember { mutableStateOf<String?>(null) }
    var customSnoozeMinutes by remember { mutableIntStateOf(15) }

    // Board Mode (Kanban vs Eisenhower)
    var activeBoardMode by remember { mutableStateOf(TaskBoardMode.KANBAN) }
    var selectedKanbanStage by remember { mutableStateOf(KanbanColumnStage.TODO) }

    // Alarm Quick Editor State
    var showAlarmCreator by remember { mutableStateOf(false) }
    var alarmDraftHour by remember { mutableIntStateOf(7) }
    var alarmDraftMinute by remember { mutableIntStateOf(0) }
    var alarmDraftTitle by remember { mutableStateOf("Morning Wake & Bullet Plan") }

    // Custom Birthday List Creator State
    var showNewCountdownListCreator by remember { mutableStateOf(false) }

    var feedbackOverlay by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // =========================================================================
    // CRUD & ACTION HANDLERS
    // =========================================================================
    fun openCreator(kind: PlannerItemKind) {
        haptic.click()
        editorKind = kind
        editingEntityId = null
        draftTitle = ""
        draftColorKey = when (kind) {
            PlannerItemKind.TASK -> "mint"
            PlannerItemKind.EVENT -> "cobalt"
            PlannerItemKind.VIDEO_CALL -> "lavender"
            PlannerItemKind.AUDIO_CALL -> "cobalt"
            PlannerItemKind.BIRTHDAY -> "coral"
            PlannerItemKind.IMPORTANT_DATE -> "rose"
            PlannerItemKind.ALARM -> "amber"
        }
        draftIconKey = when (kind) {
            PlannerItemKind.VIDEO_CALL -> "video"
            PlannerItemKind.AUDIO_CALL -> "call"
            PlannerItemKind.BIRTHDAY, PlannerItemKind.IMPORTANT_DATE -> "cake"
            PlannerItemKind.ALARM -> "alarm"
            PlannerItemKind.EVENT -> "event"
            else -> "task"
        }
        draftTimeOrDate = when (kind) {
            PlannerItemKind.BIRTHDAY, PlannerItemKind.IMPORTANT_DATE -> selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            else -> "09:30"
        }
        draftSubtitleAndMeta = when (kind) {
            PlannerItemKind.TASK -> "Sprint · 45m · Subtask 1, Subtask 2"
            PlannerItemKind.VIDEO_CALL -> "Google Meet · 30m · Deck Review"
            PlannerItemKind.AUDIO_CALL -> "Phone Audio · 15m · Quick Sync"
            PlannerItemKind.BIRTHDAY -> "VIPs · Send wishes & gift"
            PlannerItemKind.IMPORTANT_DATE -> "Milestones · Countdown target"
            PlannerItemKind.ALARM -> "Daily · Hydrate • Stretch"
            PlannerItemKind.EVENT -> "Studio · 60m · Key agenda"
        }
        showUniversalEditor = true
    }

    fun saveUniversalItem(titleInput: String, timeOrDateInput: String, subMetaInput: String) {
        val cleanTitle = titleInput.trim().take(60)
        if (cleanTitle.isBlank()) {
            feedbackOverlay = "Enter Title" to true
            haptic.click()
            return
        }
        val parts = subMetaInput.split("·", "|").map { it.trim() }
        val primaryTagOrLoc = parts.getOrNull(0)?.ifBlank { "General" } ?: "General"
        val estMins = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(5, 480) ?: 30
        val extraRaw = parts.getOrNull(2).orEmpty()
        val parsedSubtasks = extraRaw.split(",", ";", "•").map { it.trim() }.filter { it.isNotBlank() }

        val existingId = editingEntityId
        scope.launch {
            try {
                when (editorKind) {
                    PlannerItemKind.TASK -> {
                        // Coerces the value strictly between 1 and 4 safely
                        val prioInt = draftPriority.coerceIn(1, 4)

                        if (existingId != null) {
                            repository.editTask(
                                id = existingId,
                                newTitle = cleanTitle,
                                newProject = primaryTagOrLoc,
                                newDue = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                newPriority = prioInt,
                                newScheduledTime = timeOrDateInput.take(5),
                                newHaptic = true
                            )
                        } else {
                            repository.quickTask(
                                title = cleanTitle,
                                project = primaryTagOrLoc,
                                dueLabel = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                priority = prioInt,
                                scheduledTime = timeOrDateInput.take(5)
                            )
                        }
                        val targetTask = repository.current().tasks.firstOrNull {
                            it.stamp.id == existingId || it.title.equals(cleanTitle, true)
                        }
                        if (targetTask != null) {
                            val subList = if (parsedSubtasks.isNotEmpty()) {
                                parsedSubtasks.mapIndexed { idx, s ->
                                    SubtaskChecklistEntry("${targetTask.stamp.id}-st-$idx", s, false)
                                }
                            } else {
                                metaMap[targetTask.stamp.id]?.subtasks ?: listOf(
                                    SubtaskChecklistEntry("${targetTask.stamp.id}-1", "Step 1: Kickoff", false),
                                    SubtaskChecklistEntry("${targetTask.stamp.id}-2", "Step 2: Complete & verify", false)
                                )
                            }
                            metaMap[targetTask.stamp.id] = PlannerItemRichMeta(
                                itemId = targetTask.stamp.id,
                                kind = PlannerItemKind.TASK,
                                colorKey = draftColorKey,
                                iconKey = draftIconKey,
                                tagLabel = primaryTagOrLoc,
                                priorityLevel = draftPriority,
                                kanbanStage = KanbanColumnStage.TODO,
                                estimatedMinutes = estMins,
                                recurrence = draftRecurrence,
                                bulletNotes = if (parsedSubtasks.isNotEmpty()) parsedSubtasks else listOf("Priority P$draftPriority", "Est ${estMins}m"),
                                subtasks = subList
                            )
                            persistMeta()
                        }
                    }

                    PlannerItemKind.EVENT, PlannerItemKind.VIDEO_CALL, PlannerItemKind.AUDIO_CALL -> {
                        val dateIso = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val timeRangeStr = "$dateIso at ${timeOrDateInput.ifBlank { "10:00" }}"
                        if (existingId != null) {
                            repository.editEvent(
                                id = existingId,
                                newTitle = cleanTitle,
                                newTimeRange = timeRangeStr,
                                newLocation = primaryTagOrLoc,
                                newReminder = "${estMins}m · ${draftRecurrence.label}"
                            )
                        } else {
                            repository.quickEvent(
                                title = cleanTitle,
                                timeRange = timeRangeStr,
                                location = primaryTagOrLoc,
                                reminder = "${estMins}m · ${draftRecurrence.label}"
                            )
                        }
                        val targetEv = repository.current().events.firstOrNull {
                            it.stamp.id == existingId || it.title.equals(cleanTitle, true)
                        }
                        if (targetEv != null) {
                            metaMap[targetEv.stamp.id] = PlannerItemRichMeta(
                                itemId = targetEv.stamp.id,
                                kind = editorKind,
                                colorKey = draftColorKey,
                                iconKey = draftIconKey,
                                tagLabel = primaryTagOrLoc,
                                estimatedMinutes = estMins,
                                recurrence = draftRecurrence,
                                callPlatformOrLocation = primaryTagOrLoc,
                                bulletNotes = parsedSubtasks.ifEmpty { listOf("Platform: $primaryTagOrLoc", "Recurrence: ${draftRecurrence.label}") },
                                subtasks = parsedSubtasks.mapIndexed { idx, s ->
                                    SubtaskChecklistEntry("${targetEv.stamp.id}-ev-$idx", s, false)
                                }
                            )
                            persistMeta()
                        }
                    }

                    PlannerItemKind.BIRTHDAY, PlannerItemKind.IMPORTANT_DATE -> {
                        val parsedDateIso = runCatching { LocalDate.parse(timeOrDateInput.trim()).toString() }
                            .getOrDefault(selectedDate.toString())
                        val cat = if (editorKind == PlannerItemKind.BIRTHDAY) DateCategory.Birthday else DateCategory.Milestone
                        if (existingId != null) {
                            repository.editImportantDate(
                                id = existingId,
                                personName = primaryTagOrLoc,
                                title = cleanTitle,
                                dateIso = parsedDateIso,
                                category = cat,
                                reminderDaysBefore = 1,
                                notes = extraRaw.ifBlank { "Countdown list: $primaryTagOrLoc" }
                            )
                        } else {
                            repository.addImportantDate(
                                personName = primaryTagOrLoc,
                                title = cleanTitle,
                                dateIso = parsedDateIso,
                                category = cat,
                                reminderDaysBefore = 1,
                                notes = extraRaw.ifBlank { "Countdown list: $primaryTagOrLoc" }
                            )
                        }
                        if (primaryTagOrLoc !in customCountdownLists) {
                            customCountdownLists.add(primaryTagOrLoc)
                            UnifiedPlannerPrefs.saveCustomLists(context, customCountdownLists)
                        }
                    }

                    PlannerItemKind.ALARM -> {
                        val timeStr = timeOrDateInput.take(5).ifBlank { "07:00" }
                        val repeatLabel = draftRecurrence.label
                        if (existingId != null) {
                            repository.editAlarm(existingId, timeStr, cleanTitle, repeatLabel)
                        } else {
                            repository.quickAlarm(timeStr, cleanTitle, repeatLabel)
                        }
                        val targetAlarm = repository.current().alarms.firstOrNull {
                            it.stamp.id == existingId || it.label.equals(cleanTitle, true)
                        }
                        if (targetAlarm != null) {
                            metaMap[targetAlarm.stamp.id] = PlannerItemRichMeta(
                                itemId = targetAlarm.stamp.id,
                                kind = PlannerItemKind.ALARM,
                                colorKey = draftColorKey,
                                iconKey = "alarm",
                                tagLabel = repeatLabel,
                                bulletNotes = parsedSubtasks.ifEmpty { listOf("Wake cue: $cleanTitle", "Hydrate 250ml") }
                            )
                            persistMeta()
                        }
                    }
                }
                showUniversalEditor = false
                editingEntityId = null
                feedbackOverlay = "${editorKind.label} Saved" to false
                haptic.click()
            } catch (_: Exception) {
                feedbackOverlay = "Save Failed" to true
            }
        }
    }

    fun toggleSubtaskCheck(parentId: String, subtaskId: String, isTaskEntity: Boolean) {
        haptic.click()
        val currentMeta = metaMap[parentId] ?: return
        val updatedSubs = currentMeta.subtasks.map { st ->
            if (st.id == subtaskId) st.copy(done = !st.done) else st
        }
        val doneRatio = if (updatedSubs.isNotEmpty()) {
            ((updatedSubs.count { it.done }.toFloat() / updatedSubs.size) * 100).roundToInt()
        } else currentMeta.manualProgressPct

        val allSubtasksDone = updatedSubs.isNotEmpty() && updatedSubs.all { it.done }
        metaMap[parentId] = currentMeta.copy(
            subtasks = updatedSubs,
            manualProgressPct = doneRatio,
            kanbanStage = when {
                allSubtasksDone -> KanbanColumnStage.DONE
                doneRatio > 0 -> KanbanColumnStage.IN_PROGRESS
                else -> currentMeta.kanbanStage
            }
        )
        persistMeta()

        if (isTaskEntity && allSubtasksDone) {
            val taskObj = allTasks.firstOrNull { it.stamp.id == parentId }
            if (taskObj != null && !taskObj.complete) {
                scope.launch { repository.completeTask(parentId) }
            }
        }
        feedbackOverlay = "Progress: $doneRatio%" to false
    }

    fun advanceItemLiquidProgress(itemId: String, isTask: Boolean) {
        haptic.click()
        val currentMeta = metaMap[itemId]
        if (currentMeta != null && currentMeta.subtasks.isNotEmpty()) {
            val nextUnchecked = currentMeta.subtasks.firstOrNull { !it.done }
            if (nextUnchecked != null) {
                toggleSubtaskCheck(itemId, nextUnchecked.id, isTask)
                return
            }
        }
        if (isTask) {
            scope.launch { repository.completeTask(itemId) }
            feedbackOverlay = "Task Toggled" to false
        } else if (currentMeta != null) {
            val nextPct = if (currentMeta.manualProgressPct >= 100) 0 else (currentMeta.manualProgressPct + 25).coerceAtMost(100)
            metaMap[itemId] = currentMeta.copy(manualProgressPct = nextPct)
            persistMeta()
            feedbackOverlay = "Event $nextPct% Done" to false
        }
    }

    val initialPageIndex = when (initialSection) {
        UnifiedPlannerSection.SPOTLIGHT -> 0
        UnifiedPlannerSection.CALENDAR_MONTH -> 1
        UnifiedPlannerSection.AGENDA_TASKS -> 2
        UnifiedPlannerSection.KANBAN_EISENHOWER -> 3
        UnifiedPlannerSection.BIRTHDAYS_ALARMS -> 4
    }

    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { 5 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    val adaptiveSpec = com.creativeblisslabs.wearproductivity.wear.ui.components.core.LocalWearAdaptiveSpec.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior = pagerFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    // =========================================================================
                    // PAGE 0: SPOTLIGHT BOUNDARY PROGRESS RING (ALARM / CALL / TASK / EVENT)
                    // =========================================================================
                    0 -> PlannerSpotlightBoundaryPage(
                        spec = adaptiveSpec,
                        overallDayProgress = overallDayProgress,
                        ringingAlarm = ringingAlarm,
                        nextAlarm = nextEnabledAlarm,
                        spotlightTask = spotlightTask,
                        spotlightEvent = spotlightEvent,
                        spotlightMeta = (spotlightTask?.let { metaForTask(it) })
                            ?: (spotlightEvent?.let { metaForEvent(it.stamp.id, it.title, it.location) }),
                        onPrimaryCompleteOrDismiss = {
                            if (ringingAlarm != null) {
                                haptic.click()
                                scope.launch { repository.dismissActiveAlarm() }
                                feedbackOverlay = "Alarm Dismissed" to false
                            } else if (spotlightTask != null) {
                                advanceItemLiquidProgress(spotlightTask.stamp.id, isTask = true)
                            } else if (spotlightEvent != null) {
                                advanceItemLiquidProgress(spotlightEvent.stamp.id, isTask = false)
                            } else {
                                openCreator(PlannerItemKind.TASK)
                            }
                        },
                        onOpenSnoozeModal = {
                            haptic.click()
                            if (ringingAlarm != null) {
                                scope.launch { repository.snoozeActiveAlarm() }
                                feedbackOverlay = "Alarm Snoozed" to false
                            } else {
                                showSnoozeStopModalForId = spotlightTask?.stamp?.id ?: spotlightEvent?.stamp?.id ?: nextEnabledAlarm?.stamp?.id
                            }
                        },
                        onOpenStopModal = {
                            haptic.click()
                            showSnoozeStopModalForId = spotlightTask?.stamp?.id ?: spotlightEvent?.stamp?.id ?: nextEnabledAlarm?.stamp?.id
                        },
                        onTriggerWatchReminder = {
                            haptic.click()
                            val title = spotlightTask?.title ?: spotlightEvent?.title ?: nextEnabledAlarm?.label ?: "Planner Check-In"
                            GoogleCalendarWatchBridge.triggerWatchNotification(
                                context,
                                "LifeOS Schedule: $title",
                                "Day Progress: ${(overallDayProgress * 100).roundToInt()}% · Tap to view"
                            )
                            feedbackOverlay = "Reminder Sent" to false
                        },
                        onJumpToMonthCalendar = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onJumpToAgenda = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    // =========================================================================
                    // PAGE 1: NATIVE MONTH CALENDAR GRID, HORIZONTAL DATE CARDS & GCAL SYNC
                    // =========================================================================
                    1 -> CalendarMonthAndSyncPage(
                        spec = adaptiveSpec,
                        selectedDate = selectedDate,
                        displayedYearMonth = displayedYearMonth,
                        horizonView = horizonView,
                        activeFilterKind = activeFilterKind,
                        lastGCalSyncStatus = lastGCalSyncStatus,
                        tasksCountOnDate = { date ->
                            allTasks.count { taskMatchesDate(it.dueLabel, metaForTask(it), date, today) }
                        },
                        eventsCountOnDate = { date ->
                            allEvents.count { eventMatchesDate(it.timeRange, metaForEvent(it.stamp.id, it.title, it.location), date, today) } +
                                    googleCalendarEvents.count { it.startDate == date }
                        },
                        birthdaysCountOnDate = { date ->
                            allImportantDates.count {
                                val nextD = nextImportantDate(it.dateIso, it.category.name)
                                nextD == date
                            }
                        },
                        alarmsCountOnDate = { date ->
                            allAlarms.count { it.enabled && alarmRepeatOccursOn(it.repeat, date, today) }
                        },
                        onSelectDate = { date ->
                            haptic.click()
                            selectedDate = date
                            displayedYearMonth = YearMonth.from(date)
                        },
                        onChangeMonth = { deltaMonths ->
                            haptic.click()
                            displayedYearMonth = displayedYearMonth.plusMonths(deltaMonths.toLong())
                            selectedDate = displayedYearMonth.atDay(1)
                        },
                        onSelectHorizon = {
                            haptic.click()
                            horizonView = it
                        },
                        onSelectFilterKind = { kind ->
                            haptic.click()
                            activeFilterKind = if (activeFilterKind == kind) null else kind
                        },
                        onSyncGoogleCalendar = {
                            haptic.click()
                            syncWithGoogleCalendar()
                            feedbackOverlay = "Google Calendar Synced" to false
                        },
                        onOpenCreator = ::openCreator,
                        onJumpToAgenda = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    // =========================================================================
                    // PAGE 2: UNIFIED DAY AGENDA, SUBTASKS CHECKLIST & LIQUID FILL CARDS
                    // =========================================================================
                    2 -> UnifiedAgendaAndSubtasksPage(
                        spec = adaptiveSpec,
                        selectedDate = selectedDate,
                        today = today,
                        horizonView = horizonView,
                        activeFilterKind = activeFilterKind,
                        tasks = tasksForSelectedDate,
                        events = eventsForSelectedDate,
                        gCalEvents = googleCalendarEvents.filter { it.startDate == selectedDate },
                        importantDates = allImportantDates,
                        alarms = allAlarms.filter { alarmRepeatOccursOn(it.repeat, selectedDate, today) },
                        expandedItemId = expandedItemId,
                        showAddSubtaskForId = showAddSubtaskForId,
                        showUniversalEditor = showUniversalEditor,
                        editorKind = editorKind,
                        isEditing = editingEntityId != null,
                        draftTitle = draftTitle,
                        draftTimeOrDate = draftTimeOrDate,
                        draftSubtitleAndMeta = draftSubtitleAndMeta,
                        draftColorKey = draftColorKey,
                        draftIconKey = draftIconKey,
                        draftRecurrence = draftRecurrence,
                        draftPriority = draftPriority,
                        draftWeekdays = draftWeekdays,
                        metaForTask = ::metaForTask,
                        metaForEvent = ::metaForEvent,
                        itemFillProgress = ::itemFillProgress,
                        onSelectDate = {
                            haptic.click()
                            selectedDate = it
                        },
                        onSelectFilterKind = {
                            haptic.click()
                            activeFilterKind = if (activeFilterKind == it) null else it
                        },
                        onOpenCreator = ::openCreator,
                        onSelectEditorKind = {
                            haptic.click()
                            editorKind = it
                        },
                        onSelectColorKey = {
                            haptic.click()
                            draftColorKey = it
                        },
                        onSelectIconKey = {
                            haptic.click()
                            draftIconKey = it
                        },
                        onSelectRecurrence = {
                            haptic.click()
                            draftRecurrence = it
                        },
                        onSelectPriority = {
                            haptic.click()
                            draftPriority = it
                        },
                        onToggleWeekday = { day ->
                            haptic.click()
                            draftWeekdays = if (day in draftWeekdays) draftWeekdays - day else draftWeekdays + day
                        },
                        onSaveUniversalItem = ::saveUniversalItem,
                        onCancelUniversalEditor = {
                            haptic.click()
                            showUniversalEditor = false
                            editingEntityId = null
                        },
                        onToggleExpandItem = { id ->
                            haptic.click()
                            expandedItemId = if (expandedItemId == id) null else id
                        },
                        onAdvanceLiquidProgress = ::advanceItemLiquidProgress,
                        onToggleSubtask = ::toggleSubtaskCheck,
                        onOpenAddSubtask = { id ->
                            haptic.click()
                            showAddSubtaskForId = if (showAddSubtaskForId == id) null else id
                        },
                        onSaveNewSubtask = { parentId, subtaskTitle, bulletNote ->
                            val cleanSub = subtaskTitle.trim()
                            if (cleanSub.isNotBlank()) {
                                val cur = metaMap[parentId] ?: PlannerItemRichMeta(itemId = parentId)
                                val nextSubs = cur.subtasks + SubtaskChecklistEntry(
                                    id = "$parentId-st-${System.currentTimeMillis()}",
                                    title = cleanSub,
                                    done = false
                                )
                                val nextBullets = if (bulletNote.isNotBlank()) cur.bulletNotes + bulletNote.trim() else cur.bulletNotes
                                metaMap[parentId] = cur.copy(subtasks = nextSubs, bulletNotes = nextBullets)
                                persistMeta()
                                feedbackOverlay = "Subtask Added" to false
                            }
                            showAddSubtaskForId = null
                            haptic.click()
                        },
                        onEditTask = { task ->
                            val m = metaForTask(task)
                            editorKind = PlannerItemKind.TASK
                            editingEntityId = task.stamp.id
                            draftTitle = task.title
                            draftTimeOrDate = task.scheduledTime ?: "09:00"
                            draftSubtitleAndMeta = "${m.tagLabel} · ${m.estimatedMinutes}m · ${m.subtasks.joinToString(", ") { it.title }}"
                            draftColorKey = m.colorKey
                            draftIconKey = m.iconKey
                            draftPriority = m.priorityLevel
                            draftRecurrence = m.recurrence
                            showUniversalEditor = true
                            haptic.click()
                        },
                        onDeleteTask = { taskId ->
                            haptic.click()
                            scope.launch { repository.deleteTask(taskId) }
                            feedbackOverlay = "Task Deleted" to false
                        },
                        onEditEvent = { ev ->
                            val m = metaForEvent(ev.stamp.id, ev.title, ev.location)
                            editorKind = m.kind
                            editingEntityId = ev.stamp.id
                            draftTitle = ev.title
                            draftTimeOrDate = extractClockTime(ev.timeRange) ?: "10:00"
                            draftSubtitleAndMeta = "${ev.location} · ${m.estimatedMinutes}m"
                            draftColorKey = m.colorKey
                            draftIconKey = m.iconKey
                            draftRecurrence = m.recurrence
                            showUniversalEditor = true
                            haptic.click()
                        },
                        onDeleteEvent = { evId ->
                            haptic.click()
                            scope.launch { repository.deleteEvent(evId) }
                            feedbackOverlay = "Event Deleted" to false
                        },
                        onOpenSnoozeStop = { id ->
                            haptic.click()
                            showSnoozeStopModalForId = id
                        },
                        onJumpToKanban = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )

                    // =========================================================================
                    // PAGE 3: AUTHENTIC KANBAN BOARD & COLOR-CODED 2x2 EISENHOWER MATRIX
                    // =========================================================================
                    3 -> KanbanAndEisenhowerPage(
                        spec = adaptiveSpec,
                        boardMode = activeBoardMode,
                        selectedKanbanStage = selectedKanbanStage,
                        tasks = allTasks,
                        metaForTask = ::metaForTask,
                        itemFillProgress = ::itemFillProgress,
                        onSelectBoardMode = {
                            haptic.click()
                            activeBoardMode = it
                        },
                        onSelectKanbanStage = {
                            haptic.click()
                            selectedKanbanStage = it
                        },
                        onAdvanceKanbanStage = { task ->
                            haptic.click()
                            val curMeta = metaForTask(task)
                            val nextStage = when (curMeta.kanbanStage) {
                                KanbanColumnStage.BACKLOG -> KanbanColumnStage.TODO
                                KanbanColumnStage.TODO -> KanbanColumnStage.IN_PROGRESS
                                KanbanColumnStage.IN_PROGRESS -> KanbanColumnStage.DONE
                                KanbanColumnStage.DONE -> KanbanColumnStage.TODO
                            }
                            metaMap[task.stamp.id] = curMeta.copy(
                                kanbanStage = nextStage,
                                manualProgressPct = if (nextStage == KanbanColumnStage.DONE) 100 else curMeta.manualProgressPct
                            )
                            persistMeta()
                            if (nextStage == KanbanColumnStage.DONE && !task.complete) {
                                scope.launch { repository.completeTask(task.stamp.id) }
                            } else if (nextStage != KanbanColumnStage.DONE && task.complete) {
                                scope.launch { repository.completeTask(task.stamp.id) }
                            }
                            feedbackOverlay = "${task.title.take(12)} -> ${nextStage.shortLabel}" to false
                        },
                        onCycleQuadrantPriority = { task ->
                            haptic.click()
                            val curMeta = metaForTask(task)
                            val nextPrio = (curMeta.priorityLevel % 4) + 1
                            metaMap[task.stamp.id] = curMeta.copy(priorityLevel = nextPrio)
                            persistMeta()

                            // FIX: Assign nextPrio directly since LifeTask priority is an Int
                            val prioEnum = nextPrio

                            scope.launch {
                                repository.editTask(
                                    id = task.stamp.id,
                                    newTitle = task.title,
                                    newProject = task.project,
                                    newDue = task.dueLabel,
                                    newPriority = prioEnum, // Now passes the correct Int value
                                    newScheduledTime = task.scheduledTime,
                                    newHaptic = task.hapticEnabled
                                )
                            }
                            feedbackOverlay = "Moved to Q$nextPrio" to false
                        },
                        onOpenNewTask = {
                            openCreator(PlannerItemKind.TASK)
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onJumpToBirthdaysAndAlarms = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(4) }
                        }
                    )

                    // =========================================================================
                    // PAGE 4: BIRTHDAYS, COUNTDOWN LISTS, VIDEO/AUDIO CALLS & ALARMS STUDIO
                    // =========================================================================
                    else -> BirthdaysCallsAndAlarmsPage(
                        spec = adaptiveSpec,
                        today = today,
                        importantDates = allImportantDates,
                        customCountdownLists = customCountdownLists,
                        selectedCountdownList = selectedCountdownList,
                        callEvents = allEvents.filter {
                            val k = metaForEvent(it.stamp.id, it.title, it.location).kind
                            k == PlannerItemKind.VIDEO_CALL || k == PlannerItemKind.AUDIO_CALL
                        },
                        alarms = allAlarms,
                        metaForDate = ::metaForImportantDate,
                        metaForEvent = ::metaForEvent,
                        metaMap = metaMap,
                        showAlarmCreator = showAlarmCreator,
                        alarmDraftHour = alarmDraftHour,
                        alarmDraftMinute = alarmDraftMinute,
                        alarmDraftTitle = alarmDraftTitle,
                        showNewListCreator = showNewCountdownListCreator,
                        onSelectCountdownList = {
                            haptic.click()
                            selectedCountdownList = it
                        },
                        onToggleNewListCreator = {
                            haptic.click()
                            showNewCountdownListCreator = !showNewCountdownListCreator
                        },
                        onSaveNewCountdownList = { listName, personTitle, dateIsoStr ->
                            val cleanList = listName.trim().ifBlank { "Special" }
                            if (cleanList !in customCountdownLists) {
                                customCountdownLists.add(cleanList)
                                UnifiedPlannerPrefs.saveCustomLists(context, customCountdownLists)
                            }
                            selectedCountdownList = cleanList
                            if (personTitle.isNotBlank()) {
                                val validIso = runCatching { LocalDate.parse(dateIsoStr.trim()).toString() }
                                    .getOrDefault(today.plusDays(14).toString())
                                scope.launch {
                                    repository.addImportantDate(
                                        personName = cleanList,
                                        title = personTitle.trim(),
                                        dateIso = validIso,
                                        category = DateCategory.Birthday,
                                        reminderDaysBefore = 1,
                                        notes = "List: $cleanList"
                                    )
                                }
                            }
                            showNewCountdownListCreator = false
                            feedbackOverlay = "List '$cleanList' Saved" to false
                            haptic.click()
                        },
                        onOpenAddBirthdayOrCall = { kind ->
                            openCreator(kind)
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onDeleteImportantDate = { id ->
                            haptic.click()
                            scope.launch { repository.deleteImportantDate(id) }
                            feedbackOverlay = "Countdown Deleted" to false
                        },
                        onToggleAlarmCreator = {
                            haptic.click()
                            showAlarmCreator = !showAlarmCreator
                        },
                        onShiftAlarmTime = { deltaMins ->
                            haptic.click()
                            val total = (alarmDraftHour * 60 + alarmDraftMinute + deltaMins + 1440) % 1440
                            alarmDraftHour = total / 60
                            alarmDraftMinute = total % 60
                        },
                        onSaveQuickAlarm = { title, timeStr, bulletRepeat ->
                            val cleanLabel = title.trim().ifBlank { "Wake Alarm" }
                            val validTime = timeStr.take(5).ifBlank { String.format(Locale.ROOT, "%02d:%02d", alarmDraftHour, alarmDraftMinute) }
                            val bullets = bulletRepeat.split("•", ",", ";").map { it.trim() }.filter { it.isNotBlank() }
                            scope.launch {
                                repository.quickAlarm(validTime, cleanLabel, "Daily")
                                val created = repository.current().alarms.firstOrNull { it.label.equals(cleanLabel, true) }
                                if (created != null) {
                                    metaMap[created.stamp.id] = PlannerItemRichMeta(
                                        itemId = created.stamp.id,
                                        kind = PlannerItemKind.ALARM,
                                        colorKey = "amber",
                                        iconKey = "alarm",
                                        bulletNotes = bullets.ifEmpty { listOf("Morning Routine", "Check Day Agenda") }
                                    )
                                    persistMeta()
                                }
                            }
                            showAlarmCreator = false
                            feedbackOverlay = "Alarm Set for $validTime" to false
                            haptic.click()
                        },
                        onToggleAlarm = { id ->
                            haptic.click()
                            scope.launch { repository.toggleAlarm(id) }
                        },
                        onDeleteAlarm = { id ->
                            haptic.click()
                            scope.launch { repository.deleteAlarm(id) }
                            feedbackOverlay = "Alarm Deleted" to false
                        },
                        onJumpToTop = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }

            // =========================================================================
            // SNOOZE DURATION & STOP/RESCHEDULE MODAL OVERLAY
            // =========================================================================
            showSnoozeStopModalForId?.let { targetId ->
                PlannerSnoozeAndStopModal(
                    customSnoozeMinutes = customSnoozeMinutes,
                    onAdjustSnooze = { delta ->
                        haptic.click()
                        customSnoozeMinutes = (customSnoozeMinutes + delta).coerceIn(5, 240)
                    },
                    onConfirmSnooze = { mins ->
                        haptic.click()
                        val cur = metaMap[targetId] ?: PlannerItemRichMeta(itemId = targetId)
                        metaMap[targetId] = cur.copy(snoozedUntilEpochMs = System.currentTimeMillis() + mins * 60_000L)
                        persistMeta()
                        showSnoozeStopModalForId = null
                        feedbackOverlay = "Snoozed for ${mins}m" to false
                    },
                    onStopForTodayOnly = {
                        haptic.click()
                        val cur = metaMap[targetId] ?: PlannerItemRichMeta(itemId = targetId)
                        metaMap[targetId] = cur.copy(stoppedTodayIso = selectedDate.toString())
                        persistMeta()
                        showSnoozeStopModalForId = null
                        feedbackOverlay = "Stopped for Today Only" to false
                    },
                    onRescheduleToTomorrow = {
                        haptic.click()
                        val tomorrowStr = selectedDate.plusDays(1).toString()
                        val task = allTasks.firstOrNull { it.stamp.id == targetId }
                        if (task != null) {
                            scope.launch {
                                repository.editTask(
                                    id = task.stamp.id,
                                    newTitle = task.title,
                                    newProject = task.project,
                                    newDue = tomorrowStr,
                                    newPriority = task.priority,
                                    newScheduledTime = task.scheduledTime,
                                    newHaptic = task.hapticEnabled
                                )
                            }
                        }
                        showSnoozeStopModalForId = null
                        feedbackOverlay = "Moved to Tomorrow" to false
                    },
                    onDismiss = {
                        haptic.click()
                        showSnoozeStopModalForId = null
                    }
                )
            }

            // Right Bezel 5-Page Vertical Pager Indicator
            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 5,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            // In-Viewport Constrained Curved Confirmation Overlay
            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedCalendarConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }
        }
    
}

// =============================================================================
// PAGE 0: FULL-SCREEN BOUNDARY PROGRESS RING & SPOTLIGHT TRANSPORT CANVAS
// =============================================================================
@Composable
private fun PlannerSpotlightBoundaryPage(
    spec: WearAdaptiveSpec,
    overallDayProgress: Float,
    ringingAlarm: LifeAlarm?,
    nextAlarm: LifeAlarm?,
    spotlightTask: LifeTask?,
    spotlightEvent: com.creativeblisslabs.wearproductivity.shared.CalendarEvent?,
    spotlightMeta: PlannerItemRichMeta?,
    onPrimaryCompleteOrDismiss: () -> Unit,
    onOpenSnoozeModal: () -> Unit,
    onOpenStopModal: () -> Unit,
    onTriggerWatchReminder: () -> Unit,
    onJumpToMonthCalendar: () -> Unit,
    onJumpToAgenda: () -> Unit
) {
    val isRinging = ringingAlarm != null
    val primaryAccent = when {
        isRinging -> Color(0xFFFF7A9A)
        spotlightMeta != null -> resolvePlannerColor(spotlightMeta.colorKey)
        else -> Color(0xFF8EAEFF)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isRinging) 1f else overallDayProgress.coerceIn(0.08f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "plannerBoundaryProg"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "plannerWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRinging) 1200 else 3400, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "plannerWavePhase"
    )

    val centerButtonSize = (spec.screenWidth * 0.31f).coerceIn(56.dp, 72.dp)
    val sideButtonWidth = (spec.screenWidth * 0.21f).coerceIn(42.dp, 52.dp)
    val sideButtonHeight = (spec.screenWidth * 0.24f).coerceIn(46.dp, 58.dp)
    val bottomPillHeight = (spec.screenHeight * 0.14f).coerceIn(28.dp, 35.dp)

    ScreenScaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // 1. FULL-SCREEN RADIAL AMBIENT BACKDROP
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isRinging) 0.38f else 0.20f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRinging) Icons.Rounded.Alarm
                    else spotlightMeta?.let { resolvePlannerIcon(it.iconKey, it.kind) } ?: Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(spec.screenWidth * 0.52f),
                    tint = primaryAccent.copy(alpha = 0.08f)
                )
            }

            // 2. DUAL BOUNDARY CIRCULAR PROGRESS CONTAINER
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val outerStroke = 5.5.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
                val inset = 9.dp.toPx()
                drawArc(
                    color = Color(0xFFFFD177).copy(alpha = 0.45f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    startAngle = -90f,
                    sweepAngle = 360f * (if (nextAlarm != null) 0.75f else 0.3f),
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. FOREGROUND SPOTLIGHT CONTROLS (Snooze, Complete/Dismiss, Stop, Calendar, Agenda)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = spec.screenHeight * 0.12f,
                        bottom = spec.screenHeight * 0.08f,
                        start = spec.screenWidth * 0.07f,
                        end = spec.screenWidth * 0.07f
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Pill + Main Title + Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                            .clickable(onClick = onJumpToMonthCalendar)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = primaryAccent
                        )
                        Text(
                            text = if (isRinging) "ALARM RINGING NOW"
                            else "${spotlightMeta?.kind?.badge ?: "PLANNER"} · ${(overallDayProgress * 100).roundToInt()}% DONE",
                            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            maxLines = 1
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    val mainTitle = when {
                        ringingAlarm != null -> ringingAlarm.label.ifBlank { "Alarm" }
                        spotlightTask != null -> spotlightTask.title
                        spotlightEvent != null -> spotlightEvent.title
                        nextAlarm != null -> "Alarm ${nextAlarm.time}"
                        else -> "All Scheduled Done!"
                    }

                    Text(
                        text = mainTitle,
                        style = MaterialTheme.typography.titleMedium.compactLineHeight(),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    val subLine = when {
                        ringingAlarm != null -> "${ringingAlarm.time} · TAP CENTER TO DISMISS"
                        spotlightTask != null -> "${spotlightTask.project} · Est ${spotlightMeta?.estimatedMinutes ?: 30}m · Next Alm ${nextAlarm?.time ?: "--"}"
                        spotlightEvent != null -> "${spotlightEvent.timeRange} · ${spotlightEvent.location}"
                        else -> "TAP BELOW FOR MONTH GRID OR KANBAN"
                    }

                    Text(
                        text = subLine,
                        style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Transport Cluster: Snooze | Center Wavy Complete/Dismiss | Stop/Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenSnoozeModal,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Snooze Item or Alarm" },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Snooze", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp))
                        }
                    }

                    Box(
                        modifier = Modifier.size(centerButtonSize + 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawPlannerWavyRing(
                                progress = animatedProgress,
                                phase = wavePhase,
                                trackColor = primaryAccent.copy(alpha = 0.28f),
                                activeColor = primaryAccent,
                                strokeWidthPx = 3.5.dp.toPx(),
                                waveAmplitudePx = if (isRinging) 2.5.dp.toPx() else 1.4.dp.toPx(),
                                lobes = 12
                            )
                        }

                        Button(
                            onClick = onPrimaryCompleteOrDismiss,
                            modifier = Modifier.size(centerButtonSize),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isRinging) Icons.Rounded.AlarmOff else Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (isRinging) "Dismiss" else "${(overallDayProgress * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onOpenStopModal,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Stop or Reschedule" },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Stop", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp))
                        }
                    }
                }

                // Bottom Slot: Month Grid | Alert Bell | Agenda & Subtasks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.05f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onJumpToMonthCalendar,
                        modifier = Modifier
                            .height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarViewMonth, contentDescription = null,
                                modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Month", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onTriggerWatchReminder,
                        modifier = Modifier.height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = "Send " +
                                    "Alert", modifier = Modifier.size(22.dp))
                        }
                    }

                    Button(
                        onClick = onJumpToAgenda,
                        modifier = Modifier.height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Checklist, contentDescription = "Open Agenda",
                                modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPlannerWavyRing(
    progress: Float,
    phase: Float,
    trackColor: Color,
    activeColor: Color,
    strokeWidthPx: Float,
    waveAmplitudePx: Float,
    lobes: Int
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseRadius = (minOf(size.width, size.height) / 2f) - waveAmplitudePx - (strokeWidthPx / 2f)
    val totalSteps = 160

    fun buildPath(maxStep: Int): Path {
        val path = Path()
        for (step in 0..maxStep) {
            val fraction = step.toFloat() / totalSteps.toFloat()
            val angleRad = (-Math.PI / 2.0) + (fraction * 2.0 * Math.PI)
            val waveOffset = waveAmplitudePx * sin((lobes * angleRad) + phase).toFloat()
            val r = baseRadius + waveOffset
            val x = center.x + (r * cos(angleRad)).toFloat()
            val y = center.y + (r * sin(angleRad)).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    drawPath(
        path = buildPath(totalSteps),
        color = trackColor,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val activeSteps = (totalSteps * progress.coerceIn(0.04f, 1f)).toInt()
    if (activeSteps > 0) {
        drawPath(
            path = buildPath(activeSteps),
            color = activeColor,
            style = Stroke(width = strokeWidthPx * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// =============================================================================
// PAGE 1: NATIVE MONTH CALENDAR GRID, HORIZONTAL DATE SCROLLER & GOOGLE SYNC
// =============================================================================
@Composable
private fun CalendarMonthAndSyncPage(
    spec: WearAdaptiveSpec,
    selectedDate: LocalDate,
    displayedYearMonth: YearMonth,
    horizonView: CalendarHorizonView,
    activeFilterKind: PlannerItemKind?,
    lastGCalSyncStatus: String,
    tasksCountOnDate: (LocalDate) -> Int,
    eventsCountOnDate: (LocalDate) -> Int,
    birthdaysCountOnDate: (LocalDate) -> Int,
    alarmsCountOnDate: (LocalDate) -> Int,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (Int) -> Unit,
    onSelectHorizon: (CalendarHorizonView) -> Unit,
    onSelectFilterKind: (PlannerItemKind) -> Unit,
    onSyncGoogleCalendar: () -> Unit,
    onOpenCreator: (PlannerItemKind) -> Unit,
    onJumpToAgenda: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToAgenda,
                label = "Day Agenda",
                icon = Icons.Rounded.KeyboardArrowDown
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = displayedYearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())).uppercase(Locale.getDefault()),
                    icon = Icons.Rounded.CalendarMonth,
                    badgeText = lastGCalSyncStatus,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. HORIZON VIEW SELECTOR (Daily / Weekly / Monthly)
            item {
                WearActionButtonGroup(
                    options = CalendarHorizonView.entries.map { it.label },
                    selectedIndex = horizonView.ordinal,
                    onOptionSelected = { idx -> onSelectHorizon(CalendarHorizonView.entries[idx]) }
                )
            }

            // 2. HORIZONTAL SCROLLING DATE CARDS WITH MULTI-COLORED ITEM TYPE DOTS
            item {
                HorizontalColoredDateCardStrip(
                    selectedDate = selectedDate,
                    tasksCountOnDate = tasksCountOnDate,
                    eventsCountOnDate = eventsCountOnDate,
                    birthdaysCountOnDate = birthdaysCountOnDate,
                    alarmsCountOnDate = alarmsCountOnDate,
                    onSelectDate = onSelectDate
                )
            }

            // 3. NATIVE 7-COLUMN MONTH CALENDAR VIEW (Optimized for Wear OS)
            item {
                NativeWearMonthGridCard(
                    yearMonth = displayedYearMonth,
                    selectedDate = selectedDate,
                    tasksCountOnDate = tasksCountOnDate,
                    eventsCountOnDate = eventsCountOnDate,
                    birthdaysCountOnDate = birthdaysCountOnDate,
                    onSelectDate = onSelectDate,
                    onPrevMonth = { onChangeMonth(-1) },
                    onNextMonth = { onChangeMonth(1) }
                )
            }

            // 4. ITEM TYPE FILTER CHIPS & QUICK CREATION BAR
            item {
                CatalogM3ListHeaderSample(title = "FILTER & QUICK ADD")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlannerItemKind.entries.forEach { kind ->
                        val isSelected = activeFilterKind == kind
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) kind.defaultColor
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { onSelectFilterKind(kind) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = resolvePlannerIcon("", kind),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isSelected) Color.Black else kind.defaultColor
                            )
                            Text(
                                text = kind.badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 5. GOOGLE CALENDAR DEVICE SYNC CARD
            item {
                WearMetricCard(
                    title = "GOOGLE CALENDAR BRIDGE",
                    value = lastGCalSyncStatus,
                    subtitle = "Tap to refresh Android CalendarContract events",
                    icon = Icons.Rounded.Sync,
                    onClick = onSyncGoogleCalendar
                )
            }
        }
    }
}

@Composable
private fun HorizontalColoredDateCardStrip(
    selectedDate: LocalDate,
    tasksCountOnDate: (LocalDate) -> Int,
    eventsCountOnDate: (LocalDate) -> Int,
    birthdaysCountOnDate: (LocalDate) -> Int,
    alarmsCountOnDate: (LocalDate) -> Int,
    onSelectDate: (LocalDate) -> Unit
) {
    val scrollState = rememberScrollState()
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (offset in -3L..7L) {
            val d = selectedDate.plusDays(offset)
            val isSelected = d == selectedDate
            val tCnt = tasksCountOnDate(d)
            val eCnt = eventsCountOnDate(d)
            val bCnt = birthdaysCountOnDate(d)
            val aCnt = alarmsCountOnDate(d)

            Column(
                modifier = Modifier
                    .width(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectDate(d) }
                    .padding(vertical = 4.dp, horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = d.format(dayFmt).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = d.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium.tabularNums(),
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                // Color-coded item-type indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (eCnt > 0) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF8EAEFF)))
                    if (tCnt > 0) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF79E2B0)))
                    if (bCnt > 0) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFF9A7A)))
                    if (aCnt > 0) Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFFD177)))
                }
            }
        }
    }
}

@Composable
private fun NativeWearMonthGridCard(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    tasksCountOnDate: (LocalDate) -> Int,
    eventsCountOnDate: (LocalDate) -> Int,
    birthdaysCountOnDate: (LocalDate) -> Int,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val firstOfMonth = yearMonth.atDay(1)
    val startOffset = (firstOfMonth.dayOfWeek.value % 7) // Sun=0 .. Sat=6
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

    Card(
        onClick = {},
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallCircleIconBtn(icon = Icons.Default.ChevronLeft, onClick = onPrevMonth)
                Text(
                    text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                SmallCircleIconBtn(icon = Icons.Default.ChevronRight, onClick = onNextMonth)
            }

            // Weekday Header Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { wd ->
                    Text(
                        text = wd,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Calendar Weeks
            for (week in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dow in 0..6) {
                        val cellIndex = week * 7 + dow
                        val dayNum = cellIndex - startOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val cellDate = yearMonth.atDay(dayNum)
                            val isSelected = cellDate == selectedDate
                            val hasTask = tasksCountOnDate(cellDate) > 0
                            val hasEvent = eventsCountOnDate(cellDate) > 0
                            val hasBday = birthdaysCountOnDate(cellDate) > 0

                            val dotColor = when {
                                hasBday -> Color(0xFFFF9A7A)
                                hasEvent -> Color(0xFF8EAEFF)
                                hasTask -> Color(0xFF79E2B0)
                                else -> Color.Transparent
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { onSelectDate(cellDate) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (dotColor != Color.Transparent) {
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else dotColor)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(22.dp))
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// PAGE 2: UNIFIED DAY AGENDA, SUBTASKS CHECKLIST & LIQUID FILL CARDS
// =============================================================================
@Composable
private fun UnifiedAgendaAndSubtasksPage(
    spec: WearAdaptiveSpec,
    selectedDate: LocalDate,
    today: LocalDate,
    horizonView: CalendarHorizonView,
    activeFilterKind: PlannerItemKind?,
    tasks: List<LifeTask>,
    events: List<com.creativeblisslabs.wearproductivity.shared.CalendarEvent>,
    gCalEvents: List<SyncedGoogleCalendarEvent>,
    importantDates: List<ImportantDate>,
    alarms: List<LifeAlarm>,
    expandedItemId: String?,
    showAddSubtaskForId: String?,
    showUniversalEditor: Boolean,
    editorKind: PlannerItemKind,
    isEditing: Boolean,
    draftTitle: String,
    draftTimeOrDate: String,
    draftSubtitleAndMeta: String,
    draftColorKey: String,
    draftIconKey: String,
    draftRecurrence: PlannerRecurrenceMode,
    draftPriority: Int,
    draftWeekdays: Set<DayOfWeek>,
    metaForTask: (LifeTask) -> PlannerItemRichMeta,
    metaForEvent: (String, String, String) -> PlannerItemRichMeta,
    itemFillProgress: (String, Boolean) -> Float,
    onSelectDate: (LocalDate) -> Unit,
    onSelectFilterKind: (PlannerItemKind) -> Unit,
    onOpenCreator: (PlannerItemKind) -> Unit,
    onSelectEditorKind: (PlannerItemKind) -> Unit,
    onSelectColorKey: (String) -> Unit,
    onSelectIconKey: (String) -> Unit,
    onSelectRecurrence: (PlannerRecurrenceMode) -> Unit,
    onSelectPriority: (Int) -> Unit,
    onToggleWeekday: (DayOfWeek) -> Unit,
    onSaveUniversalItem: (String, String, String) -> Unit,
    onCancelUniversalEditor: () -> Unit,
    onToggleExpandItem: (String) -> Unit,
    onAdvanceLiquidProgress: (String, Boolean) -> Unit,
    onToggleSubtask: (String, String, Boolean) -> Unit,
    onOpenAddSubtask: (String) -> Unit,
    onSaveNewSubtask: (String, String, String) -> Unit,
    onEditTask: (LifeTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onEditEvent: (com.creativeblisslabs.wearproductivity.shared.CalendarEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onOpenSnoozeStop: (String) -> Unit,
    onJumpToKanban: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val daysDiff = ChronoUnit.DAYS.between(today, selectedDate)
    val daysLeftBadge = when {
        daysDiff == 0L -> "TODAY"
        daysDiff == 1L -> "TOMORROW"
        daysDiff > 1L -> "IN ${daysDiff}D"
        else -> "${-daysDiff}D AGO"
    }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = {
                    if (showUniversalEditor) onCancelUniversalEditor()
                    else onOpenCreator(activeFilterKind ?: PlannerItemKind.TASK)
                },
                label = if (showUniversalEditor) "Close" else "Create Item",
                icon = if (showUniversalEditor) Icons.Rounded.Close else Icons.Rounded.Add
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())).uppercase(Locale.getDefault()),
                    icon = Icons.Rounded.EventNote,
                    badgeText = daysLeftBadge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. UNIVERSAL MULTI-ENTITY CREATOR / EDITOR (Task, Event, Video/Audio Call, Birthday, Alarm)
            if (showUniversalEditor) {
                item {
                    CatalogM3ListHeaderSample(
                        title = if (isEditing) "EDIT ${editorKind.label.uppercase()}" else "CREATE ${editorKind.label.uppercase()}"
                    )
                }

                // Item Type Selector Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PlannerItemKind.entries.forEach { kind ->
                            val selected = editorKind == kind
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) kind.defaultColor else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onSelectEditorKind(kind) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = kind.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Custom Color, Icon & Priority (P1..P4) Swatches
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        plannerPalette.forEach { (key, col) ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (draftColorKey == key) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { onSelectColorKey(key) }
                            )
                        }
                        (1..4).forEach { p ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (draftPriority == p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onSelectPriority(p) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "P$p",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (draftPriority == p) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Recurrence Mode (Once, Daily, M-F, Weekly, Monthly, Yearly) + Custom Weekday Chips
                item {
                    WearActionButtonGroup(
                        options = PlannerRecurrenceMode.entries.map { it.label },
                        selectedIndex = draftRecurrence.ordinal,
                        onOptionSelected = { idx -> onSelectRecurrence(PlannerRecurrenceMode.entries[idx]) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            DayOfWeek.MONDAY to "M",
                            DayOfWeek.TUESDAY to "T",
                            DayOfWeek.WEDNESDAY to "W",
                            DayOfWeek.THURSDAY to "T",
                            DayOfWeek.FRIDAY to "F",
                            DayOfWeek.SATURDAY to "S",
                            DayOfWeek.SUNDAY to "S"
                        ).forEach { (day, lbl) ->
                            val isSel = day in draftWeekdays
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onToggleWeekday(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = editorKind.badge,
                        initialTitle = draftTitle,
                        initialTimeOrDate = draftTimeOrDate,
                        initialSubtitle = draftSubtitleAndMeta,
                        isEditingExisting = isEditing,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = if (editorKind == PlannerItemKind.BIRTHDAY || editorKind == PlannerItemKind.IMPORTANT_DATE) "Date (YYYY-MM-DD)" else "Time (HH:mm)",
                        subtitleFieldLabel = "Tag · Est Mins · Subtasks/Notes",
                        onSave = onSaveUniversalItem,
                        onCancel = onCancelUniversalEditor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. PROJECT TASKS WITH EXPANDABLE SUBTASKS, BULLET NOTES & LIQUID FILL
            if (activeFilterKind == null || activeFilterKind == PlannerItemKind.TASK) {
                item {
                    CatalogM3ListHeaderSample(title = "PROJECT TASKS (${tasks.size})")
                }
                items(tasks.size) { idx ->
                    val task = tasks[idx]
                    val meta = metaForTask(task)
                    val fill = itemFillProgress(task.stamp.id, task.complete)
                    val isExpanded = expandedItemId == task.stamp.id

                    M3LiquidFillPlannerCard(
                        title = task.title,
                        subtitle = "${meta.tagLabel} · P${meta.priorityLevel} · Est ${meta.estimatedMinutes}m · ${meta.recurrence.label}",
                        badgeText = "${(fill * 100).roundToInt()}%",
                        daysLeftText = daysLeftBadge,
                        accent = resolvePlannerColor(meta.colorKey),
                        icon = resolvePlannerIcon(meta.iconKey, PlannerItemKind.TASK),
                        fillProgress = fill,
                        isDone = task.complete,
                        isExpanded = isExpanded,
                        bulletNotes = meta.bulletNotes,
                        subtasks = meta.subtasks,
                        transformation = transformationSpec,
                        onCardTap = { onToggleExpandItem(task.stamp.id) },
                        onCheckOrAdvance = { onAdvanceLiquidProgress(task.stamp.id, true) },
                        onToggleSubtask = { subId -> onToggleSubtask(task.stamp.id, subId, true) },
                        onOpenAddSubtask = { onOpenAddSubtask(task.stamp.id) },
                        onOpenSnoozeStop = { onOpenSnoozeStop(task.stamp.id) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task.stamp.id) }
                    )

                    if (showAddSubtaskForId == task.stamp.id) {
                        Spacer(Modifier.height(4.dp))
                        WearExpressiveUniversalEditorCard(
                            itemTypeTitle = "SUBTASK",
                            initialTitle = "",
                            initialTimeOrDate = "15m",
                            initialSubtitle = "Bullet note detail",
                            isEditingExisting = false,
                            showTimePickerField = false,
                            showSubtitleField = true,
                            subtitleFieldLabel = "Bullet Point Description",
                            onSave = { subTitle, _, note ->
                                onSaveNewSubtask(task.stamp.id, subTitle, note)
                            },
                            onCancel = { onOpenAddSubtask(task.stamp.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. EVENTS, VIDEO CALLS, AUDIO CALLS & GOOGLE CALENDAR SYNCED ITEMS
            if (activeFilterKind == null || activeFilterKind == PlannerItemKind.EVENT ||
                activeFilterKind == PlannerItemKind.VIDEO_CALL || activeFilterKind == PlannerItemKind.AUDIO_CALL
            ) {
                item {
                    CatalogM3ListHeaderSample(title = "EVENTS & CALLS (${events.size + gCalEvents.size})")
                }
                items(events.size) { idx ->
                    val ev = events[idx]
                    val meta = metaForEvent(ev.stamp.id, ev.title, ev.location)
                    val fill = itemFillProgress(ev.stamp.id, false)
                    val isExpanded = expandedItemId == ev.stamp.id

                    M3LiquidFillPlannerCard(
                        title = ev.title,
                        subtitle = "${ displayEventTime(ev.timeRange) } · ${ev.location.ifBlank { meta.callPlatformOrLocation }}",
                        badgeText = meta.kind.badge,
                        daysLeftText = daysLeftBadge,
                        accent = resolvePlannerColor(meta.colorKey, meta.kind.defaultColor),
                        icon = resolvePlannerIcon(meta.iconKey, meta.kind),
                        fillProgress = fill,
                        isDone = fill >= 1f,
                        isExpanded = isExpanded,
                        bulletNotes = meta.bulletNotes,
                        subtasks = meta.subtasks,
                        transformation = transformationSpec,
                        onCardTap = { onToggleExpandItem(ev.stamp.id) },
                        onCheckOrAdvance = { onAdvanceLiquidProgress(ev.stamp.id, false) },
                        onToggleSubtask = { subId -> onToggleSubtask(ev.stamp.id, subId, false) },
                        onOpenAddSubtask = { onOpenAddSubtask(ev.stamp.id) },
                        onOpenSnoozeStop = { onOpenSnoozeStop(ev.stamp.id) },
                        onEdit = { onEditEvent(ev) },
                        onDelete = { onDeleteEvent(ev.stamp.id) }
                    )
                }

                // Synced Google Calendar Events
                items(gCalEvents.size) { idx ->
                    val gEv = gCalEvents[idx]
                    WearMetricCard(
                        title = "GOOGLE CALENDAR · ${gEv.timeLabel}",
                        value = gEv.title,
                        subtitle = gEv.location,
                        icon = Icons.Rounded.EventAvailable,
                        onClick = {}
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = onJumpToKanban,
                    icon = { Icon(Icons.Rounded.ViewKanban, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kanban & Eisenhower Matrix")
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.M3LiquidFillPlannerCard(
    title: String,
    subtitle: String,
    badgeText: String,
    daysLeftText: String,
    accent: Color,
    icon: ImageVector,
    fillProgress: Float,
    isDone: Boolean,
    isExpanded: Boolean,
    bulletNotes: List<String>,
    subtasks: List<SubtaskChecklistEntry>,
    transformation: TransformationSpec,
    onCardTap: () -> Unit,
    onCheckOrAdvance: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onOpenAddSubtask: () -> Unit,
    onOpenSnoozeStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val animatedFill by animateFloatAsState(
        targetValue = fillProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "plannerLiquidFill"
    )
    val revealState = rememberRevealState()

    SwipeToReveal(
        revealState = revealState,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformation),
        onSwipePrimaryAction = onDelete,
        primaryAction = {
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
            }
        },
        secondaryAction = {
            IconButton(
                onClick = onEdit,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit")
            }
        }
    ) {
        Card(
            onClick = onCardTap,
            modifier = Modifier.fillMaxWidth(),
            transformation = SurfaceTransformation(transformation),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawRoundRect(
                            color = accent.copy(alpha = 0.24f),
                            size = Size(size.width * animatedFill, size.height),
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                        )
                        drawContent()
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f))
                            .clickable(onClick = onCheckOrAdvance),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Rounded.Check else icon,
                            contentDescription = "Complete or Progress",
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            WearStatusBadge(text = daysLeftText)
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                }

                // Expanded View: Bullet Points, Subtasks Checklist & Action Strip
                if (isExpanded || subtasks.any { !it.done }) {
                    if (bulletNotes.isNotEmpty() && isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            bulletNotes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (subtasks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            subtasks.forEach { st ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                                        .clickable { onToggleSubtask(st.id) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (st.done) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (st.done) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = st.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        textDecoration = if (st.done) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${subtasks.count { it.done }}/${subtasks.size} subtasks",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = accent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SmallCircleIconBtn(icon = Icons.Rounded.Add, onClick = onOpenAddSubtask)
                            SmallCircleIconBtn(icon = Icons.Rounded.Snooze, onClick = onOpenSnoozeStop)
                            SmallCircleIconBtn(icon = Icons.Rounded.Edit, onClick = onEdit)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// PAGE 3: AUTHENTIC COLOR-CODED KANBAN BOARD & 2x2 EISENHOWER MATRIX GRID
// =============================================================================
@Composable
private fun KanbanAndEisenhowerPage(
    spec: WearAdaptiveSpec,
    boardMode: TaskBoardMode,
    selectedKanbanStage: KanbanColumnStage,
    tasks: List<LifeTask>,
    metaForTask: (LifeTask) -> PlannerItemRichMeta,
    itemFillProgress: (String, Boolean) -> Float,
    onSelectBoardMode: (TaskBoardMode) -> Unit,
    onSelectKanbanStage: (KanbanColumnStage) -> Unit,
    onAdvanceKanbanStage: (LifeTask) -> Unit,
    onCycleQuadrantPriority: (LifeTask) -> Unit,
    onOpenNewTask: () -> Unit,
    onJumpToBirthdaysAndAlarms: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onOpenNewTask,
                label = "New Task",
                icon = Icons.Rounded.Add
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = if (boardMode == TaskBoardMode.KANBAN) "KANBAN BOARD" else "EISENHOWER 2x2",
                    icon = Icons.Rounded.ViewKanban,
                    badgeText = "${tasks.size} TASKS",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Switch between Kanban Board & Eisenhower Matrix
            item {
                WearActionButtonGroup(
                    options = TaskBoardMode.entries.map { it.label },
                    selectedIndex = boardMode.ordinal,
                    onOptionSelected = { idx -> onSelectBoardMode(TaskBoardMode.entries[idx]) }
                )
            }

            if (boardMode == TaskBoardMode.KANBAN) {
                // Authentic Color-Coded Kanban Stage Selector Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        KanbanColumnStage.entries.forEach { stage ->
                            val cnt = tasks.count { metaForTask(it).kanbanStage == stage }
                            val isSelected = selectedKanbanStage == stage
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) stage.color.copy(alpha = 0.28f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.dp,
                                        color = if (isSelected) stage.color else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectKanbanStage(stage) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$cnt",
                                    style = MaterialTheme.typography.labelMedium.tabularNums(),
                                    fontWeight = FontWeight.Black,
                                    color = stage.color
                                )
                                Text(
                                    text = stage.shortLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                val stageTasks = tasks.filter { metaForTask(it).kanbanStage == selectedKanbanStage }
                item {
                    CatalogM3ListHeaderSample(title = "${selectedKanbanStage.label.uppercase()} (${stageTasks.size})")
                }

                items(stageTasks.size) { idx ->
                    val t = stageTasks[idx]
                    val meta = metaForTask(t)
                    val prog = itemFillProgress(t.stamp.id, t.complete)

                    Card(
                        onClick = { onAdvanceKanbanStage(t) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawWithContent {
                                    drawRoundRect(
                                        color = selectedKanbanStage.color.copy(alpha = 0.22f),
                                        size = Size(size.width * prog, size.height),
                                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                                    )
                                    drawContent()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${meta.tagLabel} · P${meta.priorityLevel} · ${(prog * 100).roundToInt()}% · Tap to move ->",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Rounded.ArrowForward, contentDescription = "Next Stage", tint = selectedKanbanStage.color, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                // Authentic 2x2 Color-Coded Eisenhower Matrix Grid View
                val q1 = tasks.filter { metaForTask(it).priorityLevel == 1 }
                val q2 = tasks.filter { metaForTask(it).priorityLevel == 2 }
                val q3 = tasks.filter { metaForTask(it).priorityLevel == 3 }
                val q4 = tasks.filter { metaForTask(it).priorityLevel == 4 }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        EisenhowerQuadrantTile(
                            modifier = Modifier.weight(1f),
                            title = "Q1 · DO FIRST",
                            subtitle = "Urgent & Imp",
                            tasks = q1,
                            accent = Color(0xFFFF7A9A),
                            onCycleTask = onCycleQuadrantPriority
                        )
                        EisenhowerQuadrantTile(
                            modifier = Modifier.weight(1f),
                            title = "Q2 · SCHEDULE",
                            subtitle = "Important",
                            tasks = q2,
                            accent = Color(0xFF79E2B0),
                            onCycleTask = onCycleQuadrantPriority
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        EisenhowerQuadrantTile(
                            modifier = Modifier.weight(1f),
                            title = "Q3 · DELEGATE",
                            subtitle = "Urgent Only",
                            tasks = q3,
                            accent = Color(0xFFFFD177),
                            onCycleTask = onCycleQuadrantPriority
                        )
                        EisenhowerQuadrantTile(
                            modifier = Modifier.weight(1f),
                            title = "Q4 · ELIMINATE",
                            subtitle = "Low Value",
                            tasks = q4,
                            accent = Color(0xFF8EAEFF),
                            onCycleTask = onCycleQuadrantPriority
                        )
                    }
                }
            }

            item {
                CatalogM3ChildButtonSample(
                    label = "Birthdays, Calls & Alarms",
                    onClick = onJumpToBirthdaysAndAlarms
                )
            }
        }
    }
}

@Composable
private fun EisenhowerQuadrantTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    tasks: List<LifeTask>,
    accent: Color,
    onCycleTask: (LifeTask) -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = accent, fontWeight = FontWeight.Black, maxLines = 1)
        Text("${tasks.size} items · $subtitle", style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        tasks.take(3).forEach { t ->
            Text(
                text = "• ${t.title}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onCycleTask(t) }
            )
        }
    }
}

// =============================================================================
// PAGE 4: BIRTHDAYS, CUSTOM COUNTDOWN LISTS, VIDEO/AUDIO CALLS & ALARMS STUDIO
// =============================================================================
@Composable
private fun BirthdaysCallsAndAlarmsPage(
    spec: WearAdaptiveSpec,
    today: LocalDate,
    importantDates: List<ImportantDate>,
    customCountdownLists: List<String>,
    selectedCountdownList: String,
    callEvents: List<com.creativeblisslabs.wearproductivity.shared.CalendarEvent>,
    alarms: List<LifeAlarm>,
    metaForDate: (ImportantDate) -> PlannerItemRichMeta,
    metaForEvent: (String, String, String) -> PlannerItemRichMeta,
    metaMap: Map<String, PlannerItemRichMeta>,
    showAlarmCreator: Boolean,
    alarmDraftHour: Int,
    alarmDraftMinute: Int,
    alarmDraftTitle: String,
    showNewListCreator: Boolean,
    onSelectCountdownList: (String) -> Unit,
    onToggleNewListCreator: () -> Unit,
    onSaveNewCountdownList: (String, String, String) -> Unit,
    onOpenAddBirthdayOrCall: (PlannerItemKind) -> Unit,
    onDeleteImportantDate: (String) -> Unit,
    onToggleAlarmCreator: () -> Unit,
    onShiftAlarmTime: (Int) -> Unit,
    onSaveQuickAlarm: (String, String, String) -> Unit,
    onToggleAlarm: (String) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onJumpToTop: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    val filteredDates = remember(importantDates, selectedCountdownList) {
        if (selectedCountdownList == "All") importantDates
        else importantDates.filter {
            it.personName.equals(selectedCountdownList, true) || it.notes.contains(selectedCountdownList, true)
        }
    }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToTop,
                label = "Spotlight Ring",
                icon = Icons.Rounded.KeyboardArrowUp
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "TIMERS & ALARMS",
                    icon = Icons.Rounded.Cake,
                    badgeText = "${importantDates.size} DATES · ${alarms.size} ALM",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. CUSTOM NAMED COUNTDOWN & BIRTHDAY LISTS
            item {
                CatalogM3ListHeaderSample(title = "BIRTHDAY & COUNTDOWN LISTS")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (listOf("All") + customCountdownLists).forEach { listName ->
                        val isSel = selectedCountdownList == listName
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSel) Color(0xFFFF9A7A) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onSelectCountdownList(listName) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = listName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    SmallCircleIconBtn(icon = Icons.Rounded.Add, onClick = onToggleNewListCreator)
                }
            }

            if (showNewListCreator) {
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "BIRTHDAY LIST",
                        initialTitle = "",
                        initialTimeOrDate = today.plusDays(10).toString(),
                        initialSubtitle = "Person Birthday Name",
                        isEditingExisting = false,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Date (YYYY-MM-DD)",
                        subtitleFieldLabel = "Birthday / Milestone Title",
                        onSave = { listName, dateIso, bdayTitle ->
                            onSaveNewCountdownList(listName, bdayTitle, dateIso)
                        },
                        onCancel = onToggleNewListCreator,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(filteredDates.size) { idx ->
                val d = filteredDates[idx]
                val nextTarget = nextImportantDate(d.dateIso, d.category.name)
                val countdownStr = nextTarget?.let { countdownLabel(it, today) } ?: d.dateIso

                WearSwipeableActionCard(
                    onCardClick = {},
                    onEditClick = { onOpenAddBirthdayOrCall(PlannerItemKind.BIRTHDAY) },
                    onDeleteClick = { onDeleteImportantDate(d.stamp.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Cake, contentDescription = null, tint = Color(0xFFFF9A7A), modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${d.personName} · ${d.dateIso}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        WearStatusBadge(text = countdownStr)
                    }
                }
            }

            // 2. SCHEDULED VIDEO & AUDIO CALLS WITH LOCATIONS
            item {
                CatalogM3ListHeaderSample(title = "SCHEDULED VIDEO & AUDIO CALLS")
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onOpenAddBirthdayOrCall(PlannerItemKind.VIDEO_CALL) },
                        icon = { Icon(Icons.Rounded.Videocam, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Video Call")
                    }
                    FilledTonalButton(
                        onClick = { onOpenAddBirthdayOrCall(PlannerItemKind.AUDIO_CALL) },
                        icon = { Icon(Icons.Rounded.Call, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Audio Call")
                    }
                }
            }

            items(callEvents.size) { idx ->
                val callEv = callEvents[idx]
                val meta = metaForEvent(callEv.stamp.id, callEv.title, callEv.location)
                WearMetricCard(
                    title = "${meta.kind.badge} · ${displayEventTime(callEv.timeRange)}",
                    value = callEv.title,
                    subtitle = "Location: ${callEv.location.ifBlank { "Google Meet" }}",
                    icon = resolvePlannerIcon(meta.iconKey, meta.kind),
                    onClick = {}
                )
            }

            // 3. ALARMS STUDIO WITH BULLET POINTS & WEEKDAY REPEATS
            item {
                CatalogM3ListHeaderSample(title = "ALARMS STUDIO (${alarms.size})")
            }

            item {
                FilledTonalButton(
                    onClick = onToggleAlarmCreator,
                    icon = { Icon(if (showAlarmCreator) Icons.Rounded.Close else Icons.Rounded.AlarmAdd, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showAlarmCreator) "Close Alarm Creator" else "New Alarm with Bullet Notes")
                }
            }

            if (showAlarmCreator) {
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "ALARM",
                        initialTitle = alarmDraftTitle,
                        initialTimeOrDate = String.format(Locale.ROOT, "%02d:%02d", alarmDraftHour, alarmDraftMinute),
                        initialSubtitle = "Hydrate • Review Top 3 Tasks",
                        isEditingExisting = false,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Alarm Time (HH:mm)",
                        subtitleFieldLabel = "Bullet Points (Separate with •)",
                        onSave = { title, timeStr, bullets ->
                            onSaveQuickAlarm(title, timeStr, bullets)
                        },
                        onCancel = onToggleAlarmCreator,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(alarms.size) { idx ->
                val alarm = alarms[idx]
                val meta = metaMap[alarm.stamp.id]
                WearSwipeableActionCard(
                    onCardClick = { onToggleAlarm(alarm.stamp.id) },
                    onEditClick = { onToggleAlarm(alarm.stamp.id) },
                    onDeleteClick = { onDeleteAlarm(alarm.stamp.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SplitSwitchButton(
                            checked = alarm.enabled,
                            onCheckedChange = { onToggleAlarm(alarm.stamp.id) },
                            onContainerClick = { onToggleAlarm(alarm.stamp.id) },
                            toggleContentDescription = alarm.label,
                            label = { Text("${alarm.time} · ${alarm.label}", fontWeight = FontWeight.Bold, maxLines = 1) },
                            secondaryLabel = { Text(alarm.repeat, maxLines = 1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        meta?.bulletNotes?.take(2)?.forEach { b ->
                            Text(
                                text = "• $b",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = Color(0xFFFFD177),
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SNOOZE & STOP / RESCHEDULE MODAL OVERLAY
// =============================================================================
@Composable
private fun PlannerSnoozeAndStopModal(
    customSnoozeMinutes: Int,
    onAdjustSnooze: (Int) -> Unit,
    onConfirmSnooze: (Int) -> Unit,
    onStopForTodayOnly: () -> Unit,
    onRescheduleToTomorrow: () -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onDismiss,
                label = "Close",
                icon = Icons.Rounded.Close
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 14.dp,
                end = 14.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(title = "SNOOZE OR STOP", badgeText = "${customSnoozeMinutes}M")
            }
            item {
                WearExpressiveHorizontalSlider(
                    title = "Snooze Duration",
                    valueText = "$customSnoozeMinutes min",
                    onValueDecrease = { onAdjustSnooze(-5) },
                    onValueIncrease = { onAdjustSnooze(5) },
                    canDecrease = customSnoozeMinutes > 5,
                    canIncrease = customSnoozeMinutes < 240
                )
            }
            item {
                Button(
                    onClick = { onConfirmSnooze(customSnoozeMinutes) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Snooze ${customSnoozeMinutes}m")
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(10, 30, 60).forEach { m ->
                        FilledTonalButton(
                            onClick = { onConfirmSnooze(m) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("${m}m", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onStopForTodayOnly,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop for Today Only")
                }
            }
            item {
                FilledTonalButton(
                    onClick = onRescheduleToTomorrow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Move to Tomorrow")
                }
            }
        }
    }
}

@Composable
private fun SmallCircleIconBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED VIEWPORT UTILITIES
// =============================================================================
@Composable
private fun ViewportConstrainedCalendarConfirmation(
    label: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

    LaunchedEffect(label) {
        delay(1_800L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        CurvedLayout(modifier = Modifier.fillMaxSize()) {
            confirmationDialogCurvedText(text = label, style = curvedTextStyle)
        }
        if (isError) {
            ConfirmationDialogDefaults.FailureIcon()
        } else {
            ConfirmationDialogDefaults.SuccessIcon()
        }
    }
}

@Composable
private fun WearBaselineViewport(
    baselineWatchSize: Dp = 416.dp,
    content: @Composable (WearAdaptiveSpec) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val isPhonePreview = this.maxWidth > 280.dp || this.maxHeight > 280.dp
        val viewportWidth = if (isPhonePreview) baselineWatchSize else this.maxWidth
        val viewportHeight = if (isPhonePreview) baselineWatchSize else this.maxHeight

        val spec = remember(viewportWidth, viewportHeight) {
            WearAdaptiveSpec(
                screenWidth = viewportWidth,
                screenHeight = viewportHeight,
                isLargeScreen = viewportWidth >= 225.dp,
                outerHorizontalMargin = viewportWidth * 0.052f,
                topBottomSlotHorizontalMargin = viewportWidth * 0.14f,
                topSlotPadding = viewportHeight * 0.11f,
                bottomEdgeButtonPadding = viewportHeight * 0.22f
            )
        }

        if (isPhonePreview) {
            Box(
                modifier = Modifier
                    .size(baselineWatchSize)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            ) {
                content(spec)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                content(spec)
            }
        }
    }
}

@Composable
private fun VerticalPagerIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
            .padding(vertical = 5.dp, horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotHeight by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 4.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "pagerDotHeight"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dotHeight)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

// =============================================================================
// DATE & RECURRENCE MATCHING UTILITIES
// =============================================================================
private fun taskMatchesDate(label: String, meta: PlannerItemRichMeta, date: LocalDate, today: LocalDate): Boolean {
    when (meta.recurrence) {
        PlannerRecurrenceMode.DAILY -> return true
        PlannerRecurrenceMode.WEEKDAYS -> return date.dayOfWeek in weekdays
        PlannerRecurrenceMode.MONTHLY -> return date.dayOfMonth == today.dayOfMonth
        PlannerRecurrenceMode.YEARLY -> return date.month == today.month && date.dayOfMonth == today.dayOfMonth
        else -> {}
    }
    if (label.isBlank() || label.equals("Today", true)) return date == today
    if (label.equals("Tomorrow", true)) return date == today.plusDays(1)
    val parsedIso = runCatching { LocalDate.parse(label.trim()) }.getOrNull()
    if (parsedIso != null) return parsedIso == date
    return date == today
}

private fun eventMatchesDate(timeRange: String, meta: PlannerItemRichMeta, date: LocalDate, today: LocalDate): Boolean {
    when (meta.recurrence) {
        PlannerRecurrenceMode.DAILY -> return true
        PlannerRecurrenceMode.WEEKDAYS -> return date.dayOfWeek in weekdays
        PlannerRecurrenceMode.MONTHLY -> return date.dayOfMonth == today.dayOfMonth
        PlannerRecurrenceMode.YEARLY -> return date.month == today.month && date.dayOfMonth == today.dayOfMonth
        else -> {}
    }
    val iso = Regex("""\b\d{4}-\d{2}-\d{2}\b""").find(timeRange)?.value
    if (iso != null) {
        val parsed = runCatching { LocalDate.parse(iso) }.getOrNull()
        if (parsed != null) return parsed == date
    }
    return date == today
}

private fun extractClockTime(value: String): String? =
    Regex("""\b\d{1,2}:\d{2}\b""").find(value)?.value

private fun displayEventTime(value: String): String =
    value.trim().ifEmpty { "All Day" }

private fun nextImportantDate(dateIso: String, category: String): LocalDate? {
    val original = runCatching { LocalDate.parse(dateIso.trim()) }.getOrNull() ?: return null
    val today = LocalDate.now()
    var next = runCatching { original.withYear(today.year) }.getOrElse { return original }
    if (next.isBefore(today)) next = runCatching { next.withYear(today.year + 1) }.getOrElse { next }
    return next
}

private fun countdownLabel(target: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)
    return when (days) {
        0L -> "TODAY"
        1L -> "1D LEFT"
        else -> "IN ${days}D"
    }
}

private fun alarmRepeatOccursOn(repeat: String, date: LocalDate, today: LocalDate): Boolean {
    val normal = repeat.lowercase(Locale.getDefault()).trim()
    if (normal.isBlank() || normal.contains("once")) return date == today
    if (normal.contains("daily") || normal.contains("every")) return true
    if (normal.contains("weekday") || normal == "m-f") return date.dayOfWeek in weekdays
    if (normal.contains("weekend")) return date.dayOfWeek in weekends
    return true
}

private val weekdays = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)
private val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private fun TextStyle.tabularNums(): TextStyle = this.copy(
    fontFeatureSettings = "tnum",
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

private fun TextStyle.compactLineHeight(): TextStyle = this.copy(
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)