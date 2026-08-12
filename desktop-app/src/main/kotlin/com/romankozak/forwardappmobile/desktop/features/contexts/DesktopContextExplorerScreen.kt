package com.romankozak.forwardappmobile.desktop.features.contexts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextTreeNode
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

private const val INDENT_WIDTH_DP = 20

@Composable
fun DesktopContextExplorerScreen(
    dependencies: DesktopWorkspaceDependencies = rememberDesktopWorkspaceDependencies(),
    initialContextId: String? = null,
    refreshKey: Long = 0L,
) {
    val scope = rememberCoroutineScope()
    val store =
        remember(dependencies.observeContextTree, dependencies.observeBacklog, scope, refreshKey) {
            DesktopWorkspaceStore(
                observeContextTree = dependencies.observeContextTree,
                observeBacklog = dependencies.observeBacklog,
                createContext = dependencies.createContext,
                updateContext = dependencies.updateContext,
                deleteContext = dependencies.deleteContext,
                createBacklogItem = dependencies.createBacklogItem,
                deleteBacklogItem = dependencies.deleteBacklogItem,
                updateBacklogItemContent = dependencies.updateBacklogItemContent,
                updateBacklogItemDone = dependencies.updateBacklogItemDone,
                scope = scope,
            )
        }
    val state by store.state.collectAsState()
    val dispatch = store::dispatch
    LaunchedEffect(initialContextId) {
        initialContextId?.let { contextId ->
            dispatch(WorkspaceExplorerIntent.ContextSelected(contextId))
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ContextTreePane(
            state = state,
            onIntent = dispatch,
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
        )
        DesktopBacklogReader(
            title = state.selectedContextName,
            items = state.backlogItems,
            savingItemId = state.savingBacklogItemId,
            deletingItemId = state.deletingBacklogItemId,
            editingItemId = state.editingBacklogItemId,
            isCreatingItem = state.isCreatingBacklogItem,
            draftTitle = state.backlogDraftTitle,
            draftDetails = state.backlogDraftDetails,
            draftPriority = state.backlogDraftPriority,
            onIntent = dispatch,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContextTreePane(
    state: ContextExplorerState,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Context Explorer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(WorkspaceExplorerIntent.QueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пошук контекстів") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onIntent(WorkspaceExplorerIntent.StartCreatingContext) },
                    enabled = !state.isSavingContext && state.deletingContextId == null,
                ) {
                    Text("New Context")
                }
                Button(
                    onClick = { onIntent(WorkspaceExplorerIntent.StartEditingContext) },
                    enabled = state.selectedContextId != null && !state.isSavingContext && state.deletingContextId == null,
                ) {
                    Text("Edit Context")
                }
                OutlinedButton(
                    onClick = { onIntent(WorkspaceExplorerIntent.DeleteContext) },
                    enabled = state.selectedContextId != null && !state.isSavingContext && state.deletingContextId == null,
                ) {
                    Text(if (state.deletingContextId != null) "Deleting..." else "Delete Context")
                }
            }
            if (state.creatingContextParentId != null || state.editingContextId != null) {
                CreateContextCard(
                    state = state,
                    onIntent = onIntent,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.nodes, key = { it.context.id }) { node ->
                    ContextTreeRow(
                        node = node,
                        isSelected = state.selectedContextId == node.context.id,
                        onClick = { onIntent(WorkspaceExplorerIntent.ContextSelected(node.context.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateContextCard(
    state: ContextExplorerState,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF4F8F7),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (state.editingContextId != null) "Edit Context" else "New Child Context",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = state.contextDraftName,
                onValueChange = { onIntent(WorkspaceExplorerIntent.ContextDraftNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.contextDraftDescription,
                onValueChange = { onIntent(WorkspaceExplorerIntent.ContextDraftDescriptionChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 2,
            )
            ChipRow(
                title = "Status",
                values = SharedContextStatus.entries,
                selectedValue = state.contextDraftStatus,
                label = { it.title },
                onSelect = { onIntent(WorkspaceExplorerIntent.ContextDraftStatusChanged(it)) },
            )
            ChipRow(
                title = "Default View",
                values = SharedContextView.entries,
                selectedValue = state.contextDraftView,
                label = { it.title },
                onSelect = { onIntent(WorkspaceExplorerIntent.ContextDraftViewChanged(it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onIntent(WorkspaceExplorerIntent.SaveContext) },
                    enabled = !state.isSavingContext && state.contextDraftName.isNotBlank(),
                ) {
                    Text(
                        if (state.isSavingContext) {
                            "Saving..."
                        } else if (state.editingContextId != null) {
                            "Update Context"
                        } else {
                            "Save Context"
                        },
                    )
                }
                OutlinedButton(
                    onClick = { onIntent(WorkspaceExplorerIntent.CancelContextEditing) },
                    enabled = !state.isSavingContext,
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    title: String,
    values: List<T>,
    selectedValue: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                Text(
                    text = label(value),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedValue == value) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .background(
                                if (selectedValue == value) Color(0xFF1D7C70) else Color(0xFFE7EFED),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { onSelect(value) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ContextTreeRow(
    node: SharedContextTreeNode,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) Color(0xFFE9F4F2) else Color(0xFFF8F5EE),
        tonalElevation = 0.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .width((node.depth * INDENT_WIDTH_DP).dp)
                        .height(1.dp),
            )
            Box(
                modifier =
                    Modifier
                        .background(statusColor(node.context.status), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = node.context.status.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = node.context.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                node.context.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${node.childCount} children",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusColor(status: SharedContextStatus): Color =
    when (status) {
        SharedContextStatus.NoPlan -> Color(0xFFCC7A00)
        SharedContextStatus.Planning -> Color(0xFF8E4EC6)
        SharedContextStatus.InProgress -> Color(0xFF1976D2)
        SharedContextStatus.Completed -> Color(0xFF2E7D32)
        SharedContextStatus.OnHold -> Color(0xFF8D6E63)
        SharedContextStatus.Paused -> Color(0xFFB28704)
    }
