package com.creativeblisslabs.wearproductivity.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme
import com.creativeblisslabs.wearproductivity.wear.R

@Composable
fun LifeOSWearTheme(content: @Composable () -> Unit) {
    val dynamicColorScheme = dynamicColorScheme(LocalContext.current)
    MaterialTheme(
        colorScheme = dynamicColorScheme ?: lifeOSWearColorScheme(),
        typography = lifeOSWearTypography(),
        content = content
    )
}

@Composable
private fun lifeOSWearColorScheme(): ColorScheme {
    return ColorScheme(
        primary = Color(0xFFb3c5ff),
        primaryDim = Color(0xFF91aafd),
        primaryContainer = Color(0xFF769bff),
        onPrimary = Color(0xFF002b75),
        onPrimaryContainer = Color(0xFF003080),

        secondary = Color(0xFF62dbb2),
        secondaryDim = Color(0xFF4bb894),
        secondaryContainer = Color(0xFF18a47e),
        onSecondary = Color(0xFF003829),
        onSecondaryContainer = Color(0xFF003123),

        tertiary = Color(0xFFffb59e),
        tertiaryDim = Color(0xFFf39e83),
        tertiaryContainer = Color(0xFFf17f5b),
        onTertiary = Color(0xFF5e1700),
        onTertiaryContainer = Color(0xFF681a00),

        surfaceContainerLow = Color(0xFF1a1b21),
        surfaceContainer = Color(0xFF1e2025),
        surfaceContainerHigh = Color(0xFF282a2f),

        onSurface = Color(0xFFe2e2e9),
        onSurfaceVariant = Color(0xFFc4c6d4),

        outline = Color(0xFF8e909e),
        outlineVariant = Color(0xFF434652),

        background = Color(0xFF111318),
        onBackground = Color(0xFFe2e2e9),

        error = Color(0xFFffb4ab),
        errorDim = Color(0xFFff897d),
        errorContainer = Color(0xFF93000a),
        onError = Color(0xFF690005),
        onErrorContainer = Color(0xFFffdad6)
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun lifeOSWearTypography(): Typography {
    val googleSansFlex = FontFamily(
        Font(
            resId = R.font.google_sans_flex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.width(100f)
            )
        )
    )

    return Typography(
        defaultFontFamily = googleSansFlex,
        displayLarge = MaterialTheme.typography.displayLarge.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.W400
        ),
        displayMedium = MaterialTheme.typography.displayMedium.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.W400
        ),
        displaySmall = MaterialTheme.typography.displaySmall.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        titleSmall = MaterialTheme.typography.titleSmall.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        labelLarge = MaterialTheme.typography.labelLarge.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        labelMedium = MaterialTheme.typography.labelMedium.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        labelSmall = MaterialTheme.typography.labelSmall.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.W400
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.W400
        ),
        bodySmall = MaterialTheme.typography.bodySmall.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.Medium
        ),
        bodyExtraSmall = MaterialTheme.typography.bodyExtraSmall.copy(
            fontFamily = googleSansFlex,
            fontWeight = FontWeight.W400
        )
    )
}