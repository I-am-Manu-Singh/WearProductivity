package com.creativeblisslabs.wearproductivity.mobile.ui.fasting

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
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
fun MobileFastingScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val fast = state.fast

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("INTERMITTENT FASTING", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Restaurant, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Plan: ${fast.plan}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (fast.active) "Fasting Active" else "Not Fasting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { fast.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { /* TODO: Toggle fast */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (fast.active) "End Fast" else "Start Fasting")
                }
            }
        }
    }
}
