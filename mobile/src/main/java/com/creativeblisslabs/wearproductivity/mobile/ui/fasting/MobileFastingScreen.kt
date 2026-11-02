package com.creativeblisslabs.wearproductivity.mobile.ui.fasting

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.*
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// =============================================================================
// SHARED DATA MODELS FOR FASTING STUDIO, HYDRATION, DIET & BODY VITALS
// =============================================================================
data class MobileFastingProgramPreset(
    val id: String,
    val name: String,
    val fastHours: Int,
    val eatHours: Int,
    val subtitle: String
)

data class MobileDietPlanPreset(
    val id: String,
    val name: String,
    val targetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int
)

data class MobileWeightLogPoint(
    val id: String,
    val weightKg: Double,
    val dateLabel: String
)

enum class MobileBoundaryDisplayMode(val label: String) {
    FASTING("Fasting"),
    WATER("Hydration")
}

private object MobileFastingStudioPrefs {
    private const val PREFS_NAME = "lifeos_fasting_studio_prefs_v2"
    private const val KEY_PROGRAMS = "custom_fasting_programs"
    private const val KEY_DIET_PLANS = "custom_diet_plans"
    private const val KEY_ACTIVE_PROGRAM_ID = "active_program_id"
    private const val KEY_ACTIVE_DIET_ID = "active_diet_id"

    fun loadPrograms(context: Context): List<MobileFastingProgramPreset> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PROGRAMS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 5) {
                    MobileFastingProgramPreset(
                        id = p[0],
                        name = p[1],
                        fastHours = p[2].toIntOrNull()?.coerceIn(1, 72) ?: 16,
                        eatHours = p[3].toIntOrNull()?.coerceIn(1, 24) ?: 8,
                        subtitle = p[4]
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            MobileFastingProgramPreset("prog-16-8", "16:8 Leangains", 16, 8, "16-hour fast · 8-hour eating window"),
            MobileFastingProgramPreset("prog-18-6", "18:6 Warrior", 18, 6, "18-hour fast · 6-hour eating window"),
            MobileFastingProgramPreset("prog-20-4", "20:4 OMAD", 20, 4, "20-hour fast · 4-hour eating window"),
            MobileFastingProgramPreset("prog-14-10", "14:10 Circadian", 14, 10, "14-hour fast · 10-hour eating window")
        )
    }

    fun savePrograms(context: Context, list: List<MobileFastingProgramPreset>) {
        val encoded = list.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.fastHours}|${it.eatHours}|${it.subtitle.replace("|", "")}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PROGRAMS, encoded).apply()
    }

    fun loadDietPlans(context: Context): List<MobileDietPlanPreset> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DIET_PLANS, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(";;").mapNotNull { item ->
                val p = item.split("|")
                if (p.size >= 6) {
                    MobileDietPlanPreset(
                        id = p[0],
                        name = p[1],
                        targetCalories = p[2].toIntOrNull() ?: 2200,
                        targetProteinG = p[3].toIntOrNull() ?: 150,
                        targetCarbsG = p[4].toIntOrNull() ?: 220,
                        targetFatG = p[5].toIntOrNull() ?: 70
                    )
                } else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(
            MobileDietPlanPreset("diet-maintenance", "Maintenance Balance", 2200, 140, 240, 70),
            MobileDietPlanPreset("diet-fat-loss", "Lean Cut & High Protein", 1850, 165, 150, 55),
            MobileDietPlanPreset("diet-keto", "Ketogenic Endurance", 2000, 130, 35, 150),
            MobileDietPlanPreset("diet-muscle", "Hypertrophy Surplus", 2700, 185, 320, 80)
        )
    }

    fun saveDietPlans(context: Context, list: List<MobileDietPlanPreset>) {
        val encoded = list.joinToString(";;") {
            "${it.id}|${it.name.replace("|", "")}|${it.targetCalories}|${it.targetProteinG}|${it.targetCarbsG}|${it.targetFatG}"
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_DIET_PLANS, encoded).apply()
    }
}

// 4 Navigation Tabs Matching Wear OS Fasting Pages
enum class MobileFastingTab(val label: String, val icon: ImageVector) {
    ACTIVE_FAST("Active Window", Icons.Rounded.Timer),
    FASTING_STUDIO("Protocols & Water", Icons.Rounded.WaterDrop),
    DIET_NUTRITION("Diet & Macros", Icons.Rounded.Restaurant),
    VITALS_SENSORS("Vitals & Weight", Icons.Rounded.MonitorWeight)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFastingScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fast = state.fast

    var currentTab by remember { mutableStateOf(MobileFastingTab.ACTIVE_FAST) }

    val today = remember { LocalDate.now() }
    val zoneId = remember { ZoneId.systemDefault() }
    val todayStart = remember(today) { today.atStartOfDay(zoneId).toInstant().toEpochMilli() }
    val tomorrowStart = remember(today) { today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() }

    // Date-filtered nutrition for today
    val todayFoodLogs = remember(state.foodLogs, todayStart, tomorrowStart) {
        state.foodLogs.filter { it.stamp.deletedAt == null && it.loggedAtEpochMs in todayStart until tomorrowStart }
    }
    val todayCalories = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.calories.coerceAtLeast(0) } }
    val todayProtein = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.proteinGrams.coerceAtLeast(0.0) }.toInt() }
    val todayCarbs = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.carbsGrams.coerceAtLeast(0.0) }.toInt() }
    val todayFat = remember(todayFoodLogs) { todayFoodLogs.sumOf { it.fatGrams.coerceAtLeast(0.0) }.toInt() }

    // Presets & Active Selection
    var fastingPrograms by remember { mutableStateOf(MobileFastingStudioPrefs.loadPrograms(context)) }
    var activeProgram by remember {
        mutableStateOf(fastingPrograms.firstOrNull { it.id == fast.programId } ?: fastingPrograms.first())
    }

    var dietPlans by remember { mutableStateOf(MobileFastingStudioPrefs.loadDietPlans(context)) }
    var activeDietPlan by remember { mutableStateOf(dietPlans.first()) }

    // Connected Watch Sync Info
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            watchNodeId = nodes.firstOrNull()?.id
        } catch (_: Exception) {}
    }

    fun pushWatchNotification(title: String, message: String) {
        if (watchNodeId == null) {
            syncStatusText = "Watch standby (Not connected)"
            return
        }
        scope.launch {
            try {
                val payload = "$title: $message".toByteArray(Charsets.UTF_8)
                Wearable.getMessageClient(context).sendMessage(
                    watchNodeId!!,
                    "/lifeos/find_watch",
                    payload
                ).await()
                syncStatusText = "Pushed to Wear OS Watch!"
            } catch (_: Exception) {
                syncStatusText = "Watch sync failed"
            }
        }
    }

    // Dialog state
    var showFoodDialog by remember { mutableStateOf(false) }
    var foodName by remember { mutableStateOf("") }
    var foodCals by remember { mutableStateOf("") }
    var foodProt by remember { mutableStateOf("") }
    var foodCarb by remember { mutableStateOf("") }
    var foodFatVal by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(MealType.Lunch) }

    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("${state.wellness.bodyWeight ?: 75.0}") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fasting & Nutrition Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (fast.active) "${fast.phase} Window Active · ${activeProgram.name}" else "Intermittent Fasting & Macros",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pushWatchNotification("Fasting Sync", "Active window: ${fast.phase} · ${(fast.progress * 100).toInt()}% elapsed")
                    }) {
                        Icon(Icons.Rounded.Watch, contentDescription = "Sync to Watch")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTab == MobileFastingTab.DIET_NUTRITION) {
                FloatingActionButton(
                    onClick = { showFoodDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Log Meal")
                }
            } else if (currentTab == MobileFastingTab.VITALS_SENSORS) {
                FloatingActionButton(
                    onClick = { showWeightDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Rounded.Scale, contentDescription = "Log Weight")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab Row Matching Wear OS 4 Pages
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileFastingTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(tab.label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = syncStatusText != null) {
                syncStatusText?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { syncStatusText = null }) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                MobileFastingTab.ACTIVE_FAST -> {
                    ActiveFastingBoundaryPageView(
                        fast = fast,
                        activeProgram = activeProgram,
                        waterGlasses = state.wellness.waterGlasses,
                        targetGlasses = state.wellness.targetWaterGlasses,
                        onToggleStartStop = {
                            scope.launch {
                                if (fast.active) {
                                    repository.stopFast()
                                } else {
                                    repository.startFast(activeProgram.id, activeProgram.fastHours * 3600L)
                                }
                            }
                        },
                        onTogglePauseResume = {
                            scope.launch {
                                if (fast.paused) repository.resumeFast() else repository.pauseFast()
                            }
                        },
                        onSwitchPhase = {
                            scope.launch {
                                val isEating = fast.isEatingWindow
                                val targetSec = if (isEating) activeProgram.fastHours * 3600L else activeProgram.eatHours * 3600L
                                repository.switchFastPhase(!isEating, targetSec)
                            }
                        },
                        onAddWater = {
                            scope.launch { repository.setWaterGlasses(state.wellness.waterGlasses + 1) }
                        },
                        onPushWatchAlert = { title, msg -> pushWatchNotification(title, msg) }
                    )
                }

                MobileFastingTab.FASTING_STUDIO -> {
                    FastingAndHydrationStudioPageView(
                        programs = fastingPrograms,
                        activeProgram = activeProgram,
                        onSelectProgram = { program ->
                            activeProgram = program
                            if (!fast.active) {
                                scope.launch {
                                    repository.startFast(program.id, program.fastHours * 3600L)
                                }
                            }
                        },
                        waterGlasses = state.wellness.waterGlasses,
                        targetGlasses = state.wellness.targetWaterGlasses,
                        onUpdateWater = { g -> scope.launch { repository.setWaterGlasses(g) } }
                    )
                }

                MobileFastingTab.DIET_NUTRITION -> {
                    DietAndNutritionPageView(
                        todayFoodLogs = todayFoodLogs,
                        todayCalories = todayCalories,
                        todayProtein = todayProtein,
                        todayCarbs = todayCarbs,
                        todayFat = todayFat,
                        dietPlans = dietPlans,
                        activeDietPlan = activeDietPlan,
                        onSelectDietPlan = { activeDietPlan = it },
                        onDeleteFood = { id -> scope.launch { repository.deleteFood(id) } }
                    )
                }

                MobileFastingTab.VITALS_SENSORS -> {
                    BodyVitalsAndSensorsPageView(
                        state = state,
                        onUpdateWeight = { w -> scope.launch { repository.updateWellness(weight = w) } },
                        onUpdateMoodEnergy = { mood, energy -> scope.launch { repository.updateWellness(mood = mood, energy = energy) } },
                        onOpenWeightDialog = { showWeightDialog = true }
                    )
                }
            }
        }
    }

    if (showFoodDialog) {
        AlertDialog(
            onDismissRequest = { showFoodDialog = false },
            title = { Text("Log Food Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Meal Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = foodCals, onValueChange = { foodCals = it }, label = { Text("Calories (kcal)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = foodProt, onValueChange = { foodProt = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = foodCarb, onValueChange = { foodCarb = it }, label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = foodFatVal, onValueChange = { foodFatVal = it }, label = { Text("Fat (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealType.entries.forEach { mt ->
                            FilterChip(
                                selected = selectedMealType == mt,
                                onClick = { selectedMealType = mt },
                                label = { Text(mt.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cals = foodCals.toIntOrNull() ?: 0
                        val p = foodProt.toDoubleOrNull() ?: 0.0
                        val c = foodCarb.toDoubleOrNull() ?: 0.0
                        val f = foodFatVal.toDoubleOrNull() ?: 0.0
                        if (foodName.isNotBlank() && cals > 0) {
                            scope.launch {
                                repository.logFood(foodName.trim(), cals, p, c, f, selectedMealType)
                            }
                            foodName = ""
                            foodCals = ""
                            foodProt = ""
                            foodCarb = ""
                            foodFatVal = ""
                            showFoodDialog = false
                        }
                    }
                ) {
                    Text("Add Meal")
                }
            },
            dismissButton = { TextButton(onClick = { showFoodDialog = false }) { Text("Cancel") } }
        )
    }

    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Log Body Weight") },
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Body Weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val w = weightInput.toDoubleOrNull()
                    if (w != null && w > 0.0) {
                        scope.launch { repository.updateWellness(weight = w) }
                        showWeightDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showWeightDialog = false }) { Text("Cancel") } }
        )
    }
}

// =============================================================================
// 1. ACTIVE FASTING & HYDRATION BOUNDARY PAGE (Matching Wear Page 1)
// =============================================================================
@Composable
private fun ActiveFastingBoundaryPageView(
    fast: FastSession,
    activeProgram: MobileFastingProgramPreset,
    waterGlasses: Int,
    targetGlasses: Int,
    onToggleStartStop: () -> Unit,
    onTogglePauseResume: () -> Unit,
    onSwitchPhase: () -> Unit,
    onAddWater: () -> Unit,
    onPushWatchAlert: (String, String) -> Unit
) {
    val progressClamped = fast.progress.coerceIn(0f, 1f)
    val isEating = fast.isEatingWindow

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fasting Hero Boundary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (fast.active) {
                        if (isEating) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    } else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isEating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                if (fast.active) (if (isEating) "EATING WINDOW" else "FASTING WINDOW") else "IDLE / READY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            "${(progressClamped * 100).roundToInt()}% Elapsed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        activeProgram.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        if (fast.active) "Phase: ${fast.phase} · Protocol: ${fast.plan.ifBlank { activeProgram.name }}" else activeProgram.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progressClamped },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Transport Actions: Start/Stop, Pause/Resume, Switch Fast/Eat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleStartStop,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(if (fast.active) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (fast.active) "End Fast" else "Start Fast")
                        }

                        if (fast.active) {
                            OutlinedButton(
                                onClick = onTogglePauseResume,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (fast.paused) "Resume" else "Pause")
                            }

                            FilledTonalButton(
                                onClick = onSwitchPhase,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(if (isEating) "To Fast" else "To Eat")
                            }
                        }
                    }
                }
            }
        }

        // Quick Water Hydration Quick Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF006688).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Color(0xFF006688))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Quick Hydration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${waterGlasses * 250} ml / ${targetGlasses * 250} ml ($waterGlasses glasses)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    FilledTonalButton(onClick = onAddWater) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+250ml")
                    }
                }
            }
        }

        // Companion Push to Watch
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Companion Watch Complication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Synchronize active fasting phase and water count to Wear OS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = {
                            onPushWatchAlert("Fasting Status", "${fast.phase} window (${(progressClamped * 100).toInt()}% done)")
                        }
                    ) {
                        Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push")
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. FASTING & HYDRATION STUDIO PAGE (Matching Wear Page 2)
// =============================================================================
@Composable
private fun FastingAndHydrationStudioPageView(
    programs: List<MobileFastingProgramPreset>,
    activeProgram: MobileFastingProgramPreset,
    onSelectProgram: (MobileFastingProgramPreset) -> Unit,
    waterGlasses: Int,
    targetGlasses: Int,
    onUpdateWater: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fasting Protocols
        item {
            Text("Intermittent Fasting Protocols", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(programs) { prog ->
            val isSelected = activeProgram.id == prog.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProgram(prog) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prog.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(prog.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Daily Hydration Stepper & Target Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Daily Water Intake Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${waterGlasses * 250} ml / ${targetGlasses * 250} ml ($waterGlasses glasses logged)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateWater((waterGlasses - 1).coerceAtLeast(0)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-1 Glass")
                        }
                        Button(
                            onClick = { onUpdateWater(waterGlasses + 1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 Glass")
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. DIET & NUTRITION MACROS PAGE (Matching Wear Page 3)
// =============================================================================
@Composable
private fun DietAndNutritionPageView(
    todayFoodLogs: List<FoodLog>,
    todayCalories: Int,
    todayProtein: Int,
    todayCarbs: Int,
    todayFat: Int,
    dietPlans: List<MobileDietPlanPreset>,
    activeDietPlan: MobileDietPlanPreset,
    onSelectDietPlan: (MobileDietPlanPreset) -> Unit,
    onDeleteFood: (String) -> Unit
) {
    val calPct = (todayCalories.toFloat() / activeDietPlan.targetCalories.toFloat()).coerceIn(0f, 1f)
    val protPct = (todayProtein.toFloat() / activeDietPlan.targetProteinG.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Calorie & Macro Target Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DIET TARGET (${activeDietPlan.name})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("$todayCalories / ${activeDietPlan.targetCalories} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { calPct },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Macro Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MacroStatBox("Protein", "$todayProtein / ${activeDietPlan.targetProteinG}g", Color(0xFFBA1A1A))
                        MacroStatBox("Carbs", "$todayCarbs / ${activeDietPlan.targetCarbsG}g", Color(0xFF4285F4))
                        MacroStatBox("Fat", "$todayFat / ${activeDietPlan.targetFatG}g", Color(0xFFFFB300))
                    }
                }
            }
        }

        // Diet Plans Carousel
        item {
            Text("Diet Strategy Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dietPlans) { plan ->
                    val isSel = activeDietPlan.id == plan.id
                    FilterChip(
                        selected = isSel,
                        onClick = { onSelectDietPlan(plan) },
                        label = { Text("${plan.name} (${plan.targetCalories} kcal)") }
                    )
                }
            }
        }

        // Today's Meals List
        item {
            Text("Today's Meals (${todayFoodLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (todayFoodLogs.isEmpty()) {
            item {
                Text("No meals logged today. Tap + to record food intake.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(todayFoodLogs) { meal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(meal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "P: ${meal.proteinGrams.toInt()}g · C: ${meal.carbsGrams.toInt()}g · F: ${meal.fatGrams.toInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${meal.calories} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDeleteFood(meal.stamp.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. BODY VITALS & SENSORS PAGE (Matching Wear Page 4)
// =============================================================================
@Composable
private fun BodyVitalsAndSensorsPageView(
    state: LifeState,
    onUpdateWeight: (Double) -> Unit,
    onUpdateMoodEnergy: (String, Int) -> Unit,
    onOpenWeightDialog: () -> Unit
) {
    val wellness = state.wellness
    val sleepHrs = wellness.sleepLastNightMinutes / 60
    val sleepMins = wellness.sleepLastNightMinutes % 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Biometrics Summary Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Heart Rate Telemetry
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Heart Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${wellness.currentHeartRateBpm.toInt().takeIf { it > 0 }?.let { "$it BPM" } ?: "Normal"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(wellness.heartRateAccuracy.ifBlank { "Live Sensor" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Sleep Telemetry
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = Color(0xFF7986CB))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sleep Rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (wellness.sleepLastNightMinutes > 0) "${sleepHrs}h ${sleepMins}m" else "7h 30m",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text("Recovery: Optimal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Body Weight Card with Log Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Body Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${wellness.bodyWeight ?: 75.0} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Target: 72.0 kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = onOpenWeightDialog) {
                        Text("Log Weight")
                    }
                }
            }
        }

        // Energy & Mood Log Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Daily Subjective Energy & Mood", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Energy Rating: ${wellness.energy}/10", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Slider(
                        value = wellness.energy.toFloat(),
                        onValueChange = { onUpdateMoodEnergy(wellness.mood, it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Energized", "Calm", "Focused", "Fatigued").forEach { m ->
                            FilterChip(
                                selected = wellness.mood.equals(m, ignoreCase = true),
                                onClick = { onUpdateMoodEnergy(m, wellness.energy) },
                                label = { Text(m) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroStatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
