package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import java.util.Locale

@Composable
fun FocusedProjectHeader(
    project: Context,
    onMoreActionsClick: () -> Unit,
    onProjectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.size(40.dp))
            ReservedRoleBadgeCompact(
                roleCode = project.roleCode,
                modifier = Modifier.padding(start = 8.dp, end = 6.dp),
            )
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).clickable(onClick = onProjectClick),
            )

            IconButton(onClick = onMoreActionsClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
            }
        }
    }
}

@Composable
private fun ReservedRoleBadgeCompact(
    roleCode: String?,
    modifier: Modifier = Modifier,
) {
    val normalized = roleCode?.trim()?.lowercase(Locale.ROOT)
    val badge =
        when (normalized) {
            ContextRoleRegistry.ROLE_PROJECT -> "✓" to Color(0xFF2E7D32)
            ContextRoleRegistry.ROLE_DIRECTION -> "↑" to Color(0xFF1565C0)
            ContextRoleRegistry.ROLE_MAIN_BEACON -> "✦" to Color(0xFFEF6C00)
            ContextRoleRegistry.ROLE_MANAGEMENT -> "⚙" to Color(0xFF6A1B9A)
            else -> null
        } ?: return

    Surface(
        modifier = modifier,
        color = badge.second.copy(alpha = 0.16f),
        contentColor = badge.second,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = badge.first,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
