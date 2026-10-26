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
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ExpenseDao_Impl(
  __db: RoomDatabase,
) : ExpenseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfExpenseEntity: EntityInsertAdapter<ExpenseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfExpenseEntity = object : EntityInsertAdapter<ExpenseEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `expenses` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`amount`,`currency`,`category`,`note`,`dateLabel`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ExpenseEntity) {
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
        statement.bindDouble(6, entity.amount)
        statement.bindText(7, entity.currency)
        statement.bindText(8, entity.category)
        statement.bindText(9, entity.note)
        statement.bindText(10, entity.dateLabel)
      }
    }
  }

  public override suspend fun insertExpense(expense: ExpenseEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfExpenseEntity.insert(_connection, expense)
  }

  public override fun getAllExpenses(): Flow<List<ExpenseEntity>> {
    val _sql: String = "SELECT * FROM expenses WHERE deletedAt IS NULL ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfDateLabel: Int = getColumnIndexOrThrow(_stmt, "dateLabel")
        val _result: MutableList<ExpenseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseEntity
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
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpCurrency: String
          _tmpCurrency = _stmt.getText(_columnIndexOfCurrency)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpDateLabel: String
          _tmpDateLabel = _stmt.getText(_columnIndexOfDateLabel)
          _item =
              ExpenseEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpAmount,_tmpCurrency,_tmpCategory,_tmpNote,_tmpDateLabel)
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
