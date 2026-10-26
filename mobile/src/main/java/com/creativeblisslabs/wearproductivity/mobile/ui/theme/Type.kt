package com.creativeblisslabs.wearproductivity.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.wearproductivity.mobile.R

@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f)
        )
    )
)

val LifeOSTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)