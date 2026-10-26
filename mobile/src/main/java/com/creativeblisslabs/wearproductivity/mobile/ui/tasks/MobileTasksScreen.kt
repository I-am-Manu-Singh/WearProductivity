package com.creativeblisslabs.wearproductivity.mobile.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTasksScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showTaskEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanban Board", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTaskEditor = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        }
    ) { padding ->
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            val stages = listOf("To Do", "In Progress", "Done")
            items(stages) { stage ->
                val stageTasks = state.activeTasks.filter { it.kanbanStage == stage }
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("${stageTasks.size}", color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(stageTasks) { task ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (task.complete) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                                    contentDescription = null,
                                                    tint = if (task.complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            scope.launch { repository.completeTask(task.stamp.id) }
                                                        }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (task.complete) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = task.project.ifBlank { "Inbox" },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            
                                            val priorityColor = when (task.priority) {
                                                3 -> MaterialTheme.colorScheme.error
                                                2 -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            Icon(Icons.Default.Add, contentDescription = "Priority", tint = priorityColor, modifier = Modifier.size(16.dp))
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

    if (showTaskEditor) {
        ModalBottomSheet(onDismissRequest = { showTaskEditor = false }) {
            var title by remember { mutableStateOf("") }
            var project by remember { mutableStateOf("") }
            var priority by remember { mutableIntStateOf(2) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("New Task", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = project,
                    onValueChange = { project = it },
                    label = { Text("Project / Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Priority Level", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = priority.toFloat(),
                        onValueChange = { priority = it.toInt() },
                        valueRange = 1f..3f,
                        steps = 1,
                        modifier = Modifier.width(150.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            repository.quickTask(title = title, project = project, priority = priority)
                            showTaskEditor = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank()
                ) {
                    Text("Save Task")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
