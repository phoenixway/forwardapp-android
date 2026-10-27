package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.navigation.NavTarget

@Composable
internal fun WorkspaceDashboardRelationsCard(state: WorkspaceRelationsState, onOpen: (NavTarget) -> Unit, onViewAll: () -> Unit) {
    val model = remember(state.projection) { state.projection?.dashboardRelations() }
    val colors = MaterialTheme.colorScheme
    WorkspaceDashboardSection(title = "Semantics", icon = Icons.Outlined.Share, actions = {
        if (model?.source != null || model?.groups?.isNotEmpty() == true || state.enabled) {
            TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("View all", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp))
            }
        }
    }) {
        when {
            state.loading -> Text("Завантаження…", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
            state.error != null -> Text(state.error, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            model == null || (model.source == null && model.groups.isEmpty()) ->
                Text("Пов’язаних сутностей поки немає.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            else -> {
                model.source?.let { source ->
                    DashboardRelationRow(source.targetType, source.navigation?.let { destination -> { onOpen(destination) } }, source = true) {
                        Text(if (source.targetType == "ASPECT") "Linked aspect" else "Linked orientation",
                            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text(source.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Embodied by this workspace", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
                model.groups.forEachIndexed { index, group ->
                    if (model.source != null || index > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = colors.primary.copy(alpha = 0.08f))
                    DashboardRelationRow(group.targetType, onViewAll) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(group.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelLarge,
                                color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Surface(shape = RoundedCornerShape(5.dp), color = colors.primaryContainer.copy(alpha = 0.65f)) {
                                Text(group.count.toString(), Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall, color = colors.primary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(group.firstTitle, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (group.moreCount > 0) Text(" · +${group.moreCount} more", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRelationRow(type: String, onClick: (() -> Unit)?, source: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }
        .heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).background(colors.primaryContainer.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
            val icon: ImageVector = when (type) {
                "ASPECT" -> if (source) Icons.Outlined.Diamond else Icons.Outlined.Layers
                "ORIENTATION" -> Icons.Outlined.Adjust
                "WORKSPACE" -> Icons.Outlined.WorkOutline
                else -> Icons.Outlined.Description
            }
            Icon(icon, null, Modifier.size(23.dp), tint = colors.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = colors.primary)
    }
}
