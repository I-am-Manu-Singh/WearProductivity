package com.creativeblisslabs.wearproductivity.wear.ui.components.core

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.compose.ui.geometry.Size
import androidx.wear.compose.material.Button
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.confirmationDialogCurvedText
import com.creativeblisslabs.wearproductivity.wear.ui.audio.OutputRouteDevice
import kotlinx.coroutines.delay
import kotlin.collections.forEach
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val LocalWearAdaptiveSpec = androidx.compose.runtime.compositionLocalOf<WearAdaptiveSpec> {
    error("No WearAdaptiveSpec provided")
}

data class WearAdaptiveSpec(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLargeScreen: Boolean,
    val isSmallScreen: Boolean, // < 225dp watch
    val outerHorizontalMargin: Dp,
    val topBottomSlotHorizontalMargin: Dp,
    val topSlotPadding: Dp,
    val bottomEdgeButtonPadding: Dp
)

@Composable
fun WearBaselineViewport(
    baselineWatchSize: Dp = 416.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val isPhonePreview = this.maxWidth > 280.dp || this.maxHeight > 280.dp
        val viewportWidth = if (isPhonePreview) baselineWatchSize else this.maxWidth
        val viewportHeight = if (isPhonePreview) baselineWatchSize else this.maxHeight

        val spec = androidx.compose.runtime.remember(viewportWidth, viewportHeight) {
            WearAdaptiveSpec(
                screenWidth = viewportWidth,
                screenHeight = viewportHeight,
                isLargeScreen = viewportWidth >= 225.dp,
                isSmallScreen = viewportWidth < 225.dp,
                outerHorizontalMargin = viewportWidth * 0.052f,
                topBottomSlotHorizontalMargin = viewportWidth * 0.14f,
                topSlotPadding = viewportHeight * 0.11f,
                bottomEdgeButtonPadding = viewportHeight * 0.22f
            )
        }

        val scaledDensity = if (spec.isSmallScreen) {
            androidx.compose.ui.unit.Density(
                density = androidx.compose.ui.platform.LocalDensity.current.density * 0.85f,
                fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale * 0.85f
            )
        } else {
            androidx.compose.ui.platform.LocalDensity.current
        }

        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides scaledDensity,
            LocalWearAdaptiveSpec provides spec
        ) {
            if (isPhonePreview) {
                Box(
                    modifier = Modifier
                        .size(baselineWatchSize)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Black)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    content()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    content()
                }
            }
        }
    }
}
@Composable
fun WearExpressiveHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.compactLineHeight(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (badgeText != null) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.compactLineHeight(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 2. Lazy List Helper: Retains your previous scope & wraps ListHeader automatically
@Composable
fun TransformingLazyColumnItemScope.WearExpressiveHeaderItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeText: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListHeader(
        modifier = modifier.fillMaxWidth()
    ) {
        // Calls the core component internally
        WearExpressiveHeader(
            title = title,
            icon = icon,
            badgeText = badgeText,
            onClick = onClick
        )
    }
}
/**
 * Reusable TitleCard displaying metric title, value, status subtext, and icon.
 */
@Composable
fun WearMetricCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    TitleCard(
        onClick = onClick,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        time = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Reusable Action Group displaying inline choice category chips with removal support.
 */
@Composable
fun WearActionButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveOption: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            CompactButton(
                onClick = { onOptionSelected(index) },
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Reusable EdgeButton anchored at the screen bottom for primary actions.
 */
@Composable
fun WearAnchoredEdgeButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    EdgeButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = label,
            maxLines = 1,
            fontWeight = FontWeight.Bold
        )
    }
}
/**
 * Reusable Horizontal Slider for numeric and option adjustments across Wear OS screens.
 */
@Composable
fun WearExpressiveHorizontalSlider(
    title: String,
    valueText: String,
    onValueDecrease: () -> Unit,
    onValueIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true
) {
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FIX: Changed from Button to FilledTonalIconButton to natively handle icon frames
            androidx.wear.compose.material3.FilledTonalIconButton(
                onClick = onValueDecrease,
                enabled = canDecrease,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Centered Title & Value Label
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // FIX: Changed from Button to FilledTonalIconButton to natively handle icon frames
            androidx.wear.compose.material3.FilledTonalIconButton(
                onClick = onValueIncrease,
                enabled = canIncrease,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
/**
 * Reusable Prev/Next Control Row for Wear OS screens.
 */
@Composable
fun WearExpressivePrevNextRow(
    title: String,
    subtitle: String? = null,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // FIX: Changed from Button to FilledTonalIconButton to fix icon rendering visibility
            androidx.wear.compose.material3.FilledTonalIconButton(
                onClick = onPrevious,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Balanced text container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // FIX: Changed from Button to FilledTonalIconButton to fix icon rendering visibility
            androidx.wear.compose.material3.FilledTonalIconButton(
                onClick = onNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
/**
 * Reusable Status Chip container with Expressive container styling.
 */


/**
 * Reusable Switch Button row for settings & toggles, optimized for Wear OS round displays.
 */
@Composable
fun WearExpressiveSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = if (subtitle != null) {
            {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Reusable Checkbox Button row for task items and checkable items, optimized for Wear OS round displays.
 */
@Composable
fun WearExpressiveCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null
) {
    CheckboxButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.size(4.dp))
                    WearStatusBadge(text = badgeText)
                }
            }
        },
        secondaryLabel = if (subtitle != null) {
            {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Reusable Status Badge for Wear OS expressive headers and item rows.
 */
@Composable
fun WearStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null, // Added optional icon parameter defaulting to null
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            // Only render the Icon if it is explicitly passed
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun rememberMorphingGroupShape(
    index: Int,
    totalCount: Int,
    isSelected: Boolean,
    outerRadius: Dp = 24.dp,
    innerRadius: Dp = 6.dp
): Shape {
    val targetTop = when {
        isSelected -> outerRadius
        totalCount <= 1 || index == 0 -> outerRadius
        else -> innerRadius
    }
    val targetBottom = when {
        isSelected -> outerRadius
        totalCount <= 1 || index == totalCount - 1 -> outerRadius
        else -> innerRadius
    }

    val animatedTop by animateDpAsState(
        targetValue = targetTop,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "morphTopCorner"
    )
    val animatedBottom by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "morphBottomCorner"
    )

    return RoundedCornerShape(
        topStart = animatedTop,
        topEnd = animatedTop,
        bottomStart = animatedBottom,
        bottomEnd = animatedBottom
    )
}
@Composable
fun ViewportConstrainedCurvedConfirmation(
    label: String,
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
        ConfirmationDialogDefaults.SuccessIcon()
    }
}

@Composable
fun ViewportConstrainedAlertDialog(
    spec: WearAdaptiveSpec,
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = spec.topBottomSlotHorizontalMargin, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            AlertDialogDefaults.ConfirmButton(onClick = onDismiss)
        }
    }
}

/**
 * Reusable M3 Transforming Card component that contracts and expands during scrolling in TransformingLazyColumn.
 */
@Composable
fun TransformingLazyColumnItemScope.WearTransformingCard(
    onClick: () -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    index: Int = 0,
    totalCount: Int = 1,
    isSelected: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    val shape = rememberMorphingGroupShape(index = index, totalCount = totalCount, isSelected = isSelected)
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        content()
    }
}
fun TextStyle.compactLineHeight(): TextStyle = this.copy(
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

fun TextStyle.tabularNums(): TextStyle = this.copy(
    fontFeatureSettings = "tnum",
    lineHeight = 1.1.em,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)


@Composable
fun VerticalPagerIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
            .padding(vertical = 5.dp, horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotHeight by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 4.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "pagerDotHeight"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dotHeight)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

class ScallopedPlayButtonShape(
    private val lobes: Int,
    private val waveAmplitudeRatio: Float
) : Shape {

    init {
        require(lobes >= 1) { "lobes must be at least 1" }
        require(waveAmplitudeRatio in 0f..0.5f) {
            "waveAmplitudeRatio must be between 0 and 0.5"
        }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()

        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(cx, cy)
        val totalSteps = 180

        for (i in 0..totalSteps) {
            val fraction = i.toFloat() / totalSteps
            val angle = -PI / 2.0 + fraction * 2.0 * PI

            val radius = maxRadius * (
                    1f + waveAmplitudeRatio *
                            cos(lobes * angle).toFloat()
                    )

            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        path.close()

        // Compose Path is already the correct type. No cast needed.
        return Outline.Generic(path)
    }
}
@Composable
fun PlayerControlBar(
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val sideStadiumWidth = (screenWidth * 0.22f).coerceIn(42.dp, 52.dp)
    val sideStadiumHeight = (screenWidth * 0.28f).coerceIn(54.dp, 66.dp)
    val centerBoxSize = (screenWidth * 0.42f).coerceIn(76.dp, 92.dp)

    val maxContentHeight = maxOf(sideStadiumHeight, centerBoxSize)

    val coralPinkAccent = Color(0xFFFF8A93)
    val softPinkFill = Color(0xFFFFB2B8)
    val darkIconTint = Color(0xFF470C14)
    val ringTrackCol = darkIconTint.copy(alpha = 0.55f)

    // Shape parameters
    val scallopLobes = 10
    val scallopWaveAmplitude = 0.065f

    // Circle when paused, Scalloped when playing
    val centerButtonShape = remember(isPlaying) {
        if (isPlaying) ScallopedPlayButtonShape(lobes = scallopLobes, waveAmplitudeRatio = scallopWaveAmplitude)
        else CircleShape
    }

    val prevInteractionSource = remember { MutableInteractionSource() }
    val nextInteractionSource = remember { MutableInteractionSource() }
    val playInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(maxContentHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button (Vertical Stadium Pill)
        Button(
            onClick = onPrevious,
            interactionSource = prevInteractionSource,
            modifier = Modifier
                .size(width = sideStadiumWidth, height = sideStadiumHeight)
                .clip(CircleShape)
                .semantics { contentDescription = "Previous Track" },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = coralPinkAccent,
                contentColor = darkIconTint
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Center Progress Ring & Play/Pause Button
        Box(
            modifier = Modifier.size(centerBoxSize),
            contentAlignment = Alignment.Center
        ) {
            // 1. Outer Progress Ring (Circular when paused, Scalloped when playing)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                val maxR = (minOf(size.width, size.height) / 2f) - (strokeWidth / 2f)
                val cx = size.width / 2f
                val cy = size.height / 2f
                val totalSteps = 180

                val currentAmp = if (isPlaying) scallopWaveAmplitude else 0f

                fun buildRingPath(stepCount: Int): Path {
                    val path = Path()
                    val safeStepCount = stepCount.coerceIn(1, totalSteps)

                    for (i in 0..safeStepCount) {
                        val fraction = i.toFloat() / totalSteps
                        val angle = -Math.PI / 2.0 +
                                fraction * 2.0 * Math.PI

                        val radius = maxR * (
                                1f + currentAmp *
                                        kotlin.math.cos(scallopLobes * angle).toFloat()
                                )

                        val x = cx + radius * kotlin.math.cos(angle).toFloat()
                        val y = cy + radius * kotlin.math.sin(angle).toFloat()

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    return path
                }

                // Draw Track Ring
                drawPath(
                    path = buildRingPath(totalSteps),
                    color = ringTrackCol,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Active Progress Arc
                val clampedProgress = progress.coerceIn(0f, 1f)
                if (clampedProgress > 0.005f) {
                    val activeSteps = (totalSteps * clampedProgress).toInt().coerceAtLeast(2)
                    drawPath(
                        path = buildRingPath(activeSteps),
                        color = softPinkFill,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // 2. Inner Play/Pause Button
            val innerButtonSize = centerBoxSize - 8.dp
            Box(
                modifier = Modifier
                    .size(innerButtonSize)
                    .clip(centerButtonShape)
                    .background(softPinkFill)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        onClick = onTogglePlay
                    )
                    .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = darkIconTint,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = if (!isPlaying) 1.dp else 0.dp)
                )
            }
        }

        // Next Button (Vertical Stadium Pill)
        Button(
            onClick = onNext,
            interactionSource = nextInteractionSource,
            modifier = Modifier
                .size(width = sideStadiumWidth, height = sideStadiumHeight)
                .clip(CircleShape)
                .semantics { contentDescription = "Next Track" },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = coralPinkAccent,
                contentColor = darkIconTint
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
@Composable
fun M3FullScreenCurvedVolumeAndOutputOverlay(
    volumeLevel: Float,
    selectedRoute: OutputRouteDevice,
    outputRoutes: List<OutputRouteDevice>,
    onVolumeChange: (Float) -> Unit,
    onSelectRoute: (OutputRouteDevice) -> Unit,
    onPairBluetoothDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedVol by animateFloatAsState(
        targetValue = volumeLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "curvedVolArc"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = (-dragAmount.y / size.height.coerceAtLeast(1)).coerceIn(-0.15f, 0.15f)
                    onVolumeChange((volumeLevel + delta).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val trackCol = MaterialTheme.colorScheme.surfaceContainerHigh
        val activeCol = Color(0xFF79E2B0)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val strokePx = 10.dp.toPx()
            drawArc(
                color = trackCol,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeCol,
                startAngle = 135f,
                sweepAngle = 270f * animatedVol,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {


            Text(
                text = selectedRoute.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onVolumeChange((volumeLevel - 0.08f).coerceAtLeast(0f)) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Volume Down")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (volumeLevel <= 0.01f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = activeCol,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "${(volumeLevel * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.displaySmall.tabularNums(),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                FilledTonalIconButton(
                    onClick = { onVolumeChange((volumeLevel + 0.08f).coerceAtMost(1f)) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Volume Up")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                outputRoutes.forEach { r ->
                    val isSel = r.name.equals(selectedRoute.name, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSelectRoute(r) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = r.name.take(10),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onPairBluetoothDevice)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+BT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), fontWeight = FontWeight.Bold)
                }
            }

            CompactButton(
                onClick = onDismiss,
                icon = { Icon(Icons.Default.Check, contentDescription = "Done") }
            ) {
                Text("Done")
            }
        }
    }
}