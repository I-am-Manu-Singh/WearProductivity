package com.creativeblisslabs.wearproductivity.wear.ui.habits

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
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
import com.creativeblisslabs.wearproductivity.shared.Habit
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.RoutinePhase
import com.creativeblisslabs.wearproductivity.shared.RoutineRecurrence
import com.creativeblisslabs.wearproductivity.shared.RoutineTask
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class HabitFilter(val label: String) {
    ALL("All"), TODO("To do"), DONE("Done")
}

private enum class HorizonViewMode(val label: String) {
    DAILY("Today"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class HabitCompletionMode(val label: String) {
    SINGLE_TAP("Tap Done"),
    MULTI_TAP_AMOUNT("Multi-Tap %"),
    TIMED_SESSION("Timer"),
    MOOD_REFLECTION("Mood & Log")
}

enum class TimetableSlotStatus(val label: String, val badge: String, val color: Color) {
    PENDING("Pending", "TODO", Color(0xFF8EAEFF)),
    DOING("Doing Now", "LIVE", Color(0xFF79E2B0)),
    DONE("Completed", "DONE", Color(0xFF4CAF50)),
    SKIPPED("Skipped", "SKIP", Color(0xFFFFD177)),
    NOT_DOING("Not Doing", "DROP", Color(0xFFFF7A9A)),
    DELEGATED("Delegated", "DELG", Color(0xFFBBA5FF)),
    BACKLOG("Next Day Backlog", "BKLOG", Color(0xFFFF9A7A))
}

private enum class ActiveControlModal {
    NONE,
    SNOOZE_PICKER,
    STOP_AND_RESCHEDULE,
    MOOD_SATISFACTION_LOGGER
}

data class HabitExtendedMeta(
    val habitId: String,
    val tag: String = "Core",
    val iconKey: String = "flame",
    val colorKey: String = "mint",
    val mode: HabitCompletionMode = HabitCompletionMode.MULTI_TAP_AMOUNT,
    val stepIncrement: Double = 1.0,
    val reminderTime: String = "08:00",
    val reminderEnabled: Boolean = true,
    val snoozedUntilEpochMs: Long = 0L,
    val stoppedForDateIso: String = "",
    val lastMood: String = "Focused",
    val satisfactionStars: Int = 5,
    val historyBitmask28: Int = 0b1011101101110111011011101111
)

data class TimetableSlotMeta(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val estimatedMinutes: Int,
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val status: TimetableSlotStatus = TimetableSlotStatus.PENDING,
    val dateIso: String = LocalDate.now().toString(),
    val categoryTag: String = "Deep Work",
    val orderIndex: Int = 0,
    val snoozedMinutes: Int = 0
)

// =============================================================================
// PALETTE, ICON & NOTIFICATION HELPERS
// =============================================================================
private val habitColorMap = mapOf(
    "mint" to Color(0xFF79E2B0),
    "blue" to Color(0xFF68B6FF),
    "coral" to Color(0xFFFF9A7A),
    "lavender" to Color(0xFFBBA5FF),
    "amber" to Color(0xFFFFD177),
    "rose" to Color(0xFFFF7A9A)
)

private val habitIconKeys = listOf("flame", "water", "run", "book", "code", "meditate", "music", "gym")

private fun resolveHabitColor(key: String): Color =
    habitColorMap[key.lowercase(Locale.ROOT)] ?: Color(0xFF79E2B0)

private fun resolveHabitIcon(key: String, habitName: String = ""): ImageVector = when {
    key == "water" || habitName.contains("water", true) -> Icons.Rounded.WaterDrop
    key == "run" || habitName.contains("walk", true) || habitName.contains("run", true) -> Icons.Rounded.DirectionsRun
    key == "book" || habitName.contains("read", true) -> Icons.Rounded.MenuBook
    key == "code" || habitName.contains("code", true) -> Icons.Rounded.Code
    key == "meditate" || habitName.contains("mind", true) || habitName.contains("nitnem", true) -> Icons.Rounded.SelfImprovement
    key == "music" || habitName.contains("piano", true) || habitName.contains("guitar", true) -> Icons.Rounded.MusicNote
    key == "gym" || habitName.contains("workout", true) -> Icons.Rounded.FitnessCenter
    else -> Icons.Rounded.LocalFireDepartment
}

private object HabitReminderNotifier {
    private const val CHANNEL_ID = "lifeos_habits_timetable_alerts"

    fun fireReminder(context: Context, title: String, subtitle: String) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Habits & Timetable Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        }
    }
}

// =============================================================================
// PERSISTENCE MANAGER FOR EXTENDED HABIT METADATA & MINUTE-BY-MINUTE TIMETABLE
// =============================================================================
private object HabitsTimetableStudioPrefs {
    private const val PREFS_NAME = "lifeos_habits_timetable_studio_v3"
    private const val KEY_HABIT_META = "habit_extended_meta_v3"
    private const val KEY_TIMETABLE_SLOTS = "timetable_slots_v3"
    private const val KEY_SAME_EVERY_DAY = "timetable_same_every_day"

    fun loadHabitMetaMap(context: Context): Map<String, HabitExtendedMeta> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_HABIT_META, null)
            ?: return emptyMap()
        return raw.split(";;").mapNotNull { entry ->
            val p = entry.split("|")
            if (p.size >= 12) {
                val meta = HabitExtendedMeta(
                    habitId = p[0],
                    tag = p[1],
                    iconKey = p[2],
                    colorKey = p[3],
                    mode = runCatching { HabitCompletionMode.valueOf(p[4]) }.getOrDefault(HabitCompletionMode.MULTI_TAP_AMOUNT),
                    stepIncrement = p[5].toDoubleOrNull() ?: 1.0,
                    reminderTime = p[6],
                    reminderEnabled = p[7].toBoolean(),
                    snoozedUntilEpochMs = p[8].toLongOrNull() ?: 0L,
                    stoppedForDateIso = p[9],
                    lastMood = p[10],
                    satisfactionStars = p[11].toIntOrNull() ?: 5,
                    historyBitmask28 = p.getOrNull(12)?.toIntOrNull() ?: 0b1011101101110111011011101111
                )
                meta.habitId to meta
            } else null
        }.toMap()
    }

    fun saveHabitMetaMap(context: Context, map: Map<String, HabitExtendedMeta>) {
        val encoded = map.values.joinToString(";;") {
            "${it.habitId}|${it.tag.replace("|", "")}|${it.iconKey}|${it.colorKey}|${it.mode.name}|${it.stepIncrement}|${it.reminderTime}|${it.reminderEnabled}|${it.snoozedUntilEpochMs}|${it.stoppedForDateIso}|${it.lastMood.replace("|", "")}|${it.satisfactionStars}|${it.historyBitmask28}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_HABIT_META, encoded).apply()
    }

    fun loadSameEveryDay(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SAME_EVERY_DAY, true)

    fun saveSameEveryDay(context: Context, same: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SAME_EVERY_DAY, same).apply()
    }

    fun loadTimetableSlots(context: Context, repoRoutines: List<RoutineTask>): List<TimetableSlotMeta> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TIMETABLE_SLOTS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { entry ->
                val p = entry.split("|")
                if (p.size >= 11) {
                    TimetableSlotMeta(
                        id = p[0],
                        title = p[1],
                        startTime = p[2],
                        endTime = p[3],
                        estimatedMinutes = p[4].toIntOrNull() ?: 30,
                        elapsedSeconds = p[5].toIntOrNull() ?: 0,
                        isTimerRunning = false,
                        status = runCatching { TimetableSlotStatus.valueOf(p[6]) }.getOrDefault(TimetableSlotStatus.PENDING),
                        dateIso = p[7],
                        categoryTag = p[8],
                        orderIndex = p[9].toIntOrNull() ?: 0,
                        snoozedMinutes = p[10].toIntOrNull() ?: 0
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed.sortedBy { it.orderIndex }
        }

        val todayIso = LocalDate.now().toString()
        val seeded = if (repoRoutines.isNotEmpty()) {
            repoRoutines.mapIndexed { idx, r ->
                TimetableSlotMeta(
                    id = r.stamp.id,
                    title = r.title,
                    startTime = r.startTime,
                    endTime = r.endTime,
                    estimatedMinutes = computeDurationMinutes(r.startTime, r.endTime),
                    status = if (r.doneToday) TimetableSlotStatus.DONE else TimetableSlotStatus.PENDING,
                    dateIso = todayIso,
                    categoryTag = r.phase.name,
                    orderIndex = idx
                )
            }
        } else {
            listOf(
                TimetableSlotMeta("tt-1", "Morning Hydration & Mobility", "06:30", "07:00", 30, status = TimetableSlotStatus.DONE, dateIso = todayIso, categoryTag = "Wellness", orderIndex = 0),
                TimetableSlotMeta("tt-2", "Strength / Cardio Session", "07:00", "08:00", 60, status = TimetableSlotStatus.DOING, dateIso = todayIso, categoryTag = "Fitness", orderIndex = 1),
                TimetableSlotMeta("tt-3", "Deep Architecture & Kotlin", "09:00", "11:30", 150, status = TimetableSlotStatus.PENDING, dateIso = todayIso, categoryTag = "Deep Work", orderIndex = 2),
                TimetableSlotMeta("tt-4", "Piano & Music Arrangement", "18:00", "19:00", 60, status = TimetableSlotStatus.PENDING, dateIso = todayIso, categoryTag = "Music", orderIndex = 3),
                TimetableSlotMeta("tt-5", "Evening Path & Reflection", "21:00", "21:30", 30, status = TimetableSlotStatus.PENDING, dateIso = todayIso, categoryTag = "Mindfulness", orderIndex = 4)
            )
        }
        return seeded
    }

    fun saveTimetableSlots(context: Context, list: List<TimetableSlotMeta>) {
        val encoded = list.joinToString(";;") {
            "${it.id}|${it.title.replace("|", "")}|${it.startTime}|${it.endTime}|${it.estimatedMinutes}|${it.elapsedSeconds}|${it.status.name}|${it.dateIso}|${it.categoryTag.replace("|", "")}|${it.orderIndex}|${it.snoozedMinutes}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TIMETABLE_SLOTS, encoded).apply()
    }
}

private fun computeDurationMinutes(start: String, end: String): Int {
    fun toMins(s: String): Int {
        val parts = s.trim().split(":")
        val h = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        return (h * 60) + m
    }
    val diff = toMins(end) - toMins(start)
    return if (diff > 0) diff else 30
}

// =============================================================================
// MAIN ENTRY COMPOSABLE
// =============================================================================
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearHabitsScreen(state: LifeState, repository: LifeRepository) {
    val context = LocalContext.current.applicationContext
    val haptic = rememberLifeOSHapticProvider()
    val coroutineScope = rememberCoroutineScope()
    val liveState by repository.state.collectAsState()
    val habits = liveState.activeHabits

    // Extended Habit Metadata Map
    val habitMetaMap = remember {
        mutableStateMapOf<String, HabitExtendedMeta>().apply {
            putAll(HabitsTimetableStudioPrefs.loadHabitMetaMap(context))
        }
    }

    fun metaFor(habit: Habit): HabitExtendedMeta {
        return habitMetaMap.getOrPut(habit.stamp.id) {
            val isWater = habit.unit.equals("ml", true) || habit.name.contains("water", true)
            val isSteps = habit.unit.equals("steps", true) || habit.name.contains("walk", true)
            HabitExtendedMeta(
                habitId = habit.stamp.id,
                tag = if (isWater || isSteps) "Health" else "Growth",
                iconKey = when {
                    isWater -> "water"
                    isSteps -> "run"
                    habit.name.contains("read", true) -> "book"
                    else -> "flame"
                },
                colorKey = habit.colorName.ifBlank { "mint" },
                mode = when {
                    habit.remainingSeconds > 0 -> HabitCompletionMode.TIMED_SESSION
                    habit.targetValue > 1.0 -> HabitCompletionMode.MULTI_TAP_AMOUNT
                    else -> HabitCompletionMode.SINGLE_TAP
                },
                stepIncrement = when {
                    isWater -> 250.0
                    isSteps -> 1000.0
                    habit.targetValue >= 10.0 -> (habit.targetValue / 4.0).coerceAtLeast(1.0)
                    else -> 1.0
                }
            )
        }
    }

    fun persistMeta() {
        HabitsTimetableStudioPrefs.saveHabitMetaMap(context, habitMetaMap.toMap())
    }

    // Timetable Slots & Recurrence State
    var sameTimetableEveryDay by remember { mutableStateOf(HabitsTimetableStudioPrefs.loadSameEveryDay(context)) }
    val timetableSlots = remember {
        mutableStateListOf<TimetableSlotMeta>().apply {
            addAll(HabitsTimetableStudioPrefs.loadTimetableSlots(context, liveState.activeRoutines))
        }
    }

    fun persistTimetable() {
        HabitsTimetableStudioPrefs.saveTimetableSlots(context, timetableSlots)
    }

    // Date & Horizon Selection State (Today, Weekly, Monthly + Date Chip Bar)
    var horizonMode by remember { mutableStateOf(HorizonViewMode.DAILY) }
    var selectedDateOffsetDays by remember { mutableIntStateOf(0) } // -3 .. +6 days
    val selectedDate = remember(selectedDateOffsetDays) { LocalDate.now().plusDays(selectedDateOffsetDays.toLong()) }
    val selectedDateIso = selectedDate.toString()

    // Active Filter & Staggered Card Deck Index
    var selectedFilter by remember { mutableStateOf(HabitFilter.ALL) }
    var staggeredTopIndex by remember { mutableIntStateOf(0) }
    var manageMode by remember { mutableStateOf(false) }

    // Universal Editor States (Habits & Timetable)
    var showHabitEditor by remember { mutableStateOf(false) }
    var editingHabitId by remember { mutableStateOf<String?>(null) }
    var draftName by remember { mutableStateOf("") }
    var draftTargetValue by remember { mutableStateOf("4.0") }
    var draftUnitAndStep by remember { mutableStateOf("cups · +1") }
    var draftColorKey by remember { mutableStateOf("mint") }
    var draftIconKey by remember { mutableStateOf("flame") }
    var draftTag by remember { mutableStateOf("Daily") }
    var draftReminderTime by remember { mutableStateOf("08:00") }
    var draftCompletionMode by remember { mutableStateOf(HabitCompletionMode.MULTI_TAP_AMOUNT) }

    var showTimetableEditor by remember { mutableStateOf(false) }
    var editingSlotId by remember { mutableStateOf<String?>(null) }
    var draftSlotTitle by remember { mutableStateOf("") }
    var draftSlotTimeRange by remember { mutableStateOf("09:00-10:00") }
    var draftSlotCategoryAndEst by remember { mutableStateOf("Deep Work · 60m") }

    // Active Control Modal (Snooze Picker, Stop/Reschedule, Mood/Satisfaction Logger)
    var activeModal by remember { mutableStateOf(ActiveControlModal.NONE) }
    var modalTargetHabitId by remember { mutableStateOf<String?>(null) }
    var modalTargetSlotId by remember { mutableStateOf<String?>(null) }
    var customSnoozeMinutes by remember { mutableIntStateOf(15) }

    var feedbackOverlay by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // 1-Second Live Ticker for Active Habit Timers AND Active Timetable Slot Timers
    val activeHabitTimerIds = habits.filter { it.isTimerActive && it.remainingSeconds > 0 }.map { it.stamp.id }
    val activeSlotTimerIds = timetableSlots.filter { it.isTimerRunning }.map { it.id }

    LaunchedEffect(activeHabitTimerIds, activeSlotTimerIds) {
        while (activeHabitTimerIds.isNotEmpty() || activeSlotTimerIds.isNotEmpty()) {
            delay(1_000L)
            if (activeHabitTimerIds.isNotEmpty()) {
                repository.tickHabitTimer(1)
            }
            if (activeSlotTimerIds.isNotEmpty()) {
                for (i in timetableSlots.indices) {
                    val slot = timetableSlots[i]
                    if (slot.isTimerRunning) {
                        val nextElapsed = slot.elapsedSeconds + 1
                        val targetSecs = (slot.estimatedMinutes.coerceAtLeast(1) * 60)
                        if (nextElapsed >= targetSecs) {
                            timetableSlots[i] = slot.copy(
                                elapsedSeconds = targetSecs,
                                isTimerRunning = false,
                                status = TimetableSlotStatus.DONE
                            )
                            HabitReminderNotifier.fireReminder(context, "Timetable Block Complete", slot.title)
                            feedbackOverlay = "${slot.title} Complete!" to false
                        } else {
                            timetableSlots[i] = slot.copy(elapsedSeconds = nextElapsed)
                        }
                    }
                }
            }
        }
    }

    // Filtered Habits for Selected Date (Excluding habits stopped for today)
    val visibleHabitsForDate = remember(habits, habitMetaMap.toMap(), selectedDateIso) {
        habits.filter { h ->
            val meta = habitMetaMap[h.stamp.id]
            meta?.stoppedForDateIso != selectedDateIso
        }
    }

    val filteredHabits = remember(visibleHabitsForDate, selectedFilter) {
        when (selectedFilter) {
            HabitFilter.ALL -> visibleHabitsForDate
            HabitFilter.TODO -> visibleHabitsForDate.filterNot { it.doneToday }
            HabitFilter.DONE -> visibleHabitsForDate.filter { it.doneToday }
        }
    }

    // Filtered Timetable Slots for Selected Date (or all if sameEveryDay is enabled)
    val visibleTimetableSlots = remember(timetableSlots.toList(), sameTimetableEveryDay, selectedDateIso) {
        val list = if (sameTimetableEveryDay) {
            timetableSlots
        } else {
            timetableSlots.filter { it.dateIso == selectedDateIso }
        }
        list.sortedBy { it.orderIndex }
    }

    // Composite Daily Score & Metrics Calculation
    val completedHabitsCount = visibleHabitsForDate.count { it.doneToday }
    val totalHabitsCount = visibleHabitsForDate.size
    val habitsProgress = if (totalHabitsCount > 0) {
        visibleHabitsForDate.sumOf { habitProgress(it).toDouble() }.toFloat() / totalHabitsCount.toFloat()
    } else 0f

    val doneSlotsCount = visibleTimetableSlots.count { it.status == TimetableSlotStatus.DONE }
    val actionableSlotsCount = visibleTimetableSlots.count {
        it.status != TimetableSlotStatus.SKIPPED && it.status != TimetableSlotStatus.NOT_DOING
    }.coerceAtLeast(1)
    val timetableProgress = (doneSlotsCount.toFloat() / actionableSlotsCount.toFloat()).coerceIn(0f, 1f)

    val overallDailyScore = remember(habitsProgress, timetableProgress, visibleHabitsForDate.size, visibleTimetableSlots.size) {
        val raw = if (visibleHabitsForDate.isNotEmpty() && visibleTimetableSlots.isNotEmpty()) {
            (habitsProgress * 0.55f) + (timetableProgress * 0.45f)
        } else if (visibleHabitsForDate.isNotEmpty()) {
            habitsProgress
        } else {
            timetableProgress
        }
        (raw * 100f).roundToInt().coerceIn(0, 100)
    }

    // Spotlight Active / Upcoming Habit & Timetable Item for Page 0 Full-Screen Canvas
    val spotlightHabit = visibleHabitsForDate.firstOrNull { it.isTimerActive }
        ?: visibleHabitsForDate.firstOrNull { !it.doneToday }
        ?: visibleHabitsForDate.firstOrNull()

    val spotlightSlot = visibleTimetableSlots.firstOrNull { it.isTimerRunning || it.status == TimetableSlotStatus.DOING }
        ?: visibleTimetableSlots.firstOrNull { it.status == TimetableSlotStatus.PENDING }
        ?: visibleTimetableSlots.firstOrNull()

    // =========================================================================
    // HABIT & TIMETABLE INTERACTION HANDLERS
    // =========================================================================
    fun advanceHabitProgress(habit: Habit, customIncrement: Double? = null) {
        haptic.click()
        val meta = metaFor(habit)
        val step = customIncrement ?: meta.stepIncrement.coerceAtLeast(1.0)
        coroutineScope.launch {
            if (habit.targetValue <= 1.0 || meta.mode == HabitCompletionMode.SINGLE_TAP) {
                repository.checkHabit(habit.stamp.id)
                feedbackOverlay = (if (!habit.doneToday) "${habit.name} 100% Done!" else "${habit.name} Unchecked") to false
            } else {
                val nextVal = if (habit.doneToday && habit.currentValue >= habit.targetValue) {
                    0.0
                } else {
                    (habit.currentValue + step).coerceAtMost(habit.targetValue)
                }
                repository.updateHabitProgress(habit.stamp.id, nextVal)
                val pct = ((nextVal / habit.targetValue.coerceAtLeast(1.0)) * 100).roundToInt()
                feedbackOverlay = "${habit.name}: $pct% (${displayNumber(nextVal)}/${displayNumber(habit.targetValue)} ${habit.unit})" to false
            }
        }
    }

    fun openNewHabitEditor() {
        haptic.click()
        editingHabitId = null
        draftName = ""
        draftTargetValue = "4.0"
        draftUnitAndStep = "cups · +1"
        draftColorKey = "mint"
        draftIconKey = "flame"
        draftTag = "Daily"
        draftReminderTime = "08:00"
        draftCompletionMode = HabitCompletionMode.MULTI_TAP_AMOUNT
        showHabitEditor = true
    }

    fun openEditHabitEditor(habit: Habit) {
        haptic.click()
        val meta = metaFor(habit)
        editingHabitId = habit.stamp.id
        draftName = habit.name
        draftTargetValue = displayNumber(habit.targetValue)
        draftUnitAndStep = "${habit.unit.ifBlank { "times" }} · +${displayNumber(meta.stepIncrement)}"
        draftColorKey = meta.colorKey
        draftIconKey = meta.iconKey
        draftTag = meta.tag
        draftReminderTime = meta.reminderTime
        draftCompletionMode = meta.mode
        showHabitEditor = true
    }

    fun saveHabitFromEditor(nameInput: String, targetInput: String, unitStepInput: String) {
        val cleanName = nameInput.trim().take(48)
        if (cleanName.isBlank()) {
            feedbackOverlay = "Enter Habit Name" to true
            haptic.click()
            return
        }
        val targetVal = targetInput.filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val parts = unitStepInput.split("·", "/", ",")
        val unitStr = parts.getOrNull(0)?.trim()?.ifBlank { "times" } ?: "times"
        val stepVal = parts.getOrNull(1)?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()?.coerceAtLeast(1.0)
            ?: (if (targetVal >= 100) (targetVal / 4.0) else 1.0)

        val targetId = editingHabitId
        coroutineScope.launch {
            runCatching {
                if (targetId != null) {
                    repository.editHabit(
                        id = targetId,
                        newName = cleanName,
                        newCadence = draftTag,
                        newColor = draftColorKey,
                        newTarget = targetVal,
                        newUnit = unitStr
                    )
                    val oldMeta = habitMetaMap[targetId] ?: HabitExtendedMeta(habitId = targetId)
                    habitMetaMap[targetId] = oldMeta.copy(
                        tag = draftTag,
                        iconKey = draftIconKey,
                        colorKey = draftColorKey,
                        mode = draftCompletionMode,
                        stepIncrement = stepVal,
                        reminderTime = draftReminderTime
                    )
                } else {
                    repository.quickHabit(
                        name = cleanName,
                        cadence = draftTag,
                        colorName = draftColorKey,
                        targetValue = targetVal,
                        unit = unitStr
                    )
                    val newlyCreated = repository.current().activeHabits.firstOrNull { it.name.equals(cleanName, true) }
                    if (newlyCreated != null) {
                        habitMetaMap[newlyCreated.stamp.id] = HabitExtendedMeta(
                            habitId = newlyCreated.stamp.id,
                            tag = draftTag,
                            iconKey = draftIconKey,
                            colorKey = draftColorKey,
                            mode = draftCompletionMode,
                            stepIncrement = stepVal,
                            reminderTime = draftReminderTime
                        )
                    }
                }
                persistMeta()
            }.onSuccess {
                showHabitEditor = false
                editingHabitId = null
                feedbackOverlay = (if (targetId != null) "Habit Updated" else "Habit Created") to false
                haptic.click()
            }.onFailure {
                feedbackOverlay = "Failed to Save Habit" to true
            }
        }
    }

    fun saveTimetableSlotFromEditor(titleInput: String, timeRangeInput: String, catEstInput: String) {
        val cleanTitle = titleInput.trim().take(48)
        if (cleanTitle.isBlank()) {
            feedbackOverlay = "Enter Slot Title" to true
            haptic.click()
            return
        }
        val times = timeRangeInput.split("-", "–", "to").map { it.trim() }
        val startStr = times.getOrNull(0)?.ifBlank { "09:00" } ?: "09:00"
        val endStr = times.getOrNull(1)?.ifBlank { "10:00" } ?: "10:00"

        val catParts = catEstInput.split("·", ",")
        val catTag = catParts.getOrNull(0)?.trim()?.ifBlank { "Focus" } ?: "Focus"
        val estMins = catParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(5, 480)
            ?: computeDurationMinutes(startStr, endStr)

        val slotId = editingSlotId
        if (slotId != null) {
            val idx = timetableSlots.indexOfFirst { it.id == slotId }
            if (idx >= 0) {
                timetableSlots[idx] = timetableSlots[idx].copy(
                    title = cleanTitle,
                    startTime = startStr,
                    endTime = endStr,
                    estimatedMinutes = estMins,
                    categoryTag = catTag
                )
            }
            coroutineScope.launch { repository.rescheduleRoutine(slotId, startStr, endStr) }
            feedbackOverlay = "Timetable Updated" to false
        } else {
            val newSlot = TimetableSlotMeta(
                id = "tt-${System.currentTimeMillis()}",
                title = cleanTitle,
                startTime = startStr,
                endTime = endStr,
                estimatedMinutes = estMins,
                status = TimetableSlotStatus.PENDING,
                dateIso = selectedDateIso,
                categoryTag = catTag,
                orderIndex = timetableSlots.size
            )
            timetableSlots.add(newSlot)
            coroutineScope.launch {
                repository.addRoutineTask(
                    title = cleanTitle,
                    startTime = startStr,
                    endTime = endStr,
                    phase = RoutinePhase.GENERAL,
                    recurrence = if (sameTimetableEveryDay) RoutineRecurrence.SAME_EVERY_DAY else RoutineRecurrence.CUSTOM_DAILY,
                    notes = catTag
                )
            }
            feedbackOverlay = "Added $cleanTitle" to false
        }
        persistTimetable()
        showTimetableEditor = false
        editingSlotId = null
        haptic.click()
    }

    fun moveTimetableSlotOrder(slotId: String, moveUp: Boolean) {
        haptic.click()
        val idx = timetableSlots.indexOfFirst { it.id == slotId }
        if (idx < 0) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx in timetableSlots.indices) {
            val item = timetableSlots.removeAt(idx)
            timetableSlots.add(targetIdx, item)
            for (i in timetableSlots.indices) {
                timetableSlots[i] = timetableSlots[i].copy(orderIndex = i)
            }
            persistTimetable()
            coroutineScope.launch { repository.moveRoutine(slotId, moveUp) }
            feedbackOverlay = if (moveUp) {
                "Moved Up" to false
            } else {
                "Moved Down" to false
            }
        }
    }

    fun pushPendingSlotsToNextDayBacklog() {
        haptic.click()
        val tomorrowIso = selectedDate.plusDays(1).toString()
        var movedCount = 0
        for (i in timetableSlots.indices) {
            val s = timetableSlots[i]
            if (s.status == TimetableSlotStatus.PENDING || s.status == TimetableSlotStatus.DOING) {
                timetableSlots[i] = s.copy(
                    status = TimetableSlotStatus.BACKLOG,
                    dateIso = tomorrowIso,
                    isTimerRunning = false
                )
                movedCount++
            }
        }
        persistTimetable()
        feedbackOverlay = if (movedCount > 0) "Moved $movedCount to Tomorrow Backlog" to false else "No Pending Items" to true
    }

    // 4-Page Vertical Pager:
    // Page 0 = Full-Screen Active / Upcoming Habit & Timetable Boundary Canvas (with Stop, Snooze, Backlog)
    // Page 1 = Staggered Stacked Habit Cards, Multi-Tap Liquid Fill List & GitHub Streaks
    // Page 2 = Minute-by-Minute Wear OS Tabular Timetable & Backlog Manager
    // Page 3 = Overall Daily Score, Monthly Calendar Matrix & Reminder Alerts
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
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
                    // PAGE 0: FULL-SCREEN ACTIVE/UPCOMING HABIT & TIMETABLE BOUNDARY CANVAS
                    // =========================================================================
                    0 -> ActiveHabitAndTimetableBoundaryPage(
                        spec = adaptiveSpec,
                        overallScore = overallDailyScore,
                        habitsProgress = habitsProgress,
                        timetableProgress = timetableProgress,
                        spotlightHabit = spotlightHabit,
                        spotlightHabitMeta = spotlightHabit?.let { metaFor(it) },
                        spotlightSlot = spotlightSlot,
                        onTapCompleteOrAdvanceHabit = {
                            if (spotlightHabit != null) {
                                advanceHabitProgress(spotlightHabit)
                            } else if (spotlightSlot != null) {
                                val idx = timetableSlots.indexOfFirst { it.id == spotlightSlot.id }
                                if (idx >= 0) {
                                    val cur = timetableSlots[idx]
                                    val nextRunning = !cur.isTimerRunning
                                    timetableSlots[idx] = cur.copy(
                                        isTimerRunning = nextRunning,
                                        status = if (nextRunning) TimetableSlotStatus.DOING else cur.status
                                    )
                                    persistTimetable()
                                    haptic.click()
                                }
                            }
                        },
                        onToggleSpotlightTimer = {
                            haptic.click()
                            if (spotlightSlot != null && (spotlightSlot.isTimerRunning || spotlightSlot.status == TimetableSlotStatus.DOING)) {
                                val idx = timetableSlots.indexOfFirst { it.id == spotlightSlot.id }
                                if (idx >= 0) {
                                    val s = timetableSlots[idx]
                                    timetableSlots[idx] = s.copy(
                                        isTimerRunning = !s.isTimerRunning,
                                        status = if (!s.isTimerRunning) TimetableSlotStatus.DOING else s.status
                                    )
                                    persistTimetable()
                                }
                            } else if (spotlightHabit != null) {
                                coroutineScope.launch { repository.toggleHabitTimer(spotlightHabit.stamp.id) }
                            }
                        },
                        onOpenSnoozeModal = {
                            haptic.click()
                            modalTargetHabitId = spotlightHabit?.stamp?.id
                            modalTargetSlotId = spotlightSlot?.id
                            activeModal = ActiveControlModal.SNOOZE_PICKER
                        },
                        onOpenStopAndRescheduleModal = {
                            haptic.click()
                            modalTargetHabitId = spotlightHabit?.stamp?.id
                            modalTargetSlotId = spotlightSlot?.id
                            activeModal = ActiveControlModal.STOP_AND_RESCHEDULE
                        },
                        onOpenMoodLogger = {
                            haptic.click()
                            modalTargetHabitId = spotlightHabit?.stamp?.id
                            activeModal = ActiveControlModal.MOOD_SATISFACTION_LOGGER
                        },
                        onJumpToHabitsDeck = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )

                    // =========================================================================
                    // PAGE 1: STAGGERED STACKED CARDS, MULTI-TAP LIQUID FILL & GITHUB STREAKS
                    // =========================================================================
                    1 -> HabitsStaggeredAndListDeckPage(
                        spec = adaptiveSpec,
                        horizonMode = horizonMode,
                        selectedDateOffsetDays = selectedDateOffsetDays,
                        selectedDate = selectedDate,
                        filteredHabits = filteredHabits,
                        allVisibleHabits = visibleHabitsForDate,
                        completedCount = completedHabitsCount,
                        totalCount = totalHabitsCount,
                        overallProgress = habitsProgress,
                        staggeredTopIndex = staggeredTopIndex,
                        selectedFilter = selectedFilter,
                        manageMode = manageMode,
                        showHabitEditor = showHabitEditor,
                        isEditingHabit = editingHabitId != null,
                        draftName = draftName,
                        draftTargetValue = draftTargetValue,
                        draftUnitAndStep = draftUnitAndStep,
                        draftColorKey = draftColorKey,
                        draftIconKey = draftIconKey,
                        draftTag = draftTag,
                        draftCompletionMode = draftCompletionMode,
                        metaProvider = ::metaFor,
                        onSelectHorizon = {
                            haptic.click()
                            horizonMode = it
                        },
                        onSelectDateOffset = { offset ->
                            haptic.click()
                            selectedDateOffsetDays = offset
                        },
                        onSelectFilter = {
                            haptic.click()
                            selectedFilter = it
                        },
                        onCycleStaggeredDeck = {
                            haptic.click()
                            if (visibleHabitsForDate.isNotEmpty()) {
                                staggeredTopIndex = (staggeredTopIndex + 1) % visibleHabitsForDate.size
                            }
                        },
                        onAdvanceHabit = { habit, inc -> advanceHabitProgress(habit, inc) },
                        onTimerToggleHabit = { habit ->
                            haptic.click()
                            coroutineScope.launch { repository.toggleHabitTimer(habit.stamp.id) }
                        },
                        onOpenSnoozeForHabit = { habit ->
                            haptic.click()
                            modalTargetHabitId = habit.stamp.id
                            modalTargetSlotId = null
                            activeModal = ActiveControlModal.SNOOZE_PICKER
                        },
                        onOpenStopForHabit = { habit ->
                            haptic.click()
                            modalTargetHabitId = habit.stamp.id
                            modalTargetSlotId = null
                            activeModal = ActiveControlModal.STOP_AND_RESCHEDULE
                        },
                        onOpenMoodForHabit = { habit ->
                            haptic.click()
                            modalTargetHabitId = habit.stamp.id
                            activeModal = ActiveControlModal.MOOD_SATISFACTION_LOGGER
                        },
                        onSelectColorKey = {
                            haptic.click()
                            draftColorKey = it
                        },
                        onSelectIconKey = {
                            haptic.click()
                            draftIconKey = it
                        },
                        onSelectCompletionMode = {
                            haptic.click()
                            draftCompletionMode = it
                        },
                        onOpenNewHabitEditor = ::openNewHabitEditor,
                        onOpenEditHabit = ::openEditHabitEditor,
                        onSaveHabit = ::saveHabitFromEditor,
                        onCancelHabitEditor = {
                            haptic.click()
                            showHabitEditor = false
                            editingHabitId = null
                        },
                        onDeleteHabit = { habit ->
                            haptic.click()
                            coroutineScope.launch {
                                repository.deleteHabit(habit.stamp.id)
                                feedbackOverlay = "${habit.name} Deleted" to false
                            }
                        },
                        onToggleManageMode = {
                            haptic.click()
                            manageMode = !manageMode
                        },
                        onJumpToTimetable = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    // =========================================================================
                    // PAGE 2: MINUTE-BY-MINUTE WEAR OS TABULAR TIMETABLE & BACKLOG STUDIO
                    // =========================================================================
                    2 -> TimetableTabularSchedulePage(
                        spec = adaptiveSpec,
                        selectedDate = selectedDate,
                        selectedDateOffsetDays = selectedDateOffsetDays,
                        sameEveryDay = sameTimetableEveryDay,
                        slots = visibleTimetableSlots,
                        showSlotEditor = showTimetableEditor,
                        isEditingSlot = editingSlotId != null,
                        draftSlotTitle = draftSlotTitle,
                        draftSlotTimeRange = draftSlotTimeRange,
                        draftSlotCategoryAndEst = draftSlotCategoryAndEst,
                        onSelectDateOffset = {
                            haptic.click()
                            selectedDateOffsetDays = it
                        },
                        onToggleSameEveryDay = {
                            haptic.click()
                            sameTimetableEveryDay = !sameTimetableEveryDay
                            HabitsTimetableStudioPrefs.saveSameEveryDay(context, sameTimetableEveryDay)
                            feedbackOverlay = (if (sameTimetableEveryDay) "Same Timetable Daily" else "Per-Date Timetable") to false
                        },
                        onOpenNewSlotEditor = {
                            haptic.click()
                            editingSlotId = null
                            draftSlotTitle = ""
                            draftSlotTimeRange = "10:00-11:00"
                            draftSlotCategoryAndEst = "Focus · 60m"
                            showTimetableEditor = true
                        },
                        onOpenEditSlotEditor = { slot ->
                            haptic.click()
                            editingSlotId = slot.id
                            draftSlotTitle = slot.title
                            draftSlotTimeRange = "${slot.startTime}-${slot.endTime}"
                            draftSlotCategoryAndEst = "${slot.categoryTag} · ${slot.estimatedMinutes}m"
                            showTimetableEditor = true
                        },
                        onSaveSlot = ::saveTimetableSlotFromEditor,
                        onCancelSlotEditor = {
                            haptic.click()
                            showTimetableEditor = false
                            editingSlotId = null
                        },
                        onDeleteSlot = { slotId ->
                            haptic.click()
                            timetableSlots.removeAll { it.id == slotId }
                            persistTimetable()
                            coroutineScope.launch { repository.deleteRoutine(slotId) }
                            showTimetableEditor = false
                            editingSlotId = null
                            feedbackOverlay = "Slot Deleted" to false
                        },
                        onCycleSlotStatus = { slot ->
                            haptic.click()
                            val idx = timetableSlots.indexOfFirst { it.id == slot.id }
                            if (idx >= 0) {
                                val nextStatus = when (slot.status) {
                                    TimetableSlotStatus.PENDING -> TimetableSlotStatus.DOING
                                    TimetableSlotStatus.DOING -> TimetableSlotStatus.DONE
                                    TimetableSlotStatus.DONE -> TimetableSlotStatus.SKIPPED
                                    TimetableSlotStatus.SKIPPED -> TimetableSlotStatus.NOT_DOING
                                    TimetableSlotStatus.NOT_DOING -> TimetableSlotStatus.DELEGATED
                                    TimetableSlotStatus.DELEGATED -> TimetableSlotStatus.BACKLOG
                                    TimetableSlotStatus.BACKLOG -> TimetableSlotStatus.PENDING
                                }
                                timetableSlots[idx] = slot.copy(
                                    status = nextStatus,
                                    isTimerRunning = (nextStatus == TimetableSlotStatus.DOING)
                                )
                                persistTimetable()
                                if (nextStatus == TimetableSlotStatus.DONE) {
                                    coroutineScope.launch { repository.toggleRoutine(slot.id) }
                                }
                                feedbackOverlay = "${slot.title.take(12)}: ${nextStatus.label}" to false
                            }
                        },
                        onToggleSlotTimer = { slot ->
                            haptic.click()
                            val idx = timetableSlots.indexOfFirst { it.id == slot.id }
                            if (idx >= 0) {
                                val nextRunning = !slot.isTimerRunning
                                timetableSlots[idx] = slot.copy(
                                    isTimerRunning = nextRunning,
                                    status = if (nextRunning) TimetableSlotStatus.DOING else slot.status
                                )
                                persistTimetable()
                                feedbackOverlay = (if (nextRunning) "Timer Started" else "Timer Paused") to false
                            }
                        },
                        onMoveSlotOrder = ::moveTimetableSlotOrder,
                        onOpenSnoozeOrStopSlot = { slot ->
                            haptic.click()
                            modalTargetSlotId = slot.id
                            modalTargetHabitId = null
                            activeModal = ActiveControlModal.STOP_AND_RESCHEDULE
                        },
                        onPushPendingToBacklog = ::pushPendingSlotsToNextDayBacklog,
                        onJumpToScorePage = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        }
                    )

                    // =========================================================================
                    // PAGE 3: DAILY SCORE, MONTHLY CALENDAR SUMMARY & NOTIFICATIONS
                    // =========================================================================
                    else -> HabitsMonthlySummaryAndScorePage(
                        spec = adaptiveSpec,
                        state = liveState,
                        overallScore = overallDailyScore,
                        completedHabits = completedHabitsCount,
                        totalHabits = totalHabitsCount,
                        doneSlots = doneSlotsCount,
                        totalSlots = visibleTimetableSlots.size,
                        pendingSlots = visibleTimetableSlots.filter { it.status == TimetableSlotStatus.PENDING || it.status == TimetableSlotStatus.BACKLOG },
                        onTriggerReminderNotification = {
                            haptic.click()
                            val nextItemTitle = spotlightHabit?.name ?: spotlightSlot?.title ?: "Daily Habits Check-in"
                            HabitReminderNotifier.fireReminder(
                                context,
                                "LifeOS Reminder: $nextItemTitle",
                                "Overall Daily Score: $overallDailyScore% · Tap to log progress"
                            )
                            feedbackOverlay = "Reminder Notification Sent" to false
                        },
                        onPushAllPendingToTomorrow = ::pushPendingSlotsToNextDayBacklog,
                        onJumpToTop = {
                            haptic.click()
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }

            // =========================================================================
            // INTERACTIVE MODAL OVERLAYS: SNOOZE DURATION, STOP/RESCHEDULE, MOOD LOGGER
            // =========================================================================
            if (activeModal != ActiveControlModal.NONE) {
                M3HabitAndTimetableControlModal(
                    modalType = activeModal,
                    customSnoozeMinutes = customSnoozeMinutes,
                    currentMeta = modalTargetHabitId?.let { id -> habits.firstOrNull { it.stamp.id == id }?.let { metaFor(it) } },
                    onAdjustSnoozeMinutes = { delta ->
                        haptic.click()
                        customSnoozeMinutes = (customSnoozeMinutes + delta).coerceIn(5, 240)
                    },
                    onConfirmSnooze = { mins ->
                        haptic.click()
                        modalTargetHabitId?.let { hId ->
                            val existing = habitMetaMap[hId] ?: HabitExtendedMeta(habitId = hId)
                            habitMetaMap[hId] = existing.copy(
                                snoozedUntilEpochMs = System.currentTimeMillis() + (mins * 60_000L)
                            )
                            persistMeta()
                        }
                        modalTargetSlotId?.let { sId ->
                            val idx = timetableSlots.indexOfFirst { it.id == sId }
                            if (idx >= 0) {
                                timetableSlots[idx] = timetableSlots[idx].copy(
                                    snoozedMinutes = mins,
                                    isTimerRunning = false
                                )
                                persistTimetable()
                            }
                        }
                        activeModal = ActiveControlModal.NONE
                        feedbackOverlay = "Snoozed for ${mins}m" to false
                    },
                    onStopForTodayOnly = {
                        haptic.click()
                        modalTargetHabitId?.let { hId ->
                            val existing = habitMetaMap[hId] ?: HabitExtendedMeta(habitId = hId)
                            habitMetaMap[hId] = existing.copy(stoppedForDateIso = selectedDateIso)
                            persistMeta()
                            coroutineScope.launch {
                                val h = habits.firstOrNull { it.stamp.id == hId }
                                if (h?.isTimerActive == true) repository.toggleHabitTimer(hId)
                            }
                        }
                        modalTargetSlotId?.let { sId ->
                            val idx = timetableSlots.indexOfFirst { it.id == sId }
                            if (idx >= 0) {
                                timetableSlots[idx] = timetableSlots[idx].copy(
                                    status = TimetableSlotStatus.NOT_DOING,
                                    isTimerRunning = false
                                )
                                persistTimetable()
                            }
                        }
                        activeModal = ActiveControlModal.NONE
                        feedbackOverlay = "Stopped for Today Only" to false
                    },
                    onSkipOrRescheduleDays = { daysAhead ->
                        haptic.click()
                        val targetIso = selectedDate.plusDays(daysAhead.toLong()).toString()
                        modalTargetSlotId?.let { sId ->
                            val idx = timetableSlots.indexOfFirst { it.id == sId }
                            if (idx >= 0) {
                                timetableSlots[idx] = timetableSlots[idx].copy(
                                    status = if (daysAhead == 0) TimetableSlotStatus.SKIPPED else TimetableSlotStatus.BACKLOG,
                                    dateIso = if (daysAhead == 0) selectedDateIso else targetIso,
                                    isTimerRunning = false
                                )
                                persistTimetable()
                            }
                        }
                        modalTargetHabitId?.let { hId ->
                            val existing = habitMetaMap[hId] ?: HabitExtendedMeta(habitId = hId)
                            habitMetaMap[hId] = existing.copy(stoppedForDateIso = selectedDateIso)
                            persistMeta()
                        }
                        activeModal = ActiveControlModal.NONE
                        feedbackOverlay = if (daysAhead == 0) "Marked Skipped" to false else "Moved to +${daysAhead}d Backlog" to false
                    },
                    onSaveMoodAndSatisfaction = { moodStr, stars ->
                        haptic.click()
                        modalTargetHabitId?.let { hId ->
                            val existing = habitMetaMap[hId] ?: HabitExtendedMeta(habitId = hId)
                            habitMetaMap[hId] = existing.copy(
                                lastMood = moodStr,
                                satisfactionStars = stars
                            )
                            persistMeta()
                        }
                        coroutineScope.launch { repository.updateWellness(mood = moodStr) }
                        activeModal = ActiveControlModal.NONE
                        feedbackOverlay = "Logged $moodStr ($stars★)" to false
                    },
                    onDismiss = {
                        haptic.click()
                        activeModal = ActiveControlModal.NONE
                    }
                )
            }

            // Right Bezel 4-Page Vertical Pager Indicator
            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 4,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            // In-Viewport Constrained Curved Confirmation Overlay
            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedHabitConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }
        }
    
}

// =============================================================================
// PAGE 0: FULL-SCREEN ACTIVE/UPCOMING HABIT & TIMETABLE BOUNDARY CANVAS
// =============================================================================
@Composable
private fun ActiveHabitAndTimetableBoundaryPage(
    spec: WearAdaptiveSpec,
    overallScore: Int,
    habitsProgress: Float,
    timetableProgress: Float,
    spotlightHabit: Habit?,
    spotlightHabitMeta: HabitExtendedMeta?,
    spotlightSlot: TimetableSlotMeta?,
    onTapCompleteOrAdvanceHabit: () -> Unit,
    onToggleSpotlightTimer: () -> Unit,
    onOpenSnoozeModal: () -> Unit,
    onOpenStopAndRescheduleModal: () -> Unit,
    onOpenMoodLogger: () -> Unit,
    onJumpToHabitsDeck: () -> Unit
) {
    val primaryAccent = spotlightHabitMeta?.let { resolveHabitColor(it.colorKey) } ?: MaterialTheme.colorScheme.primary
    val animatedHabitProg by animateFloatAsState(
        targetValue = habitsProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "habitBoundaryProg"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "habitWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "habitWavePhase"
    )

    val isTimerRunning = (spotlightHabit?.isTimerActive == true) || (spotlightSlot?.isTimerRunning == true)
    val spotlightPct = spotlightHabit?.let { habitProgress(it) } ?: timetableProgress

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
            // 1. FULL-SCREEN RADIAL AMBIENT CONTAINER CROPPED INSIDE BEZEL RING
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isTimerRunning) 0.28f else 0.16f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spotlightHabitMeta?.let { resolveHabitIcon(it.iconKey, spotlightHabit?.name ?: "") }
                        ?: Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(spec.screenWidth * 0.52f),
                    tint = primaryAccent.copy(alpha = 0.07f)
                )
            }

            // 2. DUAL BOUNDARY CIRCULAR PROGRESS CONTAINER (Outer = Habits, Inner = Timetable)
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
                    sweepAngle = 360f * animatedHabitProg,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )

                val inset = 9.dp.toPx()
                drawArc(
                    color = Color(0xFF8EAEFF).copy(alpha = 0.45f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    startAngle = -90f,
                    sweepAngle = 360f * timetableProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. FOREGROUND SPOTLIGHT HABIT & TIMETABLE TRANSPORT CONTROLS
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
                // Top Slot: Daily Score Pill + Active Habit/Timetable Title + Live Progress
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                            .clickable(onClick = onJumpToHabitsDeck)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = primaryAccent
                        )
                        Text(
                            text = "SCORE $overallScore% · ${spotlightHabitMeta?.tag?.uppercase() ?: "ROUTINE"}",
                            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            maxLines = 1
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = spotlightHabit?.name ?: spotlightSlot?.title ?: "All Caught Up!",
                        style = MaterialTheme.typography.titleMedium.compactLineHeight(),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    val subReadout = when {
                        spotlightSlot != null && spotlightSlot.isTimerRunning -> {
                            val rem = (spotlightSlot.estimatedMinutes * 60 - spotlightSlot.elapsedSeconds).coerceAtLeast(0)
                            "TIMETABLE ${spotlightSlot.startTime} · ${formatDuration(rem)} LEFT"
                        }
                        spotlightHabit != null -> {
                            val pct = (habitProgress(spotlightHabit) * 100).roundToInt()
                            "${displayNumber(spotlightHabit.currentValue)}/${displayNumber(spotlightHabit.targetValue)} ${spotlightHabit.unit} ($pct%) · ${spotlightHabit.streak}d🔥"
                        }
                        spotlightSlot != null -> "${spotlightSlot.startTime}–${spotlightSlot.endTime} · ${spotlightSlot.status.label}"
                        else -> "TAP BELOW TO BROWSE DECK"
                    }

                    Text(
                        text = subReadout,
                        style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Transport Row: Snooze | Center Multi-Tap Complete + Wavy Ring | Stop / Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Button: Snooze (Opens Snooze Duration Picker)
                    Button(
                        onClick = onOpenSnoozeModal,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Snooze Habit or Timetable Item" },
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

                    // Center Button: Tap to Progress % / Complete with Wavy Progress Ring
                    Box(
                        modifier = Modifier.size(centerButtonSize + 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawHabitWavyRing(
                                progress = spotlightPct.coerceIn(0.05f, 1f),
                                phase = if (isTimerRunning) wavePhase else 0f,
                                trackColor = primaryAccent.copy(alpha = 0.28f),
                                activeColor = primaryAccent,
                                strokeWidthPx = 3.5.dp.toPx(),
                                waveAmplitudePx = if (isTimerRunning) 2.dp.toPx() else 1.dp.toPx(),
                                lobes = 12
                            )
                        }

                        Button(
                            onClick = onTapCompleteOrAdvanceHabit,
                            modifier = Modifier
                                .size(centerButtonSize)
                                .semantics { contentDescription = "Progress or Complete Active Habit" },
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
                                    imageVector = if (spotlightPct >= 1f) Icons.Rounded.Check else Icons.Rounded.AddTask,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "${(spotlightPct * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Right Button: Stop Today / Skip / Move to Backlog
                    Button(
                        onClick = onOpenStopAndRescheduleModal,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Stop for Today or Move to Backlog" },
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

                // Bottom Slot: Start/Pause Timer | Mood & Satisfaction | Deck Browser
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.05f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onToggleSpotlightTimer,
                        modifier = Modifier
                            .height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isTimerRunning) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )

                        }
                    }

                    Button(
                        onClick = onOpenMoodLogger,
                        modifier = Modifier.height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.SentimentSatisfiedAlt, contentDescription = "Log " +
                                    "Mood", modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${spotlightHabitMeta?.satisfactionStars ?: 5}★",
                                style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = onJumpToHabitsDeck,
                        modifier = Modifier.height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ViewAgenda, contentDescription = "Open Staggered " +
                                    "Deck", modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawHabitWavyRing(
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
// PAGE 1: STAGGERED STACKED HABIT CARDS, LIQUID FILL CARDS & GITHUB STREAK GRIDS
// =============================================================================
@Composable
private fun HabitsStaggeredAndListDeckPage(
    spec: WearAdaptiveSpec,
    horizonMode: HorizonViewMode,
    selectedDateOffsetDays: Int,
    selectedDate: LocalDate,
    filteredHabits: List<Habit>,
    allVisibleHabits: List<Habit>,
    completedCount: Int,
    totalCount: Int,
    overallProgress: Float,
    staggeredTopIndex: Int,
    selectedFilter: HabitFilter,
    manageMode: Boolean,
    showHabitEditor: Boolean,
    isEditingHabit: Boolean,
    draftName: String,
    draftTargetValue: String,
    draftUnitAndStep: String,
    draftColorKey: String,
    draftIconKey: String,
    draftTag: String,
    draftCompletionMode: HabitCompletionMode,
    metaProvider: (Habit) -> HabitExtendedMeta,
    onSelectHorizon: (HorizonViewMode) -> Unit,
    onSelectDateOffset: (Int) -> Unit,
    onSelectFilter: (HabitFilter) -> Unit,
    onCycleStaggeredDeck: () -> Unit,
    onAdvanceHabit: (Habit, Double?) -> Unit,
    onTimerToggleHabit: (Habit) -> Unit,
    onOpenSnoozeForHabit: (Habit) -> Unit,
    onOpenStopForHabit: (Habit) -> Unit,
    onOpenMoodForHabit: (Habit) -> Unit,
    onSelectColorKey: (String) -> Unit,
    onSelectIconKey: (String) -> Unit,
    onSelectCompletionMode: (HabitCompletionMode) -> Unit,
    onOpenNewHabitEditor: () -> Unit,
    onOpenEditHabit: (Habit) -> Unit,
    onSaveHabit: (String, String, String) -> Unit,
    onCancelHabitEditor: () -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onToggleManageMode: () -> Unit,
    onJumpToTimetable: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = if (showHabitEditor) onCancelHabitEditor else onOpenNewHabitEditor,
                label = if (showHabitEditor) "Close" else "New Habit",
                icon = if (showHabitEditor) Icons.Rounded.Close else Icons.Rounded.Add,
                modifier = Modifier.scrollable(
                    state = columnState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect()
                )
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
                    title = "HABITS STUDIO",
                    icon = Icons.Filled.Checklist,
                    badgeText = "$completedCount/$totalCount · ${(overallProgress * 100).roundToInt()}%",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. HORIZON SELECTOR (Today / Weekly / Monthly)
            item {
                WearActionButtonGroup(
                    options = HorizonViewMode.entries.map { it.label },
                    selectedIndex = horizonMode.ordinal,
                    onOptionSelected = { idx -> onSelectHorizon(HorizonViewMode.entries[idx]) }
                )
            }

            // 2. HORIZONTAL SCROLLING DATE CHIP BAR (-3d to +5d)
            item {
                HorizontalDateChipBar(
                    selectedOffset = selectedDateOffsetDays,
                    onSelectOffset = onSelectDateOffset
                )
            }

            // 3. STAGGERED STACKED HABIT CARDS DECK (3D Layered Stack - Tap to Fill % or Cycle)
            if (allVisibleHabits.isNotEmpty() && !showHabitEditor) {
                item {
                    CatalogM3ListHeaderSample(title = "STAGGERED TODAY STACK")
                }
                item {
                    StaggeredStackedHabitDeck(
                        habits = allVisibleHabits,
                        topIndex = staggeredTopIndex.coerceIn(0, allVisibleHabits.lastIndex),
                        metaProvider = metaProvider,
                        onTapCompleteOrAdvance = { habit -> onAdvanceHabit(habit, null) },
                        onCycleNextCard = onCycleStaggeredDeck,
                        onOpenSnooze = onOpenSnoozeForHabit,
                        onOpenStop = onOpenStopForHabit
                    )
                }
            }

            // 4. CUSTOM HABIT EDITOR (With Custom Color, Icon, Tag, Unit, Step & Mode)
            if (showHabitEditor) {
                item {
                    CatalogM3ListHeaderSample(
                        title = if (isEditingHabit) "EDIT HABIT" else "CREATE CUSTOM HABIT"
                    )
                }

                // Completion Mode Selector
                item {
                    WearActionButtonGroup(
                        options = HabitCompletionMode.entries.map { it.label.take(8) },
                        selectedIndex = draftCompletionMode.ordinal,
                        onOptionSelected = { idx -> onSelectCompletionMode(HabitCompletionMode.entries[idx]) }
                    )
                }

                // Custom Color & Icon Swatch Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        habitColorMap.forEach { (key, colorVal) ->
                            val isSelected = draftColorKey == key
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectColorKey(key) }
                            )
                        }
                        habitIconKeys.forEach { iconKey ->
                            val isSelected = draftIconKey == iconKey
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable { onSelectIconKey(iconKey) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = resolveHabitIcon(iconKey),
                                    contentDescription = iconKey,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "HABIT",
                        initialTitle = draftName,
                        initialTimeOrDate = draftTargetValue,
                        initialSubtitle = draftUnitAndStep,
                        isEditingExisting = isEditingHabit,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Target Amount",
                        subtitleFieldLabel = "Unit & Tap Step (ml · +250)",
                        onSave = onSaveHabit,
                        onCancel = onCancelHabitEditor,
                        onDelete = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = transformationSpec
                    )
                }
            }

            // 5. FILTER BAR (All, To do, Done)
            item {
                WearActionButtonGroup(
                    options = HabitFilter.entries.map { it.label },
                    selectedIndex = selectedFilter.ordinal,
                    onOptionSelected = { index -> onSelectFilter(HabitFilter.entries[index]) }
                )
            }

            // 6. LIQUID-FILL HABIT CARDS WITH GITHUB STREAK MATRICES
            if (filteredHabits.isEmpty()) {
                item {
                    AppCard(
                        onClick = onOpenNewHabitEditor,
                        appName = { Text("Habits") },
                        title = { Text("No Matching Habits") },
                        appImage = { Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = "Tap to create a custom multi-tap or timed habit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredHabits.size) { index ->
                    val habit = filteredHabits[index]
                    val meta = metaProvider(habit)
                    M3LiquidFillHabitCard(
                        habit = habit,
                        meta = meta,
                        horizonMode = horizonMode,
                        manageMode = manageMode,
                        transformation = transformationSpec,
                        onTapAdvance = { onAdvanceHabit(habit, null) },
                        onTimerToggle = { onTimerToggleHabit(habit) },
                        onOpenSnooze = { onOpenSnoozeForHabit(habit) },
                        onOpenStop = { onOpenStopForHabit(habit) },
                        onOpenMood = { onOpenMoodForHabit(habit) },
                        onEdit = { onOpenEditHabit(habit) },
                        onDelete = { onDeleteHabit(habit) }
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = onJumpToTimetable,
                    icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Minute Timetable")
                }
            }
        }
    }
}

// =============================================================================
// STAGGERED 3D STACKED HABIT CARDS COMPONENT
// =============================================================================
@Composable
private fun StaggeredStackedHabitDeck(
    habits: List<Habit>,
    topIndex: Int,
    metaProvider: (Habit) -> HabitExtendedMeta,
    onTapCompleteOrAdvance: (Habit) -> Unit,
    onCycleNextCard: () -> Unit,
    onOpenSnooze: (Habit) -> Unit,
    onOpenStop: (Habit) -> Unit
) {
    val visibleStackCount = minOf(3, habits.size)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        for (layer in (visibleStackCount - 1) downTo 0) {
            val habitIdx = (topIndex + layer) % habits.size
            val habit = habits[habitIdx]
            val meta = metaProvider(habit)
            val accent = resolveHabitColor(meta.colorKey)
            val fillRatio = habitProgress(habit)
            val animatedFill by animateFloatAsState(
                targetValue = fillRatio,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "stackFill$layer"
            )

            val topOffset = (layer * 8).dp
            val horizontalInset = (layer * 7).dp
            val cardAlpha = 1f - (layer * 0.22f)

            Box(
                modifier = Modifier
                    .padding(top = topOffset, start = horizontalInset, end = horizontalInset)
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = cardAlpha))
                    .drawWithContent {
                        // Liquid Container Fill from Left to Right
                        drawRect(
                            color = accent.copy(alpha = 0.28f * cardAlpha),
                            size = Size(size.width * animatedFill, size.height)
                        )
                        drawContent()
                    }
                    .border(
                        width = if (layer == 0) 1.2.dp else 0.6.dp,
                        color = accent.copy(alpha = if (layer == 0) 0.7f else 0.25f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable {
                        if (layer == 0) onTapCompleteOrAdvance(habit) else onCycleNextCard()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (layer == 0) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = resolveHabitIcon(meta.iconKey, habit.name),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = accent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${meta.tag.uppercase()} · CARD ${topIndex + 1}/${habits.size}",
                                        style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 8.sp),
                                        color = accent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "${(fillRatio * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium.tabularNums(),
                                fontWeight = FontWeight.Black,
                                color = accent
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${displayNumber(habit.currentValue)}/${displayNumber(habit.targetValue)} ${habit.unit} (+${displayNumber(meta.stepIncrement)})",
                                style = MaterialTheme.typography.bodySmall.tabularNums().copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .clickable { onOpenSnooze(habit) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Snooze, contentDescription = "Snooze", modifier = Modifier.size(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .clickable { onOpenStop(habit) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = "Stop", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.25f))
                                        .clickable { onCycleNextCard() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Layers, contentDescription = "Next Stacked Card", modifier = Modifier.size(12.dp), tint = accent)
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
// LIQUID-FILL HABIT CARD WITH PER-HABIT GITHUB STREAK CONTRIBUTION MATRIX
// =============================================================================
@Composable
private fun TransformingLazyColumnItemScope.M3LiquidFillHabitCard(
    habit: Habit,
    meta: HabitExtendedMeta,
    horizonMode: HorizonViewMode,
    manageMode: Boolean,
    transformation: TransformationSpec,
    onTapAdvance: () -> Unit,
    onTimerToggle: () -> Unit,
    onOpenSnooze: () -> Unit,
    onOpenStop: () -> Unit,
    onOpenMood: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = resolveHabitColor(meta.colorKey)
    val progress = habitProgress(habit)
    val animatedFill by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "habitLiquidFill"
    )

    val isSnoozed = meta.snoozedUntilEpochMs > System.currentTimeMillis()
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
            onClick = onTapAdvance,
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
                        // Liquid Container Fill proportional to % completed
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
                    // Custom Icon + Circular Progress Ring
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp)) {
                        CircularProgressIndicator(
                            progress = { animatedFill },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 2.8.dp,
                            colors = ProgressIndicatorDefaults.colors(
                                indicatorColor = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                        Icon(
                            imageVector = if (habit.doneToday) Icons.Rounded.Check else resolveHabitIcon(meta.iconKey, habit.name),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accent
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (habit.doneToday) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            WearStatusBadge(text = "${habit.streak}d")
                        }
                        Text(
                            text = "${meta.tag} · ${displayNumber(habit.currentValue)}/${displayNumber(habit.targetValue)} ${habit.unit} · ${meta.lastMood} ${meta.satisfactionStars}★" +
                                    (if (isSnoozed) " · SNOOZED" else ""),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                }

                // Per-Habit GitHub Streak Contribution Grid (14 cells in Daily/Weekly, 28 cells in Monthly)
                val gridCells = if (horizonMode == HorizonViewMode.MONTHLY) 28 else 14
                GitHubHabitStreakStrip(
                    cellsCount = gridCells,
                    bitmask = meta.historyBitmask28,
                    doneToday = habit.doneToday,
                    accent = accent
                )

                // Quick Inline Action Row (Step +, Timer, Mood, Snooze, Stop)
                if (!manageMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap card: +${displayNumber(meta.stepIncrement)} ${habit.unit}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = accent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (meta.mode == HabitCompletionMode.TIMED_SESSION || habit.remainingSeconds > 0) {
                                SmallCircleActionIcon(
                                    icon = if (habit.isTimerActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    tint = accent,
                                    onClick = onTimerToggle
                                )
                            }
                            SmallCircleActionIcon(
                                icon = Icons.Rounded.SentimentSatisfiedAlt,
                                tint = Color(0xFFFFD177),
                                onClick = onOpenMood
                            )
                            SmallCircleActionIcon(
                                icon = Icons.Rounded.Snooze,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = onOpenSnooze
                            )
                            SmallCircleActionIcon(
                                icon = Icons.Rounded.Stop,
                                tint = MaterialTheme.colorScheme.error,
                                onClick = onOpenStop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubHabitStreakStrip(
    cellsCount: Int,
    bitmask: Int,
    doneToday: Boolean,
    accent: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (cellsCount > 14) 16.dp else 8.dp)
    ) {
        val cols = 14
        val rows = if (cellsCount > 14) 2 else 1
        val spacing = 2.dp.toPx()
        val cellW = (size.width - spacing * (cols - 1)) / cols
        val cellH = (size.height - spacing * (rows - 1)) / rows

        for (i in 0 until cellsCount) {
            val r = i / cols
            val c = i % cols
            val isLastCell = (i == cellsCount - 1)
            val isBitOn = if (isLastCell) doneToday else ((bitmask shr (i % 28)) and 1) == 1
            val cellColor = if (isBitOn) accent else Color.White.copy(alpha = 0.12f)
            drawRoundRect(
                color = cellColor,
                topLeft = Offset(c * (cellW + spacing), r * (cellH + spacing)),
                size = Size(cellW, cellH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun SmallCircleActionIcon(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = tint)
    }
}

// =============================================================================
// PAGE 2: MINUTE-BY-MINUTE WEAR OS TABULAR TIMETABLE & BACKLOG STUDIO
// =============================================================================
@Composable
private fun TimetableTabularSchedulePage(
    spec: WearAdaptiveSpec,
    selectedDate: LocalDate,
    selectedDateOffsetDays: Int,
    sameEveryDay: Boolean,
    slots: List<TimetableSlotMeta>,
    showSlotEditor: Boolean,
    isEditingSlot: Boolean,
    draftSlotTitle: String,
    draftSlotTimeRange: String,
    draftSlotCategoryAndEst: String,
    onSelectDateOffset: (Int) -> Unit,
    onToggleSameEveryDay: () -> Unit,
    onOpenNewSlotEditor: () -> Unit,
    onOpenEditSlotEditor: (TimetableSlotMeta) -> Unit,
    onSaveSlot: (String, String, String) -> Unit,
    onCancelSlotEditor: () -> Unit,
    onDeleteSlot: (String) -> Unit,
    onCycleSlotStatus: (TimetableSlotMeta) -> Unit,
    onToggleSlotTimer: (TimetableSlotMeta) -> Unit,
    onMoveSlotOrder: (String, Boolean) -> Unit,
    onOpenSnoozeOrStopSlot: (TimetableSlotMeta) -> Unit,
    onPushPendingToBacklog: () -> Unit,
    onJumpToScorePage: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val doneSlots = slots.filter { it.status == TimetableSlotStatus.DONE }
    val pendingSlots = slots.filter { it.status != TimetableSlotStatus.DONE }

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = if (showSlotEditor) onCancelSlotEditor else onOpenNewSlotEditor,
                label = if (showSlotEditor) "Close" else "Add Time Slot",
                icon = if (showSlotEditor) Icons.Rounded.Close else Icons.Rounded.Add
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
                    title = "MINUTE TIMETABLE",
                    icon = Icons.Rounded.Schedule,
                    badgeText = "${doneSlots.size}/${slots.size} DONE",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. HORIZONTAL DATE SCROLLER
            item {
                HorizontalDateChipBar(
                    selectedOffset = selectedDateOffsetDays,
                    onSelectOffset = onSelectDateOffset
                )
            }

            // 2. SAME EVERYDAY VS PER-DATE TOGGLE SWITCH
            item {
                WearExpressiveSwitchRow(
                    label = "Same Timetable Daily",
                    checked = sameEveryDay,
                    onCheckedChange = { onToggleSameEveryDay() },
                    icon = Icons.Rounded.EventRepeat,
                    subtitle = if (sameEveryDay) "Applies across all dates" else "Custom for ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}"
                )
            }

            // 3. UNIVERSAL TIMETABLE SLOT EDITOR
            if (showSlotEditor) {
                item {
                    CatalogM3ListHeaderSample(title = if (isEditingSlot) "EDIT TIME BLOCK" else "NEW TIME BLOCK")
                }
                item {
                    WearExpressiveUniversalEditorCard(
                        itemTypeTitle = "TIME SLOT",
                        initialTitle = draftSlotTitle,
                        initialTimeOrDate = draftSlotTimeRange,
                        initialSubtitle = draftSlotCategoryAndEst,
                        isEditingExisting = isEditingSlot,
                        showTimePickerField = true,
                        showSubtitleField = true,
                        timeFieldLabel = "Start-End (e.g. 09:00-10:30)",
                        subtitleFieldLabel = "Tag & Est Mins (Focus · 90m)",
                        onSave = onSaveSlot,
                        onCancel = onCancelSlotEditor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = transformationSpec
                    )
                }
            }

            // 4. WEAR OS TABULAR TIMETABLE HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TIME / EST", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text("TASK & % DONE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text("STATUS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }

            // 5. MINUTE-BY-MINUTE TABULAR ROWS (Swipe Horizontal = Edit/Delete, Buttons = Order Up/Down, Timer, Status)
            items(slots.size) { index ->
                val slot = slots[index]
                M3TabularTimetableRow(
                    slot = slot,
                    canMoveUp = index > 0,
                    canMoveDown = index < slots.lastIndex,
                    transformation = transformationSpec,
                    onCycleStatus = { onCycleSlotStatus(slot) },
                    onToggleTimer = { onToggleSlotTimer(slot) },
                    onMoveUp = { onMoveSlotOrder(slot.id, true) },
                    onMoveDown = { onMoveSlotOrder(slot.id, false) },
                    onOpenSnoozeOrStop = { onOpenSnoozeOrStopSlot(slot) },
                    onEdit = { onOpenEditSlotEditor(slot) },
                    onDelete = { onDeleteSlot(slot.id) }
                )
            }

            // 6. COMPLETED vs PENDING SUMMARY & PUSH TO NEXT-DAY BACKLOG
            if (pendingSlots.isNotEmpty()) {
                item {
                    CatalogM3ListHeaderSample(title = "PENDING / BACKLOG (${pendingSlots.size})")
                }
                item {
                    FilledTonalButton(
                        onClick = onPushPendingToBacklog,
                        icon = { Icon(Icons.Rounded.NextPlan, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move ${pendingSlots.size} Pending to Tomorrow")
                    }
                }
            }

            item {
                CatalogM3ChildButtonSample(
                    label = "Daily Score & Monthly Report",
                    onClick = onJumpToScorePage
                )
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.M3TabularTimetableRow(
    slot: TimetableSlotMeta,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    transformation: TransformationSpec,
    onCycleStatus: () -> Unit,
    onToggleTimer: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpenSnoozeOrStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val targetSeconds = (slot.estimatedMinutes.coerceAtLeast(1) * 60)
    val slotProgress = when (slot.status) {
        TimetableSlotStatus.DONE -> 1f
        else -> (slot.elapsedSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
    }

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
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Slot")
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
                Icon(Icons.Rounded.Edit, contentDescription = "Edit Slot")
            }
        }
    ) {
        Card(
            onClick = onCycleStatus,
            modifier = Modifier.fillMaxWidth(),
            transformation = SurfaceTransformation(transformation),
            shape = RoundedCornerShape(18.dp),
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
                            color = slot.status.color.copy(alpha = 0.20f),
                            size = Size(size.width * slotProgress, size.height),
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                        )
                        drawContent()
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tabular Row: Time Column | Task & Category | Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Minute-Precise Start & End Column
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slot.startTime,
                            style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Black,
                            color = slot.status.color
                        )
                        Text(
                            text = slot.endTime,
                            style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Task Name & Estimated / Timer Progress
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slot.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (slot.status == TimetableSlotStatus.DONE || slot.status == TimetableSlotStatus.NOT_DOING) {
                                TextDecoration.LineThrough
                            } else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val timerInfo = if (slot.isTimerRunning || slot.elapsedSeconds > 0) {
                            "${formatDuration(slot.elapsedSeconds)} / ${slot.estimatedMinutes}m (${(slotProgress * 100).roundToInt()}%)"
                        } else {
                            "${slot.categoryTag} · Est ${slot.estimatedMinutes}m"
                        }
                        Text(
                            text = timerInfo,
                            style = MaterialTheme.typography.bodySmall.tabularNums().copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    // Status Pill (Tap to cycle Doing / Done / Skip / Not Doing / Delegate)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(slot.status.color.copy(alpha = 0.22f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = slot.status.badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            fontWeight = FontWeight.Black,
                            color = slot.status.color
                        )
                    }
                }

                // Bottom Control Strip: Timer Start/Pause, Order Up/Down, Snooze/Backlog, Edit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallCircleActionIcon(
                            icon = if (slot.isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            tint = slot.status.color,
                            onClick = onToggleTimer
                        )
                        SmallCircleActionIcon(
                            icon = Icons.Rounded.Snooze,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onOpenSnoozeOrStop
                        )
                        SmallCircleActionIcon(
                            icon = Icons.Rounded.Edit,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = onEdit
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (canMoveUp) {
                            SmallCircleActionIcon(
                                icon = Icons.Rounded.KeyboardArrowUp,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onClick = onMoveUp
                            )
                        }
                        if (canMoveDown) {
                            SmallCircleActionIcon(
                                icon = Icons.Rounded.KeyboardArrowDown,
                                tint = MaterialTheme.colorScheme.onSurface,
                                onClick = onMoveDown
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// PAGE 3: DAILY SCORE, FULL MONTH SUMMARY MATRIX & REMINDER ALERTS
// =============================================================================
@Composable
private fun HabitsMonthlySummaryAndScorePage(
    spec: WearAdaptiveSpec,
    state: LifeState,
    overallScore: Int,
    completedHabits: Int,
    totalHabits: Int,
    doneSlots: Int,
    totalSlots: Int,
    pendingSlots: List<TimetableSlotMeta>,
    onTriggerReminderNotification: () -> Unit,
    onPushAllPendingToTomorrow: () -> Unit,
    onJumpToTop: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpToTop,
                label = "Active Ring",
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
                    title = "SCORE & MONTHLY",
                    icon = Icons.Rounded.Insights,
                    badgeText = "$overallScore / 100 PTS",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. OVERALL DAILY PRODUCTIVITY SCORE CARD
            item {
                Card(
                    onClick = onTriggerReminderNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DAILY COMPOSITE SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$overallScore% Efficiency",
                                    style = MaterialTheme.typography.titleMedium.tabularNums(),
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = "Send Alert", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "Habits: $completedHabits/$totalHabits done · Timetable: $doneSlots/$totalSlots blocks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { (overallScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }



            // 3. REAL NOTIFICATION DISPATCH & NEXT-DAY BACKLOG ROLLOVER
            item {
                CatalogM3ListHeaderSample(title = "REMINDERS & BACKLOG")
            }

            item {
                FilledTonalButton(
                    onClick = onTriggerReminderNotification,
                    icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Watch Alert Reminder")
                }
            }

            if (pendingSlots.isNotEmpty()) {
                item {
                    FilledTonalButton(
                        onClick = onPushAllPendingToTomorrow,
                        icon = { Icon(Icons.Rounded.NextPlan, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Roll ${pendingSlots.size} Pending to Tomorrow")
                    }
                }
            }
        }
    }
}

// =============================================================================
// HORIZONTAL SCROLLABLE DATE CHIP SELECTOR (DAILY / WEEKLY / MONTHLY)
// =============================================================================
@Composable
private fun HorizontalDateChipBar(
    selectedOffset: Int,
    onSelectOffset: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val today = remember { LocalDate.now() }
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (offset in -3..5) {
            val date = today.plusDays(offset.toLong())
            val isSelected = offset == selectedOffset
            val dayName = if (offset == 0) "TODAY" else date.format(dayFmt).uppercase(Locale.getDefault())

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelectOffset(offset) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium.tabularNums(),
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// =============================================================================
// MODAL OVERLAY FOR SNOOZE DURATION, STOP/TODAY ONLY, BACKLOG & MOOD LOGGING
// =============================================================================
@Composable
private fun M3HabitAndTimetableControlModal(
    modalType: ActiveControlModal,
    customSnoozeMinutes: Int,
    currentMeta: HabitExtendedMeta?,
    onAdjustSnoozeMinutes: (Int) -> Unit,
    onConfirmSnooze: (Int) -> Unit,
    onStopForTodayOnly: () -> Unit,
    onSkipOrRescheduleDays: (Int) -> Unit,
    onSaveMoodAndSatisfaction: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    var selectedMood by remember { mutableStateOf(currentMeta?.lastMood ?: "Focused") }
    var selectedStars by remember { mutableIntStateOf(currentMeta?.satisfactionStars ?: 5) }

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
            when (modalType) {
                ActiveControlModal.SNOOZE_PICKER -> {
                    item {
                        WearExpressiveHeader(title = "SNOOZE DURATION", badgeText = "${customSnoozeMinutes}M")
                    }
                    item {
                        WearExpressiveHorizontalSlider(
                            title = "Custom Snooze",
                            valueText = "$customSnoozeMinutes min",
                            onValueDecrease = { onAdjustSnoozeMinutes(-5) },
                            onValueIncrease = { onAdjustSnoozeMinutes(5) },
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
                    val presets = listOf(5, 10, 15, 30, 60)
                    items(presets.size) { idx ->
                        val mins = presets[idx]
                        FilledTonalButton(
                            onClick = { onConfirmSnooze(mins) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Snooze $mins Minutes")
                        }
                    }
                }

                ActiveControlModal.STOP_AND_RESCHEDULE -> {
                    item {
                        WearExpressiveHeader(title = "STOP OR BACKLOG", badgeText = "OPTIONS")
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
                            onClick = { onSkipOrRescheduleDays(0) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip Item Today")
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = { onSkipOrRescheduleDays(1) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Move to Tomorrow Backlog")
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = { onSkipOrRescheduleDays(2) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Schedule +2 Days Ahead")
                        }
                    }
                }

                ActiveControlModal.MOOD_SATISFACTION_LOGGER -> {
                    item {
                        WearExpressiveHeader(title = "MOOD & SATISFACTION", badgeText = "${selectedStars}★")
                    }
                    item {
                        WearActionButtonGroup(
                            options = listOf("Focused", "Energized", "Calm", "Tired"),
                            selectedIndex = listOf("Focused", "Energized", "Calm", "Tired").indexOf(selectedMood).coerceAtLeast(0),
                            onOptionSelected = { idx ->
                                selectedMood = listOf("Focused", "Energized", "Calm", "Tired")[idx]
                            }
                        )
                    }
                    item {
                        WearExpressiveHorizontalSlider(
                            title = "Satisfaction",
                            valueText = "$selectedStars / 5 ★",
                            onValueDecrease = { selectedStars = (selectedStars - 1).coerceAtLeast(1) },
                            onValueIncrease = { selectedStars = (selectedStars + 1).coerceAtMost(5) },
                            canDecrease = selectedStars > 1,
                            canIncrease = selectedStars < 5
                        )
                    }
                    item {
                        Button(
                            onClick = { onSaveMoodAndSatisfaction(selectedMood, selectedStars) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Reflection")
                        }
                    }
                }

                ActiveControlModal.NONE -> {}
            }
        }
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAY & SHARED VIEWPORT UTILITIES
// =============================================================================
@Composable
private fun ViewportConstrainedHabitConfirmation(
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


private fun habitProgress(habit: Habit): Float {
    if (habit.doneToday) return 1f
    if (habit.targetValue <= 0.0) return 0f
    return (habit.currentValue / habit.targetValue).toFloat().coerceIn(0f, 1f)
}

private fun displayNumber(value: Double): String =
    if (value.isFinite() && value == value.roundToInt().toDouble()) value.roundToInt().toString()
    else if (value.isFinite()) String.format(Locale.ROOT, "%.1f", value) else "0"

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
}
