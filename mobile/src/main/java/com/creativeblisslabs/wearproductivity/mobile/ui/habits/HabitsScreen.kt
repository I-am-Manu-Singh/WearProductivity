package com.creativeblisslabs.wearproductivity.mobile.ui.habits

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    state: LifeState,
    repository: LifeRepository
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LifeOS", style = MaterialTheme.typography.titleMedium)
                        Text("HABITS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wrist Sync Banner
            item {
                WristSyncBanner()
            }

            // Consistency Rhythm
            item {
                ConsistencyRhythmCard(state.today)
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Daily Habits",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "${state.today.habitsDone} of ${state.today.habitsTotal} Done",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Habits List
            items(state.activeHabits) { habit ->
                HabitCard(
                    habit = habit,
                    onCheck = {
                        coroutineScope.launch {
                            repository.updateHabitProgress(habit.stamp.id, habit.currentValue + 1)
                        }
                    },
                    onToggleTimer = {
                        coroutineScope.launch {
                            repository.toggleHabitTimer(habit.stamp.id)
                        }
                    }
                )
            }
            
            // Routines Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Wrist Routines",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(state.activeRoutines) { routine ->
                RoutineCard(
                    routine = routine,
                    onRun = {
                        coroutineScope.launch {
                            repository.toggleRoutine(routine.stamp.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WristSyncBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Wrist Complication Sync Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                }
                Text(
                    "Tap any habit complication on your Pixel Watch 3 to check off instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* Haptic test */ }) {
                Icon(Icons.Rounded.Sensors, contentDescription = null)
            }
        }
    }
}

@Composable
fun ConsistencyRhythmCard(today: TodaySummary) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Consistency Rhythm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "92%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Top 5% this week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "24d Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                days.forEachIndexed { index, day ->
                    val isToday = index == 5 // Saturday in example
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            day,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (index < 5) MaterialTheme.colorScheme.secondary
                                    else if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < 5) {
                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondary)
                            } else if (isToday) {
                                Text("3/4", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                Text("-", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    onCheck: () -> Unit,
    onToggleTimer: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (habit.doneToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getHabitIcon(habit.name),
                            contentDescription = null,
                            tint = if (habit.doneToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (habit.doneToday) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            ) {
                                Text(
                                    "${habit.streak}d streak",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (habit.doneToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (habit.isTimerActive) {
                            Text(
                                "Active (${habit.remainingSeconds / 60}m remaining)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                if (habit.targetValue > 1.0 || habit.remainingSeconds > 0) {
                     Button(
                        onClick = onToggleTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (habit.isTimerActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (habit.isTimerActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Rounded.Timelapse, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (habit.isTimerActive) "Active" else "Start", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    IconButton(
                        onClick = onCheck,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (habit.doneToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = null,
                            tint = if (habit.doneToday) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val progress = (habit.currentValue / habit.targetValue).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${habit.currentValue.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (progress >= 1f) "Target Met" else "${(progress * 100).toInt()}% Met",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (progress >= 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (progress >= 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun RoutineCard(routine: RoutineTask, onRun: () -> Unit) {
     Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.WbTwilight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    routine.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${routine.startTime} - ${routine.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onRun,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (routine.doneToday) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (routine.doneToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    if (routine.doneToday) Icons.Rounded.Check else Icons.Rounded.Watch,
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (routine.doneToday) "Done" else "Run", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

fun getHabitIcon(name: String): ImageVector {
    return when {
        name.contains("Water", true) || name.contains("Hydration", true) -> Icons.Rounded.WaterDrop
        name.contains("Meditation", true) || name.contains("Zen", true) -> Icons.Rounded.SelfImprovement
        name.contains("Walk", true) || name.contains("Steps", true) -> Icons.Rounded.DirectionsWalk
        name.contains("Read", true) || name.contains("Book", true) -> Icons.Rounded.MenuBook
        else -> Icons.Rounded.CheckCircle
    }
}
