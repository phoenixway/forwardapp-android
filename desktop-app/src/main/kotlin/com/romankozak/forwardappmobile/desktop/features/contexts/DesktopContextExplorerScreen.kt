package com.romankozak.forwardappmobile.desktop.features.contexts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
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
    val capabilityRegistry = remember { DesktopContextCapabilityRegistry.default() }
    var selectedView by remember { mutableStateOf<SharedContextView?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    val selectedContext = remember(state.nodes, state.selectedContextId) { state.selectedContext() }

    LaunchedEffect(initialContextId) {
        initialContextId?.let { contextId ->
            dispatch(WorkspaceExplorerIntent.ContextSelected(contextId))
        }
    }
    LaunchedEffect(selectedContext?.id, selectedContext?.defaultView) {
        selectedView = selectedContext?.defaultView
        isSettingsOpen = false
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ContextTreePane(
            state = state,
            onIntent = dispatch,
            onEditSelectedContext = {
                dispatch(WorkspaceExplorerIntent.StartEditingContext)
                isSettingsOpen = true
            },
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
        )
        ContextDetailPane(
            state = state,
            selectedContext = selectedContext,
            capabilityRegistry = capabilityRegistry,
            selectedView = selectedView ?: selectedContext?.defaultView ?: SharedContextView.Backlog,
            isSettingsOpen = isSettingsOpen,
            onViewSelected = { selectedView = it },
            onSettingsToggle = { isSettingsOpen = !isSettingsOpen },
            onIntent = dispatch,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ContextDetailPane(
    state: ContextExplorerState,
    selectedContext: SharedContextSummary?,
    capabilityRegistry: DesktopContextCapabilityRegistry,
    selectedView: SharedContextView,
    isSettingsOpen: Boolean,
    onViewSelected: (SharedContextView) -> Unit,
    onSettingsToggle: () -> Unit,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xCCFFFFFF),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val capabilities = capabilityRegistry.availableFor(selectedContext)
            val effectiveSelectedView =
                capabilities.firstOrNull { capability -> capability.view == selectedView }?.view
                    ?: capabilities.firstOrNull()?.view
                    ?: selectedView
            ContextDetailHeader(
                selectedContext = selectedContext,
                selectedView = effectiveSelectedView,
                isSettingsOpen = isSettingsOpen,
                onSettingsToggle = onSettingsToggle,
                onIntent = onIntent,
            )
            ContextViewSwitcher(
                capabilities = capabilities,
                selectedView = effectiveSelectedView,
                onViewSelected = onViewSelected,
            )
            if (isSettingsOpen && selectedContext != null) {
                ContextSettingsPanel(
                    state = state,
                    selectedContext = selectedContext,
                    onIntent = onIntent,
                )
            }
            HorizontalDivider(color = Color(0xFFE2E8E5))
            val capability = capabilities.firstOrNull { item -> item.view == effectiveSelectedView }
            if (selectedContext == null || capability == null) {
                EmptyContextDetail(modifier = Modifier.fillMaxSize())
            } else {
                capability.render(state, onIntent, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun ContextDetailHeader(
    selectedContext: SharedContextSummary?,
    selectedView: SharedContextView,
    isSettingsOpen: Boolean,
    onSettingsToggle: () -> Unit,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = selectedContext?.name ?: "No context selected",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                selectedContext?.let { context ->
                    CompactPill(text = context.status.title, color = statusColor(context.status))
                    CompactPill(text = "Default: ${context.defaultView.title}", color = Color(0xFF5D6D7E))
                }
                CompactPill(text = "View: ${selectedView.title}", color = Color(0xFF1D7C70))
            }
        }
        OutlinedButton(
            onClick = {
                if (isSettingsOpen) {
                    onIntent(WorkspaceExplorerIntent.CancelContextEditing)
                } else if (selectedContext != null) {
                    onIntent(WorkspaceExplorerIntent.StartEditingContext)
                }
                onSettingsToggle()
            },
            enabled = selectedContext != null,
        ) {
            Text(if (isSettingsOpen) "Close Settings" else "Settings")
        }
    }
}

@Composable
private fun ContextViewSwitcher(
    capabilities: List<DesktopContextCapability>,
    selectedView: SharedContextView,
    onViewSelected: (SharedContextView) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        capabilities.forEach { capability ->
            Text(
                text = capability.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedView == capability.view) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .background(
                            if (selectedView == capability.view) Color(0xFF1D7C70) else Color(0xFFE7EFED),
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { onViewSelected(capability.view) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ContextSettingsPanel(
    state: ContextExplorerState,
    selectedContext: SharedContextSummary,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
) {
    if (state.editingContextId == selectedContext.id) {
        CreateContextCard(state = state, onIntent = onIntent)
        return
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF4F8F7),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Context Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        "${selectedContext.status.title} / ${selectedContext.defaultView.title} / " +
                            "${selectedContext.enabledCapabilityIds.size} capabilities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { onIntent(WorkspaceExplorerIntent.StartEditingContext) }) {
                Text("Edit")
            }
        }
    }
}

@Composable
private fun EmptyContextDetail(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF6F1E8),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Select a context to inspect it.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactPill(
    text: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .background(color, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun ContextTreePane(
    state: ContextExplorerState,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
    onEditSelectedContext: () -> Unit,
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
                    onClick = onEditSelectedContext,
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
            if (state.creatingContextParentId != null) {
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
            CapabilityToggleRow(
                title = "Capabilities",
                values = SharedContextView.entries,
                selectedCapabilityIds = state.contextDraftEnabledCapabilityIds.toSet(),
                lockedView = state.contextDraftView,
                onToggle = { view, isEnabled ->
                    onIntent(
                        WorkspaceExplorerIntent.ContextDraftCapabilityToggled(
                            capabilityId = SharedContextCapabilityCatalog.capabilityIdFor(view),
                            isEnabled = isEnabled,
                        ),
                    )
                },
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
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
private fun CapabilityToggleRow(
    title: String,
    values: List<SharedContextView>,
    selectedCapabilityIds: Set<String>,
    lockedView: SharedContextView,
    onToggle: (SharedContextView, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { view ->
                val capabilityId = SharedContextCapabilityCatalog.capabilityIdFor(view)
                val isLocked = view == lockedView
                val isSelected = isLocked || capabilityId in selectedCapabilityIds
                Text(
                    text = view.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .background(
                                if (isSelected) Color(0xFF1D7C70) else Color(0xFFE7EFED),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable(enabled = !isLocked) { onToggle(view, !isSelected) }
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

private fun ContextExplorerState.selectedContext(): SharedContextSummary? =
    nodes.firstOrNull { node -> node.context.id == selectedContextId }?.context
