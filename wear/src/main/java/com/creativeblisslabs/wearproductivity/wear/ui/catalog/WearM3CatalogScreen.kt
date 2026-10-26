package com.creativeblisslabs.wearproductivity.wear.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.LifeState
import com.creativeblisslabs.wearproductivity.wear.catalog.m3.*
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.DurationPicker
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.LocalWearAdaptiveSpec
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearActionButtonGroup
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearAdaptiveSpec
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearAnchoredEdgeButton
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearBaselineViewport
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveCheckboxRow
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHeader
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveHorizontalSlider
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressivePrevNextRow
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearExpressiveSwitchRow
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearHeatMapCard
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearMetricCard
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearStatusBadge
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearSwipeableActionCard
import com.creativeblisslabs.wearproductivity.wear.ui.components.core.WearTransformingCard
import com.creativeblisslabs.wearproductivity.wear.ui.util.rememberLifeOSHapticProvider
import kotlinx.coroutines.delay


private enum class CatalogOverlayState {
    NONE,
    HOURS_MINUTES_PICKER,
    DATE_PICKER,
    TIME_PICKER,
    ALERT_DIALOG,
    CONFIRMATION_SUCCESS,
    CONFIRMATION_GENERIC,
    CONFIRMATION_FAILURE,
    CONFIRMATION_AUTH,
    OPEN_ON_PHONE
}

/**
 * In-Viewport Constrained Confirmation / OpenOnPhone Overlay so it never escapes
 * the 416.dp circular watch bezel on mobile phone previews.
 */
@Composable
private fun ViewportConstrainedCurvedOverlay(
    curvedLabel: String,
    isOpenOnPhone: Boolean = false,
    onDismiss: () -> Unit,
    centerContent: @Composable () -> Unit
) {

    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle
    val phoneStyle = OpenOnPhoneDialogDefaults.curvedTextStyle

    LaunchedEffect(Unit) {
        delay(2200)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        CurvedLayout(modifier = Modifier.fillMaxSize()) {
            if (isOpenOnPhone) {
                openOnPhoneDialogCurvedText(text = curvedLabel, style = phoneStyle)
            } else {
                confirmationDialogCurvedText(text = curvedLabel, style = curvedTextStyle)
            }
        }
        centerContent()
    }
}

@Composable
fun WearM3CatalogScreen(state: LifeState, repository: LifeRepository) {
    val haptic = rememberLifeOSHapticProvider()
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    var switchChecked by remember { mutableStateOf(true) }
    var checkboxChecked by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableIntStateOf(25) }
    var selectedTabIdx by remember { mutableIntStateOf(0) }
    var waterGlasses by remember { mutableIntStateOf(6) }

    var activeOverlay by remember { mutableStateOf(CatalogOverlayState.NONE) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    WearBaselineViewport {
        // 1. Retrieve the spec from CompositionLocal inside the block
        val coreSpec = LocalWearAdaptiveSpec.current
        val adaptiveSpec = WearAdaptiveSpec(
            screenWidth = coreSpec.screenWidth,
            screenHeight = coreSpec.screenHeight,
            isLargeScreen = coreSpec.isLargeScreen,
            isSmallScreen = coreSpec.isSmallScreen,
            outerHorizontalMargin = coreSpec.outerHorizontalMargin,
            topBottomSlotHorizontalMargin = coreSpec.topBottomSlotHorizontalMargin,
            topSlotPadding = coreSpec.topSlotPadding,
            bottomEdgeButtonPadding = coreSpec.bottomEdgeButtonPadding
        )
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Base Scrollable Showcase Screen
            ScreenScaffold(
                scrollState = columnState,
                timeText = { TimeText() },
                edgeButton = {
                    WearAnchoredEdgeButton(
                        onClick = { activeOverlay = CatalogOverlayState.HOURS_MINUTES_PICKER },
                        label = "Test Picker",
                        icon = Icons.Default.Timer
                    )
                }
            ) { contentPadding ->
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding(),
                        start = adaptiveSpec.outerHorizontalMargin,
                        end = adaptiveSpec.outerHorizontalMargin
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Catalog Title Header
                    item {
                        WearExpressiveHeader(
                            title = "M3 CATALOG",
                            badgeText = "SHOWCASE"
                        )
                    }

                    // Section 1: Expressive Badges & Status
                    item {
                        WearExpressiveHeader(title = "STATUS BADGES", icon = Icons.Default.AutoAwesome)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WearStatusBadge(text = "3D Bronze")
                            WearStatusBadge(text = "7D Bronze")
                            WearStatusBadge(text = "30D Silver")
                            WearStatusBadge(text = "100D Gold")
                        }
                    }

                    // Section 2: Action Button Groups & Quick Actions
                    item {
                        WearExpressiveHeader(title = "BUTTON GROUPS & ACTIONS", icon = Icons.Default.Layers)
                    }

                    item {
                        WearActionButtonGroup(
                            options = listOf("Option A", "Option B", "Option C"),
                            selectedIndex = selectedTabIdx,
                            onOptionSelected = { idx ->
                                selectedTabIdx = idx
                                haptic.click()
                            }
                        )
                    }

                    item {
                        Button(
                            onClick = { activeOverlay = CatalogOverlayState.CONFIRMATION_SUCCESS },
                            colors = ButtonDefaults.buttonColors(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Session",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Focus Session")
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactButton(
                                onClick = {
                                    selectedTabIdx = (selectedTabIdx - 1).coerceAtLeast(0)
                                    haptic.click()
                                },
                                icon = { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                            )
                            CompactButton(
                                onClick = {
                                    waterGlasses++
                                    haptic.click()
                                },
                                icon = { Icon(Icons.Default.Add, contentDescription = "Add Water") }
                            ) { Text("${waterGlasses}x") }
                            CompactButton(
                                onClick = {
                                    waterGlasses = 6
                                    sliderValue = 25
                                    haptic.click()
                                },
                                icon = { Icon(Icons.Default.Refresh, contentDescription = "Reset") }
                            )
                            CompactButton(
                                onClick = {
                                    selectedTabIdx = (selectedTabIdx + 1).coerceAtMost(2)
                                    haptic.click()
                                },
                                icon = { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                            )
                        }
                    }

                    // Upstream Buttons Showcase (All 12 Button Composables)
                    item { WearExpressiveHeader(title = "UPSTREAM M3 BUTTONS", icon = Icons.Default.Layers) }
                    item { CatalogM3ButtonSample() }
                    item { CatalogM3ButtonLoadingSample() }
                    item { CatalogM3FilledTonalButtonSample() }
                    item { CatalogM3OutlinedButtonSample() }
                    item { CatalogM3ChildButtonSample() }
                    item { CatalogM3IconButtonsRowSample() }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CatalogM3CompactButtonSample()
                            CatalogM3FilledIconButtonSample()
                            CatalogM3FilledTonalIconButtonSample()
                            CatalogM3OutlinedIconButtonSample()
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CatalogM3IconButtonSample()
                            CatalogM3IconToggleButtonSample()
                            CatalogM3TextButtonSample()
                            CatalogM3TextToggleButtonSample()
                        }
                    }
                    item { CatalogM3EdgeButtonSample(onClick = { activeOverlay = CatalogOverlayState.HOURS_MINUTES_PICKER }) }

                    // Upstream Cards & Custom Expressive Cards
                    item { WearExpressiveHeader(title = "UPSTREAM & EXPRESSIVE CARDS", icon = Icons.Default.Palette) }
                    item { CatalogM3CardSample() }
                    item { CatalogM3OutlinedCardSample() }
                    item { CatalogM3TitleCardSample() }
                    item { CatalogM3AppCardSample() }
                    item {
                        WearMetricCard(
                            title = "HYDRATION GOAL",
                            value = "$waterGlasses / 8 Cups",
                            subtitle = "Tap + above to log intake",
                            icon = Icons.Default.WaterDrop,
                            onClick = {
                                // Handle card click or navigation here
                            }
                        )
                    }
                    item {
                        WearTransformingCard(
                            transformationSpec = transformationSpec,
                            onClick = { haptic.click() }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("WearTransformingCard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Dynamic M3 surface transformation card", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    item {
                        WearSwipeableActionCard(
                            onCardClick = { /* Handle card click */ },
                            onEditClick = {
                                showConfirmationDialog = true
                                haptic.click()
                            },
                            onDeleteClick = { /* Handle delete action */ }
                        ) {
                            // This is your content block
                            Column {
                                Text(
                                    text = "Swipeable Habit Card",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tap or swipe to complete daily habit",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Swipe To Reveal Showcase
                    item { WearExpressiveHeader(title = "SWIPE TO REVEAL", icon = Icons.Default.Layers) }
                    item { CatalogM3SwipeToRevealCardSample() }
                    item { CatalogM3SwipeToRevealButtonSample() }

                    // Upstream Selection Controls Showcase
                    item { WearExpressiveHeader(title = "UPSTREAM SELECTION CONTROLS", icon = Icons.Default.Tune) }
                    item { CatalogM3CheckboxSample() }
                    item { CatalogM3SplitCheckboxSample() }
                    item { CatalogM3SwitchSample() }
                    item { CatalogM3SplitSwitchSample() }
                    item { CatalogM3RadioButtonSample() }
                    item { CatalogM3SplitRadioButtonSample() }

                    // Upstream Stepper, Fading Label Stepper, Slider & Progress Showcase
                    item { WearExpressiveHeader(title = "STEPPERS, SLIDER & PROGRESS", icon = Icons.Default.Timer) }
                    item { CatalogM3StepperSample() }
                    item { CatalogM3FadingExpandingLabelStepperSample() }
                    item { CatalogM3SliderSample() }
                    item { CatalogM3LinearProgressIndicatorSample() }
                    item { CatalogM3ProgressIndicatorsSample() }
                    item { CatalogM3LevelIndicatorSample() }
                    item { CatalogM3PageIndicatorSample() }
                    item { CatalogM3ListHeaderSample() }

                    // Upstream Pickers, Dialogs & Overlays Showcase (All Constrained to Bezel)
                    item { WearExpressiveHeader(title = "PICKERS & M3 DIALOGS", icon = Icons.Default.Layers) }
                    item { CatalogM3DatePickerSample(onOpen = { activeOverlay = CatalogOverlayState.DATE_PICKER }) }
                    item { CatalogM3TimePickerSample(onOpen = { activeOverlay = CatalogOverlayState.TIME_PICKER }) }
                    item { CatalogM3AlertDialogSample(onShow = { activeOverlay = CatalogOverlayState.ALERT_DIALOG }) }
                    item { CatalogM3ConfirmationDialogSample(onShow = { activeOverlay = CatalogOverlayState.CONFIRMATION_SUCCESS }) }
                    item { CatalogM3GenericConfirmationDialogSample(onShow = { activeOverlay = CatalogOverlayState.CONFIRMATION_GENERIC }) }
                    item { CatalogM3FailureConfirmationDialogSample(onShow = { activeOverlay = CatalogOverlayState.CONFIRMATION_FAILURE }) }
                    item { CatalogM3OpenOnPhoneDialogSample(onShow = { activeOverlay = CatalogOverlayState.OPEN_ON_PHONE }) }

                    // Upstream Placeholders, Shimmers & Arc/Segmented Progress Showcase
                    item { WearExpressiveHeader(title = "PLACEHOLDERS & ARC PROGRESS", icon = Icons.Default.Timer) }
                    item { CatalogM3PlaceholderButtonSample() }
                    item { CatalogM3PlaceholderIconButtonSample() }
                    item { CatalogM3PlaceholderCardSample() }
                    item { CatalogM3ArcProgressSample() }
                    item { CatalogM3SegmentedProgressSample() }

                    // Typography, Curved Text, Spring Motion & Fast Scrolling Transform
                    item { WearExpressiveHeader(title = "TYPOGRAPHY & MOTION", icon = Icons.Default.AutoAwesome) }
                    item { CatalogM3TypographySample() }
                    item { CatalogM3CurvedTextSample() }
                    item { CatalogM3SpringMotionSample() }
                    item { CatalogM3FastScrollingTransformSample() }

                    // Horologist Auth Suite
                    item { WearExpressiveHeader(title = "HOROLOGIST AUTH SUITE", icon = Icons.Default.Layers) }
                    item { CatalogHorologistAuthSignInSample(onSignIn = { activeOverlay = CatalogOverlayState.CONFIRMATION_AUTH }) }
                    item { CatalogHorologistAuthGuestModeSample() }
                    item { CatalogHorologistAuthSelectAccountSample() }
                    item { CatalogHorologistAuthSignInPlaceholderSample() }
                    item { CatalogHorologistAuthSignInConfirmationSample(onShow = { activeOverlay = CatalogOverlayState.CONFIRMATION_AUTH }) }

                    // Horologist Media Suite
                    item { WearExpressiveHeader(title = "HOROLOGIST MEDIA SUITE", icon = Icons.Default.Timer) }
                    item { CatalogHorologistMediaPlayerScreenSample() }
                    item { CatalogHorologistMediaInfoDisplaySample() }
                    item { CatalogHorologistMediaControlsSample() }
                    item { CatalogHorologistPodcastControlsSample() }
                    item { CatalogHorologistPlayPauseProgressSample() }
                    item { CatalogHorologistMediaShowPlaylistButtonSample() }
                    item { CatalogHorologistMediaFooterButtonsSample() }

                    // Section 4: Selection & Toggle Controls
                    item {
                        WearExpressiveHeader(title = "TOGGLES & SWITCHES", icon = Icons.Default.Tune)
                    }

                    item {
                        WearExpressiveSwitchRow(
                            label = "Expressive Switch",
                            checked = switchChecked,
                            onCheckedChange = { switchChecked = it },
                            icon = Icons.Default.Settings,
                            subtitle = if (switchChecked) "Feature active" else "Feature disabled"
                        )
                    }

                    item {
                        WearExpressiveCheckboxRow(
                            label = "Expressive Checkbox",
                            checked = checkboxChecked,
                            onCheckedChange = { checkboxChecked = it },
                            subtitle = "Checkable list row item",
                            badgeText = "M3"
                        )
                    }

                    // Section 5: Sliders & Controls
                    item {
                        WearExpressiveHeader(title = "SLIDERS & CONTROL ROWS", icon = Icons.Default.Timer)
                    }

                    item {
                        WearExpressiveHorizontalSlider(
                            title = "ADJUST DURATION",
                            valueText = "$sliderValue MIN",
                            onValueDecrease = { sliderValue = (sliderValue - 5).coerceAtLeast(5) },
                            onValueIncrease = { sliderValue = (sliderValue + 5).coerceAtMost(180) },
                            canDecrease = sliderValue > 5,
                            canIncrease = sliderValue < 180
                        )
                    }

                    item {
                        WearExpressivePrevNextRow(
                            title = "Prev / Next Row",
                            subtitle = "Option selector choice card",
                            onPrevious = { haptic.click() },
                            onNext = { haptic.click() }
                        )
                    }

                    // Section 6: Heat Maps & Activity Matrices
                    item {
                        WearExpressiveHeader(title = "ACTIVITY HEAT MAPS", icon = Icons.Default.ColorLens)
                    }

                    item {
                        WearHeatMapCard(
                            title = "Activity Heatmap",
                            subtitle = ">",
                            activityLevels = com.creativeblisslabs.wearproductivity.wear.ui.components.core.compute28DayActivityLevels(state)
                        )
                    }

                    // Section 7: Progress Indicators
                    item {
                        WearExpressiveHeader(title = "PROGRESS INDICATORS")
                    }

                    item {
                        Card(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Linear & Circular Progress", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(
                                    progress = { 0.75f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                CircularProgressIndicator(
                                    progress = { 0.75f },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. In-Viewport Constrained Full-Screen Overlays (Strictly Clipped Inside 416.dp Circle)
            when (activeOverlay) {
                CatalogOverlayState.NONE -> Unit

                CatalogOverlayState.HOURS_MINUTES_PICKER -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        DurationPicker(
                            title = "CATALOG TIMER",
                            initialMinutes = sliderValue,
                            onConfirm = { selectedMins ->
                                sliderValue = selectedMins
                                activeOverlay = CatalogOverlayState.NONE
                                haptic.click()
                            },
                            onCancel = {
                                haptic.click()
                                activeOverlay = CatalogOverlayState.NONE
                            }
                        )
                    }
                }

                CatalogOverlayState.DATE_PICKER -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        DatePicker(
                            initialDate = java.time.LocalDate.now(),
                            onDatePicked = {
                                activeOverlay = CatalogOverlayState.NONE
                                haptic.click()
                            }
                        )
                    }
                }

                CatalogOverlayState.TIME_PICKER -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        TimePicker(
                            initialTime = java.time.LocalTime.now(),
                            onTimePicked = {
                                activeOverlay = CatalogOverlayState.NONE
                                haptic.click()
                            }
                        )
                    }
                }

                CatalogOverlayState.ALERT_DIALOG -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.96f))
                            .padding(horizontal = adaptiveSpec.topBottomSlotHorizontalMargin, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "M3 Alert Dialog",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Battery saver mode enabled. Tap Confirm to apply.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AlertDialogDefaults.DismissButton(onClick = { activeOverlay = CatalogOverlayState.NONE })
                                AlertDialogDefaults.ConfirmButton(onClick = {
                                    haptic.click()
                                    activeOverlay = CatalogOverlayState.NONE
                                })
                            }
                        }
                    }
                }

                CatalogOverlayState.CONFIRMATION_SUCCESS -> {
                    ViewportConstrainedCurvedOverlay(
                        curvedLabel = "Confirmed",
                        onDismiss = { activeOverlay = CatalogOverlayState.NONE }
                    ) {
                        ConfirmationDialogDefaults.SuccessIcon()
                    }
                }

                CatalogOverlayState.CONFIRMATION_GENERIC -> {
                    ViewportConstrainedCurvedOverlay(
                        curvedLabel = "Saved to Favorites",
                        onDismiss = { activeOverlay = CatalogOverlayState.NONE }
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Saved", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                CatalogOverlayState.CONFIRMATION_FAILURE -> {
                    ViewportConstrainedCurvedOverlay(
                        curvedLabel = "Action Failed",
                        onDismiss = { activeOverlay = CatalogOverlayState.NONE }
                    ) {
                        ConfirmationDialogDefaults.FailureIcon()
                    }
                }

                CatalogOverlayState.CONFIRMATION_AUTH -> {
                    ViewportConstrainedCurvedOverlay(
                        curvedLabel = "Welcome, Manu",
                        onDismiss = { activeOverlay = CatalogOverlayState.NONE }
                    ) {
                        ConfirmationDialogDefaults.SuccessIcon()
                    }
                }

                CatalogOverlayState.OPEN_ON_PHONE -> {
                    ViewportConstrainedCurvedOverlay(
                        curvedLabel = OpenOnPhoneDialogDefaults.text,
                        isOpenOnPhone = true,
                        onDismiss = { activeOverlay = CatalogOverlayState.NONE }
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(56.dp))
                    }
                }
            }
        }
    }
}