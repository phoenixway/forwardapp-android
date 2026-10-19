package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel

/** Dashboard consumer of the existing canonical binding/catalog read model. No mutation commands. */
@Composable
fun WorkspaceDashboardSemanticsCard(
    onOpen: (NavTarget.SemanticEntityEditor) -> Unit,
    viewModel: WorkspaceLinkedSemanticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkspaceDashboardSemanticsContent(state, onOpen)
}

@Composable
internal fun WorkspaceDashboardSemanticsContent(
    state: WorkspaceLinkedSemanticsUiState,
    onOpen: (NavTarget.SemanticEntityEditor) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Семантичні зв’язки", style = MaterialTheme.typography.titleMedium)
            when (state) {
                WorkspaceLinkedSemanticsUiState.Loading -> Text("Завантаження зв’язків…")
                is WorkspaceLinkedSemanticsUiState.Unavailable -> Text(state.message)
                is WorkspaceLinkedSemanticsUiState.Ready -> {
                    if (state.links.isEmpty()) Text("Семантичних зв’язків поки немає.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.links.forEach { link ->
                        val target = link.navigationTarget()
                        ListItem(
                            headlineContent = { Text(link.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(link.dashboardDescription()) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth().clickable(enabled = target != null) {
                                target?.let(onOpen)
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun WorkspaceLinkedSemanticEntity.dashboardDescription(): String =
    "${ref?.entityClass?.displayLabel() ?: "Недоступна сутність"} · ${bindingTypes.joinToString(", ")}"
