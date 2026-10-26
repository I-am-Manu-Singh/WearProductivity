package com.creativeblisslabs.wearproductivity.mobile.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState

@Composable
fun MobileDashboardScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("DASHBOARD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.today.tasksDone} / ${state.today.tasksTotal}", style = MaterialTheme.typography.headlineSmall)
                        Text("Tasks", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.today.habitsDone} / ${state.today.habitsTotal}", style = MaterialTheme.typography.headlineSmall)
                        Text("Habits", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.today.totalCaloriesConsumed} kcal", style = MaterialTheme.typography.headlineSmall)
                        Text("Diet", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
