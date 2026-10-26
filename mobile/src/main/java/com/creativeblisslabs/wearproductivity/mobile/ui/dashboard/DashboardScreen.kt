package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.mobile.ui.components.TempoPicker
import com.creativeblisslabs.wearproductivity.mobile.ui.components.WaterTracker
import com.creativeblisslabs.wearproductivity.mobile.ui.components.core.AnimatedIcon
import com.creativeblisslabs.wearproductivity.mobile.ui.components.core.ConnectedButtonGroup
import com.creativeblisslabs.wearproductivity.mobile.ui.components.core.VerticalButtonGroup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(state: com.creativeblisslabs.wearproductivity.shared.LifeState, repository: com.creativeblisslabs.wearproductivity.shared.LifeRepository) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("Productivity") }
    var filterState by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "LifeOS",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        AnimatedIcon(
                            Icons.Rounded.Notifications,
                            contentDescription = "Notifications",
                            active = false
                        )
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        AnimatedIcon(
                            Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            active = false
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    coroutineScope.launch {
                        repository.quickTask("New Task from Mobile")
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardSummaryCard(state.today)
            }
            item {
                ConnectedButtonGroup(
                    options = listOf("All", "Pending", "Done"),
                    checked = filterState,
                    onCheckedChange = { filterState = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TempoPicker(
                        value = state.focus.minutes,
                        label = "Focus Session",
                        onValueChangeDelta = { delta ->
                            // Update focus minutes
                        },
                        onClick = { 
                            coroutineScope.launch {
                                repository.startFocusSession(state.focus.minutes, com.creativeblisslabs.wearproductivity.shared.FocusType.Pomodoro, "General")
                            }
                        }
                    )

                    WaterTracker(
                        level = state.wellness.waterGlasses * 250,
                        goal = 2000,
                        onAddWater = {
                            coroutineScope.launch {
                                repository.addWater()
                            }
                        }
                    )
                }
            }
            item {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                VerticalButtonGroup(
                    options = listOf("Productivity", "Wellness", "Routine"),
                    selected = selectedCategory,
                    onSelectedChange = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DashboardSummaryCard(today: com.creativeblisslabs.wearproductivity.shared.TodaySummary) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Good Morning!",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "You've completed ${today.tasksDone} of ${today.tasksTotal} tasks today.",
                style = MaterialTheme.typography.headlineSmall
            )
            LinearProgressIndicator(
                progress = { if (today.tasksTotal > 0) today.tasksDone.toFloat() / today.tasksTotal else 0f },
                modifier = Modifier.fillMaxWidth(),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun DashboardCategoryCard(title: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* TODO */ }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "View Details",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
