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
public class NoteDao_Impl(
  __db: RoomDatabase,
) : NoteDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNoteEntity: EntityInsertAdapter<NoteEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNoteEntity = object : EntityInsertAdapter<NoteEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `notes` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`title`,`body`,`pinned`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NoteEntity) {
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
        statement.bindText(7, entity.body)
        val _tmp: Int = if (entity.pinned) 1 else 0
        statement.bindLong(8, _tmp.toLong())
      }
    }
  }

  public override suspend fun insertNote(note: NoteEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfNoteEntity.insert(_connection, note)
  }

  public override fun getAllNotes(): Flow<List<NoteEntity>> {
    val _sql: String =
        "SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC"
    return createFlow(__db, false, arrayOf("notes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfBody: Int = getColumnIndexOrThrow(_stmt, "body")
        val _columnIndexOfPinned: Int = getColumnIndexOrThrow(_stmt, "pinned")
        val _result: MutableList<NoteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: NoteEntity
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
          val _tmpBody: String
          _tmpBody = _stmt.getText(_columnIndexOfBody)
          val _tmpPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfPinned).toInt()
          _tmpPinned = _tmp != 0
          _item =
              NoteEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpTitle,_tmpBody,_tmpPinned)
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
