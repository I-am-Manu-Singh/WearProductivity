package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.TransformationSpec
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Field behavior for [WearExpressiveUniversalEditorCard].
 */
enum class UniversalEditorFieldType {
    TEXT,
    TIME,
    DATE,
    NUMBER,
    WEEKDAYS,
    CHOICE,
    SLIDER,
    CHIPS
}

/**
 * Configuration descriptor for a discrete number sub-field (e.g. Sets, Reps, Rest, Macros, etc.)
 */
data class DiscreteNumberSubField(
    val key: String,
    val label: String,
    val initialValue: Int,
    val minimum: Int,
    val maximum: Int,
    val step: Int = 1,
    val unitSuffix: String = "",
    val useSlider: Boolean = true
)

private val editorWeekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * In-line editor card that renders natively inside a TransformingLazyColumn.
 * Contains Material 3 Wear OS Sliders, Chips, Time & Date Pickers, and unbundled discrete controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearExpressiveUniversalEditorCard(
    itemTypeTitle: String,
    initialTitle: String,
    initialTimeOrDate: String = "09:00",
    initialSubtitle: String = "",
    isEditingExisting: Boolean,
    showTimePickerField: Boolean = true,
    showSubtitleField: Boolean = true,
    timeFieldLabel: String = "Time / Schedule",
    subtitleFieldLabel: String = "Details / Category",
    onSave: (title: String, timeOrDate: String, subtitle: String) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    transformation: TransformationSpec? = null,
    timeFieldType: UniversalEditorFieldType = UniversalEditorFieldType.TEXT,
    subtitleFieldType: UniversalEditorFieldType = UniversalEditorFieldType.TEXT,
    timeOrDateChoices: List<String> = emptyList(),
    subtitleChoices: List<String> = emptyList(),
    numberMinimum: Int = 0,
    numberMaximum: Int = 100,
    numberStep: Int = 1,
    subtitleNumberMinimum: Int = 0,
    subtitleNumberMaximum: Int = 100,
    subtitleNumberStep: Int = 1,
    // Discrete sub-fields and chips for unbundled parameters
    discreteSubFields: List<DiscreteNumberSubField> = emptyList(),
    onSubFieldsChanged: ((Map<String, Int>) -> Unit)? = null,
    chipOptions: List<String> = emptyList(),
    selectedChipIndex: Int = 0,
    onChipSelected: ((Int) -> Unit)? = null,
    saveLabel: String = "Save",
    isSaving: Boolean = false,
    validationMessage: String? = null,
    onValidationError: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var timeOrDate by remember { mutableStateOf(initialTimeOrDate) }
    var subtitle by remember { mutableStateOf(initialSubtitle) }

    var localSelectedChip by remember { mutableStateOf(selectedChipIndex) }
    val subFieldValues = remember {
        mutableStateMapOf<String, Int>().apply {
            discreteSubFields.forEach { put(it.key, it.initialValue) }
        }
    }

    var activeFullScreenPicker by remember { mutableStateOf<UniversalEditorFieldType?>(null) }

    LaunchedEffect(initialTitle, initialTimeOrDate, initialSubtitle) {
        title = initialTitle
        timeOrDate = initialTimeOrDate
        subtitle = initialSubtitle
    }

    LaunchedEffect(selectedChipIndex) {
        localSelectedChip = selectedChipIndex
    }

    // Native curved full-screen pickers
    when (activeFullScreenPicker) {
        UniversalEditorFieldType.TIME -> {
            NativeFullScreenTimePicker(
                initialTime = timeOrDate,
                onConfirm = { formattedTime ->
                    timeOrDate = formattedTime
                    activeFullScreenPicker = null
                },
                onCancel = { activeFullScreenPicker = null }
            )
            return
        }
        UniversalEditorFieldType.DATE -> {
            NativeFullScreenDatePicker(
                initialDate = timeOrDate,
                onConfirm = { formattedDate ->
                    timeOrDate = formattedDate
                    activeFullScreenPicker = null
                },
                onCancel = { activeFullScreenPicker = null }
            )
            return
        }
        else -> {}
    }

    val onPerformSave: () -> Unit = {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            onValidationError?.invoke("Enter a title")
        } else {
            val validation = (if (showTimePickerField) validateUniversalValue(timeOrDate, timeFieldType, numberMinimum, numberMaximum) else null)
                ?: (if (showSubtitleField) validateUniversalValue(subtitle, subtitleFieldType, subtitleNumberMinimum, subtitleNumberMaximum) else null)
            if (validation != null) {
                onValidationError?.invoke(validation)
            } else {
                onSave(cleanTitle, timeOrDate.trim(), subtitle.trim())
            }
        }
    }

    Card(
        onClick = {},
        enabled = false,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WearExpressiveHeader(
                title = if (isEditingExisting) "EDIT ${itemTypeTitle.uppercase(Locale.ROOT)}" else "NEW ${itemTypeTitle.uppercase(Locale.ROOT)}",
                badgeText = "EDITOR"
            )

            // 1. Text Title
            UniversalEditorInputField(
                value = title,
                onValueChange = { title = it.take(64) },
                label = "$itemTypeTitle Title",
                placeholder = "Enter name...",
                keyboardType = KeyboardType.Text
            )

            // 2. Selectable Chips (Categories, Presets, Tags)
            if (chipOptions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Options / Category",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chipOptions.forEachIndexed { index, option ->
                            val isSelected = index == localSelectedChip
                            CompactButton(
                                onClick = {
                                    localSelectedChip = index
                                    onChipSelected?.invoke(index)
                                },
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 3. Discrete Unbundled Sub-Fields (Sliders / Steppers for Sets, Reps, Rest, Macros)
            if (discreteSubFields.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    discreteSubFields.forEach { subField ->
                        val currentVal = subFieldValues[subField.key] ?: subField.initialValue
                        Card(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subField.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$currentVal ${subField.unitSuffix}".trim(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (subField.useSlider) {
                                    val steps = ((subField.maximum - subField.minimum) / subField.step.coerceAtLeast(1)) - 1
                                    Slider(
                                        value = currentVal.toFloat(),
                                        onValueChange = { newVal ->
                                            val rounded = (newVal / subField.step).roundToInt() * subField.step
                                            val coerced = rounded.coerceIn(subField.minimum, subField.maximum)
                                            subFieldValues[subField.key] = coerced
                                            onSubFieldsChanged?.invoke(subFieldValues.toMap())
                                        },
                                        steps = steps.coerceAtLeast(0),
                                        valueRange = subField.minimum.toFloat()..subField.maximum.toFloat(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    NumberValueControl(
                                        value = currentVal.toString(),
                                        onValueChange = { newValStr ->
                                            val coerced = (newValStr.toIntOrNull() ?: currentVal).coerceIn(subField.minimum, subField.maximum)
                                            subFieldValues[subField.key] = coerced
                                            onSubFieldsChanged?.invoke(subFieldValues.toMap())
                                        },
                                        minimum = subField.minimum,
                                        maximum = subField.maximum,
                                        step = subField.step
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Primary Time / Date Field (With Curved Fullscreen Launchers)
            if (showTimePickerField) {
                UniversalEditorField(
                    value = timeOrDate,
                    onValueChange = { timeOrDate = it },
                    label = timeFieldLabel,
                    placeholder = "Enter value",
                    type = timeFieldType,
                    choices = timeOrDateChoices,
                    numberMinimum = numberMinimum,
                    numberMaximum = numberMaximum,
                    numberStep = numberStep,
                    onLaunchFullScreenPicker = {
                        if (timeFieldType == UniversalEditorFieldType.TIME || timeFieldType == UniversalEditorFieldType.DATE) {
                            activeFullScreenPicker = timeFieldType
                        }
                    }
                )
            }

            // 5. Subtitle Field
            if (showSubtitleField) {
                UniversalEditorField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = subtitleFieldLabel,
                    placeholder = "Optional details",
                    type = subtitleFieldType,
                    choices = subtitleChoices,
                    numberMinimum = subtitleNumberMinimum,
                    numberMaximum = subtitleNumberMaximum,
                    numberStep = subtitleNumberStep,
                    onLaunchFullScreenPicker = {
                        if (subtitleFieldType == UniversalEditorFieldType.TIME || subtitleFieldType == UniversalEditorFieldType.DATE) {
                            activeFullScreenPicker = subtitleFieldType
                        }
                    }
                )
            }

            if (!validationMessage.isNullOrBlank()) {
                Text(
                    text = validationMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Action Buttons (Cancel, Save, Delete)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onPerformSave,
                    modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isSaving) "Saving…" else saveLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (isEditingExisting && onDelete != null) {
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    type: UniversalEditorFieldType,
    choices: List<String>,
    numberMinimum: Int,
    numberMaximum: Int,
    numberStep: Int,
    onLaunchFullScreenPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (type) {
            UniversalEditorFieldType.TEXT -> UniversalEditorInputField(
                value = value,
                onValueChange = { onValueChange(it.take(120)) },
                label = "",
                placeholder = placeholder,
                keyboardType = KeyboardType.Text
            )
            UniversalEditorFieldType.TIME, UniversalEditorFieldType.DATE -> {
                PickerLaunchField(
                    value = value.ifBlank { if (type == UniversalEditorFieldType.TIME) "09:00" else LocalDate.now().toString() },
                    hint = "Tap for native curved picker",
                    icon = if (type == UniversalEditorFieldType.TIME) Icons.Rounded.Schedule else Icons.Rounded.CalendarMonth,
                    onClick = onLaunchFullScreenPicker
                )
            }
            UniversalEditorFieldType.SLIDER -> {
                val num = value.toIntOrNull()?.coerceIn(minOf(numberMinimum, numberMaximum), maxOf(numberMinimum, numberMaximum)) ?: numberMinimum
                val steps = ((numberMaximum - numberMinimum) / numberStep.coerceAtLeast(1)) - 1
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$num",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Slider(
                        value = num.toFloat(),
                        onValueChange = { onValueChange(it.roundToInt().toString()) },
                        steps = steps.coerceAtLeast(0),
                        valueRange = numberMinimum.toFloat()..numberMaximum.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            UniversalEditorFieldType.NUMBER -> NumberValueControl(
                value = value,
                onValueChange = onValueChange,
                minimum = numberMinimum,
                maximum = numberMaximum,
                step = numberStep
            )
            UniversalEditorFieldType.CHIPS, UniversalEditorFieldType.CHOICE -> {
                if (choices.isNotEmpty()) {
                    val currentIdx = choices.indexOf(value).let { if (it < 0) 0 else it }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        choices.forEachIndexed { index, option ->
                            val isSelected = index == currentIdx
                            CompactButton(
                                onClick = { onValueChange(option) },
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    ChoiceValueControl(value = value, onValueChange = onValueChange, choices = choices)
                }
            }
            UniversalEditorFieldType.WEEKDAYS -> WeekdaySelectionControl(value = value, onValueChange = onValueChange)
        }
    }
}

// =============================================================================
// NATIVE FULL-SCREEN CURVED TIME & DATE PICKERS
// =============================================================================
@Composable
private fun NativeFullScreenTimePicker(
    initialTime: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val parsed = parseTime(initialTime)
    val hourState = rememberPickerState(initialNumberOfOptions = 24, initiallySelectedIndex = parsed.first)
    val minuteState = rememberPickerState(initialNumberOfOptions = 60, initiallySelectedIndex = parsed.second)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) { TimeText() }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SELECT TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(84.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Picker(
                    state = hourState,
                    contentDescription = { "${hourState.selectedOptionIndex} hours" },
                    onSelected = {},
                    modifier = Modifier.width(52.dp).height(84.dp)
                ) { idx ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "%02d".format(Locale.ROOT, idx),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 2.dp))
                Picker(
                    state = minuteState,
                    contentDescription = { "${minuteState.selectedOptionIndex} minutes" },
                    onSelected = {},
                    modifier = Modifier.width(52.dp).height(84.dp)
                ) { idx ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "%02d".format(Locale.ROOT, idx),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            WearAnchoredEdgeButton(
                onClick = {
                    val h = hourState.selectedOptionIndex
                    val m = minuteState.selectedOptionIndex
                    onConfirm(formatTime(h, m))
                },
                label = "Confirm",
                icon = Icons.Rounded.Check
            )
        }
    }
}

@Composable
private fun NativeFullScreenDatePicker(
    initialDate: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initialLocalDate = remember(initialDate) {
        runCatching { LocalDate.parse(initialDate.trim()) }.getOrElse { LocalDate.now() }
    }
    val yearState = rememberPickerState(initialNumberOfOptions = 10, initiallySelectedIndex = 5)
    val monthState = rememberPickerState(initialNumberOfOptions = 12, initiallySelectedIndex = initialLocalDate.monthValue - 1)
    val dayState = rememberPickerState(initialNumberOfOptions = 31, initiallySelectedIndex = initialLocalDate.dayOfMonth - 1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) { TimeText() }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SELECT DATE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(84.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Picker(
                    state = yearState,
                    contentDescription = { "Year" },
                    onSelected = {},
                    modifier = Modifier.width(64.dp).height(84.dp)
                ) { idx ->
                    val yr = LocalDate.now().year - 5 + idx
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(yr.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text("-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Picker(
                    state = monthState,
                    contentDescription = { "Month" },
                    onSelected = {},
                    modifier = Modifier.width(44.dp).height(84.dp)
                ) { idx ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("%02d".format(Locale.ROOT, idx + 1), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text("-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Picker(
                    state = dayState,
                    contentDescription = { "Day" },
                    onSelected = {},
                    modifier = Modifier.width(44.dp).height(84.dp)
                ) { idx ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("%02d".format(Locale.ROOT, idx + 1), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            WearAnchoredEdgeButton(
                onClick = {
                    val yr = LocalDate.now().year - 5 + yearState.selectedOptionIndex
                    val mo = monthState.selectedOptionIndex + 1
                    val dy = dayState.selectedOptionIndex + 1
                    val safeDate = runCatching { LocalDate.of(yr, mo, dy).toString() }.getOrDefault(LocalDate.now().toString())
                    onConfirm(safeDate)
                },
                label = "Confirm",
                icon = Icons.Rounded.Check
            )
        }
    }
}

@Composable
private fun PickerLaunchField(
    value: String,
    hint: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value.ifBlank { "Not set" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Pick",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NumberValueControl(
    value: String,
    onValueChange: (String) -> Unit,
    minimum: Int,
    maximum: Int,
    step: Int
) {
    val minValue = minOf(minimum, maximum)
    val maxValue = maxOf(minimum, maximum)
    val safeStep = step.coerceAtLeast(1)
    val number = value.toIntOrNull()?.coerceIn(minValue, maxValue) ?: minValue
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = { onValueChange((number - safeStep).coerceAtLeast(minValue).toString()) },
            enabled = number > minValue,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        FilledTonalIconButton(
            onClick = { onValueChange((number + safeStep).coerceAtMost(maxValue).toString()) },
            enabled = number < maxValue,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun WeekdaySelectionControl(value: String, onValueChange: (String) -> Unit) {
    val selected = value.split(',', ' ', ';')
        .map { it.trim().take(3).replaceFirstChar { c -> c.uppercase(Locale.ROOT) } }
        .filter { it in editorWeekdays }
        .toSet()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
            editorWeekdays.take(4).forEach { day ->
                val checked = day in selected
                FilledTonalButton(
                    onClick = {
                        val next = if (checked) selected - day else selected + day
                        onValueChange(editorWeekdays.filter { it in next }.joinToString(", "))
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(day, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
            editorWeekdays.drop(4).forEach { day ->
                val checked = day in selected
                FilledTonalButton(
                    onClick = {
                        val next = if (checked) selected - day else selected + day
                        onValueChange(editorWeekdays.filter { it in next }.joinToString(", "))
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(day, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)) }
            }
            FilledTonalButton(
                onClick = { onValueChange(if (selected.size == 7) "" else editorWeekdays.joinToString(", ")) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp)
            ) { Text(if (selected.size == 7) "Clear" else "All", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)) }
        }
    }
}

@Composable
private fun ChoiceValueControl(value: String, onValueChange: (String) -> Unit, choices: List<String>) {
    val safeChoices = choices.distinct()
    if (safeChoices.isEmpty()) {
        Text("No choices configured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        return
    }
    val index = safeChoices.indexOf(value).let { if (it < 0) 0 else it }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = { onValueChange(safeChoices[(index - 1 + safeChoices.size) % safeChoices.size]) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Previous option",
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = safeChoices[index],
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        FilledTonalIconButton(
            onClick = { onValueChange(safeChoices[(index + 1) % safeChoices.size]) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Next option",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun UniversalEditorInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType
) {
    val focusRequester = remember { FocusRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .focusRequester(focusRequester)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

private fun validateUniversalValue(
    value: String,
    type: UniversalEditorFieldType,
    minimum: Int,
    maximum: Int
): String? = when (type) {
    UniversalEditorFieldType.TIME ->
        if (parseTimeOrNull(value) == null) "Use 24-hour time, HH:mm" else null
    UniversalEditorFieldType.DATE ->
        if (parseIsoDate(value) == null) "Use date format YYYY-MM-DD" else null
    UniversalEditorFieldType.NUMBER, UniversalEditorFieldType.SLIDER -> {
        val number = value.toIntOrNull()
        if (number == null || number !in minOf(minimum, maximum)..maxOf(minimum, maximum)) {
            "Enter a number from ${minOf(minimum, maximum)} to ${maxOf(minimum, maximum)}"
        } else null
    }
    else -> null
}

private fun parseTime(value: String): Pair<Int, Int> =
    parseTimeOrNull(value) ?: (9 to 0)

private fun parseTimeOrNull(value: String): Pair<Int, Int>? {
    val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(value.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}

private fun formatTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(Locale.ROOT, hour.coerceIn(0, 23), minute.coerceIn(0, 59))

private fun parseIsoDate(value: String): String? {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
    return try {
        val parsed = format.parse(value.trim()) ?: return null
        if (format.format(parsed) == value.trim()) value.trim() else null
    } catch (_: ParseException) {
        null
    }
}
