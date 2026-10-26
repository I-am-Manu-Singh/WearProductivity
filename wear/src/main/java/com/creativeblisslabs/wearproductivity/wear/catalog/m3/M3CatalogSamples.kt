package com.creativeblisslabs.wearproductivity.wear.catalog.m3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SegmentedCircularProgressIndicator
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.TitleCard
import kotlinx.coroutines.delay

// ============================================================================
// 1. BUTTON SAMPLES (12 Composables)
// ============================================================================
@Composable
fun CatalogM3ButtonSample(label: String = "Primary Button", onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(),
        modifier = Modifier.fillMaxWidth()
    ) { Text(text = label) }
}

@Composable
fun CatalogM3FilledTonalButtonSample(label: String = "Tonal Button", onClick: () -> Unit = {}) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text = label) }
}

@Composable
fun CatalogM3OutlinedButtonSample(label: String = "Outlined Button", onClick: () -> Unit = {}) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text = label) }
}

@Composable
fun CatalogM3ChildButtonSample(label: String = "Child Button", onClick: () -> Unit = {}) {
    ChildButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text = label) }
}

@Composable
fun CatalogM3CompactButtonSample(label: String = "Compact", onClick: () -> Unit = {}) {
    CompactButton(onClick = onClick) { Text(text = label) }
}

@Composable
fun CatalogM3EdgeButtonSample(label: String = "Edge Action", onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("EdgeButton Preview (Inside Container)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            EdgeButton(onClick = onClick) { Text(text = label) }
        }
    }
}

@Composable
fun CatalogM3ButtonLoadingSample() {
    var isLoading by remember { mutableStateOf(true) }
    Button(
        onClick = { isLoading = !isLoading },
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Loading...")
        } else {
            Text("Tap to Load")
        }
    }
}

@Composable
fun CatalogM3IconButtonSample(onClick: () -> Unit = {}) {
    IconButton(onClick = onClick) { Icon(Icons.Default.Favorite, contentDescription = "Favorite") }
}

@Composable
fun CatalogM3FilledIconButtonSample(onClick: () -> Unit = {}) {
    FilledIconButton(onClick = onClick) {
        Icon(Icons.Default.Favorite, contentDescription = "Filled Icon Button")
    }
}

@Composable
fun CatalogM3FilledTonalIconButtonSample(onClick: () -> Unit = {}) {
    FilledTonalIconButton(onClick = onClick) {
        Icon(Icons.Default.Favorite, contentDescription = "Filled Tonal Icon Button")
    }
}

@Composable
fun CatalogM3OutlinedIconButtonSample(onClick: () -> Unit = {}) {
    OutlinedIconButton(onClick = onClick) {
        Icon(Icons.Default.Favorite, contentDescription = "Outlined Icon Button")
    }
}

@Composable
fun CatalogM3IconButtonsRowSample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Icon Button Variants (Filled / Tonal / Outlined)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CatalogM3FilledIconButtonSample()
                CatalogM3FilledTonalIconButtonSample()
                CatalogM3OutlinedIconButtonSample()
                CatalogM3IconButtonSample()
            }
        }
    }
}

@Composable
fun CatalogM3IconToggleButtonSample(initialChecked: Boolean = false) {
    var checked by remember { mutableStateOf(initialChecked) }
    IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        Icon(Icons.Default.Favorite, contentDescription = "Toggle Favorite")
    }
}

@Composable
fun CatalogM3TextButtonSample(label: String = "Text Button", onClick: () -> Unit = {}) {
    TextButton(onClick = onClick) { Text(text = label) }
}

@Composable
fun CatalogM3TextToggleButtonSample(label: String = "Toggle", initialChecked: Boolean = false) {
    var checked by remember { mutableStateOf(initialChecked) }
    TextToggleButton(checked = checked, onCheckedChange = { checked = it }) { Text(label) }
}

// ============================================================================
// 2. CARD SAMPLES (4 Composables)
// ============================================================================
@Composable
fun CatalogM3CardSample(title: String = "Standard M3 Card", subtitle: String = "wear-m3-catalog sample", onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3OutlinedCardSample(title: String = "Outlined M3 Card", subtitle: String = "wear-m3-catalog sample", onClick: () -> Unit = {}) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3TitleCardSample(title: String = "Title Card Sample", timeText: String = "NOW", onClick: () -> Unit = {}) {
    TitleCard(
        onClick = onClick,
        title = { Text(title) },
        time = { Text(timeText) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("TitleCard content ported from wear-m3-catalog sample") }
}

@Composable
fun CatalogM3AppCardSample(appName: String = "LifeOS Productivity", title: String = "AppCard Sample", onClick: () -> Unit = {}) {
    AppCard(
        onClick = onClick,
        appName = { Text(appName) },
        title = { Text(title) },
        appImage = { Icon(Icons.Default.Favorite, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("AppCard content ported from wear-m3-catalog sample") }
}

// ============================================================================
// 3. SELECTION BUTTON SAMPLES (6 Composables)
// ============================================================================
@Composable
fun CatalogM3CheckboxSample(label: String = "Checkbox Row Item", initialChecked: Boolean = false) {
    var checked by remember { mutableStateOf(initialChecked) }
    CheckboxButton(
        checked = checked,
        onCheckedChange = { checked = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CatalogM3SplitCheckboxSample(label: String = "Split Checkbox", secondaryLabel: String = "Tap primary or checkbox", initialChecked: Boolean = false, onClick: () -> Unit = {}) {
    var checked by remember { mutableStateOf(initialChecked) }
    SplitCheckboxButton(
        checked = checked,
        onCheckedChange = { checked = it },
        onContainerClick = onClick,
        toggleContentDescription = label,
        label = { Text(label) },
        secondaryLabel = { Text(secondaryLabel) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CatalogM3SwitchSample(label: String = "Switch Row Item", initialChecked: Boolean = true) {
    var checked by remember { mutableStateOf(initialChecked) }
    SwitchButton(
        checked = checked,
        onCheckedChange = { checked = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CatalogM3SplitSwitchSample(label: String = "Split Switch", secondaryLabel: String = "Tap primary or switch", initialChecked: Boolean = true, onClick: () -> Unit = {}) {
    var checked by remember { mutableStateOf(initialChecked) }
    SplitSwitchButton(
        checked = checked,
        onCheckedChange = { checked = it },
        onContainerClick = onClick,
        toggleContentDescription = label,
        label = { Text(label) },
        secondaryLabel = { Text(secondaryLabel) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CatalogM3RadioButtonSample(label: String = "Radio Option", selected: Boolean = true, onClick: () -> Unit = {}) {
    RadioButton(
        selected = selected,
        onSelect = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CatalogM3SplitRadioButtonSample(label: String = "Split Radio", secondaryLabel: String = "Option detail", selected: Boolean = true, onClick: () -> Unit = {}) {
    SplitRadioButton(
        selected = selected,
        onSelectionClick = onClick,
        onContainerClick = onClick,
        selectionContentDescription = label,
        label = { Text(label) },
        secondaryLabel = { Text(secondaryLabel) },
        modifier = Modifier.fillMaxWidth()
    )
}

// ============================================================================
// 4. STEPPER, FADING EXPANDING LABEL STEPPER & SLIDER SAMPLES (3 Composables)
// ============================================================================
@Composable
fun CatalogM3StepperSample(title: String = "Step Goal", initialValue: Double = 10000.0) {
    var value by remember { mutableDoubleStateOf(initialValue) }
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$title: ${value.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { value = (value - 500.0).coerceAtLeast(1000.0) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                FilledTonalIconButton(onClick = { value += 500.0 }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

@Composable
fun CatalogM3FadingExpandingLabelStepperSample(initialLevel: Int = 5) {
    var level by remember { mutableIntStateOf(initialLevel) }
    var showLabel by remember { mutableStateOf(true) }

    LaunchedEffect(level) {
        showLabel = true
        delay(1800)
        showLabel = false
    }

    Card(onClick = { showLabel = true }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Fading Expanding Label Stepper", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedVisibility(visible = showLabel, enter = fadeIn(), exit = fadeOut()) {
                Text("Volume Level: $level / 10", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { level = (level - 1).coerceAtLeast(0) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text("$level", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                FilledTonalIconButton(onClick = { level = (level + 1).coerceAtMost(10) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

@Composable
fun CatalogM3SliderSample(initialValue: Float = 5f) {
    var value by remember { mutableFloatStateOf(initialValue) }
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("M3 Inline Slider (${value.toInt()}/10)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = 0f..10f,
                steps = 9,
                segmented = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// 5. PROGRESS INDICATORS (4 Composables)
// ============================================================================
@Composable
fun CatalogM3LinearProgressIndicatorSample(progress: Float = 0.68f) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Linear Progress Indicator (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CatalogM3ProgressIndicatorsSample(progress: Float = 0.75f) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Progress Indicators (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun CatalogM3ArcProgressSample(progress: Float = 0.82f) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Arc Progress Indicator (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            CircularProgressIndicator(
                progress = { progress },
                startAngle = 120f,
                endAngle = 60f,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun CatalogM3SegmentedProgressSample(segments: Int = 5, progress: Float = 0.75f) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Segmented Circular Progress ($segments Segments)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            SegmentedCircularProgressIndicator(
                segmentCount = segments,
                progress = { progress },
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// ============================================================================
// 6. LIST HEADERS (1 Composable)
// ============================================================================
@Composable
fun CatalogM3ListHeaderSample(title: String = "LIST HEADER") {
    ListHeader { Text(title.uppercase(), fontWeight = FontWeight.Bold) }
}

// ============================================================================
// 7. DIALOGS & OVERLAYS (5 Composables - Delegated to Viewport Overlay)
// ============================================================================
@Composable
fun CatalogM3AlertDialogSample(onShow: () -> Unit = {}) {
    Card(onClick = onShow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Alert Dialog Sample", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to open M3 Alert Dialog inside watch bezel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3ConfirmationDialogSample(onShow: () -> Unit = {}) {
    Card(onClick = onShow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Confirmation Dialog Sample", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to trigger confirmation feedback overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3GenericConfirmationDialogSample(onShow: () -> Unit = {}) {
    Card(onClick = onShow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Generic Confirmation Dialog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to trigger generic icon & curved text overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3FailureConfirmationDialogSample(onShow: () -> Unit = {}) {
    Card(onClick = onShow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Failure Confirmation Dialog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to trigger failure confirmation overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3OpenOnPhoneDialogSample(onShow: () -> Unit = {}) {
    Card(onClick = onShow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Open On Phone Dialog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to open live Open On Phone Dialog overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================================
// 8. LEVEL & PAGE INDICATORS (2 Composables)
// ============================================================================
@Composable
fun CatalogM3LevelIndicatorSample(level: Float = 0.65f) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Level Indicator (${(level * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(progress = { level }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CatalogM3PageIndicatorSample(currentPage: Int = 1, pageCount: Int = 4) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Page $currentPage of $pageCount", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================================
// 9. PICKERS (2 Composables)
// ============================================================================
@Composable
fun CatalogM3DatePickerSample(onOpen: () -> Unit = {}) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("M3 Date Picker Sample", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to open full-screen date picker overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3TimePickerSample(onOpen: () -> Unit = {}) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("M3 Time Picker Sample", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to open full-screen time picker overlay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================================
// 10. SWIPE TO REVEAL (2 Composables)
// ============================================================================
@Composable
fun CatalogM3SwipeToRevealCardSample(onReveal: () -> Unit = {}) {
    var revealed by remember { mutableStateOf(false) }
    Card(
        onClick = {
            revealed = !revealed
            onReveal()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Swipe To Reveal Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Swipe or tap to reveal primary/undo card actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (revealed) {
                Spacer(modifier = Modifier.width(6.dp))
                FilledIconButton(onClick = { revealed = false }) {
                    Icon(Icons.Default.Remove, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun CatalogM3SwipeToRevealButtonSample(onReveal: () -> Unit = {}) {
    var revealed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = {
                revealed = !revealed
                onReveal()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(if (revealed) "Revealed Action" else "Swipe To Reveal Button")
        }
        if (revealed) {
            FilledIconButton(onClick = { revealed = false }) {
                Icon(Icons.Default.Remove, contentDescription = "Remove Item")
            }
        }
    }
}

// ============================================================================
// 11. HOROLOGIST MEDIA SUITE (7 Composables)
// ============================================================================
@Composable
fun CatalogHorologistMediaPlayerScreenSample() {
    var isPlaying by remember { mutableStateOf(true) }
    Card(onClick = { isPlaying = !isPlaying }, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CatalogHorologistMediaInfoDisplaySample(
                title = "Midnight Synthwave",
                artist = "LifeOS Audio Engine",
                embedded = true
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = {}) { Text("⏮") }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 0.65f }, modifier = Modifier.size(44.dp))
                    FilledIconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp) // Ensures the icon scales nicely within the 34.dp button
                        )
                    }
                }
                FilledTonalIconButton(onClick = {}) { Text("⏭") }
            }
            CatalogHorologistMediaFooterButtonsSample(embedded = true)
        }
    }
}

@Composable
fun CatalogHorologistMediaInfoDisplaySample(
    title: String = "Track Title",
    artist: String = "Artist • Album",
    embedded: Boolean = false
) {
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (embedded) {
        content()
    } else {
        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(8.dp)) { content() }
        }
    }
}

@Composable
fun CatalogHorologistMediaControlsSample(
    isPlaying: Boolean = true,
    onTogglePlay: () -> Unit = {}
) {
    Card(onClick = onTogglePlay, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Horologist Media Control Buttons", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Track"
                    )
                }

                FilledIconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                FilledTonalIconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track"
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogHorologistPodcastControlsSample(
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {}
) {
    Card(onClick = onTogglePlay, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Horologist Podcast Control Buttons", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactButton(onClick = {}) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Skip back 10 seconds",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = "10s")
                    }
                }

                FilledIconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                CompactButton(onClick = {}) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward30,
                            contentDescription = "Skip forward 30 seconds",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = "30s")
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogHorologistPlayPauseProgressSample(
    progress: Float = 0.65f,
    isPlaying: Boolean = true
) {
    var playing by remember { mutableStateOf(isPlaying) }
    Card(onClick = { playing = !playing }, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Media Play/Pause Progress (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(48.dp))
                FilledIconButton(
                    onClick = { playing = !playing },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play"
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogHorologistMediaShowPlaylistButtonSample(onClick: () -> Unit = {}) {
    FilledTonalButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Show Playlist") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Show Playlist")
    }
}

@Composable
fun CatalogHorologistMediaFooterButtonsSample(embedded: Boolean = false) {
    val rowContent: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Playlist")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Favorite, contentDescription = "Like Track")
            }
        }
    }
    if (embedded) {
        rowContent()
    } else {
        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Horologist Media Footer Buttons", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                rowContent()
            }
        }
    }
}

// ============================================================================
// 12. TYPOGRAPHY & TEXT COMPONENTS (2 Composables)
// ============================================================================
@Composable
fun CatalogM3TypographySample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Display Large", style = MaterialTheme.typography.displayLarge)
            Text("Title Medium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Label Small", style = MaterialTheme.typography.labelSmall)
            Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CatalogM3CurvedTextSample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Curved Text & Arcs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Renders curved text along circular watch bezel arc", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================================
// 13. SPRING MOTION & FAST SCROLLING TRANSFORM (2 Composables)
// ============================================================================
@Composable
fun CatalogM3SpringMotionSample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Spring Physics & Surface Transformations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Dynamic morphing & scaling effects during list scrolling", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CatalogM3FastScrollingTransformSample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Fast Scrolling Transform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Rotary fast-scroll scaling & alpha surface transformation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================================
// 14. PLACEHOLDERS & SHIMMERS (3 Composables)
// ============================================================================
@Composable
fun CatalogM3PlaceholderButtonSample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Placeholder Shimmer Button", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Loading content…")
            }
        }
    }
}

@Composable
fun CatalogM3PlaceholderIconButtonSample() {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Placeholder Icon Button", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalIconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.clip(CircleShape)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CatalogM3PlaceholderCardSample() {
    Card(
        onClick = {},
        enabled = false,
        colors = CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Placeholder Shimmer Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Content loading shimmer placeholder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================================
// 15. HOROLOGIST AUTH SUITE (5 Composables)
// ============================================================================
@Composable
fun CatalogHorologistAuthSignInSample(onSignIn: () -> Unit = {}) {
    Button(
        onClick = onSignIn,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Sign in with Google")
    }
}

@Composable
fun CatalogHorologistAuthGuestModeSample(onGuestContinue: () -> Unit = {}) {
    FilledTonalButton(
        onClick = onGuestContinue,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue as Guest")
    }
}

@Composable
fun CatalogHorologistAuthSelectAccountSample() {
    var selectedAccount by remember { mutableIntStateOf(0) }
    val accounts = listOf("manu.dev@gmail.com", "work@lifeos.app")
    Card(onClick = { selectedAccount = (selectedAccount + 1) % accounts.size }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Auth Select Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            accounts.forEachIndexed { idx, email ->
                RadioButton(
                    selected = selectedAccount == idx,
                    onSelect = { selectedAccount = idx },
                    label = { Text(email, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CatalogHorologistAuthSignInPlaceholderSample() {
    Card(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Signing in…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CatalogHorologistAuthSignInConfirmationSample(onShow: () -> Unit = {}) {
    Card(onClick = onShow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Auth Sign-In Confirmation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to preview signed-in account confirmation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}