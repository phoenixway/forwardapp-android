package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Dashboard styling only. Other operational views retain their existing toolbar. */
@Composable
internal fun WorkspaceScreenHeader(
    dashboard: Boolean,
    title: String,
    glow: Float,
    onBack: () -> Unit,
    onOpenProperties: () -> Unit,
    normalLinks: @Composable (() -> Unit)?,
    normalToolbar: @Composable () -> Unit,
) {
    if (dashboard) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WORKSPACE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    FilledTonalIconButton(onClick = onOpenProperties) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Налаштування Workspace")
                    }
                }
            }
        }
    } else {
        Card(Modifier.fillMaxWidth().graphicsLayer { scaleX = glow; scaleY = glow },
            shape = RoundedCornerShape(0.dp), elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column { normalToolbar(); normalLinks?.invoke() }
        }
    }
}
