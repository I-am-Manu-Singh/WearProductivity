package com.creativeblisslabs.wearproductivity.mobile.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileCalendarScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val events = state.events.filter { it.stamp.deletedAt == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar & Events", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO: Import from Google Calendar Provider */ }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Calendar")
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
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upcoming Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (events.isEmpty()) {
                item {
                    Text(
                        "No events scheduled. Tap Sync to import from your device calendar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(events) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${event.timeRange} • ${event.location.ifBlank { "No location" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                                    Text("Attending")
                                }
                                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
