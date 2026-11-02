package com.creativeblisslabs.wearproductivity.wear.ui.dashboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Watch Face Styles matching modern Pixel Watch & Galaxy Watch watch face catalogs.
 */
enum class WatchFaceStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    MODULAR_COMPACT("MODULAR", "Modular Dashboard", "6 Complications + Bezel progress ring"),
    ANALOG_CHRONO("ANALOG_CHRONO", "Analog Chronometer", "Sub-dials with hour/min hands & sweep second"),
    DIGITAL_BIG("DIGITAL_BIG", "Digital Big Numerals", "Stacked hour/min display with minimal complications"),
    PIXEL_CONCENTRIC("PIXEL_CONCENTRIC", "Pixel Concentric", "Circular time tracks inspired by Pixel Watch"),
    SAMSUNG_SPORT("SAMSUNG_SPORT", "Galaxy Ultra Sport", "Dual telemetry gauges for heart & active calories");

    companion object {
        fun fromId(id: String): WatchFaceStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MODULAR_COMPACT
    }
}

/**
 * Font Customization options for Watch Face Time display.
 */
enum class WatchFaceFont(
    val id: String,
    val label: String,
    val fontFamily: FontFamily
) {
    GOOGLE_SANS("GOOGLE_SANS", "Google Sans", FontFamily.Default),
    ROBOTO_MONO("ROBOTO_MONO", "Monospace", FontFamily.Monospace),
    CLASSIC_SERIF("CLASSIC_SERIF", "Classic Serif", FontFamily.Serif),
    SANS_SERIF("SANS_SERIF", "Clean Sans", FontFamily.SansSerif);

    companion object {
        fun fromId(id: String): WatchFaceFont =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GOOGLE_SANS
    }
}

/**
 * Accent color schemes for Watch Face dial, rings and active complications.
 */
enum class WatchFacePalette(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color
) {
    COBALT("COBALT", "Pixel Cobalt", Color(0xFF8EAEFF), Color(0xFF68B6FF)),
    MINT("MINT", "Emerald Mint", Color(0xFF79E2B0), Color(0xFF4BB894)),
    CORAL("CORAL", "Solar Coral", Color(0xFFFF9A7A), Color(0xFFFF7A9A)),
    AMBER("AMBER", "Cyber Amber", Color(0xFFFFD177), Color(0xFFFFB76B)),
    LAVENDER("LAVENDER", "Deep Lavender", Color(0xFFBBA5FF), Color(0xFFA6B8FF)),
    MONOCHROME("MONOCHROME", "Monochrome Minimal", Color(0xFFE2E2E9), Color(0xFF8E909E));

    companion object {
        fun fromId(id: String): WatchFacePalette =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: COBALT
    }
}
