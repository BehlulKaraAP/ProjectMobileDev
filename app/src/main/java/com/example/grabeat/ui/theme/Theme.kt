package com.example.grabeat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GrabEatLightGreen,
    secondary = GrabEatGreen,
    tertiary = GrabEatLightGreen,
    background = GrabEatDarkBackground,
    surface = Color(0xFF1A1C1A),
    onPrimary = Color(0xFF003300),
    onBackground = Color(0xFFE3E3E3),
    onSurface = Color(0xFFE3E3E3)
)

private val LightColorScheme = lightColorScheme(
    primary = GrabEatGreen,
    secondary = GrabEatDarkGreen,
    tertiary = GrabEatLightGreen,
    background = GrabEatBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun GrabEatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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