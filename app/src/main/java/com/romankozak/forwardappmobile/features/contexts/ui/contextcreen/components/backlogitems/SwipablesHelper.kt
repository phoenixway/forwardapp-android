package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.backlogitems

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SwipeActionIcon(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    val buttonSize = 60.dp
    Surface(
        onClick = onClick,
        modifier = Modifier.size(buttonSize),
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = getOnColorFor(color),
            )
        }
    }
}

@Composable
private fun getOnColorFor(color: Color): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (color) {
        colorScheme.primary -> colorScheme.onPrimary
        colorScheme.secondary -> colorScheme.onSecondary
        colorScheme.tertiary -> colorScheme.onTertiary
        colorScheme.error -> colorScheme.onError
        colorScheme.inversePrimary -> colorScheme.primary
        else -> colorScheme.onSurface
    }
}
