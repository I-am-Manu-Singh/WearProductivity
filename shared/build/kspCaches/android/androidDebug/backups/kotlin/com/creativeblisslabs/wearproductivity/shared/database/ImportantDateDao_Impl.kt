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
public class ImportantDateDao_Impl(
  __db: RoomDatabase,
) : ImportantDateDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfImportantDateEntity: EntityInsertAdapter<ImportantDateEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfImportantDateEntity = object : EntityInsertAdapter<ImportantDateEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `important_dates` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`personName`,`title`,`dateIso`,`category`,`reminderDaysBefore`,`notes`,`isAnnual`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ImportantDateEntity) {
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
        statement.bindText(6, entity.personName)
        statement.bindText(7, entity.title)
        statement.bindText(8, entity.dateIso)
        statement.bindText(9, entity.category)
        statement.bindLong(10, entity.reminderDaysBefore.toLong())
        statement.bindText(11, entity.notes)
        val _tmp: Int = if (entity.isAnnual) 1 else 0
        statement.bindLong(12, _tmp.toLong())
      }
    }
  }

  public override suspend fun insertImportantDate(date: ImportantDateEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfImportantDateEntity.insert(_connection, date)
  }

  public override fun getAllImportantDates(): Flow<List<ImportantDateEntity>> {
    val _sql: String = "SELECT * FROM important_dates WHERE deletedAt IS NULL ORDER BY dateIso ASC"
    return createFlow(__db, false, arrayOf("important_dates")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfPersonName: Int = getColumnIndexOrThrow(_stmt, "personName")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDateIso: Int = getColumnIndexOrThrow(_stmt, "dateIso")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfReminderDaysBefore: Int = getColumnIndexOrThrow(_stmt,
            "reminderDaysBefore")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfIsAnnual: Int = getColumnIndexOrThrow(_stmt, "isAnnual")
        val _result: MutableList<ImportantDateEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ImportantDateEntity
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
          val _tmpPersonName: String
          _tmpPersonName = _stmt.getText(_columnIndexOfPersonName)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDateIso: String
          _tmpDateIso = _stmt.getText(_columnIndexOfDateIso)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpReminderDaysBefore: Int
          _tmpReminderDaysBefore = _stmt.getLong(_columnIndexOfReminderDaysBefore).toInt()
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpIsAnnual: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAnnual).toInt()
          _tmpIsAnnual = _tmp != 0
          _item =
              ImportantDateEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpPersonName,_tmpTitle,_tmpDateIso,_tmpCategory,_tmpReminderDaysBefore,_tmpNotes,_tmpIsAnnual)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteImportantDate(id: String, deletedAt: Long) {
    val _sql: String = "UPDATE important_dates SET deletedAt = ? WHERE id = ?"
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
