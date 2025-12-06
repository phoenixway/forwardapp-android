package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun FAHeader(
    config: FAHeaderConfig,
    modifier: Modifier = Modifier
) {
    val glowAlpha by animateFloatAsState(
        targetValue = 0.15f,
        animationSpec = tween(2000),
        label = "header_glow"
    )

    val backgroundModifier = when (config.backgroundStyle) {
        FAHeaderBackground.Default -> Modifier.background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        FAHeaderBackground.Transparent -> Modifier.background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0f))
        FAHeaderBackground.Elevated -> Modifier.background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        FAHeaderBackground.Gradient -> Modifier.background(
            brush = Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )
        )
        FAHeaderBackground.CommandDeck -> Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f + glowAlpha),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f + glowAlpha)
                )
            )
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 4.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(backgroundModifier)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) { config.left?.invoke() }

                Box(
                    contentAlignment = Alignment.CenterEnd
                ) { config.right?.invoke() }
            }
        }
    }
}
