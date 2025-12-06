package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Новий основний варіант:
 *  - приймає HeaderLayout (стратегія розкладки)
 *  - окремо приймає фон через FAHeaderBackground
 */
@Composable
fun FAHeader(
    layout: HeaderLayout,
    backgroundStyle: FAHeaderBackground = FAHeaderBackground.Default,
    modifier: Modifier = Modifier
) {
    val glowAlpha by animateFloatAsState(
        targetValue = 0.15f,
        animationSpec = tween(2000),
        label = "header_glow"
    )

    val backgroundModifier = when (backgroundStyle) {
        FAHeaderBackground.Default -> Modifier.background(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
        FAHeaderBackground.Transparent -> Modifier.background(
            color = Color.Transparent
        )
        FAHeaderBackground.Elevated -> Modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
        // status bar inset
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                layout.Content()
            }
        }
    }
}

/**
 * Старий варіант для сумісності:
 *  - приймає FAHeaderConfig (left/center/right)
 *  - адаптер у LeftCenterCombinedHeaderLayout
 */
@Composable
fun FAHeader(
    config: FAHeaderConfig,
    modifier: Modifier = Modifier
) {
    val layout = LeftCenterCombinedHeaderLayout(
        left = config.left,
        center = config.center,
        right = config.right
    )
    FAHeader(
        layout = layout,
        backgroundStyle = config.backgroundStyle,
        modifier = modifier
    )
}
