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
public class AlarmDao_Impl(
  __db: RoomDatabase,
) : AlarmDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAlarmEntity: EntityInsertAdapter<AlarmEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAlarmEntity = object : EntityInsertAdapter<AlarmEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `alarms` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`time`,`label`,`repeat`,`enabled`,`haptic`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AlarmEntity) {
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
        statement.bindText(6, entity.time)
        statement.bindText(7, entity.label)
        statement.bindText(8, entity.repeat)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        val _tmp_1: Int = if (entity.haptic) 1 else 0
        statement.bindLong(10, _tmp_1.toLong())
      }
    }
  }

  public override suspend fun insertAlarm(alarm: AlarmEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfAlarmEntity.insert(_connection, alarm)
  }

  public override fun getAllAlarms(): Flow<List<AlarmEntity>> {
    val _sql: String = "SELECT * FROM alarms WHERE deletedAt IS NULL ORDER BY time ASC"
    return createFlow(__db, false, arrayOf("alarms")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfRepeat: Int = getColumnIndexOrThrow(_stmt, "repeat")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfHaptic: Int = getColumnIndexOrThrow(_stmt, "haptic")
        val _result: MutableList<AlarmEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AlarmEntity
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
          val _tmpTime: String
          _tmpTime = _stmt.getText(_columnIndexOfTime)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpRepeat: String
          _tmpRepeat = _stmt.getText(_columnIndexOfRepeat)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpHaptic: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfHaptic).toInt()
          _tmpHaptic = _tmp_1 != 0
          _item =
              AlarmEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpTime,_tmpLabel,_tmpRepeat,_tmpEnabled,_tmpHaptic)
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
