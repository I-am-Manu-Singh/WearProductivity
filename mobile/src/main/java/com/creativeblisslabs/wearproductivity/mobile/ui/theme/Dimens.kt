package com.creativeblisslabs.wearproductivity.mobile.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
open class LifeOSDimens @OptIn(ExperimentalMaterial3ExpressiveApi::class) constructor(
    density: Density = Density(density = 1f, fontScale = 1f),
    typography: Typography = Typography(),

    val tempoPickerSize: Dp = 184.dp,
    val tempoPickerBpmTextStyle: TextStyle = typography.displayLarge,
    val tempoPickerLabelTextStyle: TextStyle = typography.bodyLargeEmphasized,

    val bottomControlsPaddingBottom: Dp = 16.dp,
    val bottomControlsCenterButtonSize: DpSize = IconButtonDefaults.largeContainerSize(),
    val bottomControlsSideButtonSize: DpSize = IconButtonDefaults.largeContainerSize(),
    val bottomControlsIconSize: Dp = IconButtonDefaults.largeIconSize,
    val bottomControlsButtonSpacing: Dp = 8.dp,
)

class CompactPortraitDimens(
    density: Density,
    typography: Typography,
) : LifeOSDimens(
    density = density,
    typography = typography,
)

class CompactLandscapeDimens(
    density: Density,
    typography: Typography,
) : LifeOSDimens(
    density = density,
    typography = typography,
    tempoPickerSize = 136.dp,
)

class MediumPortraitDimens(
    density: Density,
    typography: Typography,
) : LifeOSDimens(
    density = density,
    typography = typography,
    tempoPickerSize = 304.dp,
    bottomControlsPaddingBottom = 56.dp,
)

class ExpandedLandscapeDimens(
    density: Density,
    typography: Typography,
) : LifeOSDimens(
    density = density,
    typography = typography,
    tempoPickerSize = 184.dp,
)

fun TextUnit.toDp(density: Density): Dp = with(density) { toDp() }

val LocalDimens = staticCompositionLocalOf { LifeOSDimens() }

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberLifeOSDimens(
    windowSizeClass: WindowSizeClass,
    typography: Typography = MaterialTheme.typography,
    density: Density = LocalDensity.current,
): LifeOSDimens {
    return remember(windowSizeClass, typography, density) {
        when {
            windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> {
                CompactLandscapeDimens(density, typography)
            }
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
                ExpandedLandscapeDimens(density, typography)
            }
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
                MediumPortraitDimens(density, typography)
            }
            else -> {
                CompactPortraitDimens(density, typography)
            }
        }
    }
}
