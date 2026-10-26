package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
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
public class EventDao_Impl(
  __db: RoomDatabase,
) : EventDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfEventEntity: EntityInsertAdapter<EventEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfEventEntity = object : EntityInsertAdapter<EventEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `events` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`title`,`timeRange`,`location`,`reminder`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EventEntity) {
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
        statement.bindText(6, entity.title)
        statement.bindText(7, entity.timeRange)
        statement.bindText(8, entity.location)
        statement.bindText(9, entity.reminder)
      }
    }
  }

  public override suspend fun insertEvent(event: EventEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfEventEntity.insert(_connection, event)
  }

  public override fun getAllEvents(): Flow<List<EventEntity>> {
    val _sql: String = "SELECT * FROM events WHERE deletedAt IS NULL ORDER BY timeRange ASC"
    return createFlow(__db, false, arrayOf("events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfTimeRange: Int = getColumnIndexOrThrow(_stmt, "timeRange")
        val _columnIndexOfLocation: Int = getColumnIndexOrThrow(_stmt, "location")
        val _columnIndexOfReminder: Int = getColumnIndexOrThrow(_stmt, "reminder")
        val _result: MutableList<EventEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EventEntity
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
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpTimeRange: String
          _tmpTimeRange = _stmt.getText(_columnIndexOfTimeRange)
          val _tmpLocation: String
          _tmpLocation = _stmt.getText(_columnIndexOfLocation)
          val _tmpReminder: String
          _tmpReminder = _stmt.getText(_columnIndexOfReminder)
          _item =
              EventEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpTitle,_tmpTimeRange,_tmpLocation,_tmpReminder)
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
