package com.creativeblisslabs.wearproductivity.shared

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderKind {
    TaskDue,
    HabitPrompt,
    FastStart,
    FastEnd,
    FoodLog,
    Alarm,
    Event,
    Run,
    Gym,
    Budget,
    Focus,
    Water,
    Review
}

@Serializable
data class ReminderSpec(
    val stamp: SyncStamp,
    val kind: ReminderKind,
    val title: String,
    val fireAtLabel: String,
    val repeatRule: String? = null,
    val haptic: Boolean = true,
    val exact: Boolean = false
)

@Serializable
data class WearTileSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val module: LifeModule
)

@Serializable
data class ExportBundle(
    val tasksCsv: String,
    val habitsCsv: String,
    val foodCsv: String,
    val expensesCsv: String,
    val gymCsv: String,
    val runsCsv: String,
    val json: String
)

object LifeReminderPlanner {
    fun reminders(state: LifeState): List<ReminderSpec> = buildList {
        state.tasks.filterNot { it.complete }.forEach {
            add(ReminderSpec(it.stamp, ReminderKind.TaskDue, it.title, it.dueLabel, exact = false))
        }
        state.habits.filterNot { it.doneToday }.forEach {
            add(ReminderSpec(it.stamp, ReminderKind.HabitPrompt, it.name, "Today", repeatRule = it.cadence))
        }
        add(ReminderSpec(state.fast.stamp, ReminderKind.FastEnd, "Fast ends", state.fast.endsAtLabel, haptic = true))
        state.alarms.filter { it.enabled }.forEach {
            add(ReminderSpec(it.stamp, ReminderKind.Alarm, it.label, it.time, repeatRule = it.repeat, exact = true))
        }
        state.events.forEach {
            add(ReminderSpec(it.stamp, ReminderKind.Event, it.title, it.timeRange, exact = false))
        }
        state.runs.filterNot { it.completed }.take(1).forEach {
            add(ReminderSpec(it.stamp, ReminderKind.Run, it.title, "Planned", haptic = true))
        }
        add(ReminderSpec(state.gymWorkout.stamp, ReminderKind.Gym, state.gymWorkout.name, "Planned", haptic = true))
        state.budgets.filter { it.spent / it.limit >= 0.8 }.forEach {
            add(ReminderSpec(it.stamp, ReminderKind.Budget, "${it.category} budget", "${it.currency}${it.spent.toInt()} spent"))
        }
        add(ReminderSpec(state.focus.stamp, ReminderKind.Focus, state.focus.label, "${state.focus.remainingMinutes} min"))
        add(ReminderSpec(state.wellness.stamp, ReminderKind.Water, "Water", "${state.wellness.waterGlasses}/8 glasses"))
        state.reviews.forEach {
            add(ReminderSpec(it.stamp, ReminderKind.Review, it.title, it.cadence))
        }
    }
}

object LifeTiles {
    fun tiles(state: LifeState): List<WearTileSpec> = listOf(
        WearTileSpec("tile-today", "Today", "${state.today.tasksDone}/${state.today.tasksTotal} tasks", state.today.tasksDone / state.today.tasksTotal.toFloat().coerceAtLeast(1f), LifeModule.Today),
        WearTileSpec("tile-food", "Nutrition", "${state.today.totalCaloriesConsumed}/${state.today.calorieTarget} kcal", state.today.totalCaloriesConsumed / state.today.calorieTarget.toFloat().coerceAtLeast(1f), LifeModule.Nutrition),
        WearTileSpec("tile-fast", "Fast", "${(state.fast.progress * 100).toInt()}%", state.fast.progress, LifeModule.Fasting),
        WearTileSpec("tile-habits", "Habits", "${state.today.habitsDone}/${state.today.habitsTotal}", state.today.habitsDone / state.today.habitsTotal.toFloat().coerceAtLeast(1f), LifeModule.Habits),
        WearTileSpec("tile-run", "C25K", state.today.plannedFitness, 0.25f, LifeModule.Running),
        WearTileSpec("tile-gym", "Gym", state.gymWorkout.name, 0.45f, LifeModule.Gym),
        WearTileSpec("tile-money", "Spend", state.today.monthlySpend, 0.58f, LifeModule.Expenses)
    )
}

object LifeExporter {
    fun export(state: LifeState, syncJson: String): ExportBundle = ExportBundle(
        tasksCsv = csv("title,project,due,priority,complete", state.tasks.map { "${it.title},${it.project},${it.dueLabel},${it.priority},${it.complete}" }),
        habitsCsv = csv("name,cadence,streak,done_today", state.habits.map { "${it.name},${it.cadence},${it.streak},${it.doneToday}" }),
        foodCsv = csv("name,meal,calories,protein,carbs,fat,logged_at", state.foodLogs.map { "${it.name},${it.mealType.name},${it.calories},${it.proteinGrams},${it.carbsGrams},${it.fatGrams},${it.loggedAtLabel}" }),
        expensesCsv = csv("amount,currency,category,note,date", state.expenses.map { "${it.amount},${it.currency},${it.category},${it.note},${it.dateLabel}" }),
        gymCsv = csv("exercise,muscle,equipment,last,pr", state.gymWorkout.exercises.map { "${it.name},${it.muscleGroup},${it.equipment},${it.lastPerformance},${it.personalRecord}" }),
        runsCsv = csv("title,week,day,completed,distance_km", state.runs.map { "${it.title},${it.week},${it.day},${it.completed},${it.distanceKm ?: ""}" }),
        json = syncJson
    )

    private fun csv(header: String, rows: List<String>): String = (listOf(header) + rows).joinToString("\n")
}

object SyncPaths {
    const val LIFE_STATE = "/lifeos/state"
    const val QUICK_CAPTURE = "/lifeos/capture"
    const val REMINDER_ACK = "/lifeos/reminder_ack"
    const val WORKOUT_EVENT = "/lifeos/workout_event"
    const val RUN_EVENT = "/lifeos/run_event"
    const val LOG_FOOD = "/lifeos/log_food"
    const val AUDIO_TRANSFER = "/lifeos/audio_transfer"
    const val FILE_TRANSFER = "/lifeos/file_transfer"
    const val TOGGLE_TASK = "/lifeos/toggle_task"
    const val TOGGLE_HABIT = "/lifeos/toggle_habit"
    const val ADD_EXPENSE = "/lifeos/add_expense"
}
