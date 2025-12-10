package com.harmoniq.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = BackgroundDark,
    primaryContainer = CyanDark,
    onPrimaryContainer = TextPrimary,
    
    secondary = CyanLight,
    onSecondary = BackgroundDark,
    secondaryContainer = BackgroundElevated,
    onSecondaryContainer = TextPrimary,
    
    tertiary = Cyan,
    onTertiary = BackgroundDark,
    
    background = BackgroundDark,
    onBackground = TextPrimary,
    
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    
    surfaceVariant = BackgroundCard,
    onSurfaceVariant = TextSecondary,
    
    error = Error,
    onError = TextPrimary,
    
    outline = TextTertiary,
    outlineVariant = TextDisabled
)

private val LightColorScheme = lightColorScheme(
    primary = Cyan,
    onPrimary = Color.White,
    primaryContainer = CyanLight,
    onPrimaryContainer = TextPrimaryLight,
    
    secondary = CyanDark,
    onSecondary = Color.White,
    secondaryContainer = BackgroundElevatedLight,
    onSecondaryContainer = TextPrimaryLight,
    
    tertiary = Cyan,
    onTertiary = Color.White,
    
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    
    surface = BackgroundSurfaceLight,
    onSurface = TextPrimaryLight,
    
    surfaceVariant = BackgroundCardLight,
    onSurfaceVariant = TextSecondaryLight,
    
    error = Error,
    onError = Color.White,
    
    outline = TextTertiaryLight,
    outlineVariant = TextDisabledLight
)

@Composable
fun HarmoniqTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        "system" -> systemDarkTheme
        else -> true
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

