package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class LifeDatabase_Impl : LifeDatabase() {
  private val _taskDao: Lazy<TaskDao> = lazy {
    TaskDao_Impl(this)
  }

  private val _noteDao: Lazy<NoteDao> = lazy {
    NoteDao_Impl(this)
  }

  private val _habitDao: Lazy<HabitDao> = lazy {
    HabitDao_Impl(this)
  }

  private val _foodLogDao: Lazy<FoodLogDao> = lazy {
    FoodLogDao_Impl(this)
  }

  private val _fastDao: Lazy<FastDao> = lazy {
    FastDao_Impl(this)
  }

  private val _alarmDao: Lazy<AlarmDao> = lazy {
    AlarmDao_Impl(this)
  }

  private val _expenseDao: Lazy<ExpenseDao> = lazy {
    ExpenseDao_Impl(this)
  }

  private val _budgetDao: Lazy<BudgetDao> = lazy {
    BudgetDao_Impl(this)
  }

  private val _eventDao: Lazy<EventDao> = lazy {
    EventDao_Impl(this)
  }

  private val _runDao: Lazy<RunDao> = lazy {
    RunDao_Impl(this)
  }

  private val _gymDao: Lazy<GymDao> = lazy {
    GymDao_Impl(this)
  }

  private val _wellnessDao: Lazy<WellnessDao> = lazy {
    WellnessDao_Impl(this)
  }

  private val _focusDao: Lazy<FocusDao> = lazy {
    FocusDao_Impl(this)
  }

  private val _focusHistoryDao: Lazy<FocusHistoryDao> = lazy {
    FocusHistoryDao_Impl(this)
  }

  private val _reviewDao: Lazy<ReviewDao> = lazy {
    ReviewDao_Impl(this)
  }

  private val _importantDateDao: Lazy<ImportantDateDao> = lazy {
    ImportantDateDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "360f3a862deb07f6b174465f5d8f13b2", "b5bdf72b7ab629cad928e43e0bb87936") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `title` TEXT NOT NULL, `project` TEXT NOT NULL, `dueLabel` TEXT NOT NULL, `priority` INTEGER NOT NULL, `complete` INTEGER NOT NULL, `tagsJson` TEXT NOT NULL, `scheduledTime` TEXT, `hapticEnabled` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `pinned` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `cadence` TEXT NOT NULL, `streak` INTEGER NOT NULL, `doneToday` INTEGER NOT NULL, `colorName` TEXT NOT NULL, `targetValue` REAL NOT NULL, `currentValue` REAL NOT NULL, `unit` TEXT NOT NULL, `isTimerActive` INTEGER NOT NULL, `remainingSeconds` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `amount` REAL NOT NULL, `currency` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `dateLabel` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `food_logs` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `mealType` TEXT NOT NULL, `calories` INTEGER NOT NULL, `proteinGrams` REAL NOT NULL, `carbsGrams` REAL NOT NULL, `fatGrams` REAL NOT NULL, `loggedAtLabel` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `alarms` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `time` TEXT NOT NULL, `label` TEXT NOT NULL, `repeat` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `haptic` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `fast_sessions` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `plan` TEXT NOT NULL, `startedAtLabel` TEXT NOT NULL, `endsAtLabel` TEXT NOT NULL, `progress` REAL NOT NULL, `active` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `category` TEXT NOT NULL, `spent` REAL NOT NULL, `limit` REAL NOT NULL, `currency` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `events` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `title` TEXT NOT NULL, `timeRange` TEXT NOT NULL, `location` TEXT NOT NULL, `reminder` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `runs` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `week` INTEGER NOT NULL, `day` INTEGER NOT NULL, `title` TEXT NOT NULL, `intervalsJson` TEXT NOT NULL, `completed` INTEGER NOT NULL, `distanceKm` REAL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `gym_workouts` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `restSeconds` INTEGER NOT NULL, `active` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `gym_exercises` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `workoutId` TEXT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `equipment` TEXT NOT NULL, `lastPerformance` TEXT NOT NULL, `personalRecord` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `wellness` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `waterGlasses` INTEGER NOT NULL, `mood` TEXT NOT NULL, `energy` INTEGER NOT NULL, `bodyWeight` REAL, `steps` INTEGER NOT NULL, `sleepMinutes` INTEGER NOT NULL, `focusMinutesGoal` INTEGER NOT NULL, `currentHeartRateBpm` REAL NOT NULL, `heartRateAccuracy` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `label` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `remainingMinutes` INTEGER NOT NULL, `active` INTEGER NOT NULL, `type` TEXT NOT NULL, `subject` TEXT NOT NULL, `totalSeconds` INTEGER NOT NULL, `remainingSeconds` INTEGER NOT NULL, `isPaused` INTEGER NOT NULL, `startedAtTimestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `reviews` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `title` TEXT NOT NULL, `cadence` TEXT NOT NULL, `questionsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_history` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `subject` TEXT NOT NULL, `minutesFocused` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `important_dates` (`id` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `sourceDeviceId` TEXT NOT NULL, `deletedAt` INTEGER, `version` INTEGER NOT NULL, `personName` TEXT NOT NULL, `title` TEXT NOT NULL, `dateIso` TEXT NOT NULL, `category` TEXT NOT NULL, `reminderDaysBefore` INTEGER NOT NULL, `notes` TEXT NOT NULL, `isAnnual` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '360f3a862deb07f6b174465f5d8f13b2')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tasks`")
        connection.execSQL("DROP TABLE IF EXISTS `notes`")
        connection.execSQL("DROP TABLE IF EXISTS `habits`")
        connection.execSQL("DROP TABLE IF EXISTS `expenses`")
        connection.execSQL("DROP TABLE IF EXISTS `food_logs`")
        connection.execSQL("DROP TABLE IF EXISTS `alarms`")
        connection.execSQL("DROP TABLE IF EXISTS `fast_sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `budgets`")
        connection.execSQL("DROP TABLE IF EXISTS `events`")
        connection.execSQL("DROP TABLE IF EXISTS `runs`")
        connection.execSQL("DROP TABLE IF EXISTS `gym_workouts`")
        connection.execSQL("DROP TABLE IF EXISTS `gym_exercises`")
        connection.execSQL("DROP TABLE IF EXISTS `wellness`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `reviews`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_history`")
        connection.execSQL("DROP TABLE IF EXISTS `important_dates`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsTasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTasks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("project", TableInfo.Column("project", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("dueLabel", TableInfo.Column("dueLabel", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("priority", TableInfo.Column("priority", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("complete", TableInfo.Column("complete", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("tagsJson", TableInfo.Column("tagsJson", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("scheduledTime", TableInfo.Column("scheduledTime", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("hapticEnabled", TableInfo.Column("hapticEnabled", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("syncStatus", TableInfo.Column("syncStatus", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTasks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTasks: TableInfo = TableInfo("tasks", _columnsTasks, _foreignKeysTasks,
            _indicesTasks)
        val _existingTasks: TableInfo = read(connection, "tasks")
        if (!_infoTasks.equals(_existingTasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tasks(com.creativeblisslabs.wearproductivity.shared.database.TaskEntity).
              | Expected:
              |""".trimMargin() + _infoTasks + """
              |
              | Found:
              |""".trimMargin() + _existingTasks)
        }
        val _columnsNotes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNotes.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("body", TableInfo.Column("body", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNotes.put("pinned", TableInfo.Column("pinned", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNotes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNotes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNotes: TableInfo = TableInfo("notes", _columnsNotes, _foreignKeysNotes,
            _indicesNotes)
        val _existingNotes: TableInfo = read(connection, "notes")
        if (!_infoNotes.equals(_existingNotes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |notes(com.creativeblisslabs.wearproductivity.shared.database.NoteEntity).
              | Expected:
              |""".trimMargin() + _infoNotes + """
              |
              | Found:
              |""".trimMargin() + _existingNotes)
        }
        val _columnsHabits: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHabits.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("cadence", TableInfo.Column("cadence", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("streak", TableInfo.Column("streak", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("doneToday", TableInfo.Column("doneToday", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("colorName", TableInfo.Column("colorName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("targetValue", TableInfo.Column("targetValue", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("currentValue", TableInfo.Column("currentValue", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("unit", TableInfo.Column("unit", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("isTimerActive", TableInfo.Column("isTimerActive", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("remainingSeconds", TableInfo.Column("remainingSeconds", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHabits: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHabits: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoHabits: TableInfo = TableInfo("habits", _columnsHabits, _foreignKeysHabits,
            _indicesHabits)
        val _existingHabits: TableInfo = read(connection, "habits")
        if (!_infoHabits.equals(_existingHabits)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |habits(com.creativeblisslabs.wearproductivity.shared.database.HabitEntity).
              | Expected:
              |""".trimMargin() + _infoHabits + """
              |
              | Found:
              |""".trimMargin() + _existingHabits)
        }
        val _columnsExpenses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsExpenses.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("currency", TableInfo.Column("currency", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("note", TableInfo.Column("note", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("dateLabel", TableInfo.Column("dateLabel", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysExpenses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesExpenses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoExpenses: TableInfo = TableInfo("expenses", _columnsExpenses, _foreignKeysExpenses,
            _indicesExpenses)
        val _existingExpenses: TableInfo = read(connection, "expenses")
        if (!_infoExpenses.equals(_existingExpenses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |expenses(com.creativeblisslabs.wearproductivity.shared.database.ExpenseEntity).
              | Expected:
              |""".trimMargin() + _infoExpenses + """
              |
              | Found:
              |""".trimMargin() + _existingExpenses)
        }
        val _columnsFoodLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFoodLogs.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("mealType", TableInfo.Column("mealType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("calories", TableInfo.Column("calories", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("proteinGrams", TableInfo.Column("proteinGrams", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("carbsGrams", TableInfo.Column("carbsGrams", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("fatGrams", TableInfo.Column("fatGrams", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("loggedAtLabel", TableInfo.Column("loggedAtLabel", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodLogs.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFoodLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFoodLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFoodLogs: TableInfo = TableInfo("food_logs", _columnsFoodLogs,
            _foreignKeysFoodLogs, _indicesFoodLogs)
        val _existingFoodLogs: TableInfo = read(connection, "food_logs")
        if (!_infoFoodLogs.equals(_existingFoodLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |food_logs(com.creativeblisslabs.wearproductivity.shared.database.FoodLogEntity).
              | Expected:
              |""".trimMargin() + _infoFoodLogs + """
              |
              | Found:
              |""".trimMargin() + _existingFoodLogs)
        }
        val _columnsAlarms: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAlarms.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("time", TableInfo.Column("time", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("repeat", TableInfo.Column("repeat", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("haptic", TableInfo.Column("haptic", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAlarms: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAlarms: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAlarms: TableInfo = TableInfo("alarms", _columnsAlarms, _foreignKeysAlarms,
            _indicesAlarms)
        val _existingAlarms: TableInfo = read(connection, "alarms")
        if (!_infoAlarms.equals(_existingAlarms)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |alarms(com.creativeblisslabs.wearproductivity.shared.database.AlarmEntity).
              | Expected:
              |""".trimMargin() + _infoAlarms + """
              |
              | Found:
              |""".trimMargin() + _existingAlarms)
        }
        val _columnsFastSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFastSessions.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("plan", TableInfo.Column("plan", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("startedAtLabel", TableInfo.Column("startedAtLabel", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("endsAtLabel", TableInfo.Column("endsAtLabel", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("progress", TableInfo.Column("progress", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFastSessions.put("active", TableInfo.Column("active", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFastSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFastSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFastSessions: TableInfo = TableInfo("fast_sessions", _columnsFastSessions,
            _foreignKeysFastSessions, _indicesFastSessions)
        val _existingFastSessions: TableInfo = read(connection, "fast_sessions")
        if (!_infoFastSessions.equals(_existingFastSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |fast_sessions(com.creativeblisslabs.wearproductivity.shared.database.FastSessionEntity).
              | Expected:
              |""".trimMargin() + _infoFastSessions + """
              |
              | Found:
              |""".trimMargin() + _existingFastSessions)
        }
        val _columnsBudgets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBudgets.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("spent", TableInfo.Column("spent", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("limit", TableInfo.Column("limit", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("currency", TableInfo.Column("currency", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBudgets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBudgets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBudgets: TableInfo = TableInfo("budgets", _columnsBudgets, _foreignKeysBudgets,
            _indicesBudgets)
        val _existingBudgets: TableInfo = read(connection, "budgets")
        if (!_infoBudgets.equals(_existingBudgets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |budgets(com.creativeblisslabs.wearproductivity.shared.database.BudgetEntity).
              | Expected:
              |""".trimMargin() + _infoBudgets + """
              |
              | Found:
              |""".trimMargin() + _existingBudgets)
        }
        val _columnsEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEvents.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("timeRange", TableInfo.Column("timeRange", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("location", TableInfo.Column("location", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEvents.put("reminder", TableInfo.Column("reminder", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEvents: TableInfo = TableInfo("events", _columnsEvents, _foreignKeysEvents,
            _indicesEvents)
        val _existingEvents: TableInfo = read(connection, "events")
        if (!_infoEvents.equals(_existingEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |events(com.creativeblisslabs.wearproductivity.shared.database.EventEntity).
              | Expected:
              |""".trimMargin() + _infoEvents + """
              |
              | Found:
              |""".trimMargin() + _existingEvents)
        }
        val _columnsRuns: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRuns.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("week", TableInfo.Column("week", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("day", TableInfo.Column("day", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("intervalsJson", TableInfo.Column("intervalsJson", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("completed", TableInfo.Column("completed", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRuns.put("distanceKm", TableInfo.Column("distanceKm", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRuns: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRuns: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRuns: TableInfo = TableInfo("runs", _columnsRuns, _foreignKeysRuns, _indicesRuns)
        val _existingRuns: TableInfo = read(connection, "runs")
        if (!_infoRuns.equals(_existingRuns)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |runs(com.creativeblisslabs.wearproductivity.shared.database.RunWorkoutEntity).
              | Expected:
              |""".trimMargin() + _infoRuns + """
              |
              | Found:
              |""".trimMargin() + _existingRuns)
        }
        val _columnsGymWorkouts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGymWorkouts.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("restSeconds", TableInfo.Column("restSeconds", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymWorkouts.put("active", TableInfo.Column("active", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGymWorkouts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGymWorkouts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGymWorkouts: TableInfo = TableInfo("gym_workouts", _columnsGymWorkouts,
            _foreignKeysGymWorkouts, _indicesGymWorkouts)
        val _existingGymWorkouts: TableInfo = read(connection, "gym_workouts")
        if (!_infoGymWorkouts.equals(_existingGymWorkouts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |gym_workouts(com.creativeblisslabs.wearproductivity.shared.database.GymWorkoutEntity).
              | Expected:
              |""".trimMargin() + _infoGymWorkouts + """
              |
              | Found:
              |""".trimMargin() + _existingGymWorkouts)
        }
        val _columnsGymExercises: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGymExercises.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("workoutId", TableInfo.Column("workoutId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("muscleGroup", TableInfo.Column("muscleGroup", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("equipment", TableInfo.Column("equipment", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("lastPerformance", TableInfo.Column("lastPerformance", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGymExercises.put("personalRecord", TableInfo.Column("personalRecord", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGymExercises: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGymExercises: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGymExercises: TableInfo = TableInfo("gym_exercises", _columnsGymExercises,
            _foreignKeysGymExercises, _indicesGymExercises)
        val _existingGymExercises: TableInfo = read(connection, "gym_exercises")
        if (!_infoGymExercises.equals(_existingGymExercises)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |gym_exercises(com.creativeblisslabs.wearproductivity.shared.database.GymExerciseEntity).
              | Expected:
              |""".trimMargin() + _infoGymExercises + """
              |
              | Found:
              |""".trimMargin() + _existingGymExercises)
        }
        val _columnsWellness: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWellness.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("waterGlasses", TableInfo.Column("waterGlasses", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("mood", TableInfo.Column("mood", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("energy", TableInfo.Column("energy", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("bodyWeight", TableInfo.Column("bodyWeight", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("steps", TableInfo.Column("steps", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("sleepMinutes", TableInfo.Column("sleepMinutes", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("focusMinutesGoal", TableInfo.Column("focusMinutesGoal", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("currentHeartRateBpm", TableInfo.Column("currentHeartRateBpm", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWellness.put("heartRateAccuracy", TableInfo.Column("heartRateAccuracy", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWellness: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWellness: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWellness: TableInfo = TableInfo("wellness", _columnsWellness, _foreignKeysWellness,
            _indicesWellness)
        val _existingWellness: TableInfo = read(connection, "wellness")
        if (!_infoWellness.equals(_existingWellness)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |wellness(com.creativeblisslabs.wearproductivity.shared.database.WellnessEntity).
              | Expected:
              |""".trimMargin() + _infoWellness + """
              |
              | Found:
              |""".trimMargin() + _existingWellness)
        }
        val _columnsFocusSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFocusSessions.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("minutes", TableInfo.Column("minutes", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("remainingMinutes", TableInfo.Column("remainingMinutes",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("active", TableInfo.Column("active", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("subject", TableInfo.Column("subject", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("totalSeconds", TableInfo.Column("totalSeconds", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("remainingSeconds", TableInfo.Column("remainingSeconds",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("isPaused", TableInfo.Column("isPaused", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("startedAtTimestamp", TableInfo.Column("startedAtTimestamp",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFocusSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFocusSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFocusSessions: TableInfo = TableInfo("focus_sessions", _columnsFocusSessions,
            _foreignKeysFocusSessions, _indicesFocusSessions)
        val _existingFocusSessions: TableInfo = read(connection, "focus_sessions")
        if (!_infoFocusSessions.equals(_existingFocusSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |focus_sessions(com.creativeblisslabs.wearproductivity.shared.database.FocusSessionEntity).
              | Expected:
              |""".trimMargin() + _infoFocusSessions + """
              |
              | Found:
              |""".trimMargin() + _existingFocusSessions)
        }
        val _columnsReviews: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsReviews.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("cadence", TableInfo.Column("cadence", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReviews.put("questionsJson", TableInfo.Column("questionsJson", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysReviews: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesReviews: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoReviews: TableInfo = TableInfo("reviews", _columnsReviews, _foreignKeysReviews,
            _indicesReviews)
        val _existingReviews: TableInfo = read(connection, "reviews")
        if (!_infoReviews.equals(_existingReviews)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |reviews(com.creativeblisslabs.wearproductivity.shared.database.ReviewEntity).
              | Expected:
              |""".trimMargin() + _infoReviews + """
              |
              | Found:
              |""".trimMargin() + _existingReviews)
        }
        val _columnsFocusHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFocusHistory.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("subject", TableInfo.Column("subject", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("minutesFocused", TableInfo.Column("minutesFocused", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("completed", TableInfo.Column("completed", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFocusHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFocusHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFocusHistory: TableInfo = TableInfo("focus_history", _columnsFocusHistory,
            _foreignKeysFocusHistory, _indicesFocusHistory)
        val _existingFocusHistory: TableInfo = read(connection, "focus_history")
        if (!_infoFocusHistory.equals(_existingFocusHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |focus_history(com.creativeblisslabs.wearproductivity.shared.database.FocusHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoFocusHistory + """
              |
              | Found:
              |""".trimMargin() + _existingFocusHistory)
        }
        val _columnsImportantDates: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsImportantDates.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("sourceDeviceId", TableInfo.Column("sourceDeviceId", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("deletedAt", TableInfo.Column("deletedAt", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("version", TableInfo.Column("version", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("personName", TableInfo.Column("personName", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("dateIso", TableInfo.Column("dateIso", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("reminderDaysBefore", TableInfo.Column("reminderDaysBefore",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsImportantDates.put("isAnnual", TableInfo.Column("isAnnual", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysImportantDates: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesImportantDates: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoImportantDates: TableInfo = TableInfo("important_dates", _columnsImportantDates,
            _foreignKeysImportantDates, _indicesImportantDates)
        val _existingImportantDates: TableInfo = read(connection, "important_dates")
        if (!_infoImportantDates.equals(_existingImportantDates)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |important_dates(com.creativeblisslabs.wearproductivity.shared.database.ImportantDateEntity).
              | Expected:
              |""".trimMargin() + _infoImportantDates + """
              |
              | Found:
              |""".trimMargin() + _existingImportantDates)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tasks", "notes", "habits",
        "expenses", "food_logs", "alarms", "fast_sessions", "budgets", "events", "runs",
        "gym_workouts", "gym_exercises", "wellness", "focus_sessions", "reviews", "focus_history",
        "important_dates")
  }

  public override fun clearAllTables() {
    super.performClear(false, "tasks", "notes", "habits", "expenses", "food_logs", "alarms",
        "fast_sessions", "budgets", "events", "runs", "gym_workouts", "gym_exercises", "wellness",
        "focus_sessions", "reviews", "focus_history", "important_dates")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TaskDao::class, TaskDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(NoteDao::class, NoteDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(HabitDao::class, HabitDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FoodLogDao::class, FoodLogDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FastDao::class, FastDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AlarmDao::class, AlarmDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ExpenseDao::class, ExpenseDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BudgetDao::class, BudgetDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(EventDao::class, EventDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RunDao::class, RunDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(GymDao::class, GymDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(WellnessDao::class, WellnessDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FocusDao::class, FocusDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FocusHistoryDao::class, FocusHistoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ReviewDao::class, ReviewDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ImportantDateDao::class, ImportantDateDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun taskDao(): TaskDao = _taskDao.value

  public override fun noteDao(): NoteDao = _noteDao.value

  public override fun habitDao(): HabitDao = _habitDao.value

  public override fun foodLogDao(): FoodLogDao = _foodLogDao.value

  public override fun fastDao(): FastDao = _fastDao.value

  public override fun alarmDao(): AlarmDao = _alarmDao.value

  public override fun expenseDao(): ExpenseDao = _expenseDao.value

  public override fun budgetDao(): BudgetDao = _budgetDao.value

  public override fun eventDao(): EventDao = _eventDao.value

  public override fun runDao(): RunDao = _runDao.value

  public override fun gymDao(): GymDao = _gymDao.value

  public override fun wellnessDao(): WellnessDao = _wellnessDao.value

  public override fun focusDao(): FocusDao = _focusDao.value

  public override fun focusHistoryDao(): FocusHistoryDao = _focusHistoryDao.value

  public override fun reviewDao(): ReviewDao = _reviewDao.value

  public override fun importantDateDao(): ImportantDateDao = _importantDateDao.value
}
