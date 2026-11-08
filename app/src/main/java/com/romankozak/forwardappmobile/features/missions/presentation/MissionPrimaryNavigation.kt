package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip

internal data class MissionPrimaryNavigation(
    val targets: Map<Long, MissionPrimaryTarget>,
    val open: (Long) -> Unit,
)

/** One reactive snapshot per list. No per-card repository reads. */
@Composable
internal fun rememberMissionPrimaryNavigation(): MissionPrimaryNavigation {
    val model: MissionSemanticWorkspaceViewModel = hiltViewModel()
    val targets by model.targets.collectAsStateWithLifecycle()
    return MissionPrimaryNavigation(targets, model::openPrimary)
}

@Composable
internal fun MissionWorkspaceBadge(workspace: MissionSemanticWorkspaceAction.Open?, onOpen: () -> Unit) {
    workspace?.let {
        UnifiedMetaChip(text = "Workspace · ${it.title}", icon = Icons.Outlined.Workspaces, onClick = onOpen)
    }
}
