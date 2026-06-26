package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class BottomPanelColors(
    val container: Color,
    val content: Color,
    val mutedContent: Color,
    val border: Color,
    val action: Color,
    val selectedActionContainer: Color,
    val selectedActionContent: Color,
    val inputContainer: Color,
    val inputBorder: Color,
    val primaryActionContainer: Color,
    val primaryActionContent: Color,
)

@Composable
fun bottomPanelColors(): BottomPanelColors {
    val colorScheme = MaterialTheme.colorScheme
    val tintedContainer =
        lerp(
            colorScheme.surfaceContainerLow,
            colorScheme.primaryContainer,
            BottomPanelTokens.ContainerPrimaryTint,
        )
    val tintedInput =
        lerp(
            colorScheme.surfaceContainerHigh,
            colorScheme.primaryContainer,
            BottomPanelTokens.InputPrimaryTint,
        )
    val tintedBorder =
        lerp(
            colorScheme.outlineVariant,
            colorScheme.primary,
            BottomPanelTokens.BorderPrimaryTint,
        )
    return BottomPanelColors(
        container = tintedContainer,
        content = colorScheme.onSurface,
        mutedContent = colorScheme.onSurfaceVariant,
        border = tintedBorder.copy(alpha = BottomPanelTokens.ContainerBorderAlpha),
        action = colorScheme.primary.copy(alpha = BottomPanelTokens.ActionContentAlpha),
        selectedActionContainer = colorScheme.primaryContainer,
        selectedActionContent = colorScheme.onPrimaryContainer,
        inputContainer = tintedInput,
        inputBorder = colorScheme.primary.copy(alpha = BottomPanelTokens.InputBorderAlpha),
        primaryActionContainer = colorScheme.primary,
        primaryActionContent = colorScheme.onPrimary,
    )
}
