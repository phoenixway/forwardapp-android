package com.romankozak.forwardappmobile.ui.components.listitem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class UnifiedCheckboxStyle {
    Round,
    Square,
}

@Composable
fun UnifiedItemCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    style: UnifiedCheckboxStyle,
    modifier: Modifier = Modifier,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedBorderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    checkmarkColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val shape: Shape =
        when (style) {
            UnifiedCheckboxStyle.Round -> CircleShape
            UnifiedCheckboxStyle.Square -> RoundedCornerShape(4.dp)
        }

    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.size(32.dp),
    ) {
        Surface(
            shape = shape,
            color = if (checked) checkedColor else Color.Transparent,
            border = if (checked) null else BorderStroke(2.dp, uncheckedBorderColor),
            modifier = Modifier.size(18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = checkmarkColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
