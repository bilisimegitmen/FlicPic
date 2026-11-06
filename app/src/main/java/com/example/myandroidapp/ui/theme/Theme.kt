package com.example.myandroidapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = ColorYellow2,
    onPrimary = ColorDark0,
    primaryContainer = ColorOrange2,
    onPrimaryContainer = ColorYellow2,
    secondary = ColorOrange3,
    onSecondary = ColorDark0,
    background = ColorDark0,
    onBackground = ColorTextPrimary,
    surface = ColorDark1,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorRed1,
    onSurfaceVariant = ColorTextSecondary,
    error = ColorRed3,
    onError = ColorTextPrimary
)

@Composable
fun MyAndroidAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}

