package com.creativeblisslabs.wearproductivity.shared

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class LifeModule(val label: String) {
    Today("Today"),
    Tasks("Tasks"),
    Notes("Notes"),
    Fasting("Fasting"),
    Nutrition("Nutrition"),
    Alarms("Alarms"),
    Habits("Habits"),
    Expenses("Expenses"),
    Calendar("Calendar"),
    Running("C25K"),
    Gym("Gym"),
    Reviews("Reviews"),
    Focus("Focus"),
    Water("Water"),
    Mood("Mood")
}

@Serializable
data class SyncStamp(
    val id: String,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val sourceDeviceId: String = "local",
    val deletedAt: Long? = null,
    val version: Long = 1
)

@Serializable
data class LifeEvent(
    val stamp: SyncStamp,
    val title: String,
    val project: String = "",
    val timeRange: String,
    val location: String = "",
    val reminder: String = "",
    val complete: Boolean = false,
    val tags: List<String> = emptyList(),
    val hapticEnabled: Boolean = false,
    val syncStatus: String = "Synced"
)

@Serializable
data class LifeTask(
    val stamp: SyncStamp,
    val title: String,
    val project: String,
    val dueLabel: String,
    val priority: Int,
    val complete: Boolean = false,
    val tags: List<String> = emptyList(),
    val scheduledTime: String? = null,
    val hapticEnabled: Boolean = false,
    val syncStatus: String = "Synced",
    val kanbanStage: String = "To Do",
    val eisenhowerQuadrant: Int = 1
)

@Serializable
data class TimetableBlock(
    val stamp: SyncStamp,
    val title: String,
    val startTime: String,
    val endTime: String,
    val period: String = "Daily",
    val category: String = "Focus",
    val completed: Boolean = false
)

@Serializable
data class LifeNote(
    val stamp: SyncStamp,
    val title: String,
    val body: String,
    val linkedTaskIds: List<String> = emptyList(),
    val pinned: Boolean = false
)

@Serializable
data class Habit(
    val stamp: SyncStamp,
    val name: String,
    val cadence: String,
    val streak: Int,
    val doneToday: Boolean,
    val colorName: String = "mint",
    val targetValue: Double = 1.0,
    val currentValue: Double = 0.0,
    val unit: String = "",
    val isTimerActive: Boolean = false,
    val remainingSeconds: Int = 0
)

@Serializable
data class FastSession(
    val stamp: SyncStamp,
    val plan: String,
    val startedAtLabel: String,
    val endsAtLabel: String,
    val progress: Float,
    val active: Boolean
)

@Serializable
enum class MealType(val displayName: String, val iconName: String) {
    Breakfast("Breakfast", "FreeBreakfast"),
    Lunch("Lunch", "LunchDining"),
    Dinner("Dinner", "DinnerDining"),
    Snack("Snacks", "Cookie"),
    PreWorkout("Pre-Workout", "Bolt"),
    PostWorkout("Post-Workout", "FitnessCenter")
}

@Serializable
data class FoodLog(
    val stamp: SyncStamp,
    val name: String,
    val mealType: MealType,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val loggedAtLabel: String,
    val notes: String = ""
)

@Serializable
data class NutritionTarget(
    val targetCalories: Int = 2200,
    val targetProteinGrams: Double = 140.0,
    val targetCarbsGrams: Double = 220.0,
    val targetFatGrams: Double = 65.0,
    val targetWaterMl: Int = 3000
)

@Serializable
data class LifeAlarm(
    val stamp: SyncStamp,
    val time: String,
    val label: String,
    val repeat: String,
    val enabled: Boolean,
    val haptic: Boolean = true
)
@Serializable
data class Expense(
    val stamp: SyncStamp,
    val amount: Double,
    val currency: String,
    val category: String,
    val note: String,
    val dateLabel: String
)

@Serializable
data class Budget(
    val stamp: SyncStamp,
    val category: String,
    val spent: Double,
    val limit: Double,
    val currency: String
)

@Serializable
data class CalendarEvent(
    val stamp: SyncStamp,
    val title: String,
    val timeRange: String,
    val location: String,
    val reminder: String
) {
    constructor(
        stamp: SyncStamp,
        title: String,
        project: String,
        timeRange: String,
        location: String,
        syncStatus: String
    ) : this(
        stamp = stamp,
        title = title,
        timeRange = timeRange,
        location = location,
        reminder = ""
    )
}

@Serializable
data class RunWorkout(
    val stamp: SyncStamp,
    val week: Int,
    val day: Int,
    val title: String,
    val intervals: List<String>,
    val completed: Boolean,
    val distanceKm: Double? = null
)

@Serializable
data class GymExercise(
    val stamp: SyncStamp,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val lastPerformance: String,
    val personalRecord: String
)

@Serializable
data class GymWorkout(
    val stamp: SyncStamp,
    val name: String,
    val exercises: List<GymExercise>,
    val restSeconds: Int,
    val active: Boolean
)

@Serializable
data class ReviewPrompt(
    val stamp: SyncStamp,
    val title: String,
    val cadence: String,
    val questions: List<String>
)

@Serializable
data class FocusSession(
    val stamp: SyncStamp,
    val label: String,
    val minutes: Int,
    val remainingMinutes: Int,
    val active: Boolean,
    val type: FocusType = FocusType.Pomodoro,
    val subject: String = "General",
    val totalSeconds: Int = minutes * 60,
    val remainingSeconds: Int = remainingMinutes * 60,
    val isPaused: Boolean = false,
    val startedAtTimestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            ((totalSeconds - remainingSeconds).toFloat() / totalSeconds).coerceIn(0f, 1f)
        } else 0f

    val formattedRemaining: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            val mStr = if (mins < 10) "0$mins" else "$mins"
            val sStr = if (secs < 10) "0$secs" else "$secs"
            return "$mStr:$sStr"
        }
}

@Serializable
enum class FocusType {
    DeepWork, Pomodoro, ShortBreak, LongBreak, Custom
}

@Serializable
data class FocusHistory(
    val stamp: SyncStamp,
    val subject: String,
    val minutesFocused: Int,
    val completed: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class WellnessLog(
    val stamp: SyncStamp,
    val waterGlasses: Int,
    val mood: String,
    val energy: Int,
    val bodyWeight: Double?,
    val steps: Int = 0,
    val sleepLastNightMinutes: Int = 0,
    val focusMinutesGoal: Int = 120,
    val currentHeartRateBpm: Double = 0.0,
    val heartRateAccuracy: String = "No Sensor"
)

@Serializable
data class TodaySummary(
    val tasksDone: Int,
    val tasksTotal: Int,
    val habitsDone: Int,
    val habitsTotal: Int,
    val fastingProgress: Float,
    val nextEvent: String,
    val nextAlarm: String,
    val monthlySpend: String,
    val plannedFitness: String,
    val steps: Int = 0,
    val waterGlasses: Int = 0,
    val focusMinutes: Int = 0,
    val totalCaloriesConsumed: Int = 0,
    val totalProteinConsumed: Double = 0.0,
    val calorieTarget: Int = 2200,
    val currentHeartRate: Double = 0.0
)

@Serializable
data class UserSettings(
    val useHaptics: Boolean = true,
    val syncUrgentOnly: Boolean = false,
    val darkMode: Boolean = true,
    val weightUnit: String = "kg",
    val distanceUnit: String = "km",
    val currencySymbol: String = "₹",
    val dailyStepGoal: Int = 10000,
    val waterGoalGlasses: Int = 8,
    val focusGoalMinutes: Int = 120,
    val pomodoroWorkMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val defaultFastPlan: String = "16:8",
    val defaultGymRestSeconds: Int = 90,
    val showFocusOnDashboard: Boolean = true,
    val enableSyncNotifications: Boolean = true,
    val enableTtsVoicePrompts: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val dailyCalorieGoal: Int = 2200,
    val dailyProteinGoalGrams: Double = 140.0,
    val dailyCarbsGoalGrams: Double = 220.0,
    val dailyFatGoalGrams: Double = 65.0,
    val focusTopics: List<String> = listOf("Deep Work", "Coding & Dev", "Study / Reading", "Writing", "Design", "Admin & Planning"),
    val themeColorPalette: String = "Mint" // Options: "Mint", "Coral", "Violet", "Gold", "Blue"
)

@Serializable
    enum class DateCategory(val displayName: String, val iconName: String) {
    Birthday("Birthday", "Cake"),
    Anniversary("Anniversary", "Favorite"),
    Event("Important Event", "Event"),
    Medical("Health / Medical", "MedicalServices"),
    Deadline("Milestone / Deadline", "Flag"),
    Custom("Custom Date", "Notifications"),
    Milestone("Milestone", "Star")
}

@Serializable
data class ImportantDate(
    val stamp: SyncStamp,
    val personName: String,
    val title: String,
    val dateIso: String, // "YYYY-MM-DD" or "MM-DD"
    val category: DateCategory = DateCategory.Birthday,
    val reminderDaysBefore: Int = 1,
    val notes: String = "",
    val isAnnual: Boolean = true
) {
    fun daysUntil(): Int {
        val parts = dateIso.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return 0
        val targetMonth = if (parts.size == 3) parts[1] else parts[0]
        val targetDay = if (parts.size == 3) parts[2] else parts[1]

        val now = kotlinx.datetime.Clock.System.now()
        val currentDayOfYear = ((now.toEpochMilliseconds() / (1000L * 60L * 60L * 24L)) % 365L).toInt()
        val targetDayOfYear = ((targetMonth - 1) * 30) + targetDay
        val diff = targetDayOfYear - currentDayOfYear
        return if (diff < 0) diff + 365 else diff
    }
}

@Serializable
data class LifeState(
    @Transient
    val store: Any? = null,
    val tasks: List<LifeTask>,
    val notes: List<LifeNote>,
    val habits: List<Habit>,
    val fast: FastSession,
    val foodLogs: List<FoodLog> = emptyList(),
    val nutritionTarget: NutritionTarget = NutritionTarget(),
    val alarms: List<LifeAlarm>,
    val expenses: List<Expense>,
    val budgets: List<Budget>,
    val events: List<CalendarEvent>,
    val runs: List<RunWorkout>,
    val gymWorkout: GymWorkout,
    val reviews: List<ReviewPrompt>,
    val focus: FocusSession,
    val focusHistory: List<FocusHistory> = emptyList(),
    val routines: List<RoutineTask> = emptyList(),
    val importantDates: List<ImportantDate> = emptyList(),
    val timetableBlocks: List<TimetableBlock> = emptyList(),
    val wellness: WellnessLog,
    val settings: UserSettings = UserSettings()
) {
    val activeTasks: List<LifeTask> get() = tasks.filter { it.stamp.deletedAt == null }
    val activeHabits: List<Habit> get() = habits.filter { it.stamp.deletedAt == null }
    val activeRoutines: List<RoutineTask> get() = routines.filter { it.stamp.deletedAt == null }.sortedBy { it.orderIndex }
    val activeAlarms: List<LifeAlarm> get() = alarms.filter { it.stamp.deletedAt == null }
    val activeExpenses: List<Expense> get() = expenses.filter { it.stamp.deletedAt == null }
    val activeEvents: List<CalendarEvent> get() = events.filter { it.stamp.deletedAt == null }
    val activeNotes: List<LifeNote> get() = notes.filter { it.stamp.deletedAt == null }
    val activeFoodLogs: List<FoodLog> get() = foodLogs.filter { it.stamp.deletedAt == null }
    val activeImportantDates: List<ImportantDate> get() = importantDates.filter { it.stamp.deletedAt == null }.sortedBy { it.daysUntil() }
    val activeTimetableBlocks: List<TimetableBlock> get() = timetableBlocks.filter { it.stamp.deletedAt == null }.sortedBy { it.startTime }

    val todayCalories: Int get() = activeFoodLogs.sumOf { it.calories }
    val todayProtein: Double get() = activeFoodLogs.sumOf { it.proteinGrams }
    val todayCarbs: Double get() = activeFoodLogs.sumOf { it.carbsGrams }
    val todayFat: Double get() = activeFoodLogs.sumOf { it.fatGrams }

    val today: TodaySummary
        get() = TodaySummary(
            tasksDone = activeTasks.count { it.complete },
            tasksTotal = activeTasks.size,
            habitsDone = activeHabits.count { it.doneToday },
            habitsTotal = activeHabits.size,
            fastingProgress = fast.progress,
            nextEvent = activeEvents.firstOrNull()?.let { "${it.timeRange} ${it.title}" } ?: "No events",
            nextAlarm = activeAlarms.firstOrNull { it.enabled }?.let { "${it.time} ${it.label}" } ?: "No alarms",
            monthlySpend = budgets.firstOrNull()?.let { "${it.currency}${activeExpenses.sumOf { expense -> expense.amount }.toInt()}" } ?: "No spend",
            plannedFitness = runs.firstOrNull { !it.completed }?.title ?: gymWorkout.name,
            steps = wellness.steps,
            waterGlasses = wellness.waterGlasses,
            focusMinutes = focusHistory.sumOf { it.minutesFocused },
            totalCaloriesConsumed = todayCalories,
            totalProteinConsumed = todayProtein,
            calorieTarget = settings.dailyCalorieGoal,
            currentHeartRate = wellness.currentHeartRateBpm
        )
}

@Serializable
data class SyncPacket(
    val deviceId: String,
    val generatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val state: LifeState
)

object LifeSeed {
    fun state(): LifeState {
        return LifeState(
            store = null,
            tasks = emptyList(),
            notes = emptyList(),
            habits = emptyList(),
            fast = FastSession(
                stamp = SyncStamp("fast-init"),
                plan = "16:8",
                startedAtLabel = "",
                endsAtLabel = "",
                progress = 0f,
                active = false
            ),
            foodLogs = emptyList(),
            nutritionTarget = NutritionTarget(
                targetCalories = 2000,
                targetProteinGrams = 120.0,
                targetCarbsGrams = 200.0,
                targetFatGrams = 60.0,
                targetWaterMl = 2500
            ),
            alarms = emptyList(),
            expenses = emptyList(),
            budgets = emptyList(),
            events = emptyList(),
            runs = emptyList(),
            gymWorkout = GymWorkout(
                stamp = SyncStamp("gym-init"),
                name = "",
                exercises = emptyList(),
                restSeconds = 60,
                active = false
            ),
            reviews = emptyList(),
            focus = FocusSession(
                stamp = SyncStamp("focus-init"),
                label = "",
                minutes = 25,
                remainingMinutes = 25,
                totalSeconds = 25 * 60,
                remainingSeconds = 25 * 60,
                active = false,
                type = FocusType.Pomodoro,
                subject = ""
            ),
            focusHistory = emptyList(),
            routines = emptyList(),
            importantDates = emptyList(),
            wellness = WellnessLog(
                stamp = SyncStamp("wellness-today"),
                waterGlasses = 0,
                mood = "",
                energy = 5,
                bodyWeight = 0.0,
                steps = 0,
                sleepLastNightMinutes = 0,
                focusMinutesGoal = 120,
                currentHeartRateBpm = 0.0,
                heartRateAccuracy = ""
            ),
            settings = UserSettings(
                useHaptics = true,
                syncUrgentOnly = false,
                darkMode = true,
                weightUnit = "kg",
                distanceUnit = "km",
                currencySymbol = "₹",
                dailyStepGoal = 10000,
                waterGoalGlasses = 8,
                focusGoalMinutes = 120,
                pomodoroWorkMinutes = 25,
                pomodoroBreakMinutes = 5,
                pomodoroLongBreakMinutes = 15,
                defaultFastPlan = "16:8",
                defaultGymRestSeconds = 60,
                showFocusOnDashboard = true,
                enableSyncNotifications = true,
                enableTtsVoicePrompts = true,
                ttsSpeechRate = 1.0f,
                dailyCalorieGoal = 2000,
                dailyProteinGoalGrams = 120.0,
                dailyCarbsGoalGrams = 200.0,
                dailyFatGoalGrams = 60.0,
                focusTopics = emptyList()
            )
        )
    }
}