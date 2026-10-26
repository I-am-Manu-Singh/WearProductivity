package com.creativeblisslabs.wearproductivity.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.creativeblisslabs.wearproductivity.shared.FocusType
import com.creativeblisslabs.wearproductivity.shared.FocusSession
import com.creativeblisslabs.wearproductivity.shared.FocusHistory
import com.creativeblisslabs.wearproductivity.shared.SyncStamp
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.UserSettings
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import com.creativeblisslabs.wearproductivity.shared.SyncPaths

class LifeRepository(
    private val store: LifeStore,
    val transport: CompanionSyncTransport = LoopbackCompanionSyncTransport(),
    private val deviceId: String = "phone"
) {
    private val _state = MutableStateFlow(LifeSeed.state())
    val state: StateFlow<LifeState> = _state.asStateFlow()

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun load() {
        _state.value = store.load()
    }

    fun current(): LifeState = _state.value

    suspend fun syncWithRemote(remote: LifeState) {
        val result = LifeSyncEngine.merge(_state.value, remote)
        if (result.acceptedRemoteChanges > 0) {
            _state.value = result.mergedState
            persist()
        }
    }

    suspend fun handleRemoteSync(packet: SyncPacket) {
        syncWithRemote(packet.state)
    }

    private suspend fun updateState(block: (LifeState) -> LifeState) {
        _state.value = block(_state.value)
        persist()
    }

    suspend fun quickTask(
        title: String,
        project: String = "Inbox",
        dueLabel: String = "Today",
        scheduledTime: String? = null,
        hapticEnabled: Boolean = false,
        priority: Int = 2
    ) {
        updateState { s ->
            val task = LifeTask(
                stamp = SyncStamp("task-${ClockId.next()}", sourceDeviceId = deviceId),
                title = title,
                project = project,
                dueLabel = dueLabel,
                priority = priority.coerceIn(1, 3),
                tags = listOf("quick"),
                scheduledTime = scheduledTime,
                hapticEnabled = hapticEnabled,
                syncStatus = "Local"
            )
            s.copy(tasks = (listOf(task) + s.tasks).distinctBy { it.stamp.id })
        }
    }

    suspend fun quickNote(title: String, body: String) {
        updateState { s ->
            val note = LifeNote(
                stamp = SyncStamp("note-${ClockId.next()}", sourceDeviceId = deviceId),
                title = title,
                body = body,
                pinned = true
            )
            s.copy(notes = (listOf(note) + s.notes).distinctBy { it.stamp.id })
        }
    }

    suspend fun deleteTask(id: String) {
        updateState { s ->
            s.copy(tasks = s.tasks.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun completeTask(id: String) {
        updateState { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.stamp.id == id) task.copy(complete = !task.complete, stamp = task.stamp.bump(deviceId)) else task
            })
        }
    }

    suspend fun completeAllTasks() {
        updateState { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.stamp.deletedAt == null && !task.complete) {
                    task.copy(complete = true, stamp = task.stamp.bump(deviceId))
                } else task
            })
        }
    }

    suspend fun quickHabit(
        name: String,
        cadence: String = "Daily",
        colorName: String = "mint",
        targetValue: Double = 1.0,
        unit: String = ""
    ) {
        updateState { s ->
            val habit = Habit(
                stamp = SyncStamp("habit-${ClockId.next()}", sourceDeviceId = deviceId),
                name = name,
                cadence = cadence,
                streak = 0,
                doneToday = false,
                colorName = colorName,
                targetValue = targetValue,
                unit = unit
            )
            s.copy(habits = (listOf(habit) + s.habits).distinctBy { it.stamp.id })
        }
    }

    suspend fun checkHabit(id: String) {
        updateState { s ->
            s.copy(habits = s.habits.map { habit ->
                if (habit.stamp.id == id) {
                    val done = !habit.doneToday
                    habit.copy(doneToday = done, streak = (habit.streak + if (done) 1 else -1).coerceAtLeast(0), stamp = habit.stamp.bump(deviceId))
                } else {
                    habit
                }
            })
        }
    }

    suspend fun toggleFast() {
        updateState { s ->
            s.copy(
                fast = s.fast.copy(
                    active = !s.fast.active,
                    progress = if (s.fast.active) 1f else 0f,
                    stamp = s.fast.stamp.bump(deviceId)
                )
            )
        }
    }

    suspend fun extendFast(minutes: Int) {
        updateState { s ->
            if (!s.fast.active) return@updateState s
            // Logic to extend fasting time (e.g. by adjusting endsAtLabel or internal state)
            // For now, we'll just bump the stamp to trigger UI updates if needed
            s.copy(
                fast = s.fast.copy(
                    stamp = s.fast.stamp.bump(deviceId)
                )
            )
        }
    }

    suspend fun toggleAlarm(id: String) {
        updateState { s ->
            s.copy(alarms = s.alarms.map { alarm ->
                if (alarm.stamp.id == id) alarm.copy(enabled = !alarm.enabled, stamp = alarm.stamp.bump(deviceId)) else alarm
            })
        }
    }

    /** Adds an alarm. The default keeps existing quickAlarm(time, label) callers compatible. */
    suspend fun quickAlarm(time: String, label: String, repeat: String = "Once") {
        updateState { s ->
            val alarm = LifeAlarm(
                stamp = SyncStamp("alarm-${ClockId.next()}", sourceDeviceId = deviceId),
                time = time,
                label = label,
                repeat = repeat,
                enabled = true
            )
            s.copy(alarms = (listOf(alarm) + s.alarms).distinctBy { it.stamp.id })
        }
    }

    suspend fun quickExpense(amount: Double, category: String, note: String) {
        updateState { s ->
            val expense = Expense(
                stamp = SyncStamp("expense-${ClockId.next()}", sourceDeviceId = deviceId),
                amount = amount,
                currency = "₹",
                category = category,
                note = note,
                dateLabel = "Today"
            )
            s.copy(expenses = (listOf(expense) + s.expenses).distinctBy { it.stamp.id })
        }
    }

    suspend fun quickEvent(
        title: String,
        timeRange: String,
        location: String = "LifeOS",
        reminder: String = "10 min"
    ) {
        updateState { s ->
            val event = CalendarEvent(
                stamp = SyncStamp("event-${ClockId.next()}", sourceDeviceId = deviceId),
                title = title,
                timeRange = timeRange,
                location = location,
                reminder = reminder
            )
            s.copy(events = (listOf(event) + s.events).distinctBy { it.stamp.id })
        }
    }

    suspend fun completeNextRun() {
        updateState { s ->
            val next = s.runs.firstOrNull { !it.completed }
            if (next != null) {
                s.copy(runs = s.runs.map { run ->
                    if (run.stamp.id == next.stamp.id) run.copy(completed = true, distanceKm = run.distanceKm ?: 2.4, stamp = run.stamp.bump(deviceId)) else run
                })
            } else s
        }
    }

    /** Completes a specific planned run without inventing distance or sensor measurements. */
    suspend fun completeRunWorkout(id: String) {
        updateState { s ->
            s.copy(runs = s.runs.map { run ->
                if (run.stamp.id == id && run.stamp.deletedAt == null) {
                    run.copy(completed = true, stamp = run.stamp.bump(deviceId))
                } else run
            })
        }
    }

    suspend fun toggleGymWorkout() {
        updateState { s ->
            s.copy(gymWorkout = s.gymWorkout.copy(active = !s.gymWorkout.active, stamp = s.gymWorkout.stamp.bump(deviceId)))
        }
    }

    suspend fun logGymSet(exerciseId: String, performance: String) {
        updateState { s ->
            s.copy(gymWorkout = s.gymWorkout.copy(
                exercises = s.gymWorkout.exercises.map { exercise ->
                    if (exercise.stamp.id == exerciseId) exercise.copy(lastPerformance = performance, stamp = exercise.stamp.bump(deviceId)) else exercise
                },
                stamp = s.gymWorkout.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun tickFocusSecond(seconds: Int = 1) {
        updateState { s ->
            if (!s.focus.active || s.focus.isPaused) return@updateState s
            val remainingSecs = (s.focus.remainingSeconds - seconds).coerceAtLeast(0)
            val remainingMins = (remainingSecs + 59) / 60
            val sessionFinished = remainingSecs == 0 && s.focus.remainingSeconds > 0

            var history = s.focusHistory
            if (sessionFinished) {
                val record = FocusHistory(
                    stamp = SyncStamp("focus-h-${ClockId.next()}", sourceDeviceId = deviceId),
                    subject = s.focus.subject,
                    minutesFocused = s.focus.minutes,
                    completed = true
                )
                history = (listOf(record) + history).take(100)
            }

            s.copy(
                focus = s.focus.copy(
                    active = remainingSecs > 0,
                    remainingSeconds = remainingSecs,
                    remainingMinutes = remainingMins,
                    stamp = s.focus.stamp.bump(deviceId)
                ),
                focusHistory = history
            )
        }
    }

    suspend fun tickFocus(minutes: Int = 1) {
        tickFocusSecond(minutes * 60)
    }

    suspend fun startFocusSession(minutes: Int, type: FocusType, subject: String) {
        updateState { s ->
            val totalSecs = minutes * 60
            val label = when (type) {
                FocusType.Pomodoro -> "Pomodoro"
                FocusType.ShortBreak -> "Short Break"
                FocusType.LongBreak -> "Long Break"
                FocusType.DeepWork -> "Deep Work"
                FocusType.Custom -> "Custom Timer"
            }
            s.copy(
                focus = FocusSession(
                    stamp = SyncStamp("focus-${ClockId.next()}", sourceDeviceId = deviceId),
                    label = label,
                    minutes = minutes,
                    remainingMinutes = minutes,
                    totalSeconds = totalSecs,
                    remainingSeconds = totalSecs,
                    active = true,
                    isPaused = false,
                    type = type,
                    subject = subject,
                    startedAtTimestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    suspend fun togglePauseFocusSession() {
        updateState { s ->
            if (!s.focus.active) return@updateState s
            s.copy(
                focus = s.focus.copy(
                    isPaused = !s.focus.isPaused,
                    stamp = s.focus.stamp.bump(deviceId)
                )
            )
        }
    }

    /** Adds time to the currently running focus session without touching fasting state. */
    suspend fun extendFocusSession(minutes: Int) {
        if (minutes <= 0) return
        val requestedSeconds = minutes.coerceIn(1, 180) * 60
        updateState { s ->
            val current = s.focus
            if (!current.active) return@updateState s

            val baseTotalSeconds = maxOf(current.totalSeconds, current.remainingSeconds, 1)
            val nextTotalSeconds = (baseTotalSeconds + requestedSeconds).coerceAtMost(24 * 60 * 60)
            val addedSeconds = nextTotalSeconds - baseTotalSeconds
            if (addedSeconds <= 0) return@updateState s

            val nextRemainingSeconds = (current.remainingSeconds + addedSeconds).coerceAtMost(nextTotalSeconds)
            s.copy(
                focus = current.copy(
                    minutes = (nextTotalSeconds + 59) / 60,
                    totalSeconds = nextTotalSeconds,
                    remainingSeconds = nextRemainingSeconds,
                    remainingMinutes = (nextRemainingSeconds + 59) / 60,
                    stamp = current.stamp.bump(deviceId)
                )
            )
        }
    }

    suspend fun stopFocusSession() {
        updateState { s ->
            s.copy(
                focus = s.focus.copy(
                    active = false,
                    isPaused = false,
                    remainingSeconds = 0,
                    remainingMinutes = 0,
                    stamp = s.focus.stamp.bump(deviceId)
                )
            )
        }
    }

    suspend fun addFocusTopic(topic: String) {
        if (topic.isBlank()) return
        updateSettings { s ->
            if (s.focusTopics.contains(topic)) s
            else s.copy(focusTopics = s.focusTopics + topic)
        }
    }

    suspend fun addWater(amountMl: Int = 250) {
        updateState { s ->
            s.copy(
                wellness = s.wellness.copy(
                    waterGlasses = (s.wellness.waterGlasses + (amountMl / 250)).coerceAtMost(12),
                    stamp = s.wellness.stamp.bump(deviceId)
                )
            )
        }
    }
    suspend fun syncSystemCalendarEvents(systemEvents: List<CalendarEvent>) {
        updateState { s ->
            val existingNoSys = s.events.filter { !it.stamp.id.startsWith("sys-cal-") }
            s.copy(events = existingNoSys + systemEvents)
        }
    }
    suspend fun quickFood(
        name: String,
        mealType: MealType,
        calories: Int,
        proteinGrams: Double,
        carbsGrams: Double = 0.0,
        fatGrams: Double = 0.0,
        notes: String = ""
    ) {
        updateState { s ->
            val entry = FoodLog(
                stamp = SyncStamp("food-${ClockId.next()}", sourceDeviceId = deviceId),
                name = name,
                mealType = mealType,
                calories = calories,
                proteinGrams = proteinGrams,
                carbsGrams = carbsGrams,
                fatGrams = fatGrams,
                loggedAtLabel = "Today",
                notes = notes
            )
            s.copy(foodLogs = (listOf(entry) + s.foodLogs).distinctBy { it.stamp.id })
        }
    }

    suspend fun deleteFood(id: String) {
        updateState { s ->
            s.copy(foodLogs = s.foodLogs.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun updateWellnessVitals(steps: Int? = null, sleepMinutes: Int? = null) {
        updateState { s ->
            s.copy(
                wellness = s.wellness.copy(
                    steps = steps ?: s.wellness.steps,
                    sleepLastNightMinutes = sleepMinutes ?: s.wellness.sleepLastNightMinutes,
                    stamp = s.wellness.stamp.bump(deviceId)
                )
            )
        }
    }

    suspend fun updateHeartRate(bpm: Double, accuracy: String = "Normal") {
        updateState { s ->
            s.copy(
                wellness = s.wellness.copy(
                    currentHeartRateBpm = bpm,
                    heartRateAccuracy = accuracy,
                    stamp = s.wellness.stamp.bump(deviceId)
                )
            )
        }
    }

    suspend fun updateWellness(mood: String? = null, energy: Int? = null, weight: Double? = null) {
        updateState { s ->
            s.copy(wellness = s.wellness.copy(
                mood = mood ?: s.wellness.mood,
                energy = energy ?: s.wellness.energy,
                bodyWeight = weight ?: s.wellness.bodyWeight,
                stamp = s.wellness.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun updateSteps(steps: Long) {
        updateState { it.copy(wellness = it.wellness.copy(steps = steps.toInt(), stamp = it.wellness.stamp.bump(deviceId))) }
    }

    suspend fun logSleep(start: Long, end: Long) {
        val durationMinutes = ((end - start) / (1000 * 60)).toInt()
        updateState { it.copy(wellness = it.wellness.copy(sleepLastNightMinutes = durationMinutes, stamp = it.wellness.stamp.bump(deviceId))) }
    }

    suspend fun addRoutineTask(
        title: String,
        startTime: String,
        endTime: String,
        phase: RoutinePhase = RoutinePhase.GENERAL,
        recurrence: RoutineRecurrence = RoutineRecurrence.SAME_EVERY_DAY,
        notes: String = ""
    ) {
        updateState { s ->
            val newTask = RoutineTask(
                stamp = SyncStamp("routine-${ClockId.next()}", sourceDeviceId = deviceId),
                title = title,
                startTime = startTime,
                endTime = endTime,
                phase = phase,
                doneToday = false,
                orderIndex = s.routines.size,
                notes = notes,
                recurrence = recurrence
            )
            s.copy(routines = s.routines + newTask)
        }
    }

    suspend fun toggleRoutine(id: String) {
        updateState { s ->
            s.copy(routines = s.routines.map {
                if (it.stamp.id == id) it.copy(doneToday = !it.doneToday, stamp = it.stamp.bump(deviceId)) else it
            })
        }
    }

    suspend fun rescheduleRoutine(id: String, newStartTime: String, newEndTime: String) {
        updateState { s ->
            s.copy(routines = s.routines.map {
                if (it.stamp.id == id) it.copy(startTime = newStartTime, endTime = newEndTime, stamp = it.stamp.bump(deviceId)) else it
            })
        }
    }

    /** Updates a routine's editable fields while preserving recurrence and completion state. */
    suspend fun editRoutine(id: String, newTitle: String, newStartTime: String, newEndTime: String) {
        updateState { s ->
            s.copy(routines = s.routines.map { routine ->
                if (routine.stamp.id == id) routine.copy(
                    title = newTitle,
                    startTime = newStartTime,
                    endTime = newEndTime,
                    stamp = routine.stamp.bump(deviceId)
                ) else routine
            })
        }
    }

    suspend fun moveRoutine(id: String, moveUp: Boolean) {
        updateState { s ->
            val list = s.activeRoutines.toMutableList()
            val index = list.indexOfFirst { it.stamp.id == id }
            if (index != -1) {
                val targetIndex = if (moveUp) index - 1 else index + 1
                if (targetIndex in 0 until list.size) {
                    val item = list.removeAt(index)
                    list.add(targetIndex, item)
                    val reindexed = list.mapIndexed { idx, task -> task.copy(orderIndex = idx, stamp = task.stamp.bump(deviceId)) }
                    s.copy(routines = reindexed)
                } else s
            } else s
        }
    }

    suspend fun deleteRoutine(id: String) {
        updateState { s ->
            s.copy(routines = s.routines.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun editTask(
        id: String,
        newTitle: String,
        newProject: String,
        newDue: String,
        newPriority: Int,
        newScheduledTime: String? = null,
        newHaptic: Boolean = false
    ) {
        updateState { s ->
            s.copy(tasks = s.tasks.map {
                if (it.stamp.id == id) it.copy(
                    title = newTitle,
                    project = newProject,
                    dueLabel = newDue,
                    priority = newPriority,
                    scheduledTime = newScheduledTime,
                    hapticEnabled = newHaptic,
                    syncStatus = "Local",
                    stamp = it.stamp.bump(deviceId)
                ) else it
            })
        }
    }

    suspend fun editHabit(
        id: String,
        newName: String,
        newCadence: String,
        newColor: String = "mint",
        newTarget: Double = 1.0,
        newUnit: String = ""
    ) {
        updateState { s ->
            s.copy(habits = s.habits.map {
                if (it.stamp.id == id) it.copy(
                    name = newName,
                    cadence = newCadence,
                    colorName = newColor,
                    targetValue = newTarget,
                    unit = newUnit,
                    stamp = it.stamp.bump(deviceId)
                ) else it
            })
        }
    }

    suspend fun updateHabitProgress(id: String, newValue: Double) {
        updateState { s ->
            s.copy(habits = s.habits.map { habit ->
                if (habit.stamp.id == id) {
                    val isDone = newValue >= habit.targetValue
                    val streakDelta = if (isDone && !habit.doneToday) 1 else if (!isDone && habit.doneToday) -1 else 0
                    habit.copy(
                        currentValue = newValue,
                        doneToday = isDone,
                        streak = (habit.streak + streakDelta).coerceAtLeast(0),
                        stamp = habit.stamp.bump(deviceId)
                    )
                } else habit
            })
        }
    }

    suspend fun toggleHabitTimer(id: String) {
        updateState { s ->
            s.copy(habits = s.habits.map { habit ->
                if (habit.stamp.id == id) {
                    habit.copy(isTimerActive = !habit.isTimerActive, stamp = habit.stamp.bump(deviceId))
                } else habit
            })
        }
    }

    suspend fun tickHabitTimer(seconds: Int = 1) {
        updateState { s ->
            s.copy(habits = s.habits.map { habit ->
                if (habit.isTimerActive && habit.remainingSeconds > 0) {
                    val newRemaining = (habit.remainingSeconds - seconds).coerceAtLeast(0)
                    if (newRemaining == 0) {
                        habit.copy(
                            remainingSeconds = 0,
                            isTimerActive = false,
                            currentValue = habit.currentValue + 1,
                            doneToday = (habit.currentValue + 1) >= habit.targetValue,
                            stamp = habit.stamp.bump(deviceId)
                        )
                    } else {
                        habit.copy(remainingSeconds = newRemaining, stamp = habit.stamp.bump(deviceId))
                    }
                } else habit
            })
        }
    }

    suspend fun deleteHabit(id: String) {
        updateState { s ->
            s.copy(habits = s.habits.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun editExpense(id: String, newAmount: Double, newCategory: String, newNote: String) {
        updateState { s ->
            s.copy(expenses = s.expenses.map {
                if (it.stamp.id == id) it.copy(amount = newAmount, category = newCategory, note = newNote, stamp = it.stamp.bump(deviceId)) else it
            })
        }
    }

    suspend fun addImportantDate(
        personName: String,
        title: String,
        dateIso: String,
        category: DateCategory = DateCategory.Birthday,
        reminderDaysBefore: Int = 1,
        notes: String = ""
    ) {
        updateState { s ->
            val newDate = ImportantDate(
                stamp = SyncStamp("date-${ClockId.next()}", sourceDeviceId = deviceId),
                personName = personName,
                title = title,
                dateIso = dateIso,
                category = category,
                reminderDaysBefore = reminderDaysBefore,
                notes = notes
            )
            s.copy(importantDates = s.importantDates + newDate)
        }
    }

    suspend fun deleteImportantDate(id: String) {
        updateState { s ->
            s.copy(importantDates = s.importantDates.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun editImportantDate(
        id: String,
        personName: String,
        title: String,
        dateIso: String,
        category: DateCategory,
        reminderDaysBefore: Int,
        notes: String
    ) {
        updateState { s ->
            s.copy(importantDates = s.importantDates.map {
                if (it.stamp.id == id) it.copy(
                    personName = personName,
                    title = title,
                    dateIso = dateIso,
                    category = category,
                    reminderDaysBefore = reminderDaysBefore,
                    notes = notes,
                    stamp = it.stamp.bump(deviceId)
                ) else it
            })
        }
    }
    suspend fun dismissActiveAlarm() {
        updateState { s ->
            // Mark ringing/active alarms as handled or toggle them if needed
            s.copy(
                alarms = s.alarms.map { alarm ->
                    if (alarm.enabled && alarm.stamp.deletedAt == null) {
                        // If it's a one-time alarm (Once), disable it after ringing
                        val shouldDisable = alarm.repeat.equals("Once", ignoreCase = true)
                        alarm.copy(
                            enabled = if (shouldDisable) false else alarm.enabled,
                            stamp = alarm.stamp.bump(deviceId)
                        )
                    } else alarm
                }
            )
        }
    }

    suspend fun snoozeActiveAlarm() {
        updateState { s ->
            // Optional: Implement snooze logic or simply bump state to refresh UI
            s.copy(
                alarms = s.alarms.map { alarm ->
                    if (alarm.enabled) alarm.copy(stamp = alarm.stamp.bump(deviceId)) else alarm
                }
            )
        }
    }
    suspend fun deleteExpense(id: String) {
        updateState { s ->
            s.copy(expenses = s.expenses.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun editNote(id: String, newTitle: String, newBody: String) {
        updateState { s ->
            s.copy(notes = s.notes.map {
                if (it.stamp.id == id) it.copy(title = newTitle, body = newBody, stamp = it.stamp.bump(deviceId)) else it
            })
        }
    }

    suspend fun deleteNote(id: String) {
        updateState { s ->
            s.copy(notes = s.notes.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun editAlarm(id: String, newTime: String, newLabel: String, newRepeat: String) {
        updateState { s ->
            s.copy(alarms = s.alarms.map {
                if (it.stamp.id == id) it.copy(time = newTime, label = newLabel, repeat = newRepeat, stamp = it.stamp.bump(deviceId)) else it
            })
        }
    }

    suspend fun editEvent(id: String, newTitle: String, newTimeRange: String, newLocation: String, newReminder: String) {
        updateState { s ->
            s.copy(events = s.events.map {
                if (it.stamp.id == id) it.copy(title = newTitle, timeRange = newTimeRange, location = newLocation, reminder = newReminder, stamp = it.stamp.bump(deviceId)) else it
            })
        }
    }

    suspend fun deleteEvent(id: String) {
        updateState { s ->
            s.copy(events = s.events.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    suspend fun addGymExercise(name: String, muscleGroup: String, equipment: String, initialPerformance: String = "Tap to log") {
        updateState { s ->
            val exercise = GymExercise(
                stamp = SyncStamp("ex-${ClockId.next()}", sourceDeviceId = deviceId),
                name = name,
                muscleGroup = muscleGroup,
                equipment = equipment,
                lastPerformance = initialPerformance,
                personalRecord = "None"
            )
            s.copy(gymWorkout = s.gymWorkout.copy(
                exercises = s.gymWorkout.exercises + exercise,
                stamp = s.gymWorkout.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun editGymExercise(id: String, name: String, muscleGroup: String, equipment: String) {
        updateState { s ->
            s.copy(gymWorkout = s.gymWorkout.copy(
                exercises = s.gymWorkout.exercises.map {
                    if (it.stamp.id == id) it.copy(name = name, muscleGroup = muscleGroup, equipment = equipment, stamp = it.stamp.bump(deviceId)) else it
                },
                stamp = s.gymWorkout.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun deleteGymExercise(id: String) {
        updateState { s ->
            s.copy(gymWorkout = s.gymWorkout.copy(
                exercises = s.gymWorkout.exercises.filterNot { it.stamp.id == id },
                stamp = s.gymWorkout.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun updateGymRestSeconds(seconds: Int) {
        updateState { s ->
            s.copy(gymWorkout = s.gymWorkout.copy(
                restSeconds = seconds,
                stamp = s.gymWorkout.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun updateGymWorkoutName(name: String) {
        updateState { s ->
            s.copy(gymWorkout = s.gymWorkout.copy(
                name = name,
                stamp = s.gymWorkout.stamp.bump(deviceId)
            ))
        }
    }

    suspend fun loadRunningPreset(level: String) {
        updateState { s ->
            val newRuns = when (level) {
                "Level 1: C25K" -> listOf(
                    RunWorkout(SyncStamp("run-c25k-w1"), 1, 1, "C25K Week 1 Day 1", listOf("5 min warmup walk", "8 x (60s run / 90s walk)", "5 min cooldown walk"), false),
                    RunWorkout(SyncStamp("run-c25k-w1d2"), 1, 2, "C25K Week 1 Day 2", listOf("5 min warmup walk", "8 x (60s run / 90s walk)", "5 min cooldown walk"), false),
                    RunWorkout(SyncStamp("run-c25k-w2"), 2, 1, "C25K Week 2 Day 1", listOf("5 min warmup walk", "6 x (90s run / 2m walk)", "5 min cooldown walk"), false),
                    RunWorkout(SyncStamp("run-c25k-w3"), 3, 1, "C25K Week 3 Day 1", listOf("5 min walk", "2 x (90s run / 90s walk + 3m run / 3m walk)", "5 min walk"), false),
                    RunWorkout(SyncStamp("run-c25k-w5"), 5, 3, "C25K Week 5 Day 3", listOf("5 min warmup walk", "20 min continuous run", "5 min cooldown walk"), false),
                    RunWorkout(SyncStamp("run-c25k-w9"), 9, 3, "C25K 5K Graduation Run", listOf("5 min warmup walk", "30 min continuous run (5K)", "5 min cooldown walk"), false)
                )
                "Level 2: 10K Bridge" -> listOf(
                    RunWorkout(SyncStamp("run-10k-w1"), 1, 1, "10K Bridge Week 1 Easy", listOf("5 min warmup walk", "4 km Easy Pace (Zone 2)", "5 min cooldown walk"), false),
                    RunWorkout(SyncStamp("run-10k-w1d2"), 1, 2, "10K Bridge Intervals", listOf("5 min walk", "5 x 400m Intervals (90s jog)", "5 min walk"), false),
                    RunWorkout(SyncStamp("run-10k-w2"), 2, 3, "10K Bridge Long Run", listOf("5 min walk", "7 km Long Slow Distance", "5 min walk"), false),
                    RunWorkout(SyncStamp("run-10k-w6"), 6, 3, "10K Graduation Race", listOf("5 min warmup walk", "10 km Continuous Race Run", "5 min cooldown walk"), false)
                )
                "Level 3: Half-Marathon" -> listOf(
                    RunWorkout(SyncStamp("run-hm-w1"), 1, 1, "Half-Marathon Aerobic Base", listOf("5 min walk", "8 km Zone 2 Aerobic Run", "5 min walk"), false),
                    RunWorkout(SyncStamp("run-hm-w2"), 2, 2, "Half-Marathon VO2 Intervals", listOf("5 min walk", "5 x 1000m @ 5K Pace (400m jog)", "5 min walk"), false),
                    RunWorkout(SyncStamp("run-hm-w4"), 4, 3, "Half-Marathon Progressive Long", listOf("5 min walk", "15 km Progressive Long Run", "5 min walk"), false),
                    RunWorkout(SyncStamp("run-hm-w12"), 12, 3, "21.1 km Half-Marathon Challenge", listOf("5 min walk", "21.1 km Half Marathon Race", "5 min cooldown walk"), false)
                )
                else -> emptyList()
            }
            // Add only new preset entries so loading another plan never erases completed history.
            val existingIds = s.runs.map { it.stamp.id }.toSet()
            val additions = newRuns.filterNot { it.stamp.id in existingIds }
            s.copy(runs = additions + s.runs)
        }
    }

    suspend fun addRunWorkout(week: Int, day: Int, title: String, intervals: List<String>) {
        updateState { s ->
            val run = RunWorkout(
                stamp = SyncStamp("run-${ClockId.next()}", sourceDeviceId = deviceId),
                week = week,
                day = day,
                title = title,
                intervals = intervals,
                completed = false
            )
            s.copy(runs = s.runs + run)
        }
    }

    suspend fun deleteRunWorkout(id: String) {
        updateState { s ->
            s.copy(runs = s.runs.filterNot { it.stamp.id == id })
        }
    }

    suspend fun deleteAlarm(id: String) {
        updateState { s ->
            s.copy(alarms = s.alarms.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }

    // Fully Implemented Timetable Actions for WearTimetableScreen
    suspend fun quickTimetableBlock(
        title: String,
        startTime: String,
        endTime: String,
        period: String,
        category: String
    ) {
        updateState { s ->
            val block = TimetableBlock(
                stamp = SyncStamp("timetable-${ClockId.next()}", sourceDeviceId = deviceId),
                title = title,
                startTime = startTime,
                endTime = endTime,
                period = period,
                category = category,
                completed = false
            )
            s.copy(timetableBlocks = (listOf(block) + s.timetableBlocks).distinctBy { it.stamp.id })
        }
    }

    suspend fun completeTimetableBlock(id: String) {
        updateState { s ->
            s.copy(timetableBlocks = s.timetableBlocks.map { block ->
                if (block.stamp.id == id) block.copy(completed = !block.completed, stamp = block.stamp.bump(deviceId)) else block
            })
        }
    }

    suspend fun deleteTimetableBlock(id: String) {
        updateState { s ->
            s.copy(timetableBlocks = s.timetableBlocks.map {
                if (it.stamp.id == id) it.copy(stamp = it.stamp.delete(deviceId)) else it
            })
        }
    }
    fun exportSyncPacket(): String = json.encodeToString(SyncPacket(deviceId = deviceId, state = _state.value))

    fun exportBundle(): ExportBundle = LifeExporter.export(_state.value, exportSyncPacket())

    suspend fun updateSettings(block: (UserSettings) -> UserSettings) {
        updateState { s -> s.copy(settings = block(s.settings)) }
    }

    private suspend fun persist() {
        store.save(_state.value)
        transport.publish(SyncPacket(deviceId = deviceId, state = _state.value))
    }
}

private fun SyncStamp.bump(deviceId: String): SyncStamp =
    copy(updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), sourceDeviceId = deviceId, version = version + 1)

private fun SyncStamp.delete(deviceId: String): SyncStamp =
    copy(updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), sourceDeviceId = deviceId, deletedAt = updatedAt, version = version + 1)

private object ClockId {
    fun next(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}
