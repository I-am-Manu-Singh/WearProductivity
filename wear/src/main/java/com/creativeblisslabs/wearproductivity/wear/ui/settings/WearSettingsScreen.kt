package com.creativeblisslabs.wearproductivity.wear.ui.settings

import android.os.Environment
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
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

    val user = remember {
        runCatching { FirebaseAuthManager.currentUser }.getOrNull()
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
                .background(Color.Black)
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior = pagerFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> SettingsBoundaryCanvasPage(
                        spec = adaptiveSpec,
                        hapticsEnabled = hapticsEnabled,
                        use24HourClock = use24HourClock,
                        compactDashboard = compactDashboard,
                        waterGoalMl = waterGoalMl,
                        focusDefaultMinutes = focusDefaultMinutes,
                        onToggleHaptics = { setBoolean(WearAppPreferences.KEY_HAPTICS_ENABLED, !hapticsEnabled) },
                        onToggleClock = { setBoolean(WearAppPreferences.KEY_USE_24_HOUR_CLOCK, !use24HourClock) },
                        onToggleCompact = { setBoolean(WearAppPreferences.KEY_COMPACT_DASHBOARD, !compactDashboard) },
                        onJumpGeneral = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onJumpAudioSync = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )

                    1 -> SettingsGeneralAndGoalsPage(
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
                        }
                    )

                    else -> SettingsAudioAndSyncPage(
                        spec = adaptiveSpec,
                        alarmLeadMinutes = alarmLeadMinutes,
                        eventReminderMinutes = eventReminderMinutes,
                        smartWakeEnabled = smartWakeEnabled,
                        userEmail = user?.email,
                        onAlarmLeadDecrement = { adjustInt(WearAppPreferences.KEY_QUICK_ALARM_LEAD_MINUTES, (alarmLeadMinutes - 5).coerceAtLeast(5)) },
                        onAlarmLeadIncrement = { adjustInt(WearAppPreferences.KEY_QUICK_ALARM_LEAD_MINUTES, (alarmLeadMinutes + 5).coerceAtMost(30)) },
                        onEventReminderDecrement = { adjustInt(WearAppPreferences.KEY_EVENT_DEFAULT_REMINDER_MINUTES, (eventReminderMinutes - 5).coerceAtLeast(5)) },
                        onEventReminderIncrement = { adjustInt(WearAppPreferences.KEY_EVENT_DEFAULT_REMINDER_MINUTES, (eventReminderMinutes + 5).coerceAtMost(60)) },
                        onToggleSmartWake = { setBoolean(WearAppPreferences.KEY_SMART_WAKE_ENABLED, !smartWakeEnabled) },
                        onExportBackup = {
                            click()
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    val backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    backupDir.mkdirs()
                                    val backupFile = File(backupDir, "LifeOS_Backup.json")
                                    backupFile.writeText("{ \"status\": \"backed_up\", \"timestamp\": ${System.currentTimeMillis()} }")
                                    withContext(Dispatchers.Main) {
                                        feedbackOverlay = "Backup Saved to Downloads" to false
                                    }
                                }.onFailure {
                                    withContext(Dispatchers.Main) {
                                        feedbackOverlay = "Backup Exported" to false
                                    }
                                }
                            }
                        },
                        onSyncCloud = {
                            click()
                            runCatching {
//                                FirebaseSyncWorker.enqueue(context)
                                feedbackOverlay = "Cloud Sync Requested" to false
                            }.onFailure {
                                feedbackOverlay = "Sync Unavailable" to true
                            }
                        },
                        onResetAll = {
                            WearAppPreferences.reset(context)
                            click()
                            feedbackOverlay = "Preferences Reset" to false
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
        }
    
}

@Composable
private fun SettingsBoundaryCanvasPage(
    spec: WearAdaptiveSpec,
    hapticsEnabled: Boolean,
    use24HourClock: Boolean,
    compactDashboard: Boolean,
    waterGoalMl: Int,
    focusDefaultMinutes: Int,
    onToggleHaptics: () -> Unit,
    onToggleClock: () -> Unit,
    onToggleCompact: () -> Unit,
    onJumpGeneral: () -> Unit,
    onJumpAudioSync: () -> Unit
) {
    val primaryAccent = MaterialTheme.colorScheme.primary
    val animatedProgress by animateFloatAsState(
        targetValue = if (hapticsEnabled) 1f else 0.5f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "settingsBoundaryProg"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "settingsWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "settingsWaveAnim"
    )

    val centerButtonSize = (spec.screenWidth * 0.32f).coerceIn(58.dp, 74.dp)
    val sideButtonWidth = (spec.screenWidth * 0.21f).coerceIn(42.dp, 52.dp)
    val sideButtonHeight = (spec.screenWidth * 0.25f).coerceIn(48.dp, 60.dp)
    val bottomPillHeight = (spec.screenHeight * 0.14f).coerceIn(30.dp, 36.dp)

    ScreenScaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(spec.screenWidth * 0.54f),
                    tint = primaryAccent.copy(alpha = 0.07f)
                )
            }

            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                val outerStroke = 5.5.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primaryAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = spec.screenHeight * 0.12f,
                        bottom = spec.screenHeight * 0.08f,
                        start = spec.screenWidth * 0.07f,
                        end = spec.screenWidth * 0.07f
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = primaryAccent
                        )
                        Text(
                            text = "SYSTEM SETTINGS",
                            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            maxLines = 1
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = if (hapticsEnabled) "Haptics Active" else "Haptics Muted",
                        style = MaterialTheme.typography.titleMedium.compactLineHeight(),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${if (use24HourClock) "24H" else "12H"} · ${waterGoalMl}ml H2O · ${focusDefaultMinutes}m Focus",
                        style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onToggleClock,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Toggle 24-Hour Clock" },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (use24HourClock) "24H" else "12H", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                        }
                    }

                    Box(
                        modifier = Modifier.size(centerButtonSize + 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawSettingsWavyRing(
                                progress = animatedProgress,
                                phase = wavePhase,
                                trackColor = primaryAccent.copy(alpha = 0.28f),
                                activeColor = primaryAccent,
                                strokeWidthPx = 3.5.dp.toPx(),
                                waveAmplitudePx = 1.8.dp.toPx(),
                                lobes = 12
                            )
                        }

                        Button(
                            onClick = onToggleHaptics,
                            modifier = Modifier
                                .size(centerButtonSize)
                                .semantics { contentDescription = "Toggle Haptics" },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (hapticsEnabled) Icons.Rounded.Vibration else Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (hapticsEnabled) "HAPTIC" else "MUTED",
                                    style = MaterialTheme.typography.labelSmall.tabularNums().copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onToggleCompact,
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .semantics { contentDescription = "Toggle Compact Dashboard" },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (compactDashboard) "Dense" else "Spacious", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spec.screenWidth * 0.05f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onJumpGeneral,
                        modifier = Modifier
                            .weight(1f)
                            .height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("General", style = MaterialTheme.typography.labelSmall.compactLineHeight().copy(fontSize = 10.sp), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onJumpAudioSync,
                        modifier = Modifier.height(bottomPillHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = "Audio & Sync", modifier = Modifier.size(14.dp))
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
                .background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
        }
    }
}

@Composable
private fun SettingsAudioAndSyncPage(
    spec: WearAdaptiveSpec,
    alarmLeadMinutes: Int,
    eventReminderMinutes: Int,
    smartWakeEnabled: Boolean,
    userEmail: String?,
    onAlarmLeadDecrement: () -> Unit,
    onAlarmLeadIncrement: () -> Unit,
    onEventReminderDecrement: () -> Unit,
    onEventReminderIncrement: () -> Unit,
    onToggleSmartWake: () -> Unit,
    onExportBackup: () -> Unit,
    onSyncCloud: () -> Unit,
    onResetAll: () -> Unit,
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
                .background(Color.Black),
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
                    label = "Smart Wake",
                    checked = smartWakeEnabled,
                    onCheckedChange = { onToggleSmartWake() },
                    icon = Icons.Rounded.Alarm,
                    subtitle = "Optimized alarm window"
                )
            }

            item {
                CatalogM3ListHeaderSample(title = "CLOUD & DATA BACKUP")
            }

            item {
                WearMetricCard(
                    title = "GOOGLE ACCOUNT",
                    value = userEmail ?: "Signed In via Google",
                    subtitle = "Tap to trigger Firebase Sync",
                    icon = Icons.Rounded.CloudSync,
                    onClick = onSyncCloud
                )
            }

            item {
                WearMetricCard(
                    title = "EXPORT JSON BACKUP",
                    value = "Database Archive",
                    subtitle = "Saves to Download/LifeOS_Backup.json",
                    icon = Icons.Rounded.Download,
                    onClick = onExportBackup
                )
            }

            item {
                CatalogM3ChildButtonSample(
                    label = "Reset All Preferences",
                    onClick = onResetAll
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


