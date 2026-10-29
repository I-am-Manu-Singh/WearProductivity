package com.creativeblisslabs.wearproductivity.mobile.ui.focus

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository

@Composable
fun MobileFocusScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val focus = state.focus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("POMODORO FOCUS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "${focus.remainingMinutes}:00",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (focus.active) "Session Active" else "Ready to Focus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { /* TODO: Toggle focus session */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (focus.active) "Pause Session" else "Start 25m Focus")
                }
            }
        }
    }
}
