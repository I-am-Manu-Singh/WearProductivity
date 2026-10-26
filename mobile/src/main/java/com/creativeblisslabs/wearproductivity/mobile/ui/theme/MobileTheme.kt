package com.creativeblisslabs.wearproductivity.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Base Dark Colors
private val DarkBackground = Color(0xFF101010)
private val DarkSurface = Color(0xFF1C1C1E)
private val DarkSurfaceVariant = Color(0xFF2C2C2E)

// Mint Palette
private val MintPrimary = Color(0xFF79E2B0)
private val MintOnPrimary = Color(0xFF003820)
private val MintPrimaryContainer = Color(0xFF005231)
private val MintOnPrimaryContainer = Color(0xFF96FFCB)

// Coral Palette
private val CoralPrimary = Color(0xFFFF9A7A)
private val CoralOnPrimary = Color(0xFF380D00)
private val CoralPrimaryContainer = Color(0xFF551900)
private val CoralOnPrimaryContainer = Color(0xFFFFB59B)

// Violet Palette
private val VioletPrimary = Color(0xFFC4B5FD)
private val VioletOnPrimary = Color(0xFF2E1A47)
private val VioletPrimaryContainer = Color(0xFF452B6A)
private val VioletOnPrimaryContainer = Color(0xFFE0D4FF)

// Gold Palette
private val GoldPrimary = Color(0xFFFFD177)
private val GoldOnPrimary = Color(0xFF3D2E00)
private val GoldPrimaryContainer = Color(0xFF5A4400)
private val GoldOnPrimaryContainer = Color(0xFFFFE19D)

// Blue Palette
private val BluePrimary = Color(0xFF8EAEFF)
private val BlueOnPrimary = Color(0xFF00297B)
private val BluePrimaryContainer = Color(0xFF003DAA)
private val BlueOnPrimaryContainer = Color(0xFFD8E2FF)

fun getDarkColorScheme(paletteName: String) = when (paletteName) {
    "Coral" -> darkColorScheme(
        primary = CoralPrimary,
        onPrimary = CoralOnPrimary,
        primaryContainer = CoralPrimaryContainer,
        onPrimaryContainer = CoralOnPrimaryContainer,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant
    )
    "Violet" -> darkColorScheme(
        primary = VioletPrimary,
        onPrimary = VioletOnPrimary,
        primaryContainer = VioletPrimaryContainer,
        onPrimaryContainer = VioletOnPrimaryContainer,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant
    )
    "Gold" -> darkColorScheme(
        primary = GoldPrimary,
        onPrimary = GoldOnPrimary,
        primaryContainer = GoldPrimaryContainer,
        onPrimaryContainer = GoldOnPrimaryContainer,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant
    )
    "Blue" -> darkColorScheme(
        primary = BluePrimary,
        onPrimary = BlueOnPrimary,
        primaryContainer = BluePrimaryContainer,
        onPrimaryContainer = BlueOnPrimaryContainer,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant
    )
    else -> darkColorScheme( // Default Mint
        primary = MintPrimary,
        onPrimary = MintOnPrimary,
        primaryContainer = MintPrimaryContainer,
        onPrimaryContainer = MintOnPrimaryContainer,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant
    )
}

@Composable
fun LifeOSMobileTheme(
    paletteName: String = "Mint",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getDarkColorScheme(paletteName) // Force dark mode palette mapping for simplicity in this MVP
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
