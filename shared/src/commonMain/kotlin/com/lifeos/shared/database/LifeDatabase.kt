package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY priority ASC, updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun getAllTasksIncludingDeleted(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET complete = :complete, updatedAt = :updatedAt, version = version + 1 WHERE id = :id")
    suspend fun updateCompletion(id: String, complete: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun delete(id: String, deletedAt: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Query("UPDATE habits SET doneToday = :done, streak = :streak, updatedAt = :updatedAt, version = version + 1 WHERE id = :id")
    suspend fun updateHabit(id: String, done: Boolean, streak: Int, updatedAt: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllFoodLogs(): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLogEntity)

    @Query("UPDATE food_logs SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun deleteFoodLog(id: String, deletedAt: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)
}

@Dao
interface FastDao {
    @Query("SELECT * FROM fast_sessions LIMIT 1")
    fun getActiveFast(): Flow<FastSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFast(fast: FastSessionEntity)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms WHERE deletedAt IS NULL ORDER BY time ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE deletedAt IS NULL")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deletedAt IS NULL ORDER BY timeRange ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
}

@Dao
interface RunDao {
    @Query("SELECT * FROM runs WHERE deletedAt IS NULL ORDER BY week ASC, day ASC")
    fun getAllRuns(): Flow<List<RunWorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunWorkoutEntity)
}

@Dao
interface GymDao {
    @Query("SELECT * FROM gym_workouts LIMIT 1")
    fun getActiveWorkout(): Flow<GymWorkoutEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: GymWorkoutEntity)

    @Query("SELECT * FROM gym_exercises WHERE workoutId = :workoutId AND deletedAt IS NULL")
    fun getExercisesForWorkout(workoutId: String): Flow<List<GymExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: GymExerciseEntity)
}

@Dao
interface WellnessDao {
    @Query("SELECT * FROM wellness LIMIT 1")
    fun getWellness(): Flow<WellnessEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWellness(wellness: WellnessEntity)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions LIMIT 1")
    fun getFocus(): Flow<FocusSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFocus(focus: FocusSessionEntity)
}

@Dao
interface FocusHistoryDao {
    @Query("SELECT * FROM focus_history WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllFocusHistory(): Flow<List<FocusHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusHistory(history: FocusHistoryEntity)

    @Query("UPDATE focus_history SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun deleteFocusHistory(id: String, deletedAt: Long)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE deletedAt IS NULL")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}

@Dao
interface ImportantDateDao {
    @Query("SELECT * FROM important_dates WHERE deletedAt IS NULL ORDER BY dateIso ASC")
    fun getAllImportantDates(): Flow<List<ImportantDateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportantDate(date: ImportantDateEntity)

    @Query("UPDATE important_dates SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun deleteImportantDate(id: String, deletedAt: Long)
}

@Database(
    entities = [
        TaskEntity::class, NoteEntity::class, HabitEntity::class, ExpenseEntity::class,
        FoodLogEntity::class, AlarmEntity::class, FastSessionEntity::class, BudgetEntity::class,
        EventEntity::class, RunWorkoutEntity::class, GymWorkoutEntity::class, GymExerciseEntity::class,
        WellnessEntity::class, FocusSessionEntity::class, ReviewEntity::class, FocusHistoryEntity::class,
        ImportantDateEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun fastDao(): FastDao
    abstract fun alarmDao(): AlarmDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun eventDao(): EventDao
    abstract fun runDao(): RunDao
    abstract fun gymDao(): GymDao
    abstract fun wellnessDao(): WellnessDao
    abstract fun focusDao(): FocusDao
    abstract fun focusHistoryDao(): FocusHistoryDao
    abstract fun reviewDao(): ReviewDao
    abstract fun importantDateDao(): ImportantDateDao
}

expect class DatabaseBuilder(context: Any) {
    fun build(): LifeDatabase
}
