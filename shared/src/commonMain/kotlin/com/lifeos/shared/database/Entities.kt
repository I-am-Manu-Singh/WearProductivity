package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.creativeblisslabs.wearproductivity.shared.*

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val title: String,
    val project: String,
    val dueLabel: String,
    val priority: Int,
    val complete: Boolean,
    val tagsJson: String,
    val scheduledTime: String?,
    val hapticEnabled: Boolean,
    val syncStatus: String
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val title: String,
    val body: String,
    val pinned: Boolean
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val name: String,
    val cadence: String,
    val streak: Int,
    val doneToday: Boolean,
    val colorName: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val isTimerActive: Boolean,
    val remainingSeconds: Int
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val amount: Double,
    val currency: String,
    val category: String,
    val note: String,
    val dateLabel: String
)

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val name: String,
    val mealType: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val loggedAtLabel: String,
    val notes: String
)

fun FoodLog.toEntity() = FoodLogEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    name = name,
    mealType = mealType.name,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    loggedAtLabel = loggedAtLabel,
    notes = notes
)

fun FoodLogEntity.toModel() = FoodLog(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    name = name,
    mealType = try { MealType.valueOf(mealType) } catch (e: Exception) { MealType.Snack },
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    loggedAtLabel = loggedAtLabel,
    notes = notes
)

fun LifeTask.toEntity() = TaskEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    title = title,
    project = project,
    dueLabel = dueLabel,
    priority = priority,
    complete = complete,
    tagsJson = tags.joinToString("|"),
    scheduledTime = scheduledTime,
    hapticEnabled = hapticEnabled,
    syncStatus = syncStatus
)

fun TaskEntity.toModel() = LifeTask(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    title = title,
    project = project,
    dueLabel = dueLabel,
    priority = priority,
    complete = complete,
    tags = if (tagsJson.isEmpty()) emptyList() else tagsJson.split("|"),
    scheduledTime = scheduledTime,
    hapticEnabled = hapticEnabled,
    syncStatus = syncStatus
)

fun LifeNote.toEntity() = NoteEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    title = title,
    body = body,
    pinned = pinned
)

fun NoteEntity.toModel() = LifeNote(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    title = title,
    body = body,
    pinned = pinned
)

fun Habit.toEntity() = HabitEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    name = name,
    cadence = cadence,
    streak = streak,
    doneToday = doneToday,
    colorName = colorName,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    isTimerActive = isTimerActive,
    remainingSeconds = remainingSeconds
)

fun HabitEntity.toModel() = Habit(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    name = name,
    cadence = cadence,
    streak = streak,
    doneToday = doneToday,
    colorName = colorName,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    isTimerActive = isTimerActive,
    remainingSeconds = remainingSeconds
)

fun Expense.toEntity() = ExpenseEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    amount = amount,
    currency = currency,
    category = category,
    note = note,
    dateLabel = dateLabel
)

fun ExpenseEntity.toModel() = Expense(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    amount = amount,
    currency = currency,
    category = category,
    note = note,
    dateLabel = dateLabel
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val time: String,
    val label: String,
    val repeat: String,
    val enabled: Boolean,
    val haptic: Boolean
)

@Entity(tableName = "fast_sessions")
data class FastSessionEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val plan: String,
    val startedAtLabel: String,
    val endsAtLabel: String,
    val progress: Float,
    val active: Boolean
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val category: String,
    val spent: Double,
    val limit: Double,
    val currency: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val title: String,
    val timeRange: String,
    val location: String,
    val reminder: String
)

fun LifeAlarm.toEntity() = AlarmEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    time = time,
    label = label,
    repeat = repeat,
    enabled = enabled,
    haptic = haptic
)

fun AlarmEntity.toModel() = LifeAlarm(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    time = time,
    label = label,
    repeat = repeat,
    enabled = enabled,
    haptic = haptic
)

fun FastSession.toEntity() = FastSessionEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    plan = plan,
    startedAtLabel = startedAtLabel,
    endsAtLabel = endsAtLabel,
    progress = progress,
    active = active
)

fun FastSessionEntity.toModel() = FastSession(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    plan = plan,
    startedAtLabel = startedAtLabel,
    endsAtLabel = endsAtLabel,
    progress = progress,
    active = active
)

fun Budget.toEntity() = BudgetEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    category = category,
    spent = spent,
    limit = limit,
    currency = currency
)

fun BudgetEntity.toModel() = Budget(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    category = category,
    spent = spent,
    limit = limit,
    currency = currency
)

fun CalendarEvent.toEntity() = EventEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    title = title,
    timeRange = timeRange,
    location = location,
    reminder = reminder
)

fun EventEntity.toModel() = CalendarEvent(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    title = title,
    timeRange = timeRange,
    location = location,
    reminder = reminder
)

@Entity(tableName = "runs")
data class RunWorkoutEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val week: Int,
    val day: Int,
    val title: String,
    val intervalsJson: String,
    val completed: Boolean,
    val distanceKm: Double?
)

@Entity(tableName = "gym_workouts")
data class GymWorkoutEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val name: String,
    val restSeconds: Int,
    val active: Boolean
)

@Entity(tableName = "gym_exercises")
data class GymExerciseEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val workoutId: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val lastPerformance: String,
    val personalRecord: String
)

@Entity(tableName = "wellness")
data class WellnessEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val waterGlasses: Int,
    val mood: String,
    val energy: Int,
    val bodyWeight: Double?,
    val steps: Int = 0,
    val sleepMinutes: Int = 0,
    val focusMinutesGoal: Int = 120,
    val currentHeartRateBpm: Double = 0.0,
    val heartRateAccuracy: String = "No Sensor"
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val label: String,
    val minutes: Int,
    val remainingMinutes: Int,
    val active: Boolean,
    val type: String,
    val subject: String,
    val totalSeconds: Int = minutes * 60,
    val remainingSeconds: Int = remainingMinutes * 60,
    val isPaused: Boolean = false,
    val startedAtTimestamp: Long = 0L
)

@Entity(tableName = "focus_history")
data class FocusHistoryEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val subject: String,
    val minutesFocused: Int,
    val completed: Boolean,
    val timestamp: Long
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val title: String,
    val cadence: String,
    val questionsJson: String
)

@Entity(tableName = "important_dates")
data class ImportantDateEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val deletedAt: Long?,
    val version: Long,
    val personName: String,
    val title: String,
    val dateIso: String,
    val category: String,
    val reminderDaysBefore: Int,
    val notes: String,
    val isAnnual: Boolean
)

fun RunWorkout.toEntity() = RunWorkoutEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    week = week,
    day = day,
    title = title,
    intervalsJson = intervals.joinToString("|"),
    completed = completed,
    distanceKm = distanceKm
)

fun RunWorkoutEntity.toModel() = RunWorkout(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    week = week,
    day = day,
    title = title,
    intervals = if (intervalsJson.isEmpty()) emptyList() else intervalsJson.split("|"),
    completed = completed,
    distanceKm = distanceKm
)

fun GymWorkout.toEntity() = GymWorkoutEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    name = name,
    restSeconds = restSeconds,
    active = active
)

fun GymWorkoutEntity.toModel(exercises: List<GymExercise>) = GymWorkout(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    name = name,
    exercises = exercises,
    restSeconds = restSeconds,
    active = active
)

fun GymExercise.toEntity(workoutId: String) = GymExerciseEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    workoutId = workoutId,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    lastPerformance = lastPerformance,
    personalRecord = personalRecord
)

fun GymExerciseEntity.toModel() = GymExercise(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    lastPerformance = lastPerformance,
    personalRecord = personalRecord
)

fun WellnessLog.toEntity() = WellnessEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    waterGlasses = waterGlasses,
    mood = mood,
    energy = energy,
    bodyWeight = bodyWeight,
    steps = steps,
    sleepMinutes = sleepLastNightMinutes,
    focusMinutesGoal = focusMinutesGoal,
    currentHeartRateBpm = currentHeartRateBpm,
    heartRateAccuracy = heartRateAccuracy
)

fun WellnessEntity.toModel() = WellnessLog(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    waterGlasses = waterGlasses,
    mood = mood,
    energy = energy,
    bodyWeight = bodyWeight,
    steps = steps,
    sleepLastNightMinutes = sleepMinutes,
    focusMinutesGoal = focusMinutesGoal,
    currentHeartRateBpm = currentHeartRateBpm,
    heartRateAccuracy = heartRateAccuracy
)

fun FocusSession.toEntity() = FocusSessionEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    label = label,
    minutes = minutes,
    remainingMinutes = remainingMinutes,
    active = active,
    type = type.name,
    subject = subject,
    totalSeconds = totalSeconds,
    remainingSeconds = remainingSeconds,
    isPaused = isPaused,
    startedAtTimestamp = startedAtTimestamp
)

fun FocusSessionEntity.toModel() = FocusSession(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    label = label,
    minutes = minutes,
    remainingMinutes = remainingMinutes,
    active = active,
    type = try { FocusType.valueOf(type) } catch (e: Exception) { FocusType.Pomodoro },
    subject = subject,
    totalSeconds = if (totalSeconds > 0) totalSeconds else minutes * 60,
    remainingSeconds = if (remainingSeconds >= 0) remainingSeconds else remainingMinutes * 60,
    isPaused = isPaused,
    startedAtTimestamp = startedAtTimestamp
)

fun FocusHistory.toEntity() = FocusHistoryEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    subject = subject,
    minutesFocused = minutesFocused,
    completed = completed,
    timestamp = timestamp
)

fun FocusHistoryEntity.toModel() = FocusHistory(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    subject = subject,
    minutesFocused = minutesFocused,
    completed = completed,
    timestamp = timestamp
)

fun ReviewPrompt.toEntity() = ReviewEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    title = title,
    cadence = cadence,
    questionsJson = questions.joinToString("|")
)

fun ReviewEntity.toModel() = ReviewPrompt(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    title = title,
    cadence = cadence,
    questions = if (questionsJson.isEmpty()) emptyList() else questionsJson.split("|")
)

fun ImportantDate.toEntity() = ImportantDateEntity(
    id = stamp.id,
    updatedAt = stamp.updatedAt,
    sourceDeviceId = stamp.sourceDeviceId,
    deletedAt = stamp.deletedAt,
    version = stamp.version,
    personName = personName,
    title = title,
    dateIso = dateIso,
    category = category.name,
    reminderDaysBefore = reminderDaysBefore,
    notes = notes,
    isAnnual = isAnnual
)

fun ImportantDateEntity.toModel() = ImportantDate(
    stamp = SyncStamp(id, updatedAt, sourceDeviceId, deletedAt, version),
    personName = personName,
    title = title,
    dateIso = dateIso,
    category = try { DateCategory.valueOf(category) } catch (e: Exception) { DateCategory.Birthday },
    reminderDaysBefore = reminderDaysBefore,
    notes = notes,
    isAnnual = isAnnual
)
