package com.creativeblisslabs.wearproductivity.shared

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
enum class RoutineRecurrence {
    SAME_EVERY_DAY,
    WEEKLY_ROTATION,
    MONTHLY_ROTATION,
    INDEFINITE,

    CUSTOM_DAILY
}

@Serializable
enum class RoutinePhase {
    PREPARATION,
    SPRINT,
    REVIEW,
    RECOVERY,
    GENERAL
}
@Serializable
data class AlarmTask(
    val stamp: SyncStamp,
    val alarmId: String, // Links to a LifeAlarm
    val title: String,
    val complete: Boolean = false,
    val actionType: String = "general" // e.g., "habit", "todo", "stretch"
)
@Serializable
data class RoutineTask(
    val stamp: SyncStamp,
    val title: String,
    val startTime: String, // "06:30" (24-hour format)
    val endTime: String,   // "07:15"
    val phase: RoutinePhase = RoutinePhase.GENERAL,
    val doneToday: Boolean = false,
    val orderIndex: Int = 0,
    val notes: String = "",
    val recurrence: RoutineRecurrence = RoutineRecurrence.SAME_EVERY_DAY,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // 1=Mon, 7=Sun
) {
    fun isCurrent(currentHour: Int, currentMinute: Int): Boolean {
        val currentMinutes = currentHour * 60 + currentMinute
        val startParts = startTime.split(":").mapNotNull { it.toIntOrNull() }
        val endParts = endTime.split(":").mapNotNull { it.toIntOrNull() }
        if (startParts.size < 2 || endParts.size < 2) return false
        val startMins = startParts[0] * 60 + startParts[1]
        val endMins = endParts[0] * 60 + endParts[1]
        return currentMinutes in startMins..endMins
    }
}

@Serializable
data class RoutineStats(
    val dayCompletionRate: Float,
    val completedCount: Int,
    val totalCount: Int,
    val weeklyConsistencyPercentage: Float,
    val monthlyConsistencyPercentage: Float
)
