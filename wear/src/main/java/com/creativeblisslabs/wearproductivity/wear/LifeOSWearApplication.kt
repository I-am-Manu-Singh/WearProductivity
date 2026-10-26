package com.creativeblisslabs.wearproductivity.wear

import android.app.Application
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.RoomLifeStore
import com.creativeblisslabs.wearproductivity.shared.database.DatabaseBuilder
import com.creativeblisslabs.wearproductivity.shared.platform.AndroidWearSyncTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class LifeOSWearApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        coerceInputValues = true
    }
    
    private val database by lazy {
        DatabaseBuilder(this).build()
    }

    val repository by lazy {
        LifeRepository(
            store = RoomLifeStore(database),
            transport = AndroidWearSyncTransport(this, json),
            deviceId = "wear"
        )
    }

    val syncManager by lazy {
        com.creativeblisslabs.wearproductivity.wear.sync.WearSyncManager(this, repository, scope)
    }

    val exerciseRepository by lazy {
        com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            repository.load()
            syncManager.observeAndSync()
        }
    }
}
