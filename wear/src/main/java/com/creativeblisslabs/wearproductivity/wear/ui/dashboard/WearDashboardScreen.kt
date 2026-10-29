package com.creativeblisslabs.wearproductivity.wear.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.creativeblisslabs.wearproductivity.shared.FocusType
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearAdaptiveSpec
import com.creativeblisslabs.wearproductivity.wear.ui.settings.WearAppPreferences
import com.creativeblisslabs.wearproductivity.wear.ui.settings.rememberWearBooleanPreference
import com.creativeblisslabs.wearproductivity.wear.ui.settings.rememberWearIntPreference
import com.creativeblisslabs.wearproductivity.wear.ui.settings.rememberWearStringPreference
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ComplicationSlot(val key: String, val label: String) {
    TOP("face_top_complication", "Top Slot"),
    UPPER_LEFT("face_upper_left_complication", "Upper Left"),
    UPPER_RIGHT("face_upper_right_complication", "Upper Right"),
    BOTTOM_LEFT("face_bottom_left_complication", "Bottom Left"),
    BOTTOM_RIGHT("face_bottom_left_complication", "Bottom Right"),
    BOTTOM("face_bottom_complication","Bottom Slot"),
    RING("face_ring_complication", "Outer Bezel Ring")
}

private enum class ComplicationDataType(
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
    SLEEP("SLEEP", "SLP", "Sleep Duration", Icons.Rounded.Bedtime, Color(0xFFA6B8FF)),
    CALORIES("CALORIES", "KCAL", "Nutrition Calories", Icons.Rounded.LocalFireDepartment, Color(0xFFFFB76B)),
    EXPENSES("EXPENSES", "EXP", "Today's Spend", Icons.Rounded.AccountBalanceWallet, Color(0xFFFFD177)),
    ALARM("ALARM", "ALM", "Next Alarm", Icons.Rounded.Alarm, Color(0xFFFFD177)),
    EVENTS("EVENTS", "EVT", "Upcoming Events", Icons.Rounded.Event, Color(0xFF8EAEFF)),
    ROUTINES("ROUTINES", "RTN", "Daily Routines", Icons.Rounded.Schedule, Color(0xFFBBA5FF)),
    RUNS("RUNS", "RUN", "Run Workouts", Icons.Rounded.DirectionsRun, Color(0xFF79E2B0)),
    BATTERY("BATTERY", "BAT", "Watch Battery", Icons.Rounded.BatteryChargingFull, Color(0xFF79E2B0)),
    WEATHER("WEATHER", "WTH", "Local Weather", Icons.Rounded.WbSunny, Color(0xFFFFD177)),
    DATE("DATE", "DAY", "Current Date", Icons.Rounded.CalendarToday, Color(0xFF8EAEFF)),
    NONE("NONE", "OFF", "Hidden / Empty", Icons.Rounded.Close, Color.Gray);

    companion object {
        fun fromId(id: String): ComplicationDataType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TASKS
    }
}

private data class FaceMetric(
    val type: ComplicationDataType,
    val title: String,
    val value: String,
    val subtitle: String,
    val progress: Float = 0f,
    val visible: Boolean = true
)



@SuppressLint("SuspiciousIndentation")
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearDashboardScreen(state: LifeState, repository: LifeRepository) {
    val haptic = rememberLifeOSHapticProvider()
    val scope = rememberCoroutineScope()
    val uiState by repository.state.collectAsState()
    val appContext = LocalContext.current.applicationContext

    // Stored Preferences (Backed by local mutable states for instant reactivity when edited on-watch)
    val prefHaptics = rememberWearBooleanPreference(WearAppPreferences.KEY_HAPTICS_ENABLED, true)
    val pref24Hour = rememberWearBooleanPreference(WearAppPreferences.KEY_USE_24_HOUR_CLOCK, true)
    val prefCompact = rememberWearBooleanPreference(WearAppPreferences.KEY_COMPACT_DASHBOARD, false)
    val waterGoalMl = rememberWearIntPreference(WearAppPreferences.KEY_WATER_GOAL_ML, 2000).coerceIn(1000, 3000)
    val defaultFocusMinutes = rememberWearIntPreference(WearAppPreferences.KEY_FOCUS_DEFAULT_MINUTES, 25).coerceIn(5, 180)

    val initTop = rememberWearStringPreference(WearAppPreferences.KEY_FACE_TOP_COMPLICATION, "FAST")
    val initLeft = rememberWearStringPreference(WearAppPreferences.KEY_FACE_TOP_LEFT_COMPLICATION,
        "WATER")
    val initRight = rememberWearStringPreference(WearAppPreferences.KEY_FACE_TOP_RIGHT_COMPLICATION,
        "HABITS")
    val initBottom = rememberWearStringPreference(WearAppPreferences.KEY_FACE_BOTTOM_COMPLICATION, "ALARM")
    val initRing = rememberWearStringPreference(WearAppPreferences.KEY_FACE_RING_METRIC, "TASKS")
    val initUpperLeft = rememberWearStringPreference(ComplicationSlot.UPPER_LEFT.key, "HEART")
    val initUpperRight = rememberWearStringPreference(ComplicationSlot.UPPER_RIGHT.key, "FOCUS")

    var use24HourClock by remember(pref24Hour) { mutableStateOf(pref24Hour) }
    var compactCards by remember(prefCompact) { mutableStateOf(prefCompact) }
    var faceTop by remember(initTop) { mutableStateOf(initTop) }
    var faceUpperLeft by remember(initUpperLeft) { mutableStateOf(initUpperLeft) }
    var faceUpperRight by remember(initUpperRight) { mutableStateOf(initUpperRight) }
    var faceBottomLeft by remember(initLeft) { mutableStateOf(initLeft) }
    var faceBottomRight by remember(initRight) { mutableStateOf(initRight) }
    var faceBottom by remember(initBottom) { mutableStateOf(initBottom) }
    var faceRingMetric by remember(initRing) { mutableStateOf(initRing) }

    // Interactive Watch Face Editing States
    var isWatchFaceEditMode by remember { mutableStateOf(false) }
    var selectedSlotForEdit by remember { mutableStateOf<ComplicationSlot?>(null) }

    // Overlay Feedback State (Constrained strictly inside watch viewport)
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    // Live Second/Minute Clock Ticker
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            currentTime = now
            val millisToNextMinute = ((60 - now.second) * 1000L) - (now.nano / 1_000_000L)
            delay(millisToNextMinute.coerceAtLeast(1_000L))
        }
    }

    val clockText = remember(currentTime, use24HourClock) {
        currentTime.format(
            DateTimeFormatter.ofPattern(if (use24HourClock) "HH:mm" else "h:mm", Locale.getDefault())
        )
    }
    val amPmText = remember(currentTime, use24HourClock) {
        if (use24HourClock) "" else currentTime.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
    }
    val dateText = remember(currentTime) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    }

    // Active Focus Timer Ticker
    LaunchedEffect(uiState.focus.active, uiState.focus.isPaused) {
        while (repository.current().focus.active && !repository.current().focus.isPaused) {
            delay(1_000L)
            val liveFocus = repository.current().focus
            if (liveFocus.active && !liveFocus.isPaused) repository.tickFocusSecond(1) else break
        }
    }

    // Battery & Weather Telemetry
    var watchBatteryPercent by remember { mutableIntStateOf(85) }
    var isWatchCharging by remember { mutableStateOf(false) }
    val phoneBatteryPercent by remember { mutableIntStateOf(90) }
    val weatherTempText = "28°C"

    LaunchedEffect(Unit) {
        try {
            val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                watchBatteryPercent = ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
            }
            isWatchCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        } catch (_: Exception) {}
    }

    fun click() {
        if (prefHaptics) haptic.click()
    }

    fun runAction(success: String, action: suspend () -> Unit) {
        click()
        scope.launch {
            try {
                action()
                infoDialog = null
                statusMessage = success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                statusMessage = "Action Failed"
            }
        }
    }

    fun showInfo(title: String, body: String) {
        click()
        infoDialog = title to body
    }

    fun sendPhoneRemoteCommand(actionPath: String, actionName: String) {
        click()
        scope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(appContext).connectedNodes.await()
                val phoneNode = nodes.firstOrNull()
                if (phoneNode != null) {
                    Wearable.getMessageClient(appContext)
                        .sendMessage(phoneNode.id, "/lifeos/phone_control/$actionPath", "TOGGLE".toByteArray())
                        .await()
                    statusMessage = "$actionName Sent"
                } else {
                    statusMessage = "$actionName Queued"
                }
            } catch (_: Exception) {
                statusMessage = "$actionName Sent"
            }
        }
    }

    // Derived Domain Metrics (Every single repository entity represented)
    val visibleAlarms = uiState.alarms.filter { it.stamp.deletedAt == null }
    val enabledAlarms = visibleAlarms.count { it.enabled }
    val activeEvents = uiState.events.filter { it.stamp.deletedAt == null }
    val activeDates = uiState.importantDates.filter { it.stamp.deletedAt == null }
    val activeRoutines = uiState.routines.filter { it.stamp.deletedAt == null }
    val doneRoutines = activeRoutines.count { it.doneToday }
    val activeRuns = uiState.runs.filter { it.stamp.deletedAt == null }
    val doneRuns = activeRuns.count { it.completed }
    val taskDone = uiState.activeTasks.count { it.complete }
    val taskTotal = uiState.activeTasks.size
    val habitDone = uiState.activeHabits.count { it.doneToday }
    val habitTotal = uiState.activeHabits.size
    val waterMl = uiState.wellness.waterGlasses.coerceAtLeast(0) * 250
    val stepsCount = uiState.wellness.steps.coerceAtLeast(0)
    val sleepMinutes = uiState.wellness.sleepLastNightMinutes.coerceAtLeast(0)
    val activeFoodLogs = uiState.foodLogs.filter { it.stamp.deletedAt == null }
    val totalCalories = activeFoodLogs.sumOf { it.calories }
    val totalProtein = activeFoodLogs.sumOf { it.proteinGrams }.toInt()
    val activeExpenses = uiState.expenses.filter { it.stamp.deletedAt == null }
    val totalExpenseAmount = activeExpenses.sumOf { it.amount }.toInt()

    fun completeNextTask() {
        val next = uiState.activeTasks.firstOrNull { !it.complete }
        if (next == null) showInfo("Tasks Complete", "All $taskDone of $taskTotal tasks are finished.")
        else runAction("Done: ${next.title.take(14)}") { repository.completeTask(next.stamp.id) }
    }

    fun completeNextHabit() {
        val next = uiState.activeHabits.firstOrNull { !it.doneToday }
        if (next == null) showInfo("Habits Complete", "All $habitDone active habits completed today.")
        else runAction("Checked: ${next.name.take(14)}") { repository.checkHabit(next.stamp.id) }
    }

    fun completeNextRoutine() {
        val next = activeRoutines.firstOrNull { !it.doneToday }
        if (next == null) showInfo("Routines Complete", "All $doneRoutines of ${activeRoutines.size} routines finished.")
        else runAction("Routine: ${next.title.take(12)}") { repository.toggleRoutine(next.stamp.id) }
    }

    fun toggleFocus() {
        if (uiState.focus.active) {
            runAction(if (uiState.focus.isPaused) "Focus Resumed" else "Focus Paused") {
                repository.togglePauseFocusSession()
            }
        } else {
            runAction("Focus Started") {
                repository.startFocusSession(defaultFocusMinutes, FocusType.Pomodoro, "Watch Face")
            }
        }
    }

    fun logWater() {
        if (waterMl >= 3000) {
            showInfo("Hydration Cap", "Daily cap of 3,000 ml reached. Goal: $waterGoalMl ml.")
        } else {
            runAction("+250 ml Logged") { repository.addWater(250) }
        }
    }

    fun toggleAlarm() {
        val target = visibleAlarms.filter { it.enabled }.minByOrNull { it.time }
            ?: visibleAlarms.minByOrNull { it.time }
        if (target == null) showInfo("Alarms", "No alarms configured yet.")
        else runAction(if (target.enabled) "Alarm Off" else "Alarm On") {
            repository.toggleAlarm(target.stamp.id)
        }
    }

    fun completeNextWorkout() {
        val next = activeRuns.firstOrNull { !it.completed }
        if (next == null) showInfo("Workouts", "All ${activeRuns.size} planned runs are complete.")
        else runAction("Run Completed") { repository.completeRunWorkout(next.stamp.id) }
    }

    fun metricFor(id: String): FaceMetric {
        val type = ComplicationDataType.fromId(id)
        return when (type) {
            ComplicationDataType.FAST -> {
                val pct = (uiState.fast.progress.coerceIn(0f, 1f) * 100f).toInt()
                FaceMetric(
                    type = type,
                    title = "FAST",
                    value = if (uiState.fast.active) "$pct%" else "OFF",
                    subtitle = if (uiState.fast.active) "Active window" else "Tap to start",
                    progress = if (uiState.fast.active) uiState.fast.progress.coerceIn(0f, 1f) else 0f
                )
            }
            ComplicationDataType.WATER -> FaceMetric(
                type = type,
                title = "H2O",
                value = String.format(Locale.ROOT, "%.1fL", waterMl / 1000f),
                subtitle = "$waterMl / $waterGoalMl ml",
                progress = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
            )
            ComplicationDataType.HABITS -> FaceMetric(
                type = type,
                title = "HAB",
                value = "$habitDone/$habitTotal",
                subtitle = "Daily habits",
                progress = if (habitTotal > 0) (habitDone.toFloat() / habitTotal).coerceIn(0f, 1f) else 0f
            )
            ComplicationDataType.TASKS -> FaceMetric(
                type = type,
                title = "TSK",
                value = "$taskDone/$taskTotal",
                subtitle = "Open tasks",
                progress = if (taskTotal > 0) (taskDone.toFloat() / taskTotal).coerceIn(0f, 1f) else 0f
            )
            ComplicationDataType.FOCUS -> {
                val prog = if (uiState.focus.active && uiState.focus.totalSeconds > 0) {
                    1f - (uiState.focus.remainingSeconds.toFloat() / uiState.focus.totalSeconds.toFloat())
                } else 0f
                FaceMetric(
                    type = type,
                    title = "FOC",
                    value = when {
                        uiState.focus.active && uiState.focus.isPaused -> "PAUSE"
                        uiState.focus.active -> "${uiState.focus.remainingMinutes}M"
                        else -> "${defaultFocusMinutes}M"
                    },
                    subtitle = if (uiState.focus.active) uiState.focus.subject else "Pomodoro",
                    progress = prog.coerceIn(0f, 1f)
                )
            }
            ComplicationDataType.HEART -> {
                val bpm = uiState.wellness.currentHeartRateBpm.toInt()
                FaceMetric(
                    type = type,
                    title = "BPM",
                    value = if (bpm > 0) "$bpm" else "72",
                    subtitle = uiState.wellness.heartRateAccuracy,
                    progress = ((if (bpm > 0) bpm else 72) / 180f).coerceIn(0f, 1f)
                )
            }
            ComplicationDataType.STEPS -> FaceMetric(
                type = type,
                title = "STP",
                value = if (stepsCount >= 1000) String.format(Locale.ROOT, "%.1fk", stepsCount / 1000f) else "$stepsCount",
                subtitle = "$stepsCount / 8,000 steps",
                progress = (stepsCount / 8000f).coerceIn(0f, 1f)
            )
            ComplicationDataType.SLEEP -> {
                val hrs = sleepMinutes / 60
                val mins = sleepMinutes % 60
                FaceMetric(
                    type = type,
                    title = "SLP",
                    value = if (sleepMinutes > 0) "${hrs}h${mins}m" else "7h30m",
                    subtitle = "Last night sleep",
                    progress = ((if (sleepMinutes > 0) sleepMinutes else 450) / 480f).coerceIn(0f, 1f)
                )
            }
            ComplicationDataType.CALORIES -> FaceMetric(
                type = type,
                title = "KCAL",
                value = "$totalCalories",
                subtitle = "${totalProtein}g protein logged",
                progress = (totalCalories / 2200f).coerceIn(0f, 1f)
            )
            ComplicationDataType.EXPENSES -> FaceMetric(
                type = type,
                title = "EXP",
                value = "₹$totalExpenseAmount",
                subtitle = "${activeExpenses.size} entries today",
                progress = (totalExpenseAmount / 2000f).coerceIn(0f, 1f)
            )
            ComplicationDataType.ALARM -> {
                val alarm = visibleAlarms.filter { it.enabled }.minByOrNull { it.time } ?: visibleAlarms.minByOrNull { it.time }
                FaceMetric(
                    type = type,
                    title = "ALM",
                    value = alarm?.let { formatStoredAlarmTime(it.time, use24HourClock) } ?: "OFF",
                    subtitle = "$enabledAlarms active",
                    progress = if (enabledAlarms > 0) 1f else 0f
                )
            }
            ComplicationDataType.EVENTS -> FaceMetric(
                type = type,
                title = "EVT",
                value = "${activeEvents.size}",
                subtitle = "${activeDates.size} important dates",
                progress = (activeEvents.size / 5f).coerceIn(0f, 1f)
            )
            ComplicationDataType.ROUTINES -> FaceMetric(
                type = type,
                title = "RTN",
                value = "$doneRoutines/${activeRoutines.size}",
                subtitle = "Daily routines",
                progress = if (activeRoutines.isNotEmpty()) (doneRoutines.toFloat() / activeRoutines.size).coerceIn(0f, 1f) else 0f
            )
            ComplicationDataType.RUNS -> FaceMetric(
                type = type,
                title = "RUN",
                value = "$doneRuns/${activeRuns.size}",
                subtitle = "Workout runs",
                progress = if (activeRuns.isNotEmpty()) (doneRuns.toFloat() / activeRuns.size).coerceIn(0f, 1f) else 0f
            )
            ComplicationDataType.BATTERY -> FaceMetric(
                type = type,
                title = "BAT",
                value = "$watchBatteryPercent%",
                subtitle = "Phone $phoneBatteryPercent%",
                progress = (watchBatteryPercent / 100f).coerceIn(0f, 1f)
            )
            ComplicationDataType.WEATHER -> FaceMetric(
                type = type,
                title = "WTH",
                value = weatherTempText,
                subtitle = "Clear · Outdoor",
                progress = 0.7f
            )
            ComplicationDataType.DATE -> FaceMetric(
                type = type,
                title = "DAY",
                value = dateText.uppercase(Locale.getDefault()),
                subtitle = "Calendar",
                progress = 1f
            )
            ComplicationDataType.NONE -> FaceMetric(
                type = type,
                title = "OFF",
                value = "--",
                subtitle = "Empty slot",
                visible = false
            )
        }
    }

    fun triggerComplicationAction(metric: FaceMetric) {
        when (metric.type) {
            ComplicationDataType.FAST -> runAction(if (uiState.fast.active) "Fast Ended" else "Fast Started") {
                repository.toggleFast()
            }
            ComplicationDataType.WATER -> logWater()
            ComplicationDataType.HABITS -> completeNextHabit()
            ComplicationDataType.TASKS -> completeNextTask()
            ComplicationDataType.FOCUS -> toggleFocus()
            ComplicationDataType.HEART -> showInfo("Heart Rate Telemetry", "Current: ${metric.value} BPM (${metric.subtitle})")
            ComplicationDataType.STEPS -> runAction("+500 Steps Synced") {
                repository.updateSteps((stepsCount + 500).toLong())
            }
            ComplicationDataType.SLEEP -> showInfo("Sleep Recovery", "Last night: ${metric.value}")
            ComplicationDataType.CALORIES -> showInfo("Nutrition Summary", "$totalCalories kcal · ${totalProtein}g protein across ${activeFoodLogs.size} meals.")
            ComplicationDataType.EXPENSES -> showInfo("Daily Expenses", "Total spent: ₹$totalExpenseAmount across ${activeExpenses.size} transactions.")
            ComplicationDataType.ALARM -> toggleAlarm()
            ComplicationDataType.EVENTS -> showInfo("Agenda", "${activeEvents.size} upcoming events & ${activeDates.size} important dates.")
            ComplicationDataType.ROUTINES -> completeNextRoutine()
            ComplicationDataType.RUNS -> completeNextWorkout()
            ComplicationDataType.BATTERY -> showInfo("Battery Status", "Watch: $watchBatteryPercent% ${if (isWatchCharging) "(Charging)" else ""}\nPhone Companion: $phoneBatteryPercent%")
            ComplicationDataType.WEATHER -> showInfo("Weather Conditions", "Current: $weatherTempText · Clear sky")
            ComplicationDataType.DATE -> showInfo("Today", "$dateText · $taskDone/$taskTotal tasks done")
            ComplicationDataType.NONE -> {}
        }
    }

    fun getSlotTypeId(slot: ComplicationSlot): String = when (slot) {
        ComplicationSlot.TOP -> faceTop
        ComplicationSlot.UPPER_LEFT -> faceUpperLeft
        ComplicationSlot.UPPER_RIGHT -> faceUpperRight
        ComplicationSlot.BOTTOM_LEFT -> faceBottomLeft
        ComplicationSlot.BOTTOM_RIGHT -> faceBottomRight
        ComplicationSlot.BOTTOM -> faceBottom
        ComplicationSlot.RING -> faceRingMetric
    }

    fun updateSlotComplication(slot: ComplicationSlot, newTypeId: String) {
        click()
        when (slot) {
            ComplicationSlot.TOP -> {
                faceTop = newTypeId
                WearAppPreferences.setString(appContext, WearAppPreferences.KEY_FACE_TOP_COMPLICATION, newTypeId)
            }
            ComplicationSlot.UPPER_LEFT -> {
                faceUpperLeft = newTypeId
                WearAppPreferences.setString(appContext, ComplicationSlot.UPPER_LEFT.key, newTypeId)
            }
            ComplicationSlot.UPPER_RIGHT -> {
                faceUpperRight = newTypeId
                WearAppPreferences.setString(appContext, ComplicationSlot.UPPER_RIGHT.key, newTypeId)
            }
            ComplicationSlot.BOTTOM_LEFT -> {
                faceBottomLeft = newTypeId
                WearAppPreferences.setString(appContext, ComplicationSlot.BOTTOM_LEFT.key, newTypeId)
            }
            ComplicationSlot.BOTTOM_RIGHT -> {
                WearAppPreferences.setString(appContext, ComplicationSlot.BOTTOM_RIGHT.key, newTypeId)
            }
            ComplicationSlot.BOTTOM -> {
                faceBottom = newTypeId
                WearAppPreferences.setString(appContext, WearAppPreferences.KEY_FACE_BOTTOM_COMPLICATION, newTypeId)
            }
            ComplicationSlot.RING -> {
                faceRingMetric = newTypeId
                WearAppPreferences.setString(appContext, WearAppPreferences.KEY_FACE_RING_METRIC, newTypeId)
            }
        }
        statusMessage = "${slot.label}: ${ComplicationDataType.fromId(newTypeId).shortLabel}"
    }

    fun applyFacePreset(presetName: String) {
        click()
        when (presetName) {
            "Productivity" -> {
                updateSlotComplication(ComplicationSlot.TOP, "FOCUS")
                updateSlotComplication(ComplicationSlot.UPPER_LEFT, "TASKS")
                updateSlotComplication(ComplicationSlot.UPPER_RIGHT, "HABITS")
                updateSlotComplication(ComplicationSlot.BOTTOM_LEFT, "EVENTS")
                updateSlotComplication(ComplicationSlot.BOTTOM_RIGHT, "ROUTINES")
                updateSlotComplication(ComplicationSlot.BOTTOM, "ALARM")
                updateSlotComplication(ComplicationSlot.RING, "TASKS")
            }
            "Fitness & Bio" -> {
                updateSlotComplication(ComplicationSlot.TOP, "HEART")
                updateSlotComplication(ComplicationSlot.UPPER_LEFT, "STEPS")
                updateSlotComplication(ComplicationSlot.UPPER_RIGHT, "CALORIES")
                updateSlotComplication(ComplicationSlot.BOTTOM_LEFT, "WATER")
                updateSlotComplication(ComplicationSlot.BOTTOM_RIGHT, "FAST")
                updateSlotComplication(ComplicationSlot.BOTTOM, "RUNS")
                updateSlotComplication(ComplicationSlot.RING, "STEPS")
            }
            "Minimal" -> {
                updateSlotComplication(ComplicationSlot.TOP, "DATE")
                updateSlotComplication(ComplicationSlot.UPPER_LEFT, "NONE")
                updateSlotComplication(ComplicationSlot.UPPER_RIGHT, "NONE")
                updateSlotComplication(ComplicationSlot.BOTTOM_LEFT, "BATTERY")
                updateSlotComplication(ComplicationSlot.BOTTOM_RIGHT, "WEATHER")
                updateSlotComplication(ComplicationSlot.BOTTOM, "ALARM")
                updateSlotComplication(ComplicationSlot.RING, "FOCUS")
            }
        }
        statusMessage = "$presetName Preset Applied"
    }

    val ringMetricObj = metricFor(faceRingMetric)

    // 3-Page Vertical Pager:
    // Page 0 = Interactive Multi-Complication Watch Face
    // Page 1 = Watch Face Studio (Complication & Ring Customizer)
    // Page 2 = Complete All-Data Stats Deck
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    val adaptiveSpec = LocalWearAdaptiveSpec.current
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
                    0 -> M3WatchFacePage(
                        spec = adaptiveSpec,
                        clockText = clockText,
                        amPmText = amPmText,
                        dateText = dateText,
                        watchBatteryPercent = watchBatteryPercent,
                        isWatchCharging = isWatchCharging,
                        phoneBatteryPercent = phoneBatteryPercent,
                        weatherTempText = weatherTempText,
                        top = metricFor(faceTop),
                        upperLeft = metricFor(faceUpperLeft),
                        upperRight = metricFor(faceUpperRight),
                        bottomLeft = metricFor(faceBottomLeft),
                        bottomRight = metricFor(faceBottomRight),
                        left = metricFor(faceBottomLeft),
                        right = metricFor(faceBottomRight),
                        bottom = metricFor(faceBottom),
                        ringMetric = ringMetricObj,
                        ringEnabled = !faceRingMetric.equals("NONE", ignoreCase = true),
                        compact = compactCards,
                        isEditMode = isWatchFaceEditMode,
                        onToggleEditMode = {
                            click()
                            isWatchFaceEditMode = !isWatchFaceEditMode
                            statusMessage = if (isWatchFaceEditMode) "Tap any slot to edit" else "Watch Face Locked"
                        },
                        onComplicationClick = { slot, metric ->
                            if (isWatchFaceEditMode) {
                                click()
                                selectedSlotForEdit = slot
                            } else {
                                triggerComplicationAction(metric)
                            }
                        },
                        onOpenEditorScreen = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onOpenStatsDeck = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    1 -> M3WatchFaceCustomizerPage(
                        spec = adaptiveSpec,
                        use24HourClock = use24HourClock,
                        compactRing = compactCards,
                        getSlotMetric = { slot -> metricFor(getSlotTypeId(slot)) },
                        onSelectSlotToEdit = { slot ->
                            click()
                            selectedSlotForEdit = slot
                        },
                        onCycleSlotQuick = { slot ->
                            val current = ComplicationDataType.fromId(getSlotTypeId(slot))
                            val nextIndex = (ComplicationDataType.entries.indexOf(current) + 1) % ComplicationDataType.entries.size
                            updateSlotComplication(slot, ComplicationDataType.entries[nextIndex].id)
                        },
                        onToggle24Hour = {
                            val next = !use24HourClock
                            use24HourClock = next
                            WearAppPreferences.setBoolean(appContext, WearAppPreferences.KEY_USE_24_HOUR_CLOCK, next)
                            click()
                            statusMessage = if (next) "24-Hour Clock" else "12-Hour Clock"
                        },
                        onToggleCompactRing = {
                            val next = !compactCards
                            compactCards = next
                            WearAppPreferences.setBoolean(appContext, WearAppPreferences.KEY_COMPACT_DASHBOARD, next)
                            click()
                            statusMessage = if (next) "Continuous Bezel Ring" else "Segmented Bezel Ring"
                        },
                        onApplyPreset = ::applyFacePreset,
                        onBackToWatchFace = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onOpenStatsDeck = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        isEditMode = isWatchFaceEditMode,
                        onToggleEditMode = {
                            click()
                            isWatchFaceEditMode = !isWatchFaceEditMode
                            statusMessage = if (isWatchFaceEditMode) "Tap any slot to edit" else "Watch Face Locked"
                        }
                        )

                    2 -> M3DashboardStatsDeckPage(
                        spec = adaptiveSpec,
                        state = uiState,
                        taskDone = taskDone,
                        taskTotal = taskTotal,
                        habitDone = habitDone,
                        habitTotal = habitTotal,
                        waterMl = waterMl,
                        waterGoalMl = waterGoalMl,
                        stepsCount = stepsCount,
                        sleepMinutes = sleepMinutes,
                        totalCalories = totalCalories,
                        totalProtein = totalProtein,
                        totalExpenseAmount = totalExpenseAmount,
                        expensesCount = activeExpenses.size,
                        defaultFocusMinutes = defaultFocusMinutes,
                        enabledAlarms = enabledAlarms,
                        totalAlarms = visibleAlarms.size,
                        eventsCount = activeEvents.size,
                        routinesDone = doneRoutines,
                        routinesCount = activeRoutines.size,
                        datesCount = activeDates.size,
                        runsDone = doneRuns,
                        runsTotal = activeRuns.size,
                        watchBatteryPercent = watchBatteryPercent,
                        phoneBatteryPercent = phoneBatteryPercent,
                        onCompleteTask = ::completeNextTask,
                        onCompleteHabit = ::completeNextHabit,
                        onCompleteRoutine = ::completeNextRoutine,
                        onToggleFocus = ::toggleFocus,
                        onLogWater = ::logWater,
                        onAddSteps = {
                            runAction("+500 Steps Logged") {
                                repository.updateSteps((stepsCount + 500).toLong())
                            }
                        },
                        onToggleFast = {
                            runAction(if (uiState.fast.active) "Fast Ended" else "Fast Started") {
                                repository.toggleFast()
                            }
                        },
                        onToggleAlarm = ::toggleAlarm,
                        onCompleteWorkout = ::completeNextWorkout,
                        onShowInfo = ::showInfo,
                        onRemoteCommand = ::sendPhoneRemoteCommand,
                        onOpenWatchFaceStudio = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onOpenFace = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }

            // Full-Screen Complication Data Picker Modal (Opens when a complication slot is tapped in Edit Mode or Studio)
            selectedSlotForEdit?.let { targetSlot ->
                M3ComplicationPickerOverlay(
                    slot = targetSlot,
                    currentSelectionId = getSlotTypeId(targetSlot),
                    metricProvider = ::metricFor,
                    onSelectType = { chosenType ->
                        updateSlotComplication(targetSlot, chosenType.id)
                        selectedSlotForEdit = null
                    },
                    onDismiss = {
                        click()
                        selectedSlotForEdit = null
                    }
                )
            }

            // Native M3 Vertical Pager Indicator (3 Pages: Face, Face Studio, Stats Deck)
            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 3,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            // 1. In-Viewport Constrained Curved Confirmation Overlay
            statusMessage?.let { msg ->
                ViewportConstrainedCurvedConfirmation(
                    label = msg,
                    onDismiss = { statusMessage = null }
                )
            }

            // 2. In-Viewport Constrained M3 Alert Dialog Overlay
            infoDialog?.let { (title, body) ->
                ViewportConstrainedAlertDialog(
                    spec = adaptiveSpec,
                    title = title,
                    body = body,
                    onDismiss = { infoDialog = null }
                )
            }
        }
    
}

// =============================================================================
// PAGE 0: NATIVE WEAR M3 MULTI-COMPLICATION INTERACTIVE WATCH FACE
// =============================================================================
@Composable
private fun M3WatchFacePage(
    spec: WearAdaptiveSpec,
    clockText: String,
    amPmText: String,
    dateText: String,
    watchBatteryPercent: Int,
    isWatchCharging: Boolean,
    phoneBatteryPercent: Int,
    weatherTempText: String,
    top: FaceMetric,
    upperLeft: FaceMetric,
    upperRight: FaceMetric,
    bottomLeft: FaceMetric,
    bottomRight: FaceMetric,
    left: FaceMetric, // Available if needed, or can use bottomLeft/bottomRight
    right: FaceMetric,
    bottom: FaceMetric,
    ringMetric: FaceMetric,
    ringEnabled: Boolean,
    compact: Boolean,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onComplicationClick: (ComplicationSlot, FaceMetric) -> Unit,
    onOpenEditorScreen: () -> Unit,
    onOpenStatsDeck: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = {},
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = if (isEditMode) onOpenEditorScreen else onOpenStatsDeck,
                label = if (isEditMode) "Face Studio" else "Stats Deck",
                icon = if (isEditMode) Icons.Rounded.Tune else Icons.Rounded.ListAlt
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // 1. Outer Bezel Progress Ring
            if (ringEnabled) {
                val ringDiameter = (minOf(spec.screenWidth, spec.screenHeight) * 0.97f)
                    .coerceAtLeast(160.dp)
                if (compact) {
                    CircularProgressIndicator(
                        progress = { ringMetric.progress.coerceIn(0f, 1f) },
                        startAngle = 125f,
                        endAngle = 55f,
                        modifier = Modifier.size(ringDiameter),
                        strokeWidth = 4.dp,
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = ringMetric.type.accent,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                        )
                    )
                } else {
                    SegmentedCircularProgressIndicator(
                        segmentCount = 8,
                        progress = { ringMetric.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(ringDiameter),
                        strokeWidth = 4.dp,
                        colors = ProgressIndicatorDefaults.colors(
                            indicatorColor = ringMetric.type.accent,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                        )
                    )
                }
            }

            // 2. Watch Face Multi-Complication Grid with Safe Insets for Round Screens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = spec.screenHeight * 0.07f,
                        bottom = spec.screenHeight * 0.07f,
                        start = spec.screenWidth * 0.095f,
                        end = spec.screenWidth * 0.095f
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Row 1: Top Complication + Quick Edit Face Toggle

                    M3ComplicationPill(
                        metric = top,
                        isEditMode = isEditMode,
                        onClick = { onComplicationClick(ComplicationSlot.TOP, top) }
                    )


                // Row 2: Upper Left & Upper Right Complication Slots
                if (upperLeft.visible || upperRight.visible || isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        M3ComplicationPill(
                            metric = upperLeft,
                            isEditMode = isEditMode,
                            onClick = { onComplicationClick(ComplicationSlot.UPPER_LEFT, upperLeft) }
                        )
                        M3ComplicationPill(
                            metric = upperRight,
                            isEditMode = isEditMode,
                            onClick = { onComplicationClick(ComplicationSlot.UPPER_RIGHT, upperRight) }
                        )
                    }
                }

                // Row 3: Center Expressive Time, Date & Ring Metric Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            onClick = {
                                if (isEditMode) onComplicationClick(ComplicationSlot.RING, ringMetric)
                                else onOpenStatsDeck()
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = clockText,
                            style = MaterialTheme.typography.numeralExtraLarge.tabularNums(),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        if (amPmText.isNotBlank()) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = amPmText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateText.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium.compactLineHeight(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (isEditMode) {
                            Text(
                                text = "· RING: ${ringMetric.type.shortLabel}",
                                style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                                color = ringMetric.type.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Telemetry Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WearStatusBadge(
                            text = "$watchBatteryPercent%",
                            icon = if (isWatchCharging) Icons.Default.BatteryChargingFull else Icons.Default.Watch
                        )
                        WearStatusBadge(
                            text = "$phoneBatteryPercent%",
                            icon = Icons.Default.Smartphone
                        )
                        WearStatusBadge(
                            text = weatherTempText,
                            icon = Icons.Default.WbSunny
                        )
                    }
                }

                // Row 4: Bottom-Left & Bottom-Right Complication Slots
                if (bottomLeft.visible || bottomRight.visible || isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        M3ComplicationPill(
                            metric = bottomLeft,
                            isEditMode = isEditMode,
                            onClick = { onComplicationClick(ComplicationSlot.BOTTOM_LEFT, bottomLeft) }
                        )
                        M3ComplicationPill(
                            metric = bottomRight,
                            isEditMode = isEditMode,
                            onClick = { onComplicationClick(ComplicationSlot.BOTTOM_RIGHT, bottomRight) }
                        )
                    }
                }

                // Row 5: Bottom Slot Complication (Above EdgeButton)
                M3ComplicationPill(
                    metric = bottom,
                    isEditMode = isEditMode,
                    onClick = { onComplicationClick(ComplicationSlot.BOTTOM, bottom) }
                )
            }
        }
    }
}

@Composable
private fun M3ComplicationPill(
    metric: FaceMetric,
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    if (!metric.visible && !isEditMode) {
        Spacer(modifier = Modifier.size(width = 64.dp, height = 24.dp))
        return
    }

    val containerColor = if (isEditMode) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
    }

    val borderModifier = if (isEditMode) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier.border(0.75.dp, metric.type.accent.copy(alpha = 0.35f), CircleShape)
    }

    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(containerColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (isEditMode && !metric.visible) Icons.Rounded.Add else metric.type.icon,
            contentDescription = metric.title,
            modifier = Modifier.size(12.dp),
            tint = if (isEditMode) MaterialTheme.colorScheme.primary else metric.type.accent
        )
        Text(
            text = if (!metric.visible) "ADD" else "${metric.title} ${metric.value}",
            style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =============================================================================
// PAGE 1: WATCH FACE STUDIO (SECOND SCREEN TO EDIT COMPLICATIONS & WATCH FACE)
// =============================================================================
@Composable
private fun M3WatchFaceCustomizerPage(
    spec: WearAdaptiveSpec,
    use24HourClock: Boolean,
    compactRing: Boolean,
    getSlotMetric: (ComplicationSlot) -> FaceMetric,
    onSelectSlotToEdit: (ComplicationSlot) -> Unit,
    onCycleSlotQuick: (ComplicationSlot) -> Unit,
    onToggle24Hour: () -> Unit,
    onToggleCompactRing: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onBackToWatchFace: () -> Unit,
    onOpenStatsDeck: () -> Unit,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    ) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onBackToWatchFace,
                label = "Apply & View",
                icon = Icons.Rounded.Check
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Interactive Complication Locations List
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                CatalogM3ListHeaderSample(title = "COMPLICATION SLOTS")

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEditMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable(onClick = onToggleEditMode)
                            .semantics { contentDescription = "Toggle Watch Face Complication Edit Mode" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

//            items(ComplicationSlot.entries.size) { index ->
//                val slot = ComplicationSlot.entries[index]
//                val currentMetric = getSlotMetric(slot)
//
//                Card(
//                    onClick = { onSelectSlotToEdit(slot) },
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(20.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
//                        contentColor = MaterialTheme.colorScheme.onSurface
//                    )
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 10.dp, vertical = 6.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(30.dp)
//                                .clip(CircleShape)
//                                .background(currentMetric.type.accent.copy(alpha = 0.2f)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                imageVector = currentMetric.type.icon,
//                                contentDescription = null,
//                                modifier = Modifier.size(16.dp),
//                                tint = currentMetric.type.accent
//                            )
//                        }
//
//                        Column(modifier = Modifier.weight(1f)) {
//                            Text(
//                                text = slot.label.uppercase(Locale.getDefault()),
//                                style = MaterialTheme.typography.labelSmall.compactLineHeight(),
//                                color = MaterialTheme.colorScheme.primary,
//                                fontWeight = FontWeight.Bold,
//                                maxLines = 1
//                            )
//                            Text(
//                                text = "${currentMetric.type.fullName} (${currentMetric.value})",
//                                style = MaterialTheme.typography.bodySmall.compactLineHeight(),
//                                color = MaterialTheme.colorScheme.onSurface,
//                                fontWeight = FontWeight.SemiBold,
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis
//                            )
//                        }
//
//                        FilledTonalIconButton(
//                            onClick = { onCycleSlotQuick(slot) },
//                            modifier = Modifier.size(30.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Rounded.Refresh,
//                                contentDescription = "Cycle Data Type",
//                                modifier = Modifier.size(15.dp)
//                            )
//                        }
//                    }
//                }
//            }

            // Clock & Bezel Ring Style Settings
            item {
                CatalogM3ListHeaderSample(title = "FACE APPEARANCE")
            }

            item {
                WearExpressiveSwitchRow(
                    label = "24-Hour Clock",
                    checked = use24HourClock,
                    onCheckedChange = { onToggle24Hour() },
                    icon = Icons.Rounded.Schedule,
                    subtitle = if (use24HourClock) "Format 14:30" else "Format 2:30 PM"
                )
            }

            item {
                WearExpressiveSwitchRow(
                    label = "Continuous Bezel Ring",
                    checked = compactRing,
                    onCheckedChange = { onToggleCompactRing() },
                    icon = Icons.Rounded.DataUsage,
                    subtitle = if (compactRing) "Smooth arc ring" else "8-segment expressive ring"
                )
            }

        }
    }
}

// =============================================================================
// COMPLICATION DATA SOURCE PICKER MODAL
// =============================================================================
@Composable
private fun M3ComplicationPickerOverlay(
    slot: ComplicationSlot,
    currentSelectionId: String,
    metricProvider: (String) -> FaceMetric,
    onSelectType: (ComplicationDataType) -> Unit,
    onDismiss: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onDismiss,
                label = "Cancel",
                icon = Icons.Rounded.Close
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 12.dp,
                end = 12.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = slot.label.uppercase(Locale.getDefault()),
                    badgeText = "${slot.label.uppercase(Locale.getDefault())}SELECT DATA"
                )
            }

            items(ComplicationDataType.entries.size) { index ->
                val dataType = ComplicationDataType.entries[index]
                val preview = metricProvider(dataType.id)
                val isSelected = dataType.id.equals(currentSelectionId, ignoreCase = true)

                Card(
                    onClick = { onSelectType(dataType) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = dataType.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else dataType.accent
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dataType.fullName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Live: ${preview.value} · ${preview.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// PAGE 2: NATIVE M3 EXPRESSIVE STATS DECK (ALL TELEMETRY & QUICK ACTIONS)
// =============================================================================
@Composable
private fun M3DashboardStatsDeckPage(
    spec: WearAdaptiveSpec,
    state: LifeState,
    taskDone: Int,
    taskTotal: Int,
    habitDone: Int,
    habitTotal: Int,
    waterMl: Int,
    waterGoalMl: Int,
    stepsCount: Int,
    sleepMinutes: Int,
    totalCalories: Int,
    totalProtein: Int,
    totalExpenseAmount: Int,
    expensesCount: Int,
    defaultFocusMinutes: Int,
    enabledAlarms: Int,
    totalAlarms: Int,
    eventsCount: Int,
    routinesDone: Int,
    routinesCount: Int,
    datesCount: Int,
    runsDone: Int,
    runsTotal: Int,
    watchBatteryPercent: Int,
    phoneBatteryPercent: Int,
    onCompleteTask: () -> Unit,
    onCompleteHabit: () -> Unit,
    onCompleteRoutine: () -> Unit,
    onToggleFocus: () -> Unit,
    onLogWater: () -> Unit,
    onAddSteps: () -> Unit,
    onToggleFast: () -> Unit,
    onToggleAlarm: () -> Unit,
    onCompleteWorkout: () -> Unit,
    onShowInfo: (String, String) -> Unit,
    onRemoteCommand: (String, String) -> Unit,
    onOpenWatchFaceStudio: () -> Unit,
    onOpenFace: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onOpenFace,
                label = "Watch Face",
                icon = Icons.Rounded.KeyboardArrowUp
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Jump to Watch Face Studio
            item {
                FilledTonalButton(
                    onClick = onOpenWatchFaceStudio,
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Customize Watch Face Slots")
                }
            }

            // 1. Companion Phone Quick Remote Bar
            item {
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Lock / Unlock Toggle
                        val isLocked = true // TODO: Replace with your actual state variable
                        FilledIconButton(
                            onClick = {
                                val command = if (isLocked) "unlock" else "lock"
                                val label = if (isLocked) "Unlock" else "Lock"
                                onRemoteCommand(command, label)
                            }
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = if (isLocked) "Unlock Phone" else "Lock Phone"
                            )
                        }

                        // 2. Ringer / Mute Toggle
                        val isRingerOn = true // TODO: Replace with your actual state variable
                        FilledTonalIconButton(
                            onClick = {
                                val command = if (isRingerOn) "mute" else "ringer"
                                val label = if (isRingerOn) "Mute" else "Ringer"
                                onRemoteCommand(command, label)
                            }
                        ) {
                            Icon(
                                imageVector = if (isRingerOn) Icons.Rounded.PhonelinkRing else Icons.Rounded.VolumeMute,
                                contentDescription = if (isRingerOn) "Mute Ringer" else "Enable Ringer"
                            )
                        }

                        // 3. Bluetooth On / Off Toggle
                        val isBluetoothOn = true // TODO: Replace with your actual state variable
                        FilledTonalIconButton(
                            onClick = {
                                val command = if (isBluetoothOn) "bluetooth_off" else "bluetooth"
                                val label = if (isBluetoothOn) "Disable Bluetooth" else "Bluetooth"
                                onRemoteCommand(command, label)
                            }
                        ) {
                            Icon(
                                imageVector = if (isBluetoothOn) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                                contentDescription = if (isBluetoothOn) "Disable Bluetooth" else "Enable Bluetooth"
                            )
                        }

                        // 4. Wi-Fi On / Off Toggle
                        val isWifiOn = true // TODO: Replace with your actual state variable
                        OutlinedIconButton(
                            onClick = {
                                val command = if (isWifiOn) "wifi_off" else "wifi"
                                val label = if (isWifiOn) "Disable Wi-Fi" else "Wi-Fi"
                                onRemoteCommand(command, label)
                            }
                        ) {
                            Icon(
                                imageVector = if (isWifiOn) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                                contentDescription = if (isWifiOn) "Disable Wi-Fi" else "Enable Wi-Fi"
                            )
                        }
                    }
                }
            }

            // 2. Focus Session Primary Action
            item {
                CatalogM3ListHeaderSample(title = "FOCUS & FLOW")
            }

            item {
                val focusSubtitle = when {
                    state.focus.active && state.focus.isPaused -> "Paused · Tap to resume"
                    state.focus.active -> "${state.focus.remainingMinutes}m remaining · Tap to pause"
                    else -> "Ready · Tap to start ${defaultFocusMinutes}m Pomodoro"
                }
                TitleCard(
                    onClick = onToggleFocus,
                    title = { Text("Focus Timer") },
                    time = {
                        Text(
                            if (state.focus.active) "${state.focus.remainingMinutes}M" else "IDLE",
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(focusSubtitle, style = MaterialTheme.typography.bodySmall)
                        if (state.focus.active && state.focus.totalSeconds > 0) {
                            val prog = 1f - (state.focus.remainingSeconds.toFloat() / state.focus.totalSeconds.toFloat())
                            LinearProgressIndicator(
                                progress = { prog.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 3. Interactive Tasks, Habits & Routines
            item {
                CatalogM3ListHeaderSample(title = "DAILY GOALS")
            }

            item {
                SplitCheckboxButton(
                    checked = taskTotal > 0 && taskDone == taskTotal,
                    onCheckedChange = { onCompleteTask() },
                    onContainerClick = {
                        onShowInfo(
                            "Tasks ($taskDone/$taskTotal)",
                            "${(taskTotal - taskDone).coerceAtLeast(0)} open tasks remaining."
                        )
                    },
                    toggleContentDescription = "Complete Next Task",
                    label = { Text("Tasks ($taskDone/$taskTotal)") },
                    secondaryLabel = { Text("Tap checkbox to complete next") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SplitCheckboxButton(
                    checked = habitTotal > 0 && habitDone == habitTotal,
                    onCheckedChange = { onCompleteHabit() },
                    onContainerClick = {
                        onShowInfo(
                            "Habits ($habitDone/$habitTotal)",
                            "Active daily habit streak tracking."
                        )
                    },
                    toggleContentDescription = "Check Next Habit",
                    label = { Text("Habits ($habitDone/$habitTotal)") },
                    secondaryLabel = { Text("Tap checkbox to check next") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SplitCheckboxButton(
                    checked = routinesCount > 0 && routinesDone == routinesCount,
                    onCheckedChange = { onCompleteRoutine() },
                    onContainerClick = {
                        onShowInfo(
                            "Routines ($routinesDone/$routinesCount)",
                            "Morning, afternoon & evening routine blocks."
                        )
                    },
                    toggleContentDescription = "Complete Next Routine",
                    label = { Text("Routines ($routinesDone/$routinesCount)") },
                    secondaryLabel = { Text("Tap checkbox to complete next") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Wellness, Hydration, Steps, Sleep & Fasting
            item {
                CatalogM3ListHeaderSample(title = "WELLNESS & BIOMETRICS")
            }

            item {
                WearMetricCard(
                    title = "HYDRATION",
                    value = "$waterMl / $waterGoalMl ml",
                    subtitle = "Tap to log +250 ml cup",
                    icon = Icons.Rounded.WaterDrop,
                    onClick = onLogWater
                )
            }

            item {
                val fastPct = (state.fast.progress.coerceIn(0f, 1f) * 100f).toInt()
                SplitSwitchButton(
                    checked = state.fast.active,
                    onCheckedChange = { onToggleFast() },
                    onContainerClick = {
                        onShowInfo(
                            "Intermittent Fasting",
                            if (state.fast.active) "$fastPct% of target fast completed." else "Fasting timer is currently inactive."
                        )
                    },
                    toggleContentDescription = "Toggle Fasting",
                    label = { Text("Fasting") },
                    secondaryLabel = { Text(if (state.fast.active) "Active · $fastPct% done" else "Off · Tap to start") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                val hrs = sleepMinutes / 60
                val mins = sleepMinutes % 60
                val sleepStr = if (sleepMinutes > 0) "${hrs}h ${mins}m" else "7h 30m"
                WearMetricCard(
                    title = "STEPS & SLEEP",
                    value = "$stepsCount Steps · $sleepStr",
                    subtitle = "Tap to log +500 steps activity",
                    icon = Icons.Rounded.DirectionsWalk,
                    onClick = onAddSteps
                )
            }

            item {
                val bpmText = if (state.wellness.currentHeartRateBpm > 0.0) {
                    "${state.wellness.currentHeartRateBpm.toInt()} BPM"
                } else "72 BPM"
                WearMetricCard(
                    title = "HEART RATE & MOOD",
                    value = "$bpmText · ${state.wellness.mood}",
                    subtitle = "Energy ${state.wellness.energy}/10 · Weight ${state.wellness.bodyWeight} kg",
                    icon = Icons.Rounded.Favorite,
                    onClick = { onShowInfo("Biometrics", "Heart Rate: $bpmText\nMood: ${state.wellness.mood}\nEnergy: ${state.wellness.energy}/10") }
                )
            }

            // 5. Nutrition & Finance
            item {
                CatalogM3ListHeaderSample(title = "NUTRITION & FINANCE")
            }

            item {
                WearMetricCard(
                    title = "NUTRITION LOG",
                    value = "$totalCalories kcal · ${totalProtein}g P",
                    subtitle = "${state.foodLogs.count { it.stamp.deletedAt == null }} meals logged today",
                    icon = Icons.Rounded.LocalFireDepartment,
                    onClick = {
                        onShowInfo("Nutrition", "Calories: $totalCalories kcal\nProtein: ${totalProtein}g")
                    }
                )
            }

            item {
                WearMetricCard(
                    title = "DAILY EXPENSES",
                    value = "₹$totalExpenseAmount Spent",
                    subtitle = "$expensesCount transactions recorded",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    onClick = {
                        onShowInfo("Expenses", "Total: ₹$totalExpenseAmount across $expensesCount items.")
                    }
                )
            }

            // 6. Alarms, Workouts & Calendar Schedule
            item {
                CatalogM3ListHeaderSample(title = "TRAINING & SCHEDULE")
            }

            item {
                SplitSwitchButton(
                    checked = enabledAlarms > 0,
                    onCheckedChange = { onToggleAlarm() },
                    onContainerClick = {
                        onShowInfo("Alarms", "$enabledAlarms of $totalAlarms alarms enabled.")
                    },
                    toggleContentDescription = "Toggle Next Alarm",
                    label = { Text("Alarms ($enabledAlarms/$totalAlarms)") },
                    secondaryLabel = { Text("Tap switch to toggle next alarm") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                WearMetricCard(
                    title = "WORKOUT PLANS",
                    value = "$runsDone / $runsTotal Runs",
                    subtitle = "Tap to mark next planned run complete",
                    icon = Icons.Rounded.DirectionsRun,
                    onClick = onCompleteWorkout
                )
            }

            item {
                AppCard(
                    onClick = {
                        onShowInfo(
                            "Schedule Overview",
                            "$eventsCount events · $routinesCount routines · $datesCount important dates."
                        )
                    },
                    appName = { Text("Calendar & Dates") },
                    title = { Text("$eventsCount Events · $datesCount Dates") },
                    time = { Text("${routinesCount}R") },
                    appImage = {
                        Icon(
                            Icons.Rounded.Cake,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Tap to inspect upcoming events & routines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        
            item {
                val mediaViewModel: com.creativeblisslabs.wearproductivity.wear.media.SharedMediaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val isPlaying = mediaViewModel.isPlaying.collectAsState().value
                val progress = mediaViewModel.progress.collectAsState().value
                com.creativeblisslabs.wearproductivity.wear.ui.components.core.PlayerControlBar(
                    isPlaying = isPlaying,
                    progress = progress,
                    onTogglePlay = { mediaViewModel.togglePlay() },
                    onPrevious = { mediaViewModel.skipToPrevious() },
                    onNext = { mediaViewModel.skipToNext() }
                )
            }
}
    }
}

// =============================================================================
// IN-VIEWPORT CONSTRAINED OVERLAYS & SHARED UTILITIES
// =============================================================================

private fun formatStoredAlarmTime(raw: String, use24HourClock: Boolean): String {
    val match = Regex("""^(\d{1,2})(?::(\d{2}))?\s*(AM|PM)?$""", RegexOption.IGNORE_CASE)
        .matchEntire(raw.trim()) ?: return raw.take(8)
    var hour = match.groupValues[1].toIntOrNull() ?: return raw.take(8)
    val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
    if (minute !in 0..59) return raw.take(8)
    val period = match.groupValues[3].uppercase(Locale.ROOT)
    if (period.isNotEmpty()) {
        if (hour !in 1..12) return raw.take(8)
        hour = (hour % 12) + if (period == "PM") 12 else 0
    } else if (hour !in 0..23) return raw.take(8)

    return if (use24HourClock) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    } else {
        val twelveHour = when (hour % 12) { 0 -> 12; else -> hour % 12 }
        val suffix = if (hour < 12) "AM" else "PM"
        String.format(Locale.getDefault(), "%d:%02d %s", twelveHour, minute, suffix)
    }
}
