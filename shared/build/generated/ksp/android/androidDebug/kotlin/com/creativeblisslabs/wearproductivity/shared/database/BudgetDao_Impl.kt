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
public class BudgetDao_Impl(
  __db: RoomDatabase,
) : BudgetDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBudgetEntity: EntityInsertAdapter<BudgetEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBudgetEntity = object : EntityInsertAdapter<BudgetEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `budgets` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`category`,`spent`,`limit`,`currency`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BudgetEntity) {
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
        statement.bindText(6, entity.category)
        statement.bindDouble(7, entity.spent)
        statement.bindDouble(8, entity.limit)
        statement.bindText(9, entity.currency)
      }
    }
  }

  public override suspend fun insertBudget(budget: BudgetEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfBudgetEntity.insert(_connection, budget)
  }

  public override fun getAllBudgets(): Flow<List<BudgetEntity>> {
    val _sql: String = "SELECT * FROM budgets WHERE deletedAt IS NULL"
    return createFlow(__db, false, arrayOf("budgets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfSpent: Int = getColumnIndexOrThrow(_stmt, "spent")
        val _columnIndexOfLimit: Int = getColumnIndexOrThrow(_stmt, "limit")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _result: MutableList<BudgetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BudgetEntity
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
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpSpent: Double
          _tmpSpent = _stmt.getDouble(_columnIndexOfSpent)
          val _tmpLimit: Double
          _tmpLimit = _stmt.getDouble(_columnIndexOfLimit)
          val _tmpCurrency: String
          _tmpCurrency = _stmt.getText(_columnIndexOfCurrency)
          _item =
              BudgetEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpCategory,_tmpSpent,_tmpLimit,_tmpCurrency)
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
