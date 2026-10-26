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
public class TaskDao_Impl(
  __db: RoomDatabase,
) : TaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTaskEntity: EntityInsertAdapter<TaskEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTaskEntity = object : EntityInsertAdapter<TaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `tasks` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`title`,`project`,`dueLabel`,`priority`,`complete`,`tagsJson`,`scheduledTime`,`hapticEnabled`,`syncStatus`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
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
        statement.bindText(7, entity.project)
        statement.bindText(8, entity.dueLabel)
        statement.bindLong(9, entity.priority.toLong())
        val _tmp: Int = if (entity.complete) 1 else 0
        statement.bindLong(10, _tmp.toLong())
        statement.bindText(11, entity.tagsJson)
        val _tmpScheduledTime: String? = entity.scheduledTime
        if (_tmpScheduledTime == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpScheduledTime)
        }
        val _tmp_1: Int = if (entity.hapticEnabled) 1 else 0
        statement.bindLong(13, _tmp_1.toLong())
        statement.bindText(14, entity.syncStatus)
      }
    }
  }

  public override suspend fun insertTask(task: TaskEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfTaskEntity.insert(_connection, task)
  }

  public override fun getAllTasks(): Flow<List<TaskEntity>> {
    val _sql: String =
        "SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY priority ASC, updatedAt DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfProject: Int = getColumnIndexOrThrow(_stmt, "project")
        val _columnIndexOfDueLabel: Int = getColumnIndexOrThrow(_stmt, "dueLabel")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfComplete: Int = getColumnIndexOrThrow(_stmt, "complete")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _columnIndexOfScheduledTime: Int = getColumnIndexOrThrow(_stmt, "scheduledTime")
        val _columnIndexOfHapticEnabled: Int = getColumnIndexOrThrow(_stmt, "hapticEnabled")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
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
          val _tmpProject: String
          _tmpProject = _stmt.getText(_columnIndexOfProject)
          val _tmpDueLabel: String
          _tmpDueLabel = _stmt.getText(_columnIndexOfDueLabel)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpComplete: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfComplete).toInt()
          _tmpComplete = _tmp != 0
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          val _tmpScheduledTime: String?
          if (_stmt.isNull(_columnIndexOfScheduledTime)) {
            _tmpScheduledTime = null
          } else {
            _tmpScheduledTime = _stmt.getText(_columnIndexOfScheduledTime)
          }
          val _tmpHapticEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfHapticEnabled).toInt()
          _tmpHapticEnabled = _tmp_1 != 0
          val _tmpSyncStatus: String
          _tmpSyncStatus = _stmt.getText(_columnIndexOfSyncStatus)
          _item =
              TaskEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpTitle,_tmpProject,_tmpDueLabel,_tmpPriority,_tmpComplete,_tmpTagsJson,_tmpScheduledTime,_tmpHapticEnabled,_tmpSyncStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllTasksIncludingDeleted(): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfProject: Int = getColumnIndexOrThrow(_stmt, "project")
        val _columnIndexOfDueLabel: Int = getColumnIndexOrThrow(_stmt, "dueLabel")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfComplete: Int = getColumnIndexOrThrow(_stmt, "complete")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _columnIndexOfScheduledTime: Int = getColumnIndexOrThrow(_stmt, "scheduledTime")
        val _columnIndexOfHapticEnabled: Int = getColumnIndexOrThrow(_stmt, "hapticEnabled")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
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
          val _tmpProject: String
          _tmpProject = _stmt.getText(_columnIndexOfProject)
          val _tmpDueLabel: String
          _tmpDueLabel = _stmt.getText(_columnIndexOfDueLabel)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpComplete: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfComplete).toInt()
          _tmpComplete = _tmp != 0
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          val _tmpScheduledTime: String?
          if (_stmt.isNull(_columnIndexOfScheduledTime)) {
            _tmpScheduledTime = null
          } else {
            _tmpScheduledTime = _stmt.getText(_columnIndexOfScheduledTime)
          }
          val _tmpHapticEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfHapticEnabled).toInt()
          _tmpHapticEnabled = _tmp_1 != 0
          val _tmpSyncStatus: String
          _tmpSyncStatus = _stmt.getText(_columnIndexOfSyncStatus)
          _item =
              TaskEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpTitle,_tmpProject,_tmpDueLabel,_tmpPriority,_tmpComplete,_tmpTagsJson,_tmpScheduledTime,_tmpHapticEnabled,_tmpSyncStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateCompletion(
    id: String,
    complete: Boolean,
    updatedAt: Long,
  ) {
    val _sql: String =
        "UPDATE tasks SET complete = ?, updatedAt = ?, version = version + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (complete) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: String, deletedAt: Long) {
    val _sql: String = "UPDATE tasks SET deletedAt = ? WHERE id = ?"
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
