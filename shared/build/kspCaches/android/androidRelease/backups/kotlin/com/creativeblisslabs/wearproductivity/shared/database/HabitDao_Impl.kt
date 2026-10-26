package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HabitDao_Impl(
  __db: RoomDatabase,
) : HabitDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfHabitEntity: EntityInsertAdapter<HabitEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfHabitEntity = object : EntityInsertAdapter<HabitEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `habits` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`name`,`cadence`,`streak`,`doneToday`,`colorName`,`targetValue`,`currentValue`,`unit`,`isTimerActive`,`remainingSeconds`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: HabitEntity) {
        statement.bindText(1, entity.id)
        statement.bindLong(2, entity.updatedAt)
        statement.bindText(3, entity.sourceDeviceId)
        val _tmpDeletedAt: Long? = entity.deletedAt
        if (_tmpDeletedAt == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpDeletedAt)
        }
        statement.bindLong(5, entity.version)
        statement.bindText(6, entity.name)
        statement.bindText(7, entity.cadence)
        statement.bindLong(8, entity.streak.toLong())
        val _tmp: Int = if (entity.doneToday) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindText(10, entity.colorName)
        statement.bindDouble(11, entity.targetValue)
        statement.bindDouble(12, entity.currentValue)
        statement.bindText(13, entity.unit)
        val _tmp_1: Int = if (entity.isTimerActive) 1 else 0
        statement.bindLong(14, _tmp_1.toLong())
        statement.bindLong(15, entity.remainingSeconds.toLong())
      }
    }
  }

  public override suspend fun insertHabit(habit: HabitEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfHabitEntity.insert(_connection, habit)
  }

  public override fun getAllHabits(): Flow<List<HabitEntity>> {
    val _sql: String = "SELECT * FROM habits WHERE deletedAt IS NULL"
    return createFlow(__db, false, arrayOf("habits")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCadence: Int = getColumnIndexOrThrow(_stmt, "cadence")
        val _columnIndexOfStreak: Int = getColumnIndexOrThrow(_stmt, "streak")
        val _columnIndexOfDoneToday: Int = getColumnIndexOrThrow(_stmt, "doneToday")
        val _columnIndexOfColorName: Int = getColumnIndexOrThrow(_stmt, "colorName")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfCurrentValue: Int = getColumnIndexOrThrow(_stmt, "currentValue")
        val _columnIndexOfUnit: Int = getColumnIndexOrThrow(_stmt, "unit")
        val _columnIndexOfIsTimerActive: Int = getColumnIndexOrThrow(_stmt, "isTimerActive")
        val _columnIndexOfRemainingSeconds: Int = getColumnIndexOrThrow(_stmt, "remainingSeconds")
        val _result: MutableList<HabitEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSourceDeviceId: String
          _tmpSourceDeviceId = _stmt.getText(_columnIndexOfSourceDeviceId)
          val _tmpDeletedAt: Long?
          if (_stmt.isNull(_columnIndexOfDeletedAt)) {
            _tmpDeletedAt = null
          } else {
            _tmpDeletedAt = _stmt.getLong(_columnIndexOfDeletedAt)
          }
          val _tmpVersion: Long
          _tmpVersion = _stmt.getLong(_columnIndexOfVersion)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCadence: String
          _tmpCadence = _stmt.getText(_columnIndexOfCadence)
          val _tmpStreak: Int
          _tmpStreak = _stmt.getLong(_columnIndexOfStreak).toInt()
          val _tmpDoneToday: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDoneToday).toInt()
          _tmpDoneToday = _tmp != 0
          val _tmpColorName: String
          _tmpColorName = _stmt.getText(_columnIndexOfColorName)
          val _tmpTargetValue: Double
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue)
          val _tmpCurrentValue: Double
          _tmpCurrentValue = _stmt.getDouble(_columnIndexOfCurrentValue)
          val _tmpUnit: String
          _tmpUnit = _stmt.getText(_columnIndexOfUnit)
          val _tmpIsTimerActive: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsTimerActive).toInt()
          _tmpIsTimerActive = _tmp_1 != 0
          val _tmpRemainingSeconds: Int
          _tmpRemainingSeconds = _stmt.getLong(_columnIndexOfRemainingSeconds).toInt()
          _item =
              HabitEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpName,_tmpCadence,_tmpStreak,_tmpDoneToday,_tmpColorName,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpIsTimerActive,_tmpRemainingSeconds)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateHabit(
    id: String,
    done: Boolean,
    streak: Int,
    updatedAt: Long,
  ) {
    val _sql: String =
        "UPDATE habits SET doneToday = ?, streak = ?, updatedAt = ?, version = version + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (done) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, streak.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 4
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
