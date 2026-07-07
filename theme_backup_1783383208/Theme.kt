package com.miempresa.mclauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LucyMcColorScheme = darkColorScheme(
    primary = CyanNeon,
    secondary = BlueElectric,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSecondary = TextPrimary
)

@Composable
fun LucyMcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LucyMcColorScheme,
        content = content
    )
}
