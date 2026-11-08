package com.example.myandroidapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Theme configured to adhere strictly to design.md (Cursor Editorial Design System).
 * - Warm cream canvas (#f7f7f4)
 * - Warm near-black ink (#26251e)
 * - Cursor Orange primary CTA (#f54e00)
 * - Hairline-only depth, no drop shadows
 */
private val AppColorScheme = lightColorScheme(
    primary = ColorPrimary,
    onPrimary = ColorOnPrimary,
    primaryContainer = ColorSurfaceStrong,
    onPrimaryContainer = ColorInk,
    secondary = ColorInk,
    onSecondary = ColorCanvas,
    background = ColorCanvas,
    onBackground = ColorInk,
    surface = ColorSurfaceCard,
    onSurface = ColorInk,
    surfaceVariant = ColorCanvasSoft,
    onSurfaceVariant = ColorBody,
    outline = ColorHairlineStrong,
    outlineVariant = ColorHairline,
    error = ColorSemanticError,
    onError = ColorOnPrimary
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
