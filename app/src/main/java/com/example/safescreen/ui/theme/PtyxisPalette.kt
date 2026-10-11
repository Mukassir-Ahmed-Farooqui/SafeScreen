package com.example.safescreen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

enum class PtyxisPalette(
    val displayName: String,
    val background: Long,
    val foreground: Long,
    val primaryC: Long,
    val secondaryC: Long,
    val tertiaryC: Long,
    val errorC: Long,
) {
    COBALT_NEON("Cobalt Neon", 0xFF142838, 0xFF8FF586, 0xFF8FF586, 0xFF3BA5FF, 0xFFE9E75C, 0xFFFF2320),
    NORD("Nord", 0xFF2E3440, 0xFFD8DEE9, 0xFF88C0D0, 0xFF81A1C1, 0xFF8FBCBB, 0xFFBF616A),
    SYNTHWAVE("Synthwave", 0xFF262335, 0xFFFFFFFF, 0xFFFF7EDB, 0xFF03EDF9, 0xFFFEDE5D, 0xFFFE4450),
}

fun buildPtyxisColorScheme(palette: PtyxisPalette): ColorScheme {
    val bg = Color(palette.background)
    val fg = Color(palette.foreground)
    val primary = Color(palette.primaryC)
    val secondary = Color(palette.secondaryC)
    val tertiary = Color(palette.tertiaryC)
    val error = Color(palette.errorC)
    val black = Color.Black

    fun onColor(c: Color) = if (c.luminance() < 0.5f) Color.White else Color(0xFF0A0A0A)
    fun container(c: Color) = lerp(bg, c, 0.22f)
    fun onContainer(c: Color) = lerp(fg, c, 0.20f)

    return darkColorScheme(
        primary = primary,
        onPrimary = onColor(primary),
        primaryContainer = container(primary),
        onPrimaryContainer = onContainer(primary),
        secondary = secondary,
        onSecondary = onColor(secondary),
        secondaryContainer = container(secondary),
        onSecondaryContainer = onContainer(secondary),
        tertiary = tertiary,
        onTertiary = onColor(tertiary),
        tertiaryContainer = container(tertiary),
        onTertiaryContainer = onContainer(tertiary),
        error = error,
        onError = onColor(error),
        errorContainer = container(error),
        onErrorContainer = onContainer(error),
        background = bg,
        onBackground = fg,
        surface = bg,
        onSurface = fg,
        surfaceVariant = lerp(bg, fg, 0.12f),
        onSurfaceVariant = lerp(fg, bg, 0.25f),
        outline = lerp(bg, fg, 0.40f),
        outlineVariant = lerp(bg, fg, 0.20f),
        scrim = black,
        inverseSurface = fg,
        inverseOnSurface = bg,
        inversePrimary = primary,
        surfaceDim = lerp(bg, black, 0.20f),
        surfaceBright = lerp(bg, fg, 0.14f),
        surfaceContainerLowest = lerp(bg, black, 0.40f),
        surfaceContainerLow = lerp(bg, black, 0.20f),
        surfaceContainer = lerp(bg, fg, 0.05f),
        surfaceContainerHigh = lerp(bg, fg, 0.09f),
        surfaceContainerHighest = lerp(bg, fg, 0.13f),
    )
}
