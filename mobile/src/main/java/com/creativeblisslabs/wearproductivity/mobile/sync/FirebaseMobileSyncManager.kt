package com.creativeblisslabs.wearproductivity.mobile.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

class FirebaseMobileSyncManager(
    private val repository: LifeRepository,
    private val scope: CoroutineScope
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val json = Json { ignoreUnknownKeys = true }
    private var snapshotListener: ListenerRegistration? = null
    private val isSyncing = AtomicBoolean(false)

    private var isSyncingFromFirebase = AtomicBoolean(false)

    fun startListening() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val docRef = firestore.collection("users").document(user.uid)

        snapshotListener?.remove()
        snapshotListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val syncDataJson = snapshot.getString("syncPacketJson")
                if (!syncDataJson.isNullOrBlank()) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val packet = json.decodeFromString<SyncPacket>(syncDataJson)
                            if (packet.deviceId != "mobile") { // Prevent echo if we just pushed it
                                isSyncingFromFirebase.set(true)
                                repository.handleRemoteSync(packet)
                                isSyncingFromFirebase.set(false)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isSyncingFromFirebase.set(false)
                        }
                    }
                }
            }
        }

        // Also observe local repository changes to push up to Firebase
        scope.launch(Dispatchers.IO) {
            repository.state.collect { state ->
                if (!isSyncingFromFirebase.get()) {
                    pushLocalStateToFirebase()
                }
            }
        }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }

    private fun pushLocalStateToFirebase() {
        if (!isSyncing.compareAndSet(false, true)) return

        scope.launch(Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val currentState = repository.current()
                    val packet = SyncPacket(deviceId = "mobile", state = currentState)
                    val jsonStr = json.encodeToString(packet)

                    firestore.collection("users").document(user.uid).set(
                        mapOf("syncPacketJson" to jsonStr, "lastSyncedAt" to System.currentTimeMillis())
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSyncing.set(false)
            }
        }
    }
}
