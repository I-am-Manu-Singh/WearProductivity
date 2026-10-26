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
public class FoodLogDao_Impl(
  __db: RoomDatabase,
) : FoodLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFoodLogEntity: EntityInsertAdapter<FoodLogEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFoodLogEntity = object : EntityInsertAdapter<FoodLogEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `food_logs` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`name`,`mealType`,`calories`,`proteinGrams`,`carbsGrams`,`fatGrams`,`loggedAtLabel`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FoodLogEntity) {
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
        statement.bindText(6, entity.name)
        statement.bindText(7, entity.mealType)
        statement.bindLong(8, entity.calories.toLong())
        statement.bindDouble(9, entity.proteinGrams)
        statement.bindDouble(10, entity.carbsGrams)
        statement.bindDouble(11, entity.fatGrams)
        statement.bindText(12, entity.loggedAtLabel)
        statement.bindText(13, entity.notes)
      }
    }
  }

  public override suspend fun insertFoodLog(foodLog: FoodLogEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfFoodLogEntity.insert(_connection, foodLog)
  }

  public override fun getAllFoodLogs(): Flow<List<FoodLogEntity>> {
    val _sql: String = "SELECT * FROM food_logs WHERE deletedAt IS NULL ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("food_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMealType: Int = getColumnIndexOrThrow(_stmt, "mealType")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfProteinGrams: Int = getColumnIndexOrThrow(_stmt, "proteinGrams")
        val _columnIndexOfCarbsGrams: Int = getColumnIndexOrThrow(_stmt, "carbsGrams")
        val _columnIndexOfFatGrams: Int = getColumnIndexOrThrow(_stmt, "fatGrams")
        val _columnIndexOfLoggedAtLabel: Int = getColumnIndexOrThrow(_stmt, "loggedAtLabel")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _result: MutableList<FoodLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FoodLogEntity
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
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpMealType: String
          _tmpMealType = _stmt.getText(_columnIndexOfMealType)
          val _tmpCalories: Int
          _tmpCalories = _stmt.getLong(_columnIndexOfCalories).toInt()
          val _tmpProteinGrams: Double
          _tmpProteinGrams = _stmt.getDouble(_columnIndexOfProteinGrams)
          val _tmpCarbsGrams: Double
          _tmpCarbsGrams = _stmt.getDouble(_columnIndexOfCarbsGrams)
          val _tmpFatGrams: Double
          _tmpFatGrams = _stmt.getDouble(_columnIndexOfFatGrams)
          val _tmpLoggedAtLabel: String
          _tmpLoggedAtLabel = _stmt.getText(_columnIndexOfLoggedAtLabel)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          _item =
              FoodLogEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpName,_tmpMealType,_tmpCalories,_tmpProteinGrams,_tmpCarbsGrams,_tmpFatGrams,_tmpLoggedAtLabel,_tmpNotes)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteFoodLog(id: String, deletedAt: Long) {
    val _sql: String = "UPDATE food_logs SET deletedAt = ? WHERE id = ?"
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
