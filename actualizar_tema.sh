#!/bin/bash

# Crear backup
cp -r ~/mclauncher/app/src/main/java/com/miempresa/mclauncher/ui/theme ~/mclauncher/theme_backup_$(date +%s)

# Crear Color.kt nuevo
cat > ~/mclauncher/app/src/main/java/com/miempresa/mclauncher/ui/theme/Color.kt << 'EOF'
package com.miempresa.mclauncher.ui.theme

import androidx.compose.ui.graphics.Color

// === PALETA PREMIUM LUCYMC 2.0 ===

// Fondos
val BackgroundDeep = Color(0xFF0A0A0F)
val BackgroundCard = Color(0xFF141419)
val BackgroundElevated = Color(0xFF1C1C24)

// Acentos
val AccentPrimary = Color(0xFF7B61FF)
val AccentSecondary = Color(0xFF00D4AA)
val AccentTertiary = Color(0xFFFF6B6B)

// Estados
val StatusOnline = Color(0xFF00D4AA)
val StatusOffline = Color(0xFFFF6B6B)
val StatusWarning = Color(0xFFFFB800)

// Texto
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF8E8E93)
val TextMuted = Color(0xFF636366)

// Bordes
val BorderSubtle = Color(0xFF2C2C3A)
val BorderGlow = Color(0xFF7B61FF).copy(alpha = 0.3f)

// Compatibilidad antigua
val NeonGreen = AccentSecondary
val CyberCyan = AccentPrimary
val CyberDark = BackgroundDeep
val CyberSurface = BackgroundCard
val CyberPanel = BackgroundElevated
val CyanNeon = AccentPrimary
val BlueElectric = AccentSecondary
val CardBorder = BorderSubtle
EOF

# Crear Type.kt
cat > ~/mclauncher/app/src/main/java/com/miempresa/mclauncher/ui/theme/Type.kt << 'EOF'
package com.miempresa.mclauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LucyMcTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = (-1).sp,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)
EOF

# Actualizar Theme.kt
cat > ~/mclauncher/app/src/main/java/com/miempresa/mclauncher/ui/theme/Theme.kt << 'EOF'
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
EOF

echo "✅ Tema actualizado a LucyMC 2.0 Premium"
echo "🎨 Nuevos colores: Púrpura, Menta, Coral"
echo "📄 Archivos modificados:"
echo "   - Color.kt"
echo "   - Theme.kt"
echo "   - Type.kt (nuevo)"
