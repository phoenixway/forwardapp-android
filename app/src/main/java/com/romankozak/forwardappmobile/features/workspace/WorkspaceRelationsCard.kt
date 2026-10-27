package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.navigation.NavTarget

/** Replaces, rather than duplicates, the semantic card while the capability is enabled. */
@Composable
fun WorkspaceRelationsCard(onOpen: (NavTarget) -> Unit, viewModel: WorkspaceRelationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.enabled) {
        WorkspaceDashboardSemanticsCard(onOpen = onOpen)
        return
    }
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Semantics", style = MaterialTheme.typography.titleMedium)
            WorkspaceRelationsContent(state, onOpen)
        }
    }
}

/** Existing Workspace quick-entry location, available also outside Dashboard view. */
@Composable
fun WorkspaceRelationsEntry(onOpen: (NavTarget) -> Unit, onShowRelations: () -> Unit, viewModel: WorkspaceRelationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.enabled) {
        WorkspaceLinkedSemanticsSection(onOpen = onOpen)
        return
    }
    TextButton(onClick = onShowRelations) {
        Text("Semantics (${state.projection?.entityHubGroups()?.sumOf { it.rows.size } ?: 0})")
    }
}

/** Full Workspace view backed by the same read-only capability projection as Dashboard. */
@Composable
fun WorkspaceRelationsView(
    onOpen: (NavTarget) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkspaceRelationsViewModel = hiltViewModel(),
    workspaceTitle: String = "Workspace",
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.enabled) when {
            state.loading -> Text("Завантаження зв’язків…")
            state.error != null -> Text(state.error!!)
            state.projection?.relations.isNullOrEmpty() -> Text("Прямих зв’язків поки немає.")
            else -> WorkspaceRelationshipPanel(requireNotNull(state.projection), workspaceTitle, onOpen)
        }
    }
}

@Composable
internal fun WorkspaceRelationsContent(state: WorkspaceRelationsState, onOpen: (NavTarget) -> Unit) {
    when {
        state.loading -> Text("Завантаження зв’язків…")
        state.error != null -> Text(state.error)
        state.projection?.relations.isNullOrEmpty() -> Text("Прямих зв’язків поки немає.")
        else -> {
            WorkspaceSemanticContextActions(state.projection, onOpen)
            WorkspaceEntityHubGroups(state.projection.entityHubGroups(), onOpen)
        }
    }
}

