package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
internal fun NerIndicator(
    isActive: Boolean,
    hasText: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isActive && hasText,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "ner_indicator_transition")
        val scale by
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "ner_indicator_scale",
            )
        val alpha by
            infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 800),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "ner_indicator_alpha",
            )

        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Smart recognition active",
            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha),
            modifier = Modifier.size(18.dp).scale(scale),
        )
    }
}
