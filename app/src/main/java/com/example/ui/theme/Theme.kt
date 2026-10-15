package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentEmerald,
    onPrimary = Color(0xFF042114),
    primaryContainer = Color(0xFF003820),
    onPrimaryContainer = Color(0xFF76F8BD),
    secondary = AccentCyan,
    onSecondary = Color(0xFF00325B),
    secondaryContainer = Color(0xFF0B3A60),
    onSecondaryContainer = Color(0xFFBCE1FF),
    tertiary = AccentPurple,
    background = TerminalBlack,
    onBackground = TextPrimary,
    surface = TerminalCardBg,
    onSurface = TextPrimary,
    surfaceVariant = TerminalCardBorder,
    onSurfaceVariant = TextSecondary,
    outline = TerminalCardBorder,
    outlineVariant = TerminalCardHover,
    error = AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFEAEFF5),
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Forced deliberate tech-aesthetic colors
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Dark developer look is best for code & logs, but supports both

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
