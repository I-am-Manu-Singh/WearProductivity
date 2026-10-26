package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository

@Composable
fun MobileWorkoutScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val activeRuns = state.runs.filter { it.stamp.deletedAt == null }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("WORKOUTS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gym Session History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gym State Machine & Volume data logic goes here", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Completed exercises will sync here from watch.")
                }
            }
        }

        item {
            Text("Planned C25K Runs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(activeRuns) { run ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (run.completed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(run.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Week ${run.week} Day ${run.day} • ${run.intervals.size} intervals", style = MaterialTheme.typography.bodyMedium)
                    if (run.completed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("COMPLETED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}