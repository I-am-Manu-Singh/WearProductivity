package com.creativeblisslabs.wearproductivity.mobile.ui.habits

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.*
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// =============================================================================
// SHARED MODELS MATCHING WEAR HABITS STUDIO & TIMETABLE
// =============================================================================
enum class MobileHabitFilter(val label: String) {
    ALL("All Habits"), TODO("To Do"), DONE("Completed")
}

enum class MobileHorizonViewMode(val label: String) {
    DAILY("Today"), WEEKLY("Weekly Horizon"), MONTHLY("Monthly Matrix")
}

enum class MobileHabitCompletionMode(val label: String) {
    SINGLE_TAP("Tap Done"),
    MULTI_TAP_AMOUNT("Multi-Tap %"),
    TIMED_SESSION("Timer"),
    MOOD_REFLECTION("Mood & Reflection")
}

enum class MobileTimetableSlotStatus(val label: String, val badge: String, val color: Color) {
    PENDING("Pending", "TODO", Color(0xFF8EAEFF)),
    DOING("Doing Now", "LIVE", Color(0xFF79E2B0)),
    DONE("Completed", "DONE", Color(0xFF4CAF50)),
    SKIPPED("Skipped", "SKIP", Color(0xFFFFD177)),
    NOT_DOING("Not Doing", "DROP", Color(0xFFFF7A9A)),
    DELEGATED("Delegated", "DELG", Color(0xFFBBA5FF)),
    BACKLOG("Next Day Backlog", "BKLOG", Color(0xFFFF9A7A))
}

data class MobileHabitExtendedMeta(
    val habitId: String,
    val tag: String = "Core",
    val iconKey: String = "flame",
    val colorKey: String = "mint",
    val mode: MobileHabitCompletionMode = MobileHabitCompletionMode.MULTI_TAP_AMOUNT,
    val stepIncrement: Double = 1.0,
    val reminderTime: String = "08:00",
    val reminderEnabled: Boolean = true,
    val snoozedUntilEpochMs: Long = 0L,
    val stoppedForDateIso: String = "",
    val lastMood: String = "Focused",
    val satisfactionStars: Int = 5,
    val historyBitmask28: Int = 0
)

data class MobileTimetableSlotMeta(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val estimatedMinutes: Int,
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val status: MobileTimetableSlotStatus = MobileTimetableSlotStatus.PENDING,
    val dateIso: String = LocalDate.now().toString(),
    val categoryTag: String = "Deep Work",
    val orderIndex: Int = 0,
    val snoozedMinutes: Int = 0,
    val timerStartedAtEpochMs: Long = 0L,
    val snoozedUntilEpochMs: Long = 0L
)

val mobileHabitColorMap = mapOf(
    "mint" to Color(0xFF79E2B0),
    "blue" to Color(0xFF68B6FF),
    "coral" to Color(0xFFFF9A7A),
    "lavender" to Color(0xFFBBA5FF),
    "amber" to Color(0xFFFFD177),
    "rose" to Color(0xFFFF7A9A)
)

private object MobileHabitsTimetableStudioPrefs {
    private const val PREFS_NAME = "lifeos_habits_timetable_studio_v3"
    private const val KEY_HABIT_META = "habit_extended_meta_v3"
    private const val KEY_TIMETABLE_SLOTS = "timetable_slots_v3"

    fun loadHabitMetaMap(context: Context): Map<String, MobileHabitExtendedMeta> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HABIT_META, null) ?: return emptyMap()

        if (raw.trimStart().startsWith("[")) {
            return runCatching {
                val array = JSONArray(raw)
                buildMap {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("habitId")
                        if (id.isBlank()) continue
                        put(
                            id,
                            MobileHabitExtendedMeta(
                                habitId = id,
                                tag = item.optString("tag", "Core"),
                                iconKey = item.optString("iconKey", "flame"),
                                colorKey = item.optString("colorKey", "mint"),
                                mode = runCatching { MobileHabitCompletionMode.valueOf(item.optString("mode")) }.getOrDefault(MobileHabitCompletionMode.MULTI_TAP_AMOUNT),
                                stepIncrement = item.optDouble("stepIncrement", 1.0).takeIf { it.isFinite() && it > 0.0 } ?: 1.0,
                                reminderTime = item.optString("reminderTime", "08:00"),
                                reminderEnabled = item.optBoolean("reminderEnabled", true),
                                snoozedUntilEpochMs = item.optLong("snoozedUntilEpochMs", 0L),
                                stoppedForDateIso = item.optString("stoppedForDateIso", ""),
                                lastMood = item.optString("lastMood", "Focused"),
                                satisfactionStars = item.optInt("satisfactionStars", 5).coerceIn(1, 5),
                                historyBitmask28 = item.optInt("historyBitmask28", 0)
                            )
                        )
                    }
                }
            }.getOrDefault(emptyMap())
        }
        return emptyMap()
    }

    fun saveHabitMetaMap(context: Context, map: Map<String, MobileHabitExtendedMeta>) {
        val json = JSONArray()
        map.values.forEach { meta ->
            json.put(
                JSONObject().apply {
                    put("habitId", meta.habitId)
                    put("tag", meta.tag)
                    put("iconKey", meta.iconKey)
                    put("colorKey", meta.colorKey)
                    put("mode", meta.mode.name)
                    put("stepIncrement", meta.stepIncrement)
                    put("reminderTime", meta.reminderTime)
                    put("reminderEnabled", meta.reminderEnabled)
                    put("snoozedUntilEpochMs", meta.snoozedUntilEpochMs)
                    put("stoppedForDateIso", meta.stoppedForDateIso)
                    put("lastMood", meta.lastMood)
                    put("satisfactionStars", meta.satisfactionStars)
                    put("historyBitmask28", meta.historyBitmask28)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_HABIT_META, json.toString()).apply()
    }

    fun loadTimetableSlots(context: Context, repoRoutines: List<RoutineTask>): List<MobileTimetableSlotMeta> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TIMETABLE_SLOTS, null)
        if (!raw.isNullOrBlank() && raw.trimStart().startsWith("[")) {
            val list = runCatching {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { i ->
                    val item = array.optJSONObject(i) ?: return@mapNotNull null
                    val id = item.optString("id")
                    if (id.isBlank()) return@mapNotNull null
                    val estMins = item.optInt("estimatedMinutes", 30).coerceIn(1, 480)
                    val elapsed = item.optInt("elapsedSeconds", 0).coerceIn(0, estMins * 60)
                    MobileTimetableSlotMeta(
                        id = id,
                        title = item.optString("title", "Routine Slot"),
                        startTime = item.optString("startTime", "08:00"),
                        endTime = item.optString("endTime", "08:30"),
                        estimatedMinutes = estMins,
                        elapsedSeconds = elapsed,
                        isTimerRunning = item.optBoolean("isTimerRunning", false),
                        status = runCatching { MobileTimetableSlotStatus.valueOf(item.optString("status")) }.getOrDefault(MobileTimetableSlotStatus.PENDING),
                        dateIso = item.optString("dateIso", LocalDate.now().toString()),
                        categoryTag = item.optString("categoryTag", "General"),
                        orderIndex = item.optInt("orderIndex", i),
                        snoozedMinutes = item.optInt("snoozedMinutes", 0),
                        timerStartedAtEpochMs = item.optLong("timerStartedAtEpochMs", 0L),
                        snoozedUntilEpochMs = item.optLong("snoozedUntilEpochMs", 0L)
                    )
                }
            }.getOrNull()
            if (!list.isNullOrEmpty()) return list
        }

        // Seed from routines
        return repoRoutines.mapIndexed { i, r ->
            MobileTimetableSlotMeta(
                id = r.stamp.id,
                title = r.title,
                startTime = r.startTime,
                endTime = r.endTime,
                estimatedMinutes = 45,
                status = if (r.doneToday) MobileTimetableSlotStatus.DONE else MobileTimetableSlotStatus.PENDING,
                orderIndex = i,
                categoryTag = r.phase.name
            )
        }
    }

    fun saveTimetableSlots(context: Context, list: List<MobileTimetableSlotMeta>) {
        val array = JSONArray()
        list.forEach { slot ->
            array.put(
                JSONObject().apply {
                    put("id", slot.id)
                    put("title", slot.title)
                    put("startTime", slot.startTime)
                    put("endTime", slot.endTime)
                    put("estimatedMinutes", slot.estimatedMinutes)
                    put("elapsedSeconds", slot.elapsedSeconds)
                    put("isTimerRunning", slot.isTimerRunning)
                    put("timerStartedAtEpochMs", slot.timerStartedAtEpochMs)
                    put("status", slot.status.name)
                    put("dateIso", slot.dateIso)
                    put("categoryTag", slot.categoryTag)
                    put("orderIndex", slot.orderIndex)
                    put("snoozedMinutes", slot.snoozedMinutes)
                    put("snoozedUntilEpochMs", slot.snoozedUntilEpochMs)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TIMETABLE_SLOTS, array.toString()).apply()
    }
}

// 4 Navigation Tabs Matching WearHabitsScreen Pages
enum class MobileHabitsTab(val label: String, val icon: ImageVector) {
    SPOTLIGHT("Spotlight", Icons.Rounded.Lightbulb),
    HABITS_DECK("Habits Deck", Icons.Rounded.Checklist),
    TIMETABLE("Timetable", Icons.Rounded.Schedule),
    SUMMARY_SCORE("Score & Backlog", Icons.Rounded.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    state: LifeState,
    repository: LifeRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(MobileHabitsTab.HABITS_DECK) }

    // Connected Watch State
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            watchNodeId = nodes.firstOrNull()?.id
        } catch (_: Exception) {}
    }

    fun pushWatchReminder(title: String, message: String) {
        if (watchNodeId == null) {
            syncStatusText = "Watch standby (Not connected)"
            return
        }
        coroutineScope.launch {
            try {
                val payload = "$title: $message".toByteArray(Charsets.UTF_8)
                Wearable.getMessageClient(context).sendMessage(
                    watchNodeId!!,
                    "/lifeos/find_watch",
                    payload
                ).await()
                syncStatusText = "Reminder pushed to Watch!"
            } catch (_: Exception) {
                syncStatusText = "Watch sync failed"
            }
        }
    }

    // Extended Metadata & Timetable State
    val metaMap = remember {
        mutableStateMapOf<String, MobileHabitExtendedMeta>().apply {
            putAll(MobileHabitsTimetableStudioPrefs.loadHabitMetaMap(context))
        }
    }

    fun getMeta(habitId: String): MobileHabitExtendedMeta =
        metaMap[habitId] ?: MobileHabitExtendedMeta(habitId = habitId)

    fun updateMeta(meta: MobileHabitExtendedMeta) {
        metaMap[meta.habitId] = meta
        MobileHabitsTimetableStudioPrefs.saveHabitMetaMap(context, metaMap)
    }

    var timetableSlots by remember {
        mutableStateOf(MobileHabitsTimetableStudioPrefs.loadTimetableSlots(context, state.activeRoutines))
    }

    fun updateTimetableSlots(newSlots: List<MobileTimetableSlotMeta>) {
        timetableSlots = newSlots
        MobileHabitsTimetableStudioPrefs.saveTimetableSlots(context, newSlots)
    }

    // Quick add dialogs
    var showCreateHabitDialog by remember { mutableStateOf(false) }
    var newHabitName by remember { mutableStateOf("") }
    var newHabitTarget by remember { mutableStateOf("8") }
    var newHabitUnit by remember { mutableStateOf("cups") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Habits & Timetable Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${state.today.habitsDone} of ${state.today.habitsTotal} Habits Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pushWatchReminder("Habits Check-in", "Synchronizing habits progress from mobile companion")
                    }) {
                        Icon(Icons.Rounded.Watch, contentDescription = "Push to Watch")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateHabitDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable 4-Tab Bar matching Wear OS Page Structure
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileHabitsTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(tab.label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = syncStatusText != null) {
                syncStatusText?.let { msg ->
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
                            TextButton(onClick = { syncStatusText = null }) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                MobileHabitsTab.SPOTLIGHT -> {
                    HabitSpotlightView(
                        state = state,
                        slots = timetableSlots,
                        getMeta = ::getMeta,
                        onAdvanceHabit = { habit ->
                            coroutineScope.launch {
                                repository.updateHabitProgress(habit.stamp.id, habit.currentValue + 1)
                            }
                        },
                        onAdvanceSlot = { slot ->
                            val nextStatus = when (slot.status) {
                                MobileTimetableSlotStatus.PENDING -> MobileTimetableSlotStatus.DOING
                                MobileTimetableSlotStatus.DOING -> MobileTimetableSlotStatus.DONE
                                else -> MobileTimetableSlotStatus.PENDING
                            }
                            val updated = timetableSlots.map { if (it.id == slot.id) it.copy(status = nextStatus) else it }
                            updateTimetableSlots(updated)
                        },
                        onPushWatchAlert = { title, msg -> pushWatchReminder(title, msg) },
                        onJumpToDeck = { currentTab = MobileHabitsTab.HABITS_DECK }
                    )
                }

                MobileHabitsTab.HABITS_DECK -> {
                    HabitsDeckPageView(
                        state = state,
                        repository = repository,
                        getMeta = ::getMeta,
                        onUpdateMeta = ::updateMeta
                    )
                }

                MobileHabitsTab.TIMETABLE -> {
                    TimetableSchedulePageView(
                        slots = timetableSlots,
                        onUpdateSlots = ::updateTimetableSlots,
                        onPushSlotToWatch = { slot ->
                            pushWatchReminder("Active Routine Slot", "${slot.title} (${slot.startTime} - ${slot.endTime})")
                        }
                    )
                }

                MobileHabitsTab.SUMMARY_SCORE -> {
                    HabitsSummaryAndBacklogView(
                        state = state,
                        slots = timetableSlots,
                        onRollBacklog = { pendingSlot ->
                            val updated = timetableSlots.map {
                                if (it.id == pendingSlot.id) it.copy(status = MobileTimetableSlotStatus.BACKLOG) else it
                            }
                            updateTimetableSlots(updated)
                            pushWatchReminder("Backlog Rolled", "${pendingSlot.title} shifted to tomorrow")
                        }
                    )
                }
            }
        }
    }

    if (showCreateHabitDialog) {
        AlertDialog(
            onDismissRequest = { showCreateHabitDialog = false },
            title = { Text("Create New Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        label = { Text("Habit Name (e.g. Drink Water)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newHabitTarget,
                        onValueChange = { newHabitTarget = it },
                        label = { Text("Daily Target (e.g. 8)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newHabitUnit,
                        onValueChange = { newHabitUnit = it },
                        label = { Text("Unit (e.g. cups, ml, mins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newHabitName.isNotBlank()) {
                        coroutineScope.launch {
                            val targetVal = newHabitTarget.toDoubleOrNull() ?: 1.0
                            repository.quickHabit(
                                name = newHabitName.trim(),
                                targetValue = targetVal,
                                unit = newHabitUnit.trim()
                            )
                            showCreateHabitDialog = false
                            newHabitName = ""
                        }
                    }
                }) {
                    Text("Save Habit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateHabitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// 1. SPOTLIGHT VIEW (Matching Wear ActiveHabitAndTimetableBoundaryPage)
// =============================================================================
@Composable
private fun HabitSpotlightView(
    state: LifeState,
    slots: List<MobileTimetableSlotMeta>,
    getMeta: (String) -> MobileHabitExtendedMeta,
    onAdvanceHabit: (Habit) -> Unit,
    onAdvanceSlot: (MobileTimetableSlotMeta) -> Unit,
    onPushWatchAlert: (String, String) -> Unit,
    onJumpToDeck: () -> Unit
) {
    val pendingHabits = state.activeHabits.filter { !it.doneToday }
    val spotlightHabit = pendingHabits.firstOrNull() ?: state.activeHabits.firstOrNull()
    val activeSlot = slots.firstOrNull { it.status == MobileTimetableSlotStatus.DOING }
        ?: slots.firstOrNull { it.status == MobileTimetableSlotStatus.PENDING }

    val habitsDone = state.today.habitsDone
    val habitsTotal = state.today.habitsTotal.coerceAtLeast(1)
    val habitPct = (habitsDone.toFloat() / habitsTotal.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spotlight Mission Card
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
                                "SPOTLIGHT HABIT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            "${(habitPct * 100).toInt()}% Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (spotlightHabit != null) {
                        val meta = getMeta(spotlightHabit.stamp.id)
                        Text(
                            spotlightHabit.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Progress: ${spotlightHabit.currentValue.toInt()} / ${spotlightHabit.targetValue.toInt()} ${spotlightHabit.unit} · Mode: ${meta.mode.label} · Tag: ${meta.tag}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onAdvanceHabit(spotlightHabit) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+1 Step Done")
                            }

                            FilledTonalButton(
                                onClick = { onPushWatchAlert("Habit Spotlight", "${spotlightHabit.name} (${spotlightHabit.currentValue.toInt()}/${spotlightHabit.targetValue.toInt()})") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Push to Watch")
                            }
                        }
                    } else {
                        Text(
                            "All habits completed today!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Timetable Active Slot Card
        if (activeSlot != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = activeSlot.status.color)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Current Routine Slot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = activeSlot.status.color.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    activeSlot.status.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = activeSlot.status.color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(activeSlot.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${activeSlot.startTime} - ${activeSlot.endTime} · Category: ${activeSlot.categoryTag}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onAdvanceSlot(activeSlot) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (activeSlot.status == MobileTimetableSlotStatus.DOING) "Mark Slot Completed" else "Start Slot Now")
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onJumpToDeck,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Full Habits Deck")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =============================================================================
// 2. HABITS DECK PAGE VIEW (Multi-Tap, Step Customizer & Filters)
// =============================================================================
@Composable
private fun HabitsDeckPageView(
    state: LifeState,
    repository: LifeRepository,
    getMeta: (String) -> MobileHabitExtendedMeta,
    onUpdateMeta: (MobileHabitExtendedMeta) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var filterMode by remember { mutableStateOf(MobileHabitFilter.ALL) }
    var horizonMode by remember { mutableStateOf(MobileHorizonViewMode.DAILY) }

    val filteredHabits = remember(state.activeHabits, filterMode) {
        when (filterMode) {
            MobileHabitFilter.ALL -> state.activeHabits
            MobileHabitFilter.TODO -> state.activeHabits.filter { !it.doneToday }
            MobileHabitFilter.DONE -> state.activeHabits.filter { it.doneToday }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Horizon selector: Today / Weekly / Monthly
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MobileHorizonViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = horizonMode == mode,
                        onClick = { horizonMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = MobileHorizonViewMode.entries.size)
                    ) {
                        Text(mode.label)
                    }
                }
            }
        }

        // Filter chips: All, To Do, Done
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MobileHabitFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filterMode == f,
                        onClick = { filterMode = f },
                        label = { Text(f.label) }
                    )
                }
            }
        }

        if (filteredHabits.isEmpty()) {
            item {
                Text("No habits in this filter.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(filteredHabits) { habit ->
                val meta = getMeta(habit.stamp.id)
                val accentColor = mobileHabitColorMap[meta.colorKey] ?: MaterialTheme.colorScheme.primary

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getHabitIcon(habit.name), contentDescription = null, tint = accentColor)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${habit.currentValue.toInt()} / ${habit.targetValue.toInt()} ${habit.unit} · Step: +${meta.stepIncrement.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.updateHabitProgress(habit.stamp.id, habit.currentValue + meta.stepIncrement)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add progress")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        val prog = (habit.currentValue / habit.targetValue).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { prog },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // Mode chip & step increment selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mode: ${meta.mode.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1.0, 2.0, 5.0).forEach { inc ->
                                    AssistChip(
                                        onClick = { onUpdateMeta(meta.copy(stepIncrement = inc)) },
                                        label = { Text("+${inc.toInt()}", style = MaterialTheme.typography.labelSmall) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (meta.stepIncrement == inc) accentColor.copy(alpha = 0.2f) else Color.Transparent
                                        )
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

// =============================================================================
// 3. TIMETABLE TABULAR SCHEDULE PAGE VIEW
// =============================================================================
@Composable
private fun TimetableSchedulePageView(
    slots: List<MobileTimetableSlotMeta>,
    onUpdateSlots: (List<MobileTimetableSlotMeta>) -> Unit,
    onPushSlotToWatch: (MobileTimetableSlotMeta) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Day Timetable Slots (${slots.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(slots) { slot ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = slot.status.color.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    slot.status.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = slot.status.color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(slot.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("${slot.startTime} - ${slot.endTime} · ${slot.categoryTag}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPushSlotToWatch(slot) }) {
                            Icon(Icons.Rounded.Watch, contentDescription = "Push to watch", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            val nextStatus = when (slot.status) {
                                MobileTimetableSlotStatus.PENDING -> MobileTimetableSlotStatus.DOING
                                MobileTimetableSlotStatus.DOING -> MobileTimetableSlotStatus.DONE
                                else -> MobileTimetableSlotStatus.PENDING
                            }
                            val updated = slots.map { if (it.id == slot.id) it.copy(status = nextStatus) else it }
                            onUpdateSlots(updated)
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Cycle status", tint = slot.status.color)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. SUMMARY SCORE & BACKLOG ROLLOVER VIEW (Matching Wear OS Page 4)
// =============================================================================
@Composable
private fun HabitsSummaryAndBacklogView(
    state: LifeState,
    slots: List<MobileTimetableSlotMeta>,
    onRollBacklog: (MobileTimetableSlotMeta) -> Unit
) {
    val habitsDone = state.today.habitsDone
    val habitsTotal = state.today.habitsTotal.coerceAtLeast(1)
    val slotsDone = slots.count { it.status == MobileTimetableSlotStatus.DONE }
    val slotsTotal = slots.size.coerceAtLeast(1)

    val overallScore = ((habitsDone.toFloat() / habitsTotal.toFloat() * 0.5f + slotsDone.toFloat() / slotsTotal.toFloat() * 0.5f) * 100).toInt()
    val pendingSlots = slots.filter { it.status == MobileTimetableSlotStatus.PENDING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Discipline Score Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DAILY CONSISTENCY SCORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$overallScore%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (overallScore >= 80) Color(0xFF4CAF50) else Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$habitsDone/$habitsTotal Habits · $slotsDone/$slotsTotal Routine Slots",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Pending Slots & Next-Day Backlog Rollover
        item {
            Text("Pending Routine Slots for Backlog Rollover", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (pendingSlots.isEmpty()) {
            item {
                Text("No pending slots. All routine activities cleared!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(pendingSlots) { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(slot.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${slot.startTime} - ${slot.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { onRollBacklog(slot) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Push to Tomorrow")
                        }
                    }
                }
            }
        }
    }
}

fun getHabitIcon(name: String): ImageVector {
    return when {
        name.contains("Water", true) || name.contains("Hydration", true) -> Icons.Rounded.WaterDrop
        name.contains("Meditation", true) || name.contains("Zen", true) -> Icons.Rounded.SelfImprovement
        name.contains("Walk", true) || name.contains("Steps", true) -> Icons.Rounded.DirectionsWalk
        name.contains("Read", true) || name.contains("Book", true) -> Icons.Rounded.MenuBook
        else -> Icons.Rounded.CheckCircle
    }
}
