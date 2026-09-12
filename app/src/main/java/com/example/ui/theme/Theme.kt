package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeometricBalanceColorScheme = darkColorScheme(
    primary = VoltPurpleAccent,
    onPrimary = Color.Black,
    primaryContainer = VoltPurpleContainer,
    onPrimaryContainer = VoltPurpleLight,
    secondary = VoltPurpleLight,
    onSecondary = Color.Black,
    tertiary = VoltGreenStatus,
    onTertiary = Color.Black,
    background = VoltDarkBackground,
    onBackground = VoltTextPrimary,
    surface = VoltSurface,
    onSurface = VoltTextPrimary,
    surfaceVariant = VoltSurfaceVariant,
    onSurfaceVariant = VoltTextSecondary,
    outline = VoltSurfaceBorder,
    outlineVariant = VoltSurfaceBorderLight
)

@Composable
fun VoltTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GeometricBalanceColorScheme,
        typography = Typography,
        content = content
    )
}
