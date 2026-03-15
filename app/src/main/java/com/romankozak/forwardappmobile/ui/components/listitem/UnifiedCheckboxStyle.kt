package com.romankozak.forwardappmobile.ui.components.listitem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class UnifiedCheckboxStyle {
    Round,
    Square,
}

data class UnifiedCheckboxColors(
    val checked: Color,
    val uncheckedBorder: Color,
    val checkmark: Color,
)

@Composable
fun unifiedCheckboxColors(
    checked: Color = MaterialTheme.colorScheme.primary,
    uncheckedBorder: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    checkmark: Color = MaterialTheme.colorScheme.onPrimary,
): UnifiedCheckboxColors = UnifiedCheckboxColors(
    checked = checked,
    uncheckedBorder = uncheckedBorder,
    checkmark = checkmark,
)
