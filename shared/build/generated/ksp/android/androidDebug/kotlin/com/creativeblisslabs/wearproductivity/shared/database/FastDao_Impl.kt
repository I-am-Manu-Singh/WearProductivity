package com.creativeblisslabs.wearproductivity.shared.database

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Float
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
public class FastDao_Impl(
  __db: RoomDatabase,
) : FastDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFastSessionEntity: EntityInsertAdapter<FastSessionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFastSessionEntity = object : EntityInsertAdapter<FastSessionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `fast_sessions` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`plan`,`startedAtLabel`,`endsAtLabel`,`progress`,`active`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FastSessionEntity) {
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
        statement.bindText(6, entity.plan)
        statement.bindText(7, entity.startedAtLabel)
        statement.bindText(8, entity.endsAtLabel)
        statement.bindDouble(9, entity.progress.toDouble())
        val _tmp: Int = if (entity.active) 1 else 0
        statement.bindLong(10, _tmp.toLong())
      }
    }
  }

  public override suspend fun updateFast(fast: FastSessionEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfFastSessionEntity.insert(_connection, fast)
  }

  public override fun getActiveFast(): Flow<FastSessionEntity?> {
    val _sql: String = "SELECT * FROM fast_sessions LIMIT 1"
    return createFlow(__db, false, arrayOf("fast_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfPlan: Int = getColumnIndexOrThrow(_stmt, "plan")
        val _columnIndexOfStartedAtLabel: Int = getColumnIndexOrThrow(_stmt, "startedAtLabel")
        val _columnIndexOfEndsAtLabel: Int = getColumnIndexOrThrow(_stmt, "endsAtLabel")
        val _columnIndexOfProgress: Int = getColumnIndexOrThrow(_stmt, "progress")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _result: FastSessionEntity?
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
          val _tmpPlan: String
          _tmpPlan = _stmt.getText(_columnIndexOfPlan)
          val _tmpStartedAtLabel: String
          _tmpStartedAtLabel = _stmt.getText(_columnIndexOfStartedAtLabel)
          val _tmpEndsAtLabel: String
          _tmpEndsAtLabel = _stmt.getText(_columnIndexOfEndsAtLabel)
          val _tmpProgress: Float
          _tmpProgress = _stmt.getDouble(_columnIndexOfProgress).toFloat()
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          _result =
              FastSessionEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpPlan,_tmpStartedAtLabel,_tmpEndsAtLabel,_tmpProgress,_tmpActive)
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
