package de.gruppenkalender.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val KinshipBlue = Color(0xFF2F62AD)
val KinshipBlueLight = Color(0xFF4A76C0)
val KinshipBluePale = Color(0xFFD9E5FF)
val KinshipOrange = Color(0xFFF2994A)
val KinshipGreen = Color(0xFF27AE60)
val KinshipRed = Color(0xFFBA1A1A)
val KinshipBackground = Color(0xFFF9F9FC)
val KinshipSurfaceLow = Color(0xFFF3F3F6)
val KinshipOutline = Color(0xFF737782)
val KinshipInk = Color(0xFF1A1C1E)

private val KinshipColors =
    lightColorScheme(
        primary = KinshipBlue,
        onPrimary = Color.White,
        primaryContainer = KinshipBlueLight,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF904D00),
        secondaryContainer = Color(0xFFFFDCC3),
        tertiary = Color(0xFF006D37),
        background = KinshipBackground,
        onBackground = KinshipInk,
        surface = Color.White,
        onSurface = KinshipInk,
        surfaceVariant = Color(0xFFE8E8EA),
        onSurfaceVariant = Color(0xFF434751),
        outline = KinshipOutline,
        outlineVariant = Color(0xFFC3C6D2),
        error = KinshipRed,
    )

private val KinshipTypography =
    Typography(
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.2.sp,
            ),
    )

@Composable
fun GroupCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KinshipColors,
        typography = KinshipTypography,
        content = content,
    )
}
