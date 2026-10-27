package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind

@Composable
fun WorkspaceOccurrencesDialog(
    onDismiss: () -> Unit,
    onOpen: (NavTarget.ContextHierarchy) -> Unit,
    viewModel: WorkspaceOccurrencesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Появи у Navigation") },
        text = {
            when {
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error != null -> Text(state.error!!)
                state.occurrences.isEmpty() -> Text("Цей Workspace ще не має появ у Navigation.")
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(state.occurrences, key = { it.placementId }) { occurrence ->
                        ListItem(
                            headlineContent = { Text(occurrence.path.joinToString(" › ")) },
                            supportingContent = { Text(if (occurrence.kind == PlacementKind.PRIMARY) "Основна поява" else "LINK · додаткова поява") },
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(occurrence.destination) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } },
    )
}
