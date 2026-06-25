package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
    return BottomPanelColors(
        container = colorScheme.surfaceContainerLow,
        content = colorScheme.onSurface,
        mutedContent = colorScheme.onSurfaceVariant,
        border = colorScheme.outlineVariant.copy(alpha = BottomPanelTokens.ContainerBorderAlpha),
        action = colorScheme.onSurfaceVariant.copy(alpha = BottomPanelTokens.ActionContentAlpha),
        selectedActionContainer = colorScheme.primaryContainer,
        selectedActionContent = colorScheme.onPrimaryContainer,
        inputContainer = colorScheme.surfaceContainerHigh,
        inputBorder = colorScheme.outlineVariant.copy(alpha = BottomPanelTokens.InputBorderAlpha),
        primaryActionContainer = colorScheme.primary,
        primaryActionContent = colorScheme.onPrimary,
    )
}
