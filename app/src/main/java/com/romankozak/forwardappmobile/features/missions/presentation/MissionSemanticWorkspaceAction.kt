package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.semanticeditor.EmbodyingWorkspaceDialogContent

/** Creation dialog outlives the dismissed card menu; one ViewModel per screen, never per row. */
@Composable
internal fun MissionWorkspaceDialogHost(model: MissionSemanticWorkspaceViewModel = hiltViewModel()) {
    val state by model.creation.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdId) { model.consumeCreated()?.let(model::openWorkspace) }
    EmbodyingWorkspaceDialogContent(state, model::setName, model::setDescription, model::confirm, model::cancel)
}

@Composable
internal fun MissionWorkspaceMenuAction(missionId: Long, model: MissionSemanticWorkspaceViewModel = hiltViewModel(), onDismiss: () -> Unit) {
    val actions by model.actions.collectAsStateWithLifecycle()
    MissionWorkspaceActionItem(actions[missionId]) { model.perform(missionId); onDismiss() }
}

@Composable
internal fun MissionWorkspaceActionItem(action: MissionSemanticWorkspaceAction?, onClick: () -> Unit) {
    if (action == null) return
    ListItem(
        headlineContent = { Text(if (action is MissionSemanticWorkspaceAction.Open) "Відкрити робочий простір" else "Створити робочий простір для орієнтира…") },
        supportingContent = if (action is MissionSemanticWorkspaceAction.Open) ({ Text(action.title) }) else null,
        leadingContent = { Icon(Icons.Outlined.Workspaces, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    SubtleActionDivider()
}
