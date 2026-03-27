package com.example.birdtrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandLightBlue,
    secondary = BrandOrange,
    tertiary = BrandLeafGreen,
    background = DarkCharcoalGreen,
    surface = DarkCharcoalGreen,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,         // Brand Orange for key actions and titles
    onPrimary = Color.White,
    secondary = BrandLightBlue,    // Brand Light Blue for accents
    onSecondary = Color.Black,
    tertiary = BrandLeafGreen,     // Brand Green for secondary details
    background = BrandDarkBlue,    // Dark Blue background from your image
    onBackground = Color.White,    // White text on the dark blue background
    surface = Color(0xFFF0F4F8),   // Very light surface for cards (like the paper in the icon)
    onSurface = BrandDarkBlue,     // Dark blue text on light cards for perfect visibility
    outline = BrandLightBlue,      // Light blue for lines and borders
    surfaceVariant = Color(0xFFE1E8F0),
    onSurfaceVariant = BrandDarkBlue
)

@Composable
fun BirdTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
