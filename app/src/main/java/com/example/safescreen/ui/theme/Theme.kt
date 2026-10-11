package com.example.safescreen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun SafeScreenTheme(
    palette: PtyxisPalette = PtyxisPalette.COBALT_NEON,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(palette) { buildPtyxisColorScheme(palette) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
