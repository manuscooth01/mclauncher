package com.miempresa.mclauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LucyMcColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    tertiary = AccentTertiary,
    background = BackgroundDeep,
    surface = BackgroundCard,
    surfaceVariant = BackgroundElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = StatusOffline,
    onError = Color.White
)

@Composable
fun LucyMcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LucyMcColorScheme,
        typography = LucyMcTypography,
        content = content
    )
}
