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
public class FocusHistoryDao_Impl(
  __db: RoomDatabase,
) : FocusHistoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFocusHistoryEntity: EntityInsertAdapter<FocusHistoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFocusHistoryEntity = object : EntityInsertAdapter<FocusHistoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `focus_history` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`subject`,`minutesFocused`,`completed`,`timestamp`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FocusHistoryEntity) {
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
        statement.bindText(6, entity.subject)
        statement.bindLong(7, entity.minutesFocused.toLong())
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindLong(9, entity.timestamp)
      }
    }
  }

  public override suspend fun insertFocusHistory(history: FocusHistoryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFocusHistoryEntity.insert(_connection, history)
  }

  public override fun getAllFocusHistory(): Flow<List<FocusHistoryEntity>> {
    val _sql: String = "SELECT * FROM focus_history WHERE deletedAt IS NULL ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("focus_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfMinutesFocused: Int = getColumnIndexOrThrow(_stmt, "minutesFocused")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<FocusHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FocusHistoryEntity
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
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpMinutesFocused: Int
          _tmpMinutesFocused = _stmt.getLong(_columnIndexOfMinutesFocused).toInt()
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              FocusHistoryEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpSubject,_tmpMinutesFocused,_tmpCompleted,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteFocusHistory(id: String, deletedAt: Long) {
    val _sql: String = "UPDATE focus_history SET deletedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, deletedAt)
        _argIndex = 2
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
