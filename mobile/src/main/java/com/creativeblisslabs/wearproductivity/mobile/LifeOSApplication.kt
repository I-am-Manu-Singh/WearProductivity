package com.creativeblisslabs.wearproductivity.mobile

import android.app.Application
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.RoomLifeStore
import com.creativeblisslabs.wearproductivity.shared.database.DatabaseBuilder
import com.creativeblisslabs.wearproductivity.shared.platform.AndroidWearSyncTransport
import com.creativeblisslabs.wearproductivity.mobile.sync.MobileWearSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class LifeOSApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
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
            deviceId = "phone"
        )
    }

    val wearSyncManager by lazy {
        MobileWearSyncManager(this, repository, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.load()
            wearSyncManager.observeAndSync()
        }
    }
}
