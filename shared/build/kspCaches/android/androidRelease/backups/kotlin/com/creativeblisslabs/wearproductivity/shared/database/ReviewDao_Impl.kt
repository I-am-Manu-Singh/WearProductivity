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
public class ReviewDao_Impl(
  __db: RoomDatabase,
) : ReviewDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfReviewEntity: EntityInsertAdapter<ReviewEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfReviewEntity = object : EntityInsertAdapter<ReviewEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `reviews` (`id`,`updatedAt`,`sourceDeviceId`,`deletedAt`,`version`,`title`,`cadence`,`questionsJson`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ReviewEntity) {
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
        statement.bindText(7, entity.cadence)
        statement.bindText(8, entity.questionsJson)
      }
    }
  }

  public override suspend fun insertReview(review: ReviewEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfReviewEntity.insert(_connection, review)
  }

  public override fun getAllReviews(): Flow<List<ReviewEntity>> {
    val _sql: String = "SELECT * FROM reviews WHERE deletedAt IS NULL"
    return createFlow(__db, false, arrayOf("reviews")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSourceDeviceId: Int = getColumnIndexOrThrow(_stmt, "sourceDeviceId")
        val _columnIndexOfDeletedAt: Int = getColumnIndexOrThrow(_stmt, "deletedAt")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfCadence: Int = getColumnIndexOrThrow(_stmt, "cadence")
        val _columnIndexOfQuestionsJson: Int = getColumnIndexOrThrow(_stmt, "questionsJson")
        val _result: MutableList<ReviewEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ReviewEntity
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
          val _tmpCadence: String
          _tmpCadence = _stmt.getText(_columnIndexOfCadence)
          val _tmpQuestionsJson: String
          _tmpQuestionsJson = _stmt.getText(_columnIndexOfQuestionsJson)
          _item =
              ReviewEntity(_tmpId,_tmpUpdatedAt,_tmpSourceDeviceId,_tmpDeletedAt,_tmpVersion,_tmpTitle,_tmpCadence,_tmpQuestionsJson)
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
