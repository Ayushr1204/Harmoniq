package com.harmoniq.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - Cyan accent like in the reference
val Cyan = Color(0xFF00D4FF)
val CyanLight = Color(0xFF5EECFF)
val CyanDark = Color(0xFF00A0C4)

// Background Colors
val BackgroundDark = Color(0xFF000000)
val BackgroundCard = Color(0xFF121212)
val BackgroundElevated = Color(0xFF1A1A1A)
val BackgroundSurface = Color(0xFF0D0D0D)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Color(0xFF6B6B6B)
val TextDisabled = Color(0xFF404040)

// Gradient Colors for dynamic backgrounds
val GradientStart = Color(0xFF1A1A2E)
val GradientMid = Color(0xFF16213E)
val GradientEnd = Color(0xFF0F3460)

// Status Colors
val Success = Color(0xFF1DB954)
val Error = Color(0xFFE53935)
val Warning = Color(0xFFFFB300)

// Lyrics Colors
val LyricActive = Color(0xFFFFFFFF)
val LyricInactive = Color(0xFF4A4A4A)
val LyricHighlight = Color(0xFF00D4FF)

// Card Colors
val CardBackground = Color(0xFF181818)
val CardBackgroundHover = Color(0xFF282828)

// Player Colors
val SliderActive = Color(0xFF00D4FF)
val SliderInactive = Color(0xFF404040)
val SliderThumb = Color(0xFFFFFFFF)

// Light Theme Colors
val BackgroundLight = Color(0xFFF5F5F7)
val BackgroundCardLight = Color(0xFFFFFFFF)
val BackgroundElevatedLight = Color(0xFFE8E8ED)
val BackgroundSurfaceLight = Color(0xFFFAFAFC)
val TextPrimaryLight = Color(0xFF000000)  // Jet black
val TextSecondaryLight = Color(0xFF000000)  // Jet black
val TextTertiaryLight = Color(0xFF3C3C3C)  // Dark gray for less prominent elements
val TextDisabledLight = Color(0xFF9E9E9E)
val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBackgroundHoverLight = Color(0xFFF2F2F7)

// Helper function to convert accent color string to Color
fun getAccentColorFromString(colorName: String): Color {
    return when (colorName.lowercase()) {
        "cyan" -> Color(0xFF00D4FF)
        "purple" -> Color(0xFFBB86FC)
        "green" -> Color(0xFF4CAF50)
        "orange" -> Color(0xFFFF9800)
        "pink" -> Color(0xFFE91E63)
        "blue" -> Color(0xFF2196F3)
        "yellow" -> Color(0xFFFFEB3B)
        "white" -> Color(0xFFFFFFFF)
        else -> Cyan // Default to cyan
    }
}

