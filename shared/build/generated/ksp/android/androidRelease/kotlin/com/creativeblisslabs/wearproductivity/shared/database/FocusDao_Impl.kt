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
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class FocusDao_Impl(
  __db: RoomDatabase,
) : FocusDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFocusSessionEntity: EntityInsertAdapter<FocusSessionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFocusSessionEntity = object : EntityInsertAdapter<FocusSessionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `focus_sessions` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`label`,`minutes`,`remainingMinutes`,`active`,`type`,`subject`,`totalSeconds`,`remainingSeconds`,`isPaused`,`startedAtTimestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FocusSessionEntity) {
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
        statement.bindText(6, entity.label)
        statement.bindLong(7, entity.minutes.toLong())
        statement.bindLong(8, entity.remainingMinutes.toLong())
        val _tmp: Int = if (entity.active) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindText(10, entity.type)
        statement.bindText(11, entity.subject)
        statement.bindLong(12, entity.totalSeconds.toLong())
        statement.bindLong(13, entity.remainingSeconds.toLong())
        val _tmp_1: Int = if (entity.isPaused) 1 else 0
        statement.bindLong(14, _tmp_1.toLong())
        statement.bindLong(15, entity.startedAtTimestamp)
      }
    }
  }

  public override suspend fun updateFocus(focus: FocusSessionEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfFocusSessionEntity.insert(_connection, focus)
  }

  public override fun getFocus(): Flow<FocusSessionEntity?> {
    val _sql: String = "SELECT * FROM focus_sessions LIMIT 1"
    return createFlow(__db, false, arrayOf("focus_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMinutes: Int = getColumnIndexOrThrow(_stmt, "minutes")
        val _columnIndexOfRemainingMinutes: Int = getColumnIndexOrThrow(_stmt, "remainingMinutes")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfTotalSeconds: Int = getColumnIndexOrThrow(_stmt, "totalSeconds")
        val _columnIndexOfRemainingSeconds: Int = getColumnIndexOrThrow(_stmt, "remainingSeconds")
        val _columnIndexOfIsPaused: Int = getColumnIndexOrThrow(_stmt, "isPaused")
        val _columnIndexOfStartedAtTimestamp: Int = getColumnIndexOrThrow(_stmt,
            "startedAtTimestamp")
        val _result: FocusSessionEntity?
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
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpMinutes: Int
          _tmpMinutes = _stmt.getLong(_columnIndexOfMinutes).toInt()
          val _tmpRemainingMinutes: Int
          _tmpRemainingMinutes = _stmt.getLong(_columnIndexOfRemainingMinutes).toInt()
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpTotalSeconds: Int
          _tmpTotalSeconds = _stmt.getLong(_columnIndexOfTotalSeconds).toInt()
          val _tmpRemainingSeconds: Int
          _tmpRemainingSeconds = _stmt.getLong(_columnIndexOfRemainingSeconds).toInt()
          val _tmpIsPaused: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPaused).toInt()
          _tmpIsPaused = _tmp_1 != 0
          val _tmpStartedAtTimestamp: Long
          _tmpStartedAtTimestamp = _stmt.getLong(_columnIndexOfStartedAtTimestamp)
          _result =
              FocusSessionEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpLabel,_tmpMinutes,_tmpRemainingMinutes,_tmpActive,_tmpType,_tmpSubject,_tmpTotalSeconds,_tmpRemainingSeconds,_tmpIsPaused,_tmpStartedAtTimestamp)
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
