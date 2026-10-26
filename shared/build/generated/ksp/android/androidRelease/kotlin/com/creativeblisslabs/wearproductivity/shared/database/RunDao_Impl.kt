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
public class RunDao_Impl(
  __db: RoomDatabase,
) : RunDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRunWorkoutEntity: EntityInsertAdapter<RunWorkoutEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRunWorkoutEntity = object : EntityInsertAdapter<RunWorkoutEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `runs` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`week`,`day`,`title`,`intervalsJson`,`completed`,`distanceKm`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RunWorkoutEntity) {
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
        statement.bindLong(6, entity.week.toLong())
        statement.bindLong(7, entity.day.toLong())
        statement.bindText(8, entity.title)
        statement.bindText(9, entity.intervalsJson)
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        val _tmpDistanceKm: Double? = entity.distanceKm
        if (_tmpDistanceKm == null) {
          statement.bindNull(11)
        } else {
          statement.bindDouble(11, _tmpDistanceKm)
        }
      }
    }
  }

  public override suspend fun insertRun(run: RunWorkoutEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfRunWorkoutEntity.insert(_connection, run)
  }

  public override fun getAllRuns(): Flow<List<RunWorkoutEntity>> {
    val _sql: String = "SELECT * FROM runs WHERE deletedAt IS NULL ORDER BY week ASC, day ASC"
    return createFlow(__db, false, arrayOf("runs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfWeek: Int = getColumnIndexOrThrow(_stmt, "week")
        val _columnIndexOfDay: Int = getColumnIndexOrThrow(_stmt, "day")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfIntervalsJson: Int = getColumnIndexOrThrow(_stmt, "intervalsJson")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfDistanceKm: Int = getColumnIndexOrThrow(_stmt, "distanceKm")
        val _result: MutableList<RunWorkoutEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RunWorkoutEntity
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
          val _tmpWeek: Int
          _tmpWeek = _stmt.getLong(_columnIndexOfWeek).toInt()
          val _tmpDay: Int
          _tmpDay = _stmt.getLong(_columnIndexOfDay).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpIntervalsJson: String
          _tmpIntervalsJson = _stmt.getText(_columnIndexOfIntervalsJson)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpDistanceKm: Double?
          if (_stmt.isNull(_columnIndexOfDistanceKm)) {
            _tmpDistanceKm = null
          } else {
            _tmpDistanceKm = _stmt.getDouble(_columnIndexOfDistanceKm)
          }
          _item =
              RunWorkoutEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpWeek,_tmpDay,_tmpTitle,_tmpIntervalsJson,_tmpCompleted,_tmpDistanceKm)
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
