package com.harmoniq.app.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractAccentColorFromImage(
    imageUrl: String,
    imageLoader: ImageLoader,
    context: Context,
    defaultColor: Color = Color(0xFF00D4FF)
): Color = withContext(Dispatchers.IO) {
    try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(200) // Reduce size for faster processing
            .build()
        
        val result = imageLoader.execute(request)
        
        if (result is SuccessResult) {
            val drawable = result.drawable
            val bitmap = drawable.toBitmap()
            
            // Use async palette generation for better performance
            val palette = Palette.from(bitmap)
                .maximumColorCount(16) // Reduce color count for faster processing
                .generate()
            
            // Try multiple swatches to get the best accent color
            // Priority: Vibrant > Light Vibrant > Dark Vibrant > Muted > Dominant
            val vibrant = palette.vibrantSwatch
            val lightVibrant = palette.lightVibrantSwatch
            val darkVibrant = palette.darkVibrantSwatch
            val muted = palette.mutedSwatch
            val dominant = palette.dominantSwatch
            
            val swatch = vibrant ?: lightVibrant ?: darkVibrant ?: muted ?: dominant
            
            val color = if (swatch != null) {
                // Use the RGB from the swatch
                Color(swatch.rgb)
            } else {
                defaultColor
            }
            
            // Ensure color is vibrant and visible
            val adjustedColor = adjustColorForAccent(color)
            return@withContext adjustedColor
        }
    } catch (e: Exception) {
        android.util.Log.e("ColorExtractor", "Error extracting color: ${e.message}")
    }
    
    defaultColor
}

private fun adjustColorForAccent(color: Color): Color {
    val hsl = colorToHSL(color)
    
    // Keep the original hue (the actual color from the album art)
    val hue = hsl[0]
    
    // Boost saturation to make it more vibrant (but don't overdo it)
    val adjustedSaturation = hsl[1].coerceIn(0.4f, 0.9f)
    
    // Ensure lightness is in a visible range (not too dark, not too light)
    // Prefer slightly lighter colors for better visibility
    val adjustedLightness = hsl[2].coerceIn(0.35f, 0.75f)
    
    return hslToColor(floatArrayOf(hue, adjustedSaturation, adjustedLightness))
}

private fun colorToHSL(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val l = (max + min) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))
    
    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta + (if (g < b) 6f else 0f)) / 6f
        max == g -> ((b - r) / delta + 2f) / 6f
        else -> ((r - g) / delta + 4f) / 6f
    }
    
    return floatArrayOf(h * 360f, s, l)
}

private fun hslToColor(hsl: FloatArray): Color {
    val h = hsl[0] / 360f
    val s = hsl[1]
    val l = hsl[2]
    
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h * 6f) % 2f - 1f))
    val m = l - c / 2f
    
    val (r, g, b) = when {
        h < 1f / 6f -> Triple(c, x, 0f)
        h < 2f / 6f -> Triple(x, c, 0f)
        h < 3f / 6f -> Triple(0f, c, x)
        h < 4f / 6f -> Triple(0f, x, c)
        h < 5f / 6f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

