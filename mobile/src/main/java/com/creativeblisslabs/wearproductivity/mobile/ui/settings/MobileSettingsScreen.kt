package com.creativeblisslabs.wearproductivity.mobile.ui.settings

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.SyncPacket
import com.creativeblisslabs.wearproductivity.shared.SyncPaths
import com.creativeblisslabs.wearproductivity.shared.auth.FirebaseAuthManager
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// =============================================================================
// ENUMS & TABS MATCHING WEAR GENERAL DECK + SYSTEM & SYNC
// =============================================================================
enum class MobileSettingsTab(val label: String, val icon: ImageVector) {
    GENERAL("General Deck", Icons.Rounded.Tune),
    WEAR_REMOTE("Watch Studio", Icons.Rounded.Watch),
    CLOUD_SYNC("Cloud & Backup", Icons.Rounded.CloudSync)
}

// Watch Preferences Key Constants (Identical to WearAppPreferences on Watch)
private object WearPreferenceKeys {
    const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    const val KEY_USE_24_HOUR_CLOCK = "use_24_hour_clock"
    const val KEY_COMPACT_DASHBOARD = "compact_dashboard_cards"
    const val KEY_WATER_GOAL_ML = "water_goal_ml"
    const val KEY_FOCUS_DEFAULT_MINUTES = "focus_default_minutes"
    const val KEY_TASK_DEFAULT_PRIORITY = "task_default_priority"
    const val KEY_QUICK_ALARM_LEAD_MINUTES = "quick_alarm_lead_minutes"
    const val KEY_EVENT_DEFAULT_REMINDER_MINUTES = "event_default_reminder_minutes"
    const val KEY_SMART_WAKE_ENABLED = "smart_wake_enabled"
    const val KEY_AUDIO_SHUFFLE_DEFAULT = "audio_shuffle_default"
    const val KEY_AUDIO_REPEAT_MODE = "audio_repeat_mode"
    const val KEY_AUDIO_EQ_PRESET = "audio_eq_preset"
}

private object MobileWearConfigPrefs {
    private const val PREFS_NAME = "lifeos_mobile_wear_config_cache"

    fun getBoolean(context: Context, key: String, def: Boolean): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(key, def)

    fun setBoolean(context: Context, key: String, v: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, v).apply()
    }

    fun getInt(context: Context, key: String, def: Int): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(key, def)

    fun setInt(context: Context, key: String, v: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(key, v).apply()
    }

    fun getString(context: Context, key: String, def: String): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, def) ?: def

    fun setString(context: Context, key: String, v: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(key, v).apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

// =============================================================================
// MAIN COMPOSABLE: MOBILE SETTINGS SCREEN
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileSettingsScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MobileSettingsTab.GENERAL) }
    var user by remember { mutableStateOf(FirebaseAuthManager.currentUser) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showWatchResetDialog by remember { mutableStateOf(false) }

    // Connected Watch Name
    var connectedWatchName by remember { mutableStateOf("Wear OS Watch") }
    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.firstOrNull()?.let { connectedWatchName = it.displayName }
        } catch (_: Exception) {}
    }

    // Wear OS Remote Preferences State Cached on Companion
    var hapticsEnabled by remember {
        mutableStateOf(MobileWearConfigPrefs.getBoolean(context, WearPreferenceKeys.KEY_HAPTICS_ENABLED, true))
    }
    var use24HourClock by remember {
        mutableStateOf(MobileWearConfigPrefs.getBoolean(context, WearPreferenceKeys.KEY_USE_24_HOUR_CLOCK, true))
    }
    var compactDashboard by remember {
        mutableStateOf(MobileWearConfigPrefs.getBoolean(context, WearPreferenceKeys.KEY_COMPACT_DASHBOARD, false))
    }
    var waterGoalMl by remember {
        mutableIntStateOf(MobileWearConfigPrefs.getInt(context, WearPreferenceKeys.KEY_WATER_GOAL_ML, 2500))
    }
    var focusDefaultMinutes by remember {
        mutableIntStateOf(MobileWearConfigPrefs.getInt(context, WearPreferenceKeys.KEY_FOCUS_DEFAULT_MINUTES, 25))
    }
    var taskPriority by remember {
        mutableIntStateOf(MobileWearConfigPrefs.getInt(context, WearPreferenceKeys.KEY_TASK_DEFAULT_PRIORITY, 2))
    }
    var alarmLeadMinutes by remember {
        mutableIntStateOf(MobileWearConfigPrefs.getInt(context, WearPreferenceKeys.KEY_QUICK_ALARM_LEAD_MINUTES, 10))
    }
    var eventReminderMinutes by remember {
        mutableIntStateOf(MobileWearConfigPrefs.getInt(context, WearPreferenceKeys.KEY_EVENT_DEFAULT_REMINDER_MINUTES, 15))
    }
    var smartWakeEnabled by remember {
        mutableStateOf(MobileWearConfigPrefs.getBoolean(context, WearPreferenceKeys.KEY_SMART_WAKE_ENABLED, true))
    }

    // Restore Backup JSON Launcher
    val backupPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream: java.io.InputStream ->
                            val jsonString = stream.bufferedReader().use { it.readText() }
                            val restoredState = Json { ignoreUnknownKeys = true }.decodeFromString(
                                com.creativeblisslabs.wearproductivity.shared.LifeState.serializer(),
                                jsonString
                            )
                            repository.syncWithRemote(restoredState)
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                statusNotice = "Successfully restored LifeOS database from backup file"
                            }
                        }
                    } catch (e: Exception) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            statusNotice = "Backup restore failed: ${e.message}"
                        }
                    }
                }
            }
        }
    }

    // Helper: Push Remote Preference to Connected Watch
    fun pushWearPreference(type: String, key: String, value: String) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                nodes.firstOrNull()?.let { node ->
                    val payload = "$type:$key:$value"
                    Wearable.getMessageClient(context).sendMessage(
                        node.id,
                        "/lifeos/wear_preference_update",
                        payload.toByteArray(Charsets.UTF_8)
                    ).await()
                    statusNotice = "Pushed $key to $connectedWatchName"
                } ?: run {
                    statusNotice = "Saved locally (Watch offline)"
                }
            } catch (_: Exception) {
                statusNotice = "Failed to push to watch"
            }
        }
    }

    // Helper: Push Full State to Watch Data Layer
    fun syncFullStateToWatch() {
        scope.launch {
            try {
                val packet = SyncPacket(deviceId = "phone-companion", state = state)
                val jsonStr = Json { ignoreUnknownKeys = true }.encodeToString(SyncPacket.serializer(), packet)
                val putDataReq = PutDataMapRequest.create(SyncPaths.LIFE_STATE).apply {
                    dataMap.putString("json", jsonStr)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(putDataReq).await()
                statusNotice = "Pushed full LifeOS state to $connectedWatchName"
            } catch (_: Exception) {
                statusNotice = "Failed to broadcast to watch"
            }
        }
    }

    DisposableEffect(Unit) {
        val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
        val listener = FirebaseAuth.AuthStateListener { _ ->
            user = FirebaseAuthManager.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings & Remote Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Watch Configuration • Cloud • Offline Backup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { syncFullStateToWatch() }) {
                        Icon(Icons.Rounded.Sync, contentDescription = "Sync Full State")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 3 Screen Tabs: General Deck, Watch Studio, Cloud & Backup
            PrimaryTabRow(
                selectedTabIndex = activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileSettingsTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tab.label, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = activeTab,
                modifier = Modifier.weight(1f),
                label = "settings_tab_content"
            ) { targetTab ->
                when (targetTab) {
                    MobileSettingsTab.GENERAL -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Interaction & Display
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Interaction & Display", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SettingToggleRow(
                                            title = "24-Hour Clock Format",
                                            subtitle = if (state.settings.use24HourClock) "Displaying 18:45" else "Displaying 6:45 PM",
                                            icon = Icons.Rounded.Schedule,
                                            checked = state.settings.use24HourClock,
                                            onCheckedChange = { chk ->
                                                scope.launch {
                                                    repository.updateSettings { it.copy(use24HourClock = chk) }
                                                }
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        SettingToggleRow(
                                            title = "Smart Wake Anticipation",
                                            subtitle = if (state.settings.smartWakeEnabled) "Tactile waking sequence before alarms" else "Disabled",
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

                            // 2. Daily Targets & Planner Steppers
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Daily Targets & Productivity Defaults", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Focus Session Stepper
                                        SettingStepperRow(
                                            title = "Default Focus Session",
                                            valueText = "$focusDefaultMinutes min",
                                            onDecrement = {
                                                if (focusDefaultMinutes > 5) {
                                                    focusDefaultMinutes -= 5
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_FOCUS_DEFAULT_MINUTES, focusDefaultMinutes)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_FOCUS_DEFAULT_MINUTES, focusDefaultMinutes.toString())
                                                }
                                            },
                                            onIncrement = {
                                                if (focusDefaultMinutes < 180) {
                                                    focusDefaultMinutes += 5
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_FOCUS_DEFAULT_MINUTES, focusDefaultMinutes)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_FOCUS_DEFAULT_MINUTES, focusDefaultMinutes.toString())
                                                }
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        // Water Intake Goal Stepper
                                        SettingStepperRow(
                                            title = "Water Intake Goal",
                                            valueText = "$waterGoalMl ml",
                                            onDecrement = {
                                                if (waterGoalMl > 1000) {
                                                    waterGoalMl -= 250
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_WATER_GOAL_ML, waterGoalMl)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_WATER_GOAL_ML, waterGoalMl.toString())
                                                }
                                            },
                                            onIncrement = {
                                                if (waterGoalMl < 5000) {
                                                    waterGoalMl += 250
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_WATER_GOAL_ML, waterGoalMl)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_WATER_GOAL_ML, waterGoalMl.toString())
                                                }
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        // Task Default Priority Stepper
                                        SettingStepperRow(
                                            title = "Default Task Priority",
                                            valueText = when (taskPriority) {
                                                1 -> "Low (P1)"
                                                2 -> "Normal (P2)"
                                                else -> "High (P3)"
                                            },
                                            onDecrement = {
                                                if (taskPriority > 1) {
                                                    taskPriority -= 1
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_TASK_DEFAULT_PRIORITY, taskPriority)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_TASK_DEFAULT_PRIORITY, taskPriority.toString())
                                                }
                                            },
                                            onIncrement = {
                                                if (taskPriority < 3) {
                                                    taskPriority += 1
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_TASK_DEFAULT_PRIORITY, taskPriority)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_TASK_DEFAULT_PRIORITY, taskPriority.toString())
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Status Notice
                            statusNotice?.let {
                                item {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    MobileSettingsTab.WEAR_REMOTE -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Wear Connected Device Banner
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(connectedWatchName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text("Bidirectional Wear OS Sync Link Active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }

                            // 2. Watch System Toggles
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Remote Watch Display & Haptics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        SettingToggleRow(
                                            title = "Haptic Vibration Engine",
                                            subtitle = if (hapticsEnabled) "Wrist haptic cues active on watch" else "Tactile cues muted on watch",
                                            icon = Icons.Rounded.Vibration,
                                            checked = hapticsEnabled,
                                            onCheckedChange = { chk ->
                                                hapticsEnabled = chk
                                                MobileWearConfigPrefs.setBoolean(context, WearPreferenceKeys.KEY_HAPTICS_ENABLED, chk)
                                                pushWearPreference("bool", WearPreferenceKeys.KEY_HAPTICS_ENABLED, chk.toString())
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        SettingToggleRow(
                                            title = "24-Hour Watch Timeface",
                                            subtitle = if (use24HourClock) "Watchface military format (18:45)" else "Watchface AM/PM format (6:45 PM)",
                                            icon = Icons.Rounded.Timer,
                                            checked = use24HourClock,
                                            onCheckedChange = { chk ->
                                                use24HourClock = chk
                                                MobileWearConfigPrefs.setBoolean(context, WearPreferenceKeys.KEY_USE_24_HOUR_CLOCK, chk)
                                                pushWearPreference("bool", WearPreferenceKeys.KEY_USE_24_HOUR_CLOCK, chk.toString())
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        SettingToggleRow(
                                            title = "Compact Watch Dashboard",
                                            subtitle = if (compactDashboard) "Dense card tiles for round dials" else "Standard expressive layout",
                                            icon = Icons.Rounded.Dashboard,
                                            checked = compactDashboard,
                                            onCheckedChange = { chk ->
                                                compactDashboard = chk
                                                MobileWearConfigPrefs.setBoolean(context, WearPreferenceKeys.KEY_COMPACT_DASHBOARD, chk)
                                                pushWearPreference("bool", WearPreferenceKeys.KEY_COMPACT_DASHBOARD, chk.toString())
                                            }
                                        )
                                    }
                                }
                            }

                            // 3. Timing & Alarms Remote Tuning
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Watch Timing & Alarms Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(14.dp))

                                        SettingStepperRow(
                                            title = "Alarm Lead Time",
                                            valueText = "$alarmLeadMinutes min",
                                            onDecrement = {
                                                if (alarmLeadMinutes > 5) {
                                                    alarmLeadMinutes -= 5
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_QUICK_ALARM_LEAD_MINUTES, alarmLeadMinutes)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_QUICK_ALARM_LEAD_MINUTES, alarmLeadMinutes.toString())
                                                }
                                            },
                                            onIncrement = {
                                                if (alarmLeadMinutes < 30) {
                                                    alarmLeadMinutes += 5
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_QUICK_ALARM_LEAD_MINUTES, alarmLeadMinutes)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_QUICK_ALARM_LEAD_MINUTES, alarmLeadMinutes.toString())
                                                }
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        SettingStepperRow(
                                            title = "Event Reminder Notice",
                                            valueText = "$eventReminderMinutes min",
                                            onDecrement = {
                                                if (eventReminderMinutes > 5) {
                                                    eventReminderMinutes -= 5
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_EVENT_DEFAULT_REMINDER_MINUTES, eventReminderMinutes)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_EVENT_DEFAULT_REMINDER_MINUTES, eventReminderMinutes.toString())
                                                }
                                            },
                                            onIncrement = {
                                                if (eventReminderMinutes < 60) {
                                                    eventReminderMinutes += 5
                                                    MobileWearConfigPrefs.setInt(context, WearPreferenceKeys.KEY_EVENT_DEFAULT_REMINDER_MINUTES, eventReminderMinutes)
                                                    pushWearPreference("int", WearPreferenceKeys.KEY_EVENT_DEFAULT_REMINDER_MINUTES, eventReminderMinutes.toString())
                                                }
                                            }
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                        SettingToggleRow(
                                            title = "Smart Wake on Wrist",
                                            subtitle = if (smartWakeEnabled) "Watch vibrates before target alarm" else "Disabled on watch",
                                            icon = Icons.Rounded.Alarm,
                                            checked = smartWakeEnabled,
                                            onCheckedChange = { chk ->
                                                smartWakeEnabled = chk
                                                MobileWearConfigPrefs.setBoolean(context, WearPreferenceKeys.KEY_SMART_WAKE_ENABLED, chk)
                                                pushWearPreference("bool", WearPreferenceKeys.KEY_SMART_WAKE_ENABLED, chk.toString())
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. Reset Watch Preferences Button
                            item {
                                OutlinedButton(
                                    onClick = { showWatchResetDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset Remote Watch Preferences")
                                }
                            }
                        }
                    }

                    MobileSettingsTab.CLOUD_SYNC -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // 1. Google Account & Cloud Sync Card
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
                                                    syncFullStateToWatch()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (user != null) "Sync Cloud & Watch" else "Sign In")
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Backup, Import & Export Suite (Wi-Fi, Bluetooth, Mobile Data & Nearby)
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
                                                Text("Omni-Sync, Backup & Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Sync & export database across Bluetooth, Wi-Fi, Mobile Data & Cloud", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        val jsonStr = Json {
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

                                            FilledTonalButton(
                                                onClick = {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                                                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                                                        type = "*/*"
                                                    }
                                                    backupPickerLauncher.launch(intent)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Restore")
                                            }

                                            OutlinedButton(
                                                onClick = { syncFullStateToWatch() },
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

                            // 3. Reset Local Database
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Data Safety & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                }
            }
        }
    }

    // Modal: Reset Local Database Confirmation
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

    // Modal: Reset Watch Preferences Confirmation
    if (showWatchResetDialog) {
        AlertDialog(
            onDismissRequest = { showWatchResetDialog = false },
            icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Remote Watch Preferences?") },
            text = { Text("This will reset all display, haptic, water goal, and focus defaults on $connectedWatchName to factory settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                MobileWearConfigPrefs.reset(context)
                                hapticsEnabled = true
                                use24HourClock = true
                                compactDashboard = false
                                waterGoalMl = 2500
                                focusDefaultMinutes = 25
                                taskPriority = 2
                                alarmLeadMinutes = 10
                                eventReminderMinutes = 15
                                smartWakeEnabled = true

                                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                                nodes.firstOrNull()?.let { node ->
                                    Wearable.getMessageClient(context).sendMessage(
                                        node.id,
                                        "/lifeos/reset_wear_preferences",
                                        byteArrayOf()
                                    ).await()
                                }
                                statusNotice = "Reset watch preferences on $connectedWatchName"
                            } catch (_: Exception) {}
                            showWatchResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Watch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatchResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================
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

@Composable
private fun SettingStepperRow(
    title: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease")
            }
            IconButton(onClick = onIncrement) {
                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase")
            }
        }
    }
}
