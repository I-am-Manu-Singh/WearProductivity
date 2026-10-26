package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class GymDao_Impl(
  __db: RoomDatabase,
) : GymDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfGymWorkoutEntity: EntityInsertAdapter<GymWorkoutEntity>

  private val __insertAdapterOfGymExerciseEntity: EntityInsertAdapter<GymExerciseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfGymWorkoutEntity = object : EntityInsertAdapter<GymWorkoutEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `gym_workouts` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`name`,`restSeconds`,`active`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GymWorkoutEntity) {
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
        statement.bindLong(7, entity.restSeconds.toLong())
        val _tmp: Int = if (entity.active) 1 else 0
        statement.bindLong(8, _tmp.toLong())
      }
    }
    this.__insertAdapterOfGymExerciseEntity = object : EntityInsertAdapter<GymExerciseEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `gym_exercises` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`workoutId`,`name`,`muscleGroup`,`equipment`,`lastPerformance`,`personalRecord`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GymExerciseEntity) {
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
        statement.bindText(6, entity.workoutId)
        statement.bindText(7, entity.name)
        statement.bindText(8, entity.muscleGroup)
        statement.bindText(9, entity.equipment)
        statement.bindText(10, entity.lastPerformance)
        statement.bindText(11, entity.personalRecord)
      }
    }
  }

  public override suspend fun insertWorkout(workout: GymWorkoutEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGymWorkoutEntity.insert(_connection, workout)
  }

  public override suspend fun insertExercise(exercise: GymExerciseEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGymExerciseEntity.insert(_connection, exercise)
  }

  public override fun getActiveWorkout(): Flow<GymWorkoutEntity?> {
    val _sql: String = "SELECT * FROM gym_workouts LIMIT 1"
    return createFlow(__db, false, arrayOf("gym_workouts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfRestSeconds: Int = getColumnIndexOrThrow(_stmt, "restSeconds")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _result: GymWorkoutEntity?
        if (_stmt.step()) {
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
          val _tmpRestSeconds: Int
          _tmpRestSeconds = _stmt.getLong(_columnIndexOfRestSeconds).toInt()
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          _result =
              GymWorkoutEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpName,_tmpRestSeconds,_tmpActive)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getExercisesForWorkout(workoutId: String): Flow<List<GymExerciseEntity>> {
    val _sql: String = "SELECT * FROM gym_exercises WHERE workoutId = ? AND deletedAt IS NULL"
    return createFlow(__db, false, arrayOf("gym_exercises")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, workoutId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfWorkoutId: Int = getColumnIndexOrThrow(_stmt, "workoutId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMuscleGroup: Int = getColumnIndexOrThrow(_stmt, "muscleGroup")
        val _columnIndexOfEquipment: Int = getColumnIndexOrThrow(_stmt, "equipment")
        val _columnIndexOfLastPerformance: Int = getColumnIndexOrThrow(_stmt, "lastPerformance")
        val _columnIndexOfPersonalRecord: Int = getColumnIndexOrThrow(_stmt, "personalRecord")
        val _result: MutableList<GymExerciseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GymExerciseEntity
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
          val _tmpWorkoutId: String
          _tmpWorkoutId = _stmt.getText(_columnIndexOfWorkoutId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpMuscleGroup: String
          _tmpMuscleGroup = _stmt.getText(_columnIndexOfMuscleGroup)
          val _tmpEquipment: String
          _tmpEquipment = _stmt.getText(_columnIndexOfEquipment)
          val _tmpLastPerformance: String
          _tmpLastPerformance = _stmt.getText(_columnIndexOfLastPerformance)
          val _tmpPersonalRecord: String
          _tmpPersonalRecord = _stmt.getText(_columnIndexOfPersonalRecord)
          _item =
              GymExerciseEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpWorkoutId,_tmpName,_tmpMuscleGroup,_tmpEquipment,_tmpLastPerformance,_tmpPersonalRecord)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
