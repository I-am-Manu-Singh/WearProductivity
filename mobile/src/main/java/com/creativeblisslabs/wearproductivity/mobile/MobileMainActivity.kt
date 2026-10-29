package com.creativeblisslabs.wearproductivity.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.mobile.sync.FirebaseMobileSyncManager

import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Restaurant

enum class MobileRoute(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Rounded.Dashboard),
    TASKS("Tasks", Icons.Rounded.FormatListBulleted),
    CALENDAR("Calendar", Icons.Rounded.Event),
    FOCUS("Focus", Icons.Rounded.Timer),
    FASTING("Fasting", Icons.Rounded.Restaurant),
    HEALTH("Health", Icons.Rounded.Favorite),
    WORKOUT("Workout", Icons.Rounded.DirectionsRun),
    FILES("Files", Icons.Rounded.Folder),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

class MobileMainActivity : ComponentActivity() {

    private lateinit var syncManager: FirebaseMobileSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                android.Manifest.permission.BODY_SENSORS,
                android.Manifest.permission.ACTIVITY_RECOGNITION,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            val missing = permissions.any { 
                androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED 
            }
            if (missing) {
                androidx.core.app.ActivityCompat.requestPermissions(this, permissions, 102)
            }
        }

        val repository = (application as LifeOSApplication).repository
        syncManager = FirebaseMobileSyncManager(repository, (application as LifeOSApplication).applicationScope)
        syncManager.startListening()

        setContent {
            val state by repository.state.collectAsState()
            com.creativeblisslabs.wearproductivity.mobile.ui.theme.LifeOSMobileTheme(
                paletteName = state.settings.themeColorPalette
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MobileAppNavigation(repository)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncManager.stopListening()
    }
}

@Composable
fun MobileAppNavigation(repository: LifeRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MobileRoute.entries.forEach { route ->
                    NavigationBarItem(
                        icon = { Icon(route.icon, contentDescription = route.label) },
                        label = { Text(route.label) },
                        selected = currentRoute == route.name,
                        onClick = {
                            navController.navigate(route.name) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MobileRoute.DASHBOARD.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MobileRoute.DASHBOARD.name) { com.creativeblisslabs.wearproductivity.mobile.ui.dashboard.MobileDashboardScreen(repository) }
            composable(MobileRoute.TASKS.name) { com.creativeblisslabs.wearproductivity.mobile.ui.tasks.MobileTasksScreen(repository) }
            composable(MobileRoute.CALENDAR.name) { com.creativeblisslabs.wearproductivity.mobile.ui.calendar.MobileCalendarScreen(repository) }
            composable(MobileRoute.FOCUS.name) { com.creativeblisslabs.wearproductivity.mobile.ui.focus.MobileFocusScreen(repository) }
            composable(MobileRoute.FASTING.name) { com.creativeblisslabs.wearproductivity.mobile.ui.fasting.MobileFastingScreen(repository) }
            composable(MobileRoute.HEALTH.name) { com.creativeblisslabs.wearproductivity.mobile.ui.dashboard.MobileHealthScreen(repository) }
            composable(MobileRoute.WORKOUT.name) { com.creativeblisslabs.wearproductivity.mobile.ui.dashboard.MobileWorkoutScreen(repository) }
            composable(MobileRoute.FILES.name) { com.creativeblisslabs.wearproductivity.mobile.ui.files.MobileFileExplorerScreen(repository) }
            composable(MobileRoute.SETTINGS.name) { com.creativeblisslabs.wearproductivity.mobile.ui.settings.MobileSettingsScreen(repository) }
        }
    }
}