package com.creativeblisslabs.wearproductivity.mobile.ui.workout

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
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// =============================================================================
// SHARED MODELS MATCHING WEAR WORKOUT STUDIO & MULTI-WEEK TRAINING
// =============================================================================
enum class MobileWorkoutDiscipline(val label: String, val shortLabel: String, val accent: Color) {
    GYM("Gym Strength", "Gym", Color(0xFFFF9A7A)),
    RUNNING("Running", "Run", Color(0xFF79E2B0)),
    SWIMMING("Swimming", "Swim", Color(0xFF68B6FF)),
    CALISTHENICS("Calisthenics", "Calis", Color(0xFFBBA5FF));

    val icon: ImageVector
        get() = when (this) {
            GYM -> Icons.Rounded.FitnessCenter
            RUNNING -> Icons.AutoMirrored.Rounded.DirectionsRun
            SWIMMING -> Icons.Rounded.Pool
            CALISTHENICS -> Icons.Rounded.SportsGymnastics
        }
}

data class MobileWorkoutProgramItem(
    val id: String,
    val name: String,
    val discipline: MobileWorkoutDiscipline,
    val muscleOrZone: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Double,
    val restSeconds: Int,
    val stepDurationSeconds: Int,
    val steps: List<String>
)

data class MobilePersonalRecordEntry(
    val id: String,
    val exerciseName: String,
    val discipline: MobileWorkoutDiscipline,
    val currentValue: Double,
    val previousBest: Double,
    val targetGoal: Double,
    val unit: String
)

data class MobileWorkoutHistoryRecord(
    val id: String,
    val title: String,
    val discipline: MobileWorkoutDiscipline,
    val setsCompleted: Int,
    val totalRepsOrLaps: Int,
    val totalVolumeOrDist: Double,
    val durationSeconds: Int,
    val isNewPr: Boolean,
    val timestampLabel: String,
    val timestampEpochMillis: Long = 0L
)

private object MobileWorkoutStudioPrefs {
    private const val PREFS_NAME = "lifeos_workout_studio_prefs_v3"
    private const val KEY_PROGRAMS = "custom_workout_programs_v3"
    private const val KEY_PR_LIST = "workout_pr_records_v3"
    private const val KEY_HISTORY = "workout_session_history_v3"
    private const val KEY_ACTIVE_TRAINING_PROGRAM_ID = "active_training_program_id"
    private const val KEY_COMPLETED_TRAINING_DAYS = "completed_training_days_set"

    fun loadPrograms(context: Context, repoRuns: List<RunWorkout>): List<MobileWorkoutProgramItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PROGRAMS, null)

        if (!raw.isNullOrBlank() && raw.trimStart().startsWith("[")) {
            val parsed = runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val obj = array.optJSONObject(index) ?: continue
                        val discipline = runCatching {
                            MobileWorkoutDiscipline.valueOf(obj.optString("discipline"))
                        }.getOrDefault(MobileWorkoutDiscipline.GYM)

                        val stepsArray = obj.optJSONArray("steps")
                        val steps = buildList {
                            if (stepsArray != null) {
                                for (stepIndex in 0 until stepsArray.length()) {
                                    stepsArray.optString(stepIndex).takeIf { it.isNotBlank() }?.let(::add)
                                }
                            }
                        }

                        val item = MobileWorkoutProgramItem(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            discipline = discipline,
                            muscleOrZone = obj.optString("muscleOrZone"),
                            targetSets = obj.optInt("targetSets", 4).coerceIn(1, 30),
                            targetReps = obj.optInt("targetReps", 10).coerceIn(1, 200),
                            targetWeightKg = obj.optDouble("targetWeightKg", 60.0).takeIf { it.isFinite() }?.coerceIn(0.0, 500.0) ?: 60.0,
                            restSeconds = obj.optInt("restSeconds", 60).coerceIn(10, 600),
                            stepDurationSeconds = obj.optInt("stepDurationSeconds", 180).coerceIn(15, 7200),
                            steps = steps
                        )
                        if (item.id.isNotBlank() && item.name.isNotBlank()) add(item)
                    }
                }
            }.getOrDefault(emptyList())
            if (parsed.isNotEmpty()) return parsed
        }

        // Built-in seed routines
        val builtins = listOf(
            MobileWorkoutProgramItem(
                id = "prog-barbell-squat",
                name = "Barbell Back Squat",
                discipline = MobileWorkoutDiscipline.GYM,
                muscleOrZone = "Quads & Glutes",
                targetSets = 4,
                targetReps = 8,
                targetWeightKg = 85.0,
                restSeconds = 90,
                stepDurationSeconds = 180,
                steps = listOf("Warmup Bar × 10", "60kg × 8", "75kg × 8", "85kg × 8")
            ),
            MobileWorkoutProgramItem(
                id = "prog-bench-press",
                name = "Flat Bench Press",
                discipline = MobileWorkoutDiscipline.GYM,
                muscleOrZone = "Chest & Triceps",
                targetSets = 4,
                targetReps = 10,
                targetWeightKg = 70.0,
                restSeconds = 90,
                stepDurationSeconds = 180,
                steps = listOf("Warmup Bar × 12", "50kg × 10", "60kg × 10", "70kg × 8")
            ),
            MobileWorkoutProgramItem(
                id = "prog-weighted-pullup",
                name = "Weighted Pull-Up",
                discipline = MobileWorkoutDiscipline.CALISTHENICS,
                muscleOrZone = "Lats & Biceps",
                targetSets = 4,
                targetReps = 6,
                targetWeightKg = 15.0,
                restSeconds = 120,
                stepDurationSeconds = 180,
                steps = listOf("Bodyweight × 8", "+10kg × 6", "+15kg × 6", "+15kg × 5")
            )
        )

        val runItems = repoRuns.take(3).map { run ->
            MobileWorkoutProgramItem(
                id = run.stamp.id,
                name = run.title,
                discipline = MobileWorkoutDiscipline.RUNNING,
                muscleOrZone = "W${run.week} · D${run.day}",
                targetSets = run.intervals.size.coerceAtLeast(3),
                targetReps = 1,
                targetWeightKg = run.distanceKm ?: 3.5,
                restSeconds = 60,
                stepDurationSeconds = 180,
                steps = run.intervals.ifEmpty { listOf("Warmup 5m", "Main Run Interval", "Cooldown 5m") }
            )
        }
        return builtins + runItems
    }

    fun savePrograms(context: Context, list: List<MobileWorkoutProgramItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val steps = JSONArray().apply { item.steps.forEach(::put) }
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("discipline", item.discipline.name)
                    put("muscleOrZone", item.muscleOrZone)
                    put("targetSets", item.targetSets)
                    put("targetReps", item.targetReps)
                    put("targetWeightKg", item.targetWeightKg)
                    put("restSeconds", item.restSeconds)
                    put("stepDurationSeconds", item.stepDurationSeconds)
                    put("steps", steps)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PROGRAMS, array.toString()).apply()
    }

    fun loadPrList(context: Context): List<MobilePersonalRecordEntry> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PR_LIST, null)
        if (!raw.isNullOrBlank() && raw.trimStart().startsWith("[")) {
            val jsonList = runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                    val id = obj.optString("id")
                    if (id.isBlank()) return@mapNotNull null
                    MobilePersonalRecordEntry(
                        id = id,
                        exerciseName = obj.optString("exerciseName", "Exercise"),
                        discipline = runCatching { MobileWorkoutDiscipline.valueOf(obj.optString("discipline")) }.getOrDefault(MobileWorkoutDiscipline.GYM),
                        currentValue = obj.optDouble("currentValue", 60.0),
                        previousBest = obj.optDouble("previousBest", 55.0),
                        targetGoal = obj.optDouble("targetGoal", 75.0),
                        unit = obj.optString("unit", "kg")
                    )
                }
            }.getOrNull()
            if (!jsonList.isNullOrEmpty()) return jsonList
        }

        return listOf(
            MobilePersonalRecordEntry("pr-squat", "Back Squat", MobileWorkoutDiscipline.GYM, 85.0, 80.0, 100.0, "kg"),
            MobilePersonalRecordEntry("pr-bench", "Bench Press", MobileWorkoutDiscipline.GYM, 70.0, 65.0, 80.0, "kg"),
            MobilePersonalRecordEntry("pr-deadlift", "Deadlift", MobileWorkoutDiscipline.GYM, 120.0, 110.0, 140.0, "kg"),
            MobilePersonalRecordEntry("pr-5k", "5K Pace", MobileWorkoutDiscipline.RUNNING, 24.5, 26.0, 22.0, "mins")
        )
    }

    fun savePrList(context: Context, list: List<MobilePersonalRecordEntry>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("exerciseName", item.exerciseName)
                    put("discipline", item.discipline.name)
                    put("currentValue", item.currentValue)
                    put("previousBest", item.previousBest)
                    put("targetGoal", item.targetGoal)
                    put("unit", item.unit)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PR_LIST, array.toString()).apply()
    }

    fun loadCompletedTrainingDays(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_COMPLETED_TRAINING_DAYS, emptySet()) ?: emptySet()
    }
}

// 4 Navigation Tabs Matching Wear OS Workout Screen Pages
enum class MobileWorkoutTab(val label: String, val icon: ImageVector) {
    ACTIVE_WORKOUT("Active Routine", Icons.Rounded.PlayCircle),
    LIVE_CONTROLS("Steps & Sets", Icons.Rounded.Tune),
    PROGRAMS_BUILDER("Programs & Plans", Icons.Rounded.FitnessCenter),
    ANALYTICS_PRS("PRs & Strength", Icons.Rounded.EmojiEvents)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileWorkoutScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(MobileWorkoutTab.ACTIVE_WORKOUT) }

    // Programs & PRs
    var programs by remember { mutableStateOf(MobileWorkoutStudioPrefs.loadPrograms(context, state.runs)) }
    var activeProgram by remember { mutableStateOf(programs.first()) }
    var prList by remember { mutableStateOf(MobileWorkoutStudioPrefs.loadPrList(context)) }
    var completedDays by remember { mutableStateOf(MobileWorkoutStudioPrefs.loadCompletedTrainingDays(context)) }

    // Active session state
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var currentSetNumber by remember { mutableIntStateOf(1) }
    var currentReps by remember { mutableIntStateOf(activeProgram.targetReps) }
    var currentWeight by remember { mutableDoubleStateOf(activeProgram.targetWeightKg) }
    var isRunning by remember { mutableStateOf(false) }
    var isRestingBreak by remember { mutableStateOf(false) }
    var restRemainingSeconds by remember { mutableIntStateOf(activeProgram.restSeconds) }

    // Wear OS Sync State
    var watchNodeId by remember { mutableStateOf<String?>(null) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            watchNodeId = nodes.firstOrNull()?.id
        } catch (_: Exception) {}
    }

    fun sendWatchWorkoutCommand(command: String, title: String) {
        if (watchNodeId == null) {
            syncStatusText = "Watch standby (Not connected)"
            return
        }
        scope.launch {
            try {
                Wearable.getMessageClient(context).sendMessage(
                    watchNodeId!!,
                    "/lifeos/workout/$command",
                    title.toByteArray(Charsets.UTF_8)
                ).await()
                syncStatusText = "$title triggered on Watch!"
            } catch (_: Exception) {
                syncStatusText = "Watch workout sync failed"
            }
        }
    }

    // PR Editor dialog
    var showPrDialog by remember { mutableStateOf(false) }
    var newPrExercise by remember { mutableStateOf("") }
    var newPrCurrent by remember { mutableStateOf("80.0") }
    var newPrGoal by remember { mutableStateOf("100.0") }
    var newPrUnit by remember { mutableStateOf("kg") }
    var newPrDiscipline by remember { mutableStateOf(MobileWorkoutDiscipline.GYM) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Strength & Workout Studio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${activeProgram.name} · ${activeProgram.discipline.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sendWatchWorkoutCommand("start_gym", "Push ${activeProgram.name} to Watch")
                    }) {
                        Icon(Icons.Rounded.Watch, contentDescription = "Push to Watch")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTab == MobileWorkoutTab.ANALYTICS_PRS) {
                FloatingActionButton(
                    onClick = { showPrDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add PR")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 4-Tab Navigation Suite Matching Wear OS Page Structure
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                MobileWorkoutTab.entries.forEach { tab ->
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
                MobileWorkoutTab.ACTIVE_WORKOUT -> {
                    ActiveWorkoutBoundaryPageView(
                        activeProgram = activeProgram,
                        currentExerciseIndex = currentExerciseIndex,
                        totalExercises = programs.size,
                        currentSetNumber = currentSetNumber,
                        targetSets = activeProgram.targetSets,
                        currentReps = currentReps,
                        currentWeight = currentWeight,
                        isRunning = isRunning,
                        isRestingBreak = isRestingBreak,
                        onToggleStartPause = { isRunning = !isRunning },
                        onCompleteSet = {
                            if (currentSetNumber < activeProgram.targetSets) {
                                currentSetNumber++
                                isRestingBreak = true
                            } else {
                                isRunning = false
                                isRestingBreak = false
                            }
                        },
                        onNextExercise = {
                            if (currentExerciseIndex < programs.size - 1) {
                                currentExerciseIndex++
                                activeProgram = programs[currentExerciseIndex]
                                currentSetNumber = 1
                                currentReps = activeProgram.targetReps
                                currentWeight = activeProgram.targetWeightKg
                            }
                        },
                        onPrevExercise = {
                            if (currentExerciseIndex > 0) {
                                currentExerciseIndex--
                                activeProgram = programs[currentExerciseIndex]
                                currentSetNumber = 1
                                currentReps = activeProgram.targetReps
                                currentWeight = activeProgram.targetWeightKg
                            }
                        },
                        onPushWatchWorkout = { cmd, title -> sendWatchWorkoutCommand(cmd, title) }
                    )
                }

                MobileWorkoutTab.LIVE_CONTROLS -> {
                    WorkoutStepsAndLiveControlsPageView(
                        activeProgram = activeProgram,
                        currentReps = currentReps,
                        currentWeight = currentWeight,
                        currentSet = currentSetNumber,
                        onUpdateReps = { currentReps = it },
                        onUpdateWeight = { currentWeight = it }
                    )
                }

                MobileWorkoutTab.PROGRAMS_BUILDER -> {
                    WorkoutProgramsAndEditorPageView(
                        programs = programs,
                        activeProgram = activeProgram,
                        runs = state.runs,
                        completedDays = completedDays,
                        onSelectProgram = { prog ->
                            activeProgram = prog
                            currentExerciseIndex = programs.indexOf(prog).coerceAtLeast(0)
                            currentSetNumber = 1
                            currentReps = prog.targetReps
                            currentWeight = prog.targetWeightKg
                        },
                        onToggleRunWorkout = { runId ->
                            scope.launch { repository.completeRunWorkout(runId) }
                        }
                    )
                }

                MobileWorkoutTab.ANALYTICS_PRS -> {
                    WorkoutAnalyticsAndPrPageView(
                        prList = prList,
                        onOpenAddPr = { showPrDialog = true }
                    )
                }
            }
        }
    }

    if (showPrDialog) {
        AlertDialog(
            onDismissRequest = { showPrDialog = false },
            title = { Text("Log New Personal Record (PR)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPrExercise,
                        onValueChange = { newPrExercise = it },
                        label = { Text("Exercise Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPrCurrent,
                            onValueChange = { newPrCurrent = it },
                            label = { Text("Current PR") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPrGoal,
                            onValueChange = { newPrGoal = it },
                            label = { Text("Goal Target") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newPrUnit,
                        onValueChange = { newPrUnit = it },
                        label = { Text("Unit (kg, reps, mins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPrExercise.isNotBlank()) {
                        val cur = newPrCurrent.toDoubleOrNull() ?: 60.0
                        val goal = newPrGoal.toDoubleOrNull() ?: 80.0
                        val newEntry = MobilePersonalRecordEntry(
                            id = "pr-${System.currentTimeMillis()}",
                            exerciseName = newPrExercise.trim(),
                            discipline = newPrDiscipline,
                            currentValue = cur,
                            previousBest = cur * 0.9,
                            targetGoal = goal,
                            unit = newPrUnit.trim().ifBlank { "kg" }
                        )
                        val updated = prList + newEntry
                        prList = updated
                        MobileWorkoutStudioPrefs.savePrList(context, updated)
                        showPrDialog = false
                        newPrExercise = ""
                    }
                }) {
                    Text("Save PR")
                }
            },
            dismissButton = { TextButton(onClick = { showPrDialog = false }) { Text("Cancel") } }
        )
    }
}

// =============================================================================
// 1. ACTIVE WORKOUT BOUNDARY CANVAS VIEW (Matching Wear Page 1)
// =============================================================================
@Composable
private fun ActiveWorkoutBoundaryPageView(
    activeProgram: MobileWorkoutProgramItem,
    currentExerciseIndex: Int,
    totalExercises: Int,
    currentSetNumber: Int,
    targetSets: Int,
    currentReps: Int,
    currentWeight: Double,
    isRunning: Boolean,
    isRestingBreak: Boolean,
    onToggleStartPause: () -> Unit,
    onCompleteSet: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onPushWatchWorkout: (String, String) -> Unit
) {
    val setProgress = (currentSetNumber.toFloat() / targetSets.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Exercise Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRestingBreak) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
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
                            color = activeProgram.discipline.accent
                        ) {
                            Text(
                                "EXERCISE ${currentExerciseIndex + 1} OF $totalExercises",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            if (isRestingBreak) "RESTING BREAK" else "ACTIVE SET $currentSetNumber/$targetSets",
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
                        "${currentWeight.roundToInt()}kg × $currentReps REPS",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        activeProgram.muscleOrZone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { setProgress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Transport Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevExercise,
                            enabled = currentExerciseIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Exercise")
                        }

                        Button(
                            onClick = onCompleteSet,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (currentSetNumber >= targetSets) "Finish Exercise" else "Complete Set $currentSetNumber")
                        }

                        IconButton(
                            onClick = onNextExercise,
                            enabled = currentExerciseIndex < totalExercises - 1
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Exercise")
                        }
                    }
                }
            }
        }

        // Live Set & Rep Increment Steppers
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkoutMetricPill("TARGET SETS", "$targetSets", Icons.Rounded.Repeat)
                    WorkoutMetricPill("TARGET REPS", "$currentReps", Icons.Rounded.Numbers)
                    WorkoutMetricPill("REST TIME", "${activeProgram.restSeconds}s", Icons.Rounded.Timer)
                }
            }
        }

        // Push Routine to Wear OS
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
                        Text("Companion Watch Guided Workout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Stream active exercise rep counts and rest timers to your Wear OS device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = {
                            onPushWatchWorkout("start_gym", "Started ${activeProgram.name}")
                        }
                    ) {
                        Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start on Watch")
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. LIVE CONTROLS & STEPS PAGE (Matching Wear Page 2)
// =============================================================================
@Composable
private fun WorkoutStepsAndLiveControlsPageView(
    activeProgram: MobileWorkoutProgramItem,
    currentReps: Int,
    currentWeight: Double,
    currentSet: Int,
    onUpdateReps: (Int) -> Unit,
    onUpdateWeight: (Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Live Set Adjuster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // Quick Steppers for Weight & Reps
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Weight (kg)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${currentWeight.roundToInt()} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onUpdateWeight((currentWeight - 2.5).coerceAtLeast(0.0)) }) { Text("-2.5kg") }
                            Button(onClick = { onUpdateWeight(currentWeight + 2.5) }) { Text("+2.5kg") }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Reps Count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$currentReps reps", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onUpdateReps((currentReps - 1).coerceAtLeast(1)) }) { Text("-1") }
                            Button(onClick = { onUpdateReps(currentReps + 1) }) { Text("+1") }
                        }
                    }
                }
            }
        }

        // Exercise Step Progression
        item {
            Text("Workout Progression Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(activeProgram.steps) { step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(activeProgram.discipline.accent, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// =============================================================================
// 3. PROGRAMS BUILDER & PLANS (Matching Wear Page 3)
// =============================================================================
@Composable
private fun WorkoutProgramsAndEditorPageView(
    programs: List<MobileWorkoutProgramItem>,
    activeProgram: MobileWorkoutProgramItem,
    runs: List<RunWorkout>,
    completedDays: Set<String>,
    onSelectProgram: (MobileWorkoutProgramItem) -> Unit,
    onToggleRunWorkout: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Strength & Gym Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(prog.discipline.accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(prog.discipline.icon, contentDescription = null, tint = prog.discipline.accent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(prog.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${prog.muscleOrZone} · ${prog.targetSets} sets × ${prog.targetReps} reps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // C25K Run Training Plans Section
        item {
            Text("C25K Running Plans (${runs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(runs) { run ->
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = run.completed,
                            onCheckedChange = { onToggleRunWorkout(run.stamp.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(run.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Week ${run.week} · Day ${run.day} · ${run.distanceKm ?: 3.5} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (run.completed) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            if (run.completed) "COMPLETED" else "PENDING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (run.completed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. ANALYTICS & PRS PAGE (Matching Wear Page 4)
// =============================================================================
@Composable
private fun WorkoutAnalyticsAndPrPageView(
    prList: List<MobilePersonalRecordEntry>,
    onOpenAddPr: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personal Records & Maxes (${prList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenAddPr) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add PR")
                }
            }
        }

        items(prList) { pr ->
            val progress = (pr.currentValue / pr.targetGoal).toFloat().coerceIn(0f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(pr.discipline.accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = pr.discipline.accent)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pr.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Previous: ${pr.previousBest.roundToInt()} ${pr.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("${pr.currentValue.roundToInt()} ${pr.unit}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("Goal: ${pr.targetGoal.roundToInt()} ${pr.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = pr.discipline.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutMetricPill(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
