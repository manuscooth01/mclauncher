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

// Aliases de color para la UI
val TextOnDark = TextPrimary
val TextMutedColor = TextMuted
val StatusOk = StatusOnline
val StatusError = StatusOffline
val StatusWarn = StatusWarning
