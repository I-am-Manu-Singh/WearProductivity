package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class WellnessDao_Impl(
  __db: RoomDatabase,
) : WellnessDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfWellnessEntity: EntityInsertAdapter<WellnessEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfWellnessEntity = object : EntityInsertAdapter<WellnessEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `wellness` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`waterGlasses`,`mood`,`energy`,`bodyWeight`,`steps`,`sleepMinutes`,`focusMinutesGoal`,`currentHeartRateBpm`,`heartRateAccuracy`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WellnessEntity) {
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
        statement.bindLong(6, entity.waterGlasses.toLong())
        statement.bindText(7, entity.mood)
        statement.bindLong(8, entity.energy.toLong())
        val _tmpBodyWeight: Double? = entity.bodyWeight
        if (_tmpBodyWeight == null) {
          statement.bindNull(9)
        } else {
          statement.bindDouble(9, _tmpBodyWeight)
        }
        statement.bindLong(10, entity.steps.toLong())
        statement.bindLong(11, entity.sleepMinutes.toLong())
        statement.bindLong(12, entity.focusMinutesGoal.toLong())
        statement.bindDouble(13, entity.currentHeartRateBpm)
        statement.bindText(14, entity.heartRateAccuracy)
      }
    }
  }

  public override suspend fun updateWellness(wellness: WellnessEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfWellnessEntity.insert(_connection, wellness)
  }

  public override fun getWellness(): Flow<WellnessEntity?> {
    val _sql: String = "SELECT * FROM wellness LIMIT 1"
    return createFlow(__db, false, arrayOf("wellness")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfWaterGlasses: Int = getColumnIndexOrThrow(_stmt, "waterGlasses")
        val _columnIndexOfMood: Int = getColumnIndexOrThrow(_stmt, "mood")
        val _columnIndexOfEnergy: Int = getColumnIndexOrThrow(_stmt, "energy")
        val _columnIndexOfBodyWeight: Int = getColumnIndexOrThrow(_stmt, "bodyWeight")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfSleepMinutes: Int = getColumnIndexOrThrow(_stmt, "sleepMinutes")
        val _columnIndexOfFocusMinutesGoal: Int = getColumnIndexOrThrow(_stmt, "focusMinutesGoal")
        val _columnIndexOfCurrentHeartRateBpm: Int = getColumnIndexOrThrow(_stmt,
            "currentHeartRateBpm")
        val _columnIndexOfHeartRateAccuracy: Int = getColumnIndexOrThrow(_stmt, "heartRateAccuracy")
        val _result: WellnessEntity?
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
          val _tmpWaterGlasses: Int
          _tmpWaterGlasses = _stmt.getLong(_columnIndexOfWaterGlasses).toInt()
          val _tmpMood: String
          _tmpMood = _stmt.getText(_columnIndexOfMood)
          val _tmpEnergy: Int
          _tmpEnergy = _stmt.getLong(_columnIndexOfEnergy).toInt()
          val _tmpBodyWeight: Double?
          if (_stmt.isNull(_columnIndexOfBodyWeight)) {
            _tmpBodyWeight = null
          } else {
            _tmpBodyWeight = _stmt.getDouble(_columnIndexOfBodyWeight)
          }
          val _tmpSteps: Int
          _tmpSteps = _stmt.getLong(_columnIndexOfSteps).toInt()
          val _tmpSleepMinutes: Int
          _tmpSleepMinutes = _stmt.getLong(_columnIndexOfSleepMinutes).toInt()
          val _tmpFocusMinutesGoal: Int
          _tmpFocusMinutesGoal = _stmt.getLong(_columnIndexOfFocusMinutesGoal).toInt()
          val _tmpCurrentHeartRateBpm: Double
          _tmpCurrentHeartRateBpm = _stmt.getDouble(_columnIndexOfCurrentHeartRateBpm)
          val _tmpHeartRateAccuracy: String
          _tmpHeartRateAccuracy = _stmt.getText(_columnIndexOfHeartRateAccuracy)
          _result =
              WellnessEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpWaterGlasses,_tmpMood,_tmpEnergy,_tmpBodyWeight,_tmpSteps,_tmpSleepMinutes,_tmpFocusMinutesGoal,_tmpCurrentHeartRateBpm,_tmpHeartRateAccuracy)
        } else {
          _result = null
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
