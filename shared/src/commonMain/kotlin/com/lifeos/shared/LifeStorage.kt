package com.creativeblisslabs.wearproductivity.shared

import com.creativeblisslabs.wearproductivity.shared.database.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LifeStore {
    suspend fun load(): LifeState
    suspend fun save(state: LifeState)
}

class RoomLifeStore(private val db: LifeDatabase) : LifeStore {
    override suspend fun load(): LifeState {
        val tasks = db.taskDao().getAllTasksIncludingDeleted().first().map { it.toModel() }
        val habits = db.habitDao().getAllHabits().first().map { it.toModel() }
        val expenses = db.expenseDao().getAllExpenses().first().map { it.toModel() }
        val foodLogs = db.foodLogDao().getAllFoodLogs().first().map { it.toModel() }
        val notes = db.noteDao().getAllNotes().first().map { it.toModel() }
        val fast = db.fastDao().getActiveFast().first()?.toModel() ?: LifeSeed.state().fast
        val alarms = db.alarmDao().getAllAlarms().first().map { it.toModel() }
        val budgets = db.budgetDao().getAllBudgets().first().map { it.toModel() }
        val events = db.eventDao().getAllEvents().first().map { it.toModel() }
        val runs = db.runDao().getAllRuns().first().map { it.toModel() }
        
        val wellness = db.wellnessDao().getWellness().first()?.toModel() ?: LifeSeed.state().wellness
        val focus = db.focusDao().getFocus().first()?.toModel() ?: LifeSeed.state().focus
        val gymEntity = db.gymDao().getActiveWorkout().first()
        val gymExercises = gymEntity?.let { db.gymDao().getExercisesForWorkout(it.id).first().map { e -> e.toModel() } } ?: emptyList()
        val gymWorkout = gymEntity?.toModel(gymExercises) ?: LifeSeed.state().gymWorkout

        val reviews = db.reviewDao().getAllReviews().first().map { it.toModel() }.ifEmpty { LifeSeed.state().reviews }

        return LifeState(
            tasks = tasks,
            notes = notes,
            habits = habits,
            fast = fast,
            foodLogs = foodLogs,
            alarms = alarms,
            expenses = expenses,
            budgets = budgets,
            events = events,
            runs = runs,
            gymWorkout = gymWorkout,
            reviews = reviews,
            focus = focus,
            wellness = wellness
        )
    }

    override suspend fun save(state: LifeState) {
        state.tasks.forEach { db.taskDao().insertTask(it.toEntity()) }
        state.habits.forEach { db.habitDao().insertHabit(it.toEntity()) }
        state.expenses.forEach { db.expenseDao().insertExpense(it.toEntity()) }
        state.foodLogs.forEach { db.foodLogDao().insertFoodLog(it.toEntity()) }
        state.notes.forEach { db.noteDao().insertNote(it.toEntity()) }
        db.fastDao().updateFast(state.fast.toEntity())
        state.alarms.forEach { db.alarmDao().insertAlarm(it.toEntity()) }
        state.budgets.forEach { db.budgetDao().insertBudget(it.toEntity()) }
        state.events.forEach { db.eventDao().insertEvent(it.toEntity()) }
        state.runs.forEach { db.runDao().insertRun(it.toEntity()) }
        db.wellnessDao().updateWellness(state.wellness.toEntity())
        db.focusDao().updateFocus(state.focus.toEntity())
        state.reviews.forEach { db.reviewDao().insertReview(it.toEntity()) }
        
        val workout = state.gymWorkout
        db.gymDao().insertWorkout(workout.toEntity())
        workout.exercises.forEach { db.gymDao().insertExercise(it.toEntity(workout.stamp.id)) }
    }
}

class InMemoryLifeStore(initialState: LifeState = LifeSeed.state()) : LifeStore {
    private var state = initialState
    override suspend fun load(): LifeState = state
    override suspend fun save(state: LifeState) { this.state = state }
}

@Serializable
data class SyncChange(
    val entityId: String,
    val module: LifeModule,
    val operation: String,
    val changedAt: Long,
    val sourceDeviceId: String
)

data class SyncMergeResult(
    val mergedState: LifeState,
    val acceptedRemoteChanges: Int,
    val keptLocalChanges: Int
)

object LifeSyncEngine {
    fun merge(local: LifeState, remote: LifeState): SyncMergeResult {
        var remoteChanges = 0
        
        fun <T> mergeList(l: List<T>, r: List<T>, s: (T) -> SyncStamp): List<T> {
            val byId = linkedMapOf<String, T>()
            l.forEach { byId[s(it).id] = it }
            r.forEach { incoming ->
                val id = s(incoming).id
                val existing = byId[id]
                if (existing == null) {
                    byId[id] = incoming
                    remoteChanges++
                } else {
                    val winner = newer(existing, incoming, s)
                    if (winner === incoming) {
                        byId[id] = incoming
                        remoteChanges++
                    }
                }
            }
            return byId.values.sortedByDescending { s(it).updatedAt }
        }

        val merged = local.copy(
            tasks = mergeList(local.tasks, remote.tasks) { it.stamp },
            notes = mergeList(local.notes, remote.notes) { it.stamp },
            habits = mergeList(local.habits, remote.habits) { it.stamp },
            foodLogs = mergeList(local.foodLogs, remote.foodLogs) { it.stamp },
            fast = newer(local.fast, remote.fast) { it.stamp }.also { if (it === remote.fast && it !== local.fast) remoteChanges++ },
            alarms = mergeList(local.alarms, remote.alarms) { it.stamp },
            expenses = mergeList(local.expenses, remote.expenses) { it.stamp },
            budgets = mergeList(local.budgets, remote.budgets) { it.stamp },
            events = mergeList(local.events, remote.events) { it.stamp },
            runs = mergeList(local.runs, remote.runs) { it.stamp },
            gymWorkout = newer(local.gymWorkout, remote.gymWorkout) { it.stamp }.also { if (it === remote.gymWorkout && it !== local.gymWorkout) remoteChanges++ },
            reviews = mergeList(local.reviews, remote.reviews) { it.stamp },
            focus = newer(local.focus, remote.focus) { it.stamp }.also { if (it === remote.focus && it !== local.focus) remoteChanges++ },
            wellness = newer(local.wellness, remote.wellness) { it.stamp }.also { if (it === remote.wellness && it !== local.wellness) remoteChanges++ }
        )
        return SyncMergeResult(merged, acceptedRemoteChanges = remoteChanges, keptLocalChanges = 0)
    }

    private fun <T> newer(local: T, remote: T, stamp: (T) -> SyncStamp): T {
        val sL = stamp(local)
        val sR = stamp(remote)
        return when {
            sR.version > sL.version -> remote
            sR.version < sL.version -> local
            sR.updatedAt > sL.updatedAt -> remote
            else -> local
        }
    }
}

interface CompanionSyncTransport {
    fun publish(packet: SyncPacket)
    fun latestPacket(): SyncPacket?
    fun sendQuickCapture(title: String, module: LifeModule)
    fun acknowledgeReminder(reminderId: String)
    fun sendFile(fileUri: String, path: String)
}

class LoopbackCompanionSyncTransport : CompanionSyncTransport {
    private var packet: SyncPacket? = null
    override fun publish(packet: SyncPacket) { this.packet = packet }
    override fun latestPacket(): SyncPacket? = packet
    override fun sendQuickCapture(title: String, module: LifeModule) = Unit
    override fun acknowledgeReminder(reminderId: String) = Unit
    override fun sendFile(fileUri: String, path: String) = Unit
}
