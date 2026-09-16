package com.miempresa.mclauncher.ui.theme

import androidx.compose.ui.graphics.Color

// === LUCYMC DESIGN SYSTEM ===

// Dark Theme (default)
val DarkPrimary = Color(0xFFBB86FC)
val DarkSecondary = Color(0xFF03DAC6)
val DarkTertiary = Color(0xFFFF7597)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOnBackground = Color(0xFFE1E1E1)
val DarkOnSurface = Color(0xFFE1E1E1)
val DarkOnSurfaceVariant = Color(0xFF9E9E9E)
val DarkOutline = Color(0xFF3C3C3C)
val DarkError = Color(0xFFCF6679)
val DarkOnError = Color(0xFF000000)

// Light Theme
val LightPrimary = Color(0xFF6750A4)
val LightSecondary = Color(0xFF625B71)
val LightTertiary = Color(0xFF7D5260)
val LightBackground = Color(0xFFFFFBFE)
val LightSurface = Color(0xFFFFFBFE)
val LightSurfaceVariant = Color(0xFFE7E0EC)
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnSurface = Color(0xFF1C1B1F)
val LightOnSurfaceVariant = Color(0xFF49454F)
val LightOutline = Color(0xFF79747E)
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)

// Accent colors (consistent across themes)
val NeonGreen = Color(0xFF00E676)
val NeonGreenDim = Color(0xFF00C853)
val CyberCyan = Color(0xFF00BCD4)
val CyberCyanDim = Color(0xFF0097A7)
val StatusOk = Color(0xFF4CAF50)
val StatusError = Color(0xFFEF5350)
val StatusWarn = Color(0xFFFFA726)

// Pre-computed alpha variants (avoids Color.copy() in composition)
val DarkSurface80 = DarkSurface.copy(alpha = 0.8f)
val DarkSurface60 = DarkSurface.copy(alpha = 0.6f)
val DarkSurface30 = DarkSurface.copy(alpha = 0.3f)
val DarkSurface15 = DarkSurface.copy(alpha = 0.15f)
val DarkSurface08 = DarkSurface.copy(alpha = 0.08f)
val DarkOutline30 = DarkOutline.copy(alpha = 0.3f)
val DarkOutline15 = DarkOutline.copy(alpha = 0.15f)
val DarkOnSurfaceVariant50 = DarkOnSurfaceVariant.copy(alpha = 0.5f)
val DarkOnSurfaceVariant40 = DarkOnSurfaceVariant.copy(alpha = 0.4f)

val LightSurface80 = LightSurface.copy(alpha = 0.8f)
val LightSurface60 = LightSurface.copy(alpha = 0.6f)
val LightOutline30 = LightOutline.copy(alpha = 0.3f)
val LightOutline15 = LightOutline.copy(alpha = 0.15f)

// Aliases for backward compatibility
val CyberDark = DarkBackground
val CyberPanel = DarkSurfaceVariant
val TextOnDark = DarkOnBackground
