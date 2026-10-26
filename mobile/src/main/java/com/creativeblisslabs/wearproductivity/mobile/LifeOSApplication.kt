package com.creativeblisslabs.wearproductivity.mobile

import android.app.Application
import com.creativeblisslabs.wearproductivity.shared.InMemoryLifeStore
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.platform.AndroidWearSyncTransport
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
    
    // In a real app, use RoomLifeStore with LifeDatabase
    val repository by lazy {
        LifeRepository(
            store = InMemoryLifeStore(),
            transport = AndroidWearSyncTransport(this, json),
            deviceId = "phone"
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.load()
        }
    }
}
