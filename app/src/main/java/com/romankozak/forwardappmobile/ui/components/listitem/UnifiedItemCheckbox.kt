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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun UnifiedItemCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    style: UnifiedCheckboxStyle,
    modifier: Modifier = Modifier,
    colors: UnifiedCheckboxColors = unifiedCheckboxColors(),
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
            color = if (checked) colors.checked else Color.Transparent,
            border = if (checked) null else BorderStroke(2.dp, colors.uncheckedBorder),
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
                        tint = colors.checkmark,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
