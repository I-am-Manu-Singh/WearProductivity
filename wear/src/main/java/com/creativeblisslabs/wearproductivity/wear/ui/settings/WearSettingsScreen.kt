package com.creativeblisslabs.wearproductivity.wear.ui.settings

import android.os.Environment
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.shared.auth.FirebaseAuthManager
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.sync.FirebaseSyncWorker
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.*
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearSettingsScreen(state: LifeState, repository: LifeRepository) {
    val context = LocalContext.current.applicationContext
    val haptic = rememberLifeOSHapticProvider()
    val scope = rememberCoroutineScope()

    val hapticsEnabled = rememberWearBooleanPreference(WearAppPreferences.KEY_HAPTICS_ENABLED, true)
    val use24HourClock = rememberWearBooleanPreference(WearAppPreferences.KEY_USE_24_HOUR_CLOCK, true)
    val compactDashboard = rememberWearBooleanPreference(WearAppPreferences.KEY_COMPACT_DASHBOARD, false)
    val waterGoalMl = rememberWearIntPreference(WearAppPreferences.KEY_WATER_GOAL_ML, 2000).coerceIn(1000, 5000)
    val focusDefaultMinutes = rememberWearIntPreference(WearAppPreferences.KEY_FOCUS_DEFAULT_MINUTES, 25).coerceIn(5, 180)
    val taskPriority = rememberWearIntPreference(WearAppPreferences.KEY_TASK_DEFAULT_PRIORITY, 2).coerceIn(1, 3)
    val alarmLeadMinutes = rememberWearIntPreference(WearAppPreferences.KEY_QUICK_ALARM_LEAD_MINUTES, 10).coerceIn(5, 30)
    val eventReminderMinutes = rememberWearIntPreference(WearAppPreferences.KEY_EVENT_DEFAULT_REMINDER_MINUTES, 10).coerceIn(5, 60)
    val smartWakeEnabled = rememberWearBooleanPreference(WearAppPreferences.KEY_SMART_WAKE_ENABLED, true)

    var feedbackOverlay by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showResetConfirmation by remember {
        mutableStateOf(false)
    }
    fun click() { if (hapticsEnabled) haptic.click() }
    fun setBoolean(key: String, value: Boolean) {
        click()
        WearAppPreferences.setBoolean(context, key, value)
        feedbackOverlay = "Setting Updated" to false
    }
    fun adjustInt(key: String, value: Int) {
        click()
        WearAppPreferences.setInt(context, key, value)
        feedbackOverlay = "Setting Updated" to false
    }

    var user by remember {
        mutableStateOf(com.creativeblisslabs.wearproductivity.shared.auth.FirebaseAuthManager.currentUser)
    }

    LaunchedEffect(Unit) {
        user = com.creativeblisslabs.wearproductivity.shared.auth.FirebaseAuthManager.currentUser
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    val adaptiveSpec = com.creativeblisslabs.wearproductivity.wear.ui.components.core.LocalWearAdaptiveSpec.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior = pagerFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    
                    0 -> SettingsGeneralAndGoalsPage(
                        spec = adaptiveSpec,
                        hapticsEnabled = hapticsEnabled,
                        use24HourClock = use24HourClock,
                        compactDashboard = compactDashboard,
                        waterGoalMl = waterGoalMl,
                        focusDefaultMinutes = focusDefaultMinutes,
                        taskPriority = taskPriority,
                        onToggleHaptics = { setBoolean(WearAppPreferences.KEY_HAPTICS_ENABLED, !hapticsEnabled) },
                        onToggleClock = { setBoolean(WearAppPreferences.KEY_USE_24_HOUR_CLOCK, !use24HourClock) },
                        onToggleCompact = { setBoolean(WearAppPreferences.KEY_COMPACT_DASHBOARD, !compactDashboard) },
                        onWaterDecrement = { if (waterGoalMl > 1000) adjustInt(WearAppPreferences.KEY_WATER_GOAL_ML, waterGoalMl - 250) },
                        onWaterIncrement = { if (waterGoalMl < 5000) adjustInt(WearAppPreferences.KEY_WATER_GOAL_ML, waterGoalMl + 250) },
                        onFocusDecrement = { adjustInt(WearAppPreferences.KEY_FOCUS_DEFAULT_MINUTES, (focusDefaultMinutes - 5).coerceAtLeast(5)) },
                        onFocusIncrement = { adjustInt(WearAppPreferences.KEY_FOCUS_DEFAULT_MINUTES, (focusDefaultMinutes + 5).coerceAtMost(180)) },
                        onJumpAudioSync = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onPriorityDecrease = {
                            adjustInt(
                                WearAppPreferences.KEY_TASK_DEFAULT_PRIORITY,
                                (taskPriority - 1).coerceAtLeast(1)
                            )
                        },
                        onPriorityIncrease = {
                            adjustInt(
                                WearAppPreferences.KEY_TASK_DEFAULT_PRIORITY,
                                (taskPriority + 1).coerceAtMost(3)
                            )
                        },
                    )

                    else -> SettingsAudioAndSyncPage(
                        spec = adaptiveSpec,
                        alarmLeadMinutes = alarmLeadMinutes,
                        eventReminderMinutes = eventReminderMinutes,
                        smartWakeEnabled = smartWakeEnabled,
                        userEmail = user?.email,
                        isSignedIn = user != null,
                        onAlarmLeadDecrement = { adjustInt(WearAppPreferences.KEY_QUICK_ALARM_LEAD_MINUTES, (alarmLeadMinutes - 5).coerceAtLeast(5)) },
                        onAlarmLeadIncrement = { adjustInt(WearAppPreferences.KEY_QUICK_ALARM_LEAD_MINUTES, (alarmLeadMinutes + 5).coerceAtMost(30)) },
                        onEventReminderDecrement = { adjustInt(WearAppPreferences.KEY_EVENT_DEFAULT_REMINDER_MINUTES, (eventReminderMinutes - 5).coerceAtLeast(5)) },
                        onEventReminderIncrement = { adjustInt(WearAppPreferences.KEY_EVENT_DEFAULT_REMINDER_MINUTES, (eventReminderMinutes + 5).coerceAtMost(60)) },
                        onToggleSmartWake = { setBoolean(WearAppPreferences
                            .KEY_SMART_WAKE_ENABLED, !smartWakeEnabled) },
                        onSyncCloud = {
                            click()

                            if (user == null) {
                                feedbackOverlay = "Sign In Required" to true
                            } else {
                                runCatching {
                                    FirebaseSyncWorker.enqueue(context)
                                }.onSuccess {
                                    feedbackOverlay = "Sync Request Queued" to false
                                }.onFailure { error ->
                                    feedbackOverlay = (
                                            error.message
                                                ?.take(60)
                                                ?.let { "Queue Failed: $it" }
                                                ?: "Sync Could Not Be Queued"
                                            ) to true
                                }
                            }
                        },
                        onRequestReset = {
                            click()
                            showResetConfirmation = true
                        },
                        onJumpGeneral = {
                            click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
            }

            VerticalPagerIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 3,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = adaptiveSpec.screenWidth * 0.02f)
            )

            feedbackOverlay?.let { (msg, isError) ->
                ViewportConstrainedSettingsConfirmation(
                    label = msg,
                    isError = isError,
                    onDismiss = { feedbackOverlay = null }
                )
            }
            if (showResetConfirmation) {
                Dialog(
                    onDismissRequest = {
                        showResetConfirmation = false
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )

                        Text(
                            text = "Reset Preferences?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "All saved Wear preferences will return to their defaults. Your app data is not intended to be deleted by this action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    click()
                                    showResetConfirmation = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    click()
                                    WearAppPreferences.reset(context)
                                    showResetConfirmation = false
                                    feedbackOverlay = "Preferences Reset" to false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
        }
    
}


private fun DrawScope.drawSettingsWavyRing(
    progress: Float,
    phase: Float,
    trackColor: Color,
    activeColor: Color,
    strokeWidthPx: Float,
    waveAmplitudePx: Float,
    lobes: Int
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseRadius = (minOf(size.width, size.height) / 2f) - waveAmplitudePx - (strokeWidthPx / 2f)
    val totalSteps = 160

    fun buildPath(maxStep: Int): Path {
        val path = Path()
        for (step in 0..maxStep) {
            val fraction = step.toFloat() / totalSteps.toFloat()
            val angleRad = (-Math.PI / 2.0) + (fraction * 2.0 * Math.PI)
            val waveOffset = waveAmplitudePx * sin((lobes * angleRad) + phase).toFloat()
            val r = baseRadius + waveOffset
            val x = center.x + (r * cos(angleRad)).toFloat()
            val y = center.y + (r * sin(angleRad)).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    drawPath(
        path = buildPath(totalSteps),
        color = trackColor,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val activeSteps = (totalSteps * progress.coerceIn(0.04f, 1f)).toInt()
    if (activeSteps > 0) {
        drawPath(
            path = buildPath(activeSteps),
            color = activeColor,
            style = Stroke(width = strokeWidthPx * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun SettingsGeneralAndGoalsPage(
    spec: WearAdaptiveSpec,
    hapticsEnabled: Boolean,
    use24HourClock: Boolean,
    compactDashboard: Boolean,
    waterGoalMl: Int,
    focusDefaultMinutes: Int,
    onPriorityDecrease: () -> Unit,
    onPriorityIncrease: () -> Unit,
    taskPriority: Int,
    onToggleHaptics: () -> Unit,
    onToggleClock: () -> Unit,
    onToggleCompact: () -> Unit,
    onWaterDecrement: () -> Unit,
    onWaterIncrement: () -> Unit,
    onFocusDecrement: () -> Unit,
    onFocusIncrement: () -> Unit,
    onJumpAudioSync: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpAudioSync,
                label = "Audio & Sync",
                icon = Icons.Rounded.GraphicEq
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement
            = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "GENERAL DECK",
                    badgeText = "PREFS"
                )
            }

            item {
                CatalogM3ListHeaderSample(title = "INTERACTION & DISPLAY")
            }

            item {
                WearExpressiveSwitchRow(
                    label = "Haptics Vibration",
                    checked = hapticsEnabled,
                    onCheckedChange = { onToggleHaptics() },
                    icon = Icons.Rounded.Vibration,
                    subtitle = if (hapticsEnabled) "Tactile feedback active" else "Muted"
                )
            }

            item {
                WearExpressiveSwitchRow(
                    label = "24-Hour Clock",
                    checked = use24HourClock,
                    onCheckedChange = { onToggleClock() },
                    icon = Icons.Rounded.Timer,
                    subtitle = if (use24HourClock) "Format 18:45" else "Format 6:45 PM"
                )
            }

            item {
                WearExpressiveSwitchRow(
                    label = "Compact Dashboard",
                    checked = compactDashboard,
                    onCheckedChange = { onToggleCompact() },
                    icon = Icons.Rounded.Dashboard,
                    subtitle = "Denser card layout"
                )
            }

            item {
                CatalogM3ListHeaderSample(title = "DAILY TARGET GOALS")
            }

            item {
                WearExpressiveHorizontalSlider(
                    title = "Water Intake Goal",
                    valueText = "$waterGoalMl ml",
                    onValueDecrease = onWaterDecrement,
                    onValueIncrease = onWaterIncrement,
                    canDecrease = waterGoalMl > 1000,
                    canIncrease = waterGoalMl < 5000
                )
            }

            item {
                WearExpressiveHorizontalSlider(
                    title = "Default Focus Session",
                    valueText = "$focusDefaultMinutes min",
                    onValueDecrease = onFocusDecrement,
                    onValueIncrease = onFocusIncrement,
                    canDecrease = focusDefaultMinutes > 5,
                    canIncrease = focusDefaultMinutes < 180
                )
            }
            item {
                WearExpressiveHorizontalSlider(
                    title = "Default Task Priority",
                    valueText = when (taskPriority) {
                        1 -> "Low"
                        2 -> "Normal"
                        else -> "High"
                    },
                    onValueDecrease = onPriorityDecrease,
                    onValueIncrease = onPriorityIncrease,
                    canDecrease = taskPriority > 1,
                    canIncrease = taskPriority < 3
                )
            }
        }
    }
}

@Composable
private fun SettingsAudioAndSyncPage(
    spec: WearAdaptiveSpec,
    alarmLeadMinutes: Int,
    eventReminderMinutes: Int,
    smartWakeEnabled: Boolean,
    isSignedIn: Boolean,
    userEmail: String?,
    onAlarmLeadDecrement: () -> Unit,
    onAlarmLeadIncrement: () -> Unit,
    onEventReminderDecrement: () -> Unit,
    onEventReminderIncrement: () -> Unit,
    onToggleSmartWake: () -> Unit,
    onSyncCloud: () -> Unit,
    onRequestReset: () -> Unit,
    onJumpGeneral: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = columnState,
        timeText = { TimeText() },
        edgeButton = {
            WearAnchoredEdgeButton(
                onClick = onJumpGeneral,
                label = "General Deck",
                icon = Icons.Rounded.Tune
            )
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = spec.outerHorizontalMargin,
                end = spec.outerHorizontalMargin
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WearExpressiveHeader(
                    title = "SYSTEM & SYNC",
                    badgeText = "CLOUD"
                )
            }

            item {
                CatalogM3ListHeaderSample(title = "TIMING & ALARMS")
            }

            item {
                WearExpressiveHorizontalSlider(
                    title = "Alarm Lead Time",
                    valueText = "$alarmLeadMinutes min",
                    onValueDecrease = onAlarmLeadDecrement,
                    onValueIncrease = onAlarmLeadIncrement,
                    canDecrease = alarmLeadMinutes > 5,
                    canIncrease = alarmLeadMinutes < 30
                )
            }

            item {
                WearExpressiveHorizontalSlider(
                    title = "Event Reminder",
                    valueText = "$eventReminderMinutes min",
                    onValueDecrease = onEventReminderDecrement,
                    onValueIncrease = onEventReminderIncrement,
                    canDecrease = eventReminderMinutes > 5,
                    canIncrease = eventReminderMinutes < 60
                )
            }

            item {
                WearExpressiveSwitchRow(
                    label = "Smart Wake Preference",
                    checked = smartWakeEnabled,
                    onCheckedChange = { onToggleSmartWake() },
                    icon = Icons.Rounded.Alarm,
                    subtitle = if (smartWakeEnabled) {
                        "Enabled · Scheduling not connected"
                    } else {
                        "Disabled"
                    }
                )
            }
            item {
                CatalogM3ListHeaderSample(title = "CLOUD & DATA BACKUP")
            }
            item {
                WearMetricCard(
                    title = "GOOGLE ACCOUNT",
                    value = when {
                        !isSignedIn -> "Not Signed In"
                        !userEmail.isNullOrBlank() -> userEmail
                        else -> "Signed In"
                    },
                    subtitle = when {
                        !isSignedIn -> "Sign in to enable cloud sync"
                        else -> "Tap to queue Firebase Sync"
                    },
                    icon = Icons.Rounded.CloudSync,
                    onClick = onSyncCloud
                )
            }


            item {
                CatalogM3ChildButtonSample(
                    label = "Reset Wear Preferences",
                    onClick = onRequestReset
                )
            }
        }
    }
}

@Composable
private fun ViewportConstrainedSettingsConfirmation(
    label: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

    LaunchedEffect(label) {
        delay(1_800L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        CurvedLayout(modifier = Modifier.fillMaxSize()) {
            confirmationDialogCurvedText(text = label, style = curvedTextStyle)
        }
        if (isError) {
            ConfirmationDialogDefaults.FailureIcon()
        } else {
            ConfirmationDialogDefaults.SuccessIcon()
        }
    }
}


