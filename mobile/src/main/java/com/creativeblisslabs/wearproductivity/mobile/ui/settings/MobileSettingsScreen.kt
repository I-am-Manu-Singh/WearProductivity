package com.creativeblisslabs.wearproductivity.mobile.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.auth.FirebaseAuthManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileSettingsScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf(FirebaseAuthManager.currentUser) }
    var showResetDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { _ ->
            user = FirebaseAuthManager.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Google Account & Cloud Sync
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Cloud Synchronization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(user?.email ?: "Not signed in to Google", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (user != null) {
                                OutlinedButton(
                                    onClick = { FirebaseAuth.getInstance().signOut() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign Out")
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        // Trigger live sync
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (user != null) "Sync Now" else "Sign In")
                            }
                        }
                    }
                }
            }

            // 2. Preferences & Customization
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Companion Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        SettingToggleRow(
                            title = "24-Hour Clock Format",
                            subtitle = "Use 24-hour military notation across screens",
                            icon = Icons.Rounded.Schedule,
                            checked = state.settings.use24HourClock,
                            onCheckedChange = { chk ->
                                scope.launch {
                                    repository.updateSettings { it.copy(use24HourClock = chk) }
                                }
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        SettingToggleRow(
                            title = "Smart Wake Anticipation",
                            subtitle = "Gently vibrates before scheduled waking alarms",
                            icon = Icons.Rounded.Alarm,
                            checked = state.settings.smartWakeEnabled,
                            onCheckedChange = { chk ->
                                scope.launch {
                                    repository.updateSettings { it.copy(smartWakeEnabled = chk) }
                                }
                            }
                        )
                    }
                }
            }

            // 3. Backup, Export & Restore (Wi-Fi, Bluetooth & Local Data)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Backup & Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Export full LifeOS database via Bluetooth, Wi-Fi or Files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val jsonStr = kotlinx.serialization.json.Json {
                                            prettyPrint = true
                                            ignoreUnknownKeys = true
                                        }.encodeToString(
                                            com.creativeblisslabs.wearproductivity.shared.LifeState.serializer(),
                                            state
                                        )
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "LifeOS_Backup_${System.currentTimeMillis()}.json")
                                            putExtra(android.content.Intent.EXTRA_TEXT, jsonStr)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Export LifeOS Backup via Bluetooth, Wi-Fi or Nearby Share"))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            // Broadcast full state directly across Wear Data Layer to Watch
                                            val packet = com.creativeblisslabs.wearproductivity.shared.SyncPacket(
                                                deviceId = "phone-companion",
                                                state = state
                                            )
                                            val jsonStr = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.encodeToString(
                                                com.creativeblisslabs.wearproductivity.shared.SyncPacket.serializer(),
                                                packet
                                            )
                                            val putDataReq = com.google.android.gms.wearable.PutDataMapRequest.create(
                                                com.creativeblisslabs.wearproductivity.shared.SyncPaths.LIFE_STATE
                                            ).apply {
                                                dataMap.putString("json", jsonStr)
                                                dataMap.putLong("timestamp", System.currentTimeMillis())
                                            }.asPutDataRequest().setUrgent()

                                            com.google.android.gms.wearable.Wearable.getDataClient(context).putDataItem(putDataReq).await()
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("To Watch")
                            }
                        }
                    }
                }
            }

            // 4. Data Safety & Reset
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Data & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "LifeOS maintains offline-first local state synchronized end-to-end with your Wear OS watch.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Local LifeOS Database")
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently clear all habits, tasks, routines, and telemetry on this phone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.resetAllData()
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
