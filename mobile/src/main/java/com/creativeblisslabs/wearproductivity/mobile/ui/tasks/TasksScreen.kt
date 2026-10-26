package com.creativeblisslabs.wearproductivity.mobile.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.LifeTask
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    state: LifeState,
    repository: LifeRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.TaskAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Tasks & Lists",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    coroutineScope.launch {
                        repository.quickTask("New Task")
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Task", fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.width(8.dp))
                VerticalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Rounded.Mic, contentDescription = "Voice Task", modifier = Modifier.size(20.dp))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FilterChipBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                taskCount = state.activeTasks.size
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val todayTasks = state.activeTasks.filter { !it.complete }
                val completedTasks = state.activeTasks.filter { it.complete }

                if (todayTasks.isNotEmpty()) {
                    item {
                        SectionHeader("Today's Focus", "${todayTasks.size} of ${state.activeTasks.size}")
                    }
                    items(todayTasks) { task ->
                        TaskItem(
                            task = task,
                            onToggle = {
                                coroutineScope.launch {
                                    repository.completeTask(task.stamp.id)
                                }
                            }
                        )
                    }
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader("Completed", completedTasks.size.toString())
                    }
                    items(completedTasks) { task ->
                        TaskItem(
                            task = task,
                            onToggle = {
                                coroutineScope.launch {
                                    repository.completeTask(task.stamp.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    taskCount: Int
) {
    val filters = listOf("All", "Today", "Work", "Groceries", "Voice Notes")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = when (filter) {
                        "All" -> Icons.Rounded.DoneAll
                        "Today" -> Icons.Rounded.Today
                        "Work" -> Icons.Rounded.BusinessCenter
                        "Groceries" -> Icons.Rounded.ShoppingBag
                        "Voice Notes" -> Icons.Rounded.Mic
                        else -> Icons.AutoMirrored.Rounded.Label
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        filter,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filter == "All") {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        ) {
                            Text(
                                taskCount.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    count,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        TextButton(onClick = { /* TODO */ }) {
            Text("Sort by urgency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun TaskItem(
    task: LifeTask,
    onToggle: () -> Unit
) {
    val backgroundColor = if (task.complete) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainer

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (task.complete) Modifier.alpha(0.6f) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            CustomCheckbox(
                checked = task.complete,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.complete) TextDecoration.LineThrough else null,
                    color = if (task.complete) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (task.scheduledTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (task.hapticEnabled) Icons.Rounded.Vibration else Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${task.scheduledTime}${if (task.hapticEnabled) " Haptic" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (task.syncStatus == "Synced") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (task.complete) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Watch,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Done via Watch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            if (task.project.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = if (task.project == "Design") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "#${task.project}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (task.project == "Design") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val checkboxColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.secondary else Color.Transparent
    )
    val borderColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    )
    val scale by animateFloatAsState(if (checked) 1f else 0.8f)

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(checkboxColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}
