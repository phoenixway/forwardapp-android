package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip

@Composable
internal fun BacklogGoalWorkspaceBadge(title: String, onOpen: () -> Unit) {
    UnifiedMetaChip(text = "Workspace · $title", icon = Icons.Outlined.Workspaces, onClick = onOpen)
}
