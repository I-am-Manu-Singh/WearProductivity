package com.creativeblisslabs.wearproductivity.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.wear.ui.audio.WearAudioScreen
import com.creativeblisslabs.wearproductivity.wear.ui.calendar.WearCalendarScreen
import com.creativeblisslabs.wearproductivity.wear.ui.catalog.WearM3CatalogScreen
import com.creativeblisslabs.wearproductivity.wear.ui.dashboard.WearDashboardScreen
import com.creativeblisslabs.wearproductivity.wear.ui.fasting.WearFastingScreen
import com.creativeblisslabs.wearproductivity.wear.ui.files.WearFileExplorerScreen
import com.creativeblisslabs.wearproductivity.wear.ui.focus.WearFocusScreen
import com.creativeblisslabs.wearproductivity.wear.ui.habits.WearHabitsScreen
import com.creativeblisslabs.wearproductivity.wear.ui.settings.WearSettingsScreen
import com.creativeblisslabs.wearproductivity.wear.ui.theme.LifeOSWearTheme
import com.creativeblisslabs.wearproductivity.wear.ui.workout.WearWorkoutScreen
import timber.log.Timber

val LocalIsAmbient = compositionLocalOf { false }

class WearMainActivity : ComponentActivity() {

    private val isAmbientState = mutableStateOf(false)

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
                androidx.core.app.ActivityCompat.requestPermissions(this, permissions, 101)
            }
        }

        val repository = (application as LifeOSWearApplication).repository
        val exerciseRepository = (application as LifeOSWearApplication).exerciseRepository
        val mediaViewModel = androidx.lifecycle.ViewModelProvider(this)[com.creativeblisslabs.wearproductivity.wear.media.SharedMediaViewModel::class.java]
        
        setContent {
            CompositionLocalProvider(LocalIsAmbient provides isAmbientState.value) {
                LifeOSWearTheme {
                    val state by repository.state.collectAsState()
                    com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearBaselineViewport {
                        MainNavigation(state, repository, exerciseRepository, mediaViewModel)
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun WearAppPreview() {
    LifeOSWearTheme {
        val mockState = com.creativeblisslabs.wearproductivity.shared.LifeSeed.state()
        val mockStore = object : Any() {}
        val mockRepository = LifeRepository(store = mockStore as com.creativeblisslabs.wearproductivity.shared.LifeStore)
        val mockExerciseRepo = com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository(androidx.compose.ui.platform.LocalContext.current)

        com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearBaselineViewport {
            MainNavigation(state = mockState, repository = mockRepository, exerciseRepository = mockExerciseRepo)
        }
    }
}

@Composable
fun MainNavigation(
    state: com.creativeblisslabs.wearproductivity.shared.LifeState, 
    repository: LifeRepository,
    exerciseRepository: com.creativeblisslabs.wearproductivity.wear.workout.ExerciseRepository,
    mediaViewModel: com.creativeblisslabs.wearproductivity.wear.media.SharedMediaViewModel? = null
) {
    val pagerState = rememberPagerState(pageCount = { 11 })
    val isAmbient = LocalIsAmbient.current

    AppScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isAmbient
            ) { page ->
                when (page) {
                    0 -> WearDashboardScreen(state, repository)
                    1 -> WearCalendarScreen(state, repository, mediaViewModel)
                    2 -> WearHabitsScreen(state, repository, mediaViewModel)
                    3 -> WearFocusScreen(state, repository, mediaViewModel)
                    4 -> WearFastingScreen(state, repository, mediaViewModel)
                    5 -> WearWorkoutScreen(state, repository, exerciseRepository, mediaViewModel)
                    6 -> {
                        var workoutFlowState by remember {
                            mutableIntStateOf(0)
                        }

                        var activeRoutineIds by remember {
                            mutableStateOf<List<String>>(emptyList())
                        }

                        // Resolve the IDs returned by the setup screen.
                        val resolvedRoutineIds = remember(
                            activeRoutineIds,
                            exerciseRepository.allExercises
                        ) {
                            val availableIds =
                                exerciseRepository.allExercises.mapTo(mutableSetOf()) { it.id }

                            activeRoutineIds.filter { it in availableIds }
                        }

                        when (workoutFlowState) {
                            0 -> com.creativeblisslabs.wearproductivity.wear.ui.workout
                                .WearExerciseSetupScreen(exerciseRepository) { generatedIds ->
                                    activeRoutineIds = generatedIds
                                    workoutFlowState = 1

                                    Timber.tag("WorkoutNavigation")
                                        .d(
                                            "Generated workout: count=${generatedIds.size}, " +
                                                    "IDs=$generatedIds"
                                        )
                                }

                            1 -> {
                                com.creativeblisslabs.wearproductivity.wear.ui.workout
                                    .WearWorkoutListScreen(
                                        repository = exerciseRepository,
                                        routineIds = resolvedRoutineIds
                                    ) {
                                        workoutFlowState = 2
                                    }
                            }

                            2 -> {
                                WearWorkoutScreen(state, repository, exerciseRepository, mediaViewModel)
                            }
                        }
                    }
                    7 -> WearAudioScreen(state, repository, mediaViewModel)
                    8 -> WearFileExplorerScreen(state, repository)
                    9 -> WearM3CatalogScreen(state, repository)
                    10 -> WearSettingsScreen(state, repository)
                }
            }

            if (!isAmbient) {
                HorizontalPageIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}
