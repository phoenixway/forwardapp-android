package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    shape: Shape = RoundedCornerShape(3.dp),
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    checkmarkColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Анімація кольору для фону
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedColor,
        animationSpec = tween(durationMillis = 200), // Плавний перехід
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
                color = backgroundColor, // Використовуємо анімований колір
                shape = shape
            )
            .border(
                width = 1.dp,
                color = animatedBorderColor, // Використовуємо анімований колір
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Анімація появи та зникнення галочки
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = checkmarkColor,
                modifier = Modifier.size(size * 0.75f) // 👈 Трохи більша іконка
            )
        }
    }
}