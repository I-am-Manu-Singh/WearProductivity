package com.creativeblisslabs.wearproductivity.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.wearproductivity.mobile.ui.dashboard.DashboardScreen
import com.creativeblisslabs.wearproductivity.mobile.ui.habits.HabitsScreen
import com.creativeblisslabs.wearproductivity.mobile.ui.tasks.TasksScreen
import com.creativeblisslabs.wearproductivity.mobile.ui.theme.LifeOSTheme
import com.creativeblisslabs.wearproductivity.shared.LifeState

class MainActivity : ComponentActivity() {

    // Access the repository from the Application instance
    private val repository by lazy { (application as LifeOSApplication).repository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Collect the live state directly from the repository's state flow
            val currentState by repository.state.collectAsState()
            var currentTab by remember { mutableStateOf("Today") }

            LifeOSTheme {
                Scaffold(
                    bottomBar = {
                        LifeNavBar(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentTab) {
                            "Today" -> DashboardScreen(state = currentState, repository = repository)
                            "Habits" -> HabitsScreen(state = currentState, repository = repository)
                            "Tasks" -> TasksScreen(state = currentState, repository = repository)
                            else -> DashboardScreen(state = currentState, repository = repository)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LifeNavBar(currentTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        val tabs = listOf(
            NavTab("Today", Icons.Rounded.Dashboard),
            NavTab("Tasks", Icons.Rounded.TaskAlt),
            NavTab("Habits", Icons.Rounded.Checklist),
            NavTab("Health", Icons.Rounded.Timer)
        )

        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab.name,
                onClick = { onTabSelected(tab.name) },
                icon = { Icon(tab.icon, contentDescription = tab.name) },
                label = { Text(tab.name, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

data class NavTab(val name: String, val icon: ImageVector)
