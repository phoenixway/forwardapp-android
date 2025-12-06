package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

 @Composable
fun FAHeader(
    config: FAHeaderConfig,
    modifier: Modifier = Modifier
) {
    val backgroundModifier = when (config.backgroundStyle) {
        FAHeaderBackground.Default -> Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        FAHeaderBackground.Transparent -> Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0f))
        FAHeaderBackground.Elevated -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        FAHeaderBackground.Gradient -> Modifier.background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(backgroundModifier)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) { config.left?.invoke() }

        Box(
            Modifier.weight(2f),
            contentAlignment = Alignment.Center
        ) { config.center?.invoke() }

        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) { config.right?.invoke() }
    }
}
