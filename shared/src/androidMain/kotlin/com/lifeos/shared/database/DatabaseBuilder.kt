package com.creativeblisslabs.wearproductivity.shared.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual class DatabaseBuilder actual constructor(context: Any) {
    private val androidContext = context as Context
    actual fun build(): LifeDatabase {
        val dbFile = androidContext.getDatabasePath("lifeos.db")
        return Room.databaseBuilder<LifeDatabase>(
            context = androidContext,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration()
            .build()
    }
}
