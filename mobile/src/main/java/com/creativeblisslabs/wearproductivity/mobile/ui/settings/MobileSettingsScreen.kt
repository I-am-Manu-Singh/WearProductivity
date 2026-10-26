package com.creativeblisslabs.wearproductivity.mobile.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.auth.FirebaseAuthManager

@Composable
fun MobileSettingsScreen(repository: LifeRepository) {
    val state by repository.state.collectAsState()
    val user = FirebaseAuthManager.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Google Account & Cloud Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user?.email ?: "Not signed in", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* TODO: Trigger Google SignIn intent on Mobile */ }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (user != null) "Sign Out" else "Sign In with Google")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("App Theme Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Syncs with Wear OS", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
