package com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    shape: Shape = CircleShape,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    checkmarkColor: Color = Color.White
) {
    // Додаємо логування для відстеження стану
    LaunchedEffect(checked) {
        Log.d("CustomCheckbox", "Checkbox state changed to: $checked")
    }

    val interactionSource = remember { MutableInteractionSource() }

    // Анімація кольору для фону
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedColor,
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColorAnimation"
    )

    // Анімація кольору для рамки
    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) checkedColor else borderColor,
        animationSpec = tween(durationMillis = 200),
        label = "borderColorAnimation"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = animatedBorderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(), // Додаємо ripple для візуальної підказки
                role = Role.Checkbox,
                onClick = {
                    Log.d("CustomCheckbox", "Checkbox clicked! Current: $checked -> New: ${!checked}")
                    onCheckedChange(!checked)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Показуємо галочку без анімації для простоти дебагу
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = checkmarkColor,
                modifier = Modifier.size(size * 0.75f)
            )
        }
    }
}