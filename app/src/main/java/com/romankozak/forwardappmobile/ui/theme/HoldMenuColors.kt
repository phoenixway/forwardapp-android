package com.romankozak.forwardappmobile.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class HoldMenuColors(
    val background: Color,
    val border: Color,
    val itemHoverBackground: Color,
    val itemText: Color,
    val itemTextMuted: Color,
    val tooltipBackground: Color,
    val tooltipText: Color,
    val scrim: Color,
)

val LocalHoldMenuColors = staticCompositionLocalOf {
    HoldMenuColors(
        background = Color(0xFF1F1F1F),
        border = Color(0x33FFFFFF),
        itemHoverBackground = Color(0x33FFFFFF),
        itemText = Color.White,
        itemTextMuted = Color(0xCCFFFFFF),
        tooltipBackground = Color(0xFF2A2A2A),
        tooltipText = Color.White,
        scrim = Color(0x66000000)
    )
}

fun holdMenuColorsFromScheme(
    colorScheme: ColorScheme,
    isDark: Boolean
): HoldMenuColors {
    val hoverAlpha = if (isDark) 0.22f else 0.14f
    val scrimAlpha = if (isDark) 0.55f else 0.35f
    val borderAlpha = if (isDark) 0.45f else 0.3f

    return HoldMenuColors(
        background = colorScheme.surface.copy(alpha = 0.96f),
        border = colorScheme.outlineVariant.copy(alpha = borderAlpha),
        itemHoverBackground = colorScheme.primary.copy(alpha = hoverAlpha),
        itemText = colorScheme.onSurface,
        itemTextMuted = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
        tooltipBackground = colorScheme.secondaryContainer.copy(alpha = 0.95f),
        tooltipText = colorScheme.onSecondaryContainer,
        scrim = colorScheme.scrim.copy(alpha = scrimAlpha)
    )
}
