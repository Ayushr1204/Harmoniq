package com.harmoniq.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Dynamic accent color that changes with album art
val LocalDynamicAccentColor = compositionLocalOf<Color> { Cyan }

@Composable
fun DynamicAccentColorProvider(
    accentColor: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDynamicAccentColor provides accentColor) {
        content()
    }
}

// Extension to get dynamic accent color
val dynamicAccent: Color
    @Composable get() = LocalDynamicAccentColor.current

