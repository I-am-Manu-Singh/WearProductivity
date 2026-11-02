package com.creativeblisslabs.wearproductivity.mobile.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class MobileWorkoutTab(val label: String, val icon: ImageVector) {
    ACTIVE("Active / Programs", Icons.Rounded.FitnessCenter),
    RUNS("C25K Runs", Icons.Rounded.DirectionsRun),
    RECORDS("PRs & Strength", Icons.Rounded.EmojiEvents)
}

data class MobilePR(
    val exercise: String,
    val best: String,
    val target: String,
    val discipline: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileWorkoutScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MobileWorkoutTab.ACTIVE) }
    val activeRuns = remember(state.runs) { state.runs.filter { it.stamp.deletedAt == null } }
    val completedRuns = activeRuns.count { it.completed }

    var statusNotice by remember { mutableStateOf<String?>(null) }

    fun sendWearWorkoutCommand(cmd: String) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                nodes.firstOrNull()?.let { node ->
                    Wearable.getMessageClient(context).sendMessage(
                        node.id,
                        "/lifeos/workout/$cmd",
                        cmd.toByteArray()
                    ).await()
                    statusNotice = "Command '$cmd' delivered to Wear OS"
                } ?: run {
                    statusNotice = "No Watch connected"
                }
            } catch (_: Exception) {
                statusNotice = "Delivery failed"
            }
        }
    }

    val prList = remember {
        listOf(
            MobilePR("Barbell Bench Press", "100 kg", "120 kg", "Gym Strength"),
            MobilePR("Barbell Back Squat", "140 kg", "160 kg", "Gym Strength"),
            MobilePR("Conventional Deadlift", "175 kg", "200 kg", "Gym Strength"),
            MobilePR("5K Outdoor Pace", "4:45 /km", "4:15 /km", "Running"),
            MobilePR("Pull-Ups (Bodyweight)", "18 reps", "25 reps", "Calisthenics")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workouts & Training", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs matching Wear OS Disciplines
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                MobileWorkoutTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(20.dp)) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (selectedTab) {
                    MobileWorkoutTab.ACTIVE -> {
                        // 1. Wear OS Live Workout Remote
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Wear OS Training State", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Start or control guided workout sessions on watch", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Icon(Icons.Rounded.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { sendWearWorkoutCommand("start_gym") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Gym")
                                        }
                                        OutlinedButton(
                                            onClick = { sendWearWorkoutCommand("start_run") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.DirectionsRun, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Run")
                                        }
                                    }

                                    statusNotice?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // 2. Training Disciplines Overview
                        item {
                            Text("Supported Disciplines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DisciplineTile("Gym Strength", "Heavy Compounds", Icons.Rounded.FitnessCenter, Color(0xFFFF9A7A), Modifier.weight(1f))
                                DisciplineTile("Running", "C25K Cardio", Icons.Rounded.DirectionsRun, Color(0xFF79E2B0), Modifier.weight(1f))
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DisciplineTile("Swimming", "Laps & Heart Rate", Icons.Rounded.Pool, Color(0xFF68B6FF), Modifier.weight(1f))
                                DisciplineTile("Calisthenics", "Bodyweight Flow", Icons.Rounded.SportsGymnastics, Color(0xFFBBA5FF), Modifier.weight(1f))
                            }
                        }
                    }

                    MobileWorkoutTab.RUNS -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("C25K Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("$completedRuns/${activeRuns.size} Completed", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val prog = if (activeRuns.isNotEmpty()) completedRuns.toFloat() / activeRuns.size else 0f
                                    LinearProgressIndicator(
                                        progress = { prog },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            Text("Interval Programs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (activeRuns.isEmpty()) {
                            item {
                                Text("No program runs found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(activeRuns) { run ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (run.completed) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(run.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Week ${run.week} · Day ${run.day} · ${run.intervals.size} intervals",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        FilledIconToggleButton(
                                            checked = run.completed,
                                            onCheckedChange = {
                                                scope.launch {
                                                    repository.toggleRun(run.stamp.id)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                if (run.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = "Toggle completion"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MobileWorkoutTab.RECORDS -> {
                        item {
                            Text("Personal Records & Maxes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        items(prList) { pr ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(pr.exercise, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(pr.discipline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(pr.best, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        Text("Goal: ${pr.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun DisciplineTile(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
