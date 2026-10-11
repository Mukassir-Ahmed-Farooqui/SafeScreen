package com.example.safescreen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    background = Ink,
    onBackground = TextHigh,
    surface = Slate,
    onSurface = TextHigh,
    surfaceVariant = SlateHigh,
    onSurfaceVariant = TextMuted,
    error = Danger
)

private val LightColors = lightColorScheme(
    primary = MintDark,
    onPrimary = Cloud,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = Color(0xFF5B6B84),
    error = Danger
)

@Composable
fun SafeScreenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
