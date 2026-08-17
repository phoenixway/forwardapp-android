package com.romankozak.forwardappmobile.desktop.features.contexts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerState
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary

@Composable
fun DesktopContextDashboardView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val selectedContext = state.selectedContext()
    val childContexts = remember(state.nodes, selectedContext?.id) { state.childContextsOf(selectedContext?.id) }
    val openItems = state.backlogItems.count { item -> !item.isDone }
    val completedItems = state.backlogItems.count { item -> item.isDone }
    val criticalItems = state.backlogItems.count { item -> item.priority == SharedBacklogPriority.Critical && !item.isDone }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ContextOverviewPanel(selectedContext = selectedContext)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardMetricTile("Open", openItems.toString(), Modifier.weight(1f))
                DashboardMetricTile("Done", completedItems.toString(), Modifier.weight(1f))
                DashboardMetricTile("Children", childContexts.size.toString(), Modifier.weight(1f))
                DashboardMetricTile("Critical", criticalItems.toString(), Modifier.weight(1f))
            }
        }
        item {
            DashboardSection(title = "Backlog Mix") {
                val byKind = state.backlogItems.groupingBy { item -> item.kind }.eachCount()
                if (byKind.isEmpty()) {
                    EmptyText("No backlog material in this context yet.")
                } else {
                    byKind.entries.sortedByDescending { entry -> entry.value }.forEach { (kind, count) ->
                        CompactInfoRow(label = kind.title, value = count.toString())
                    }
                }
            }
        }
        item {
            DashboardSection(title = "Child Contexts") {
                if (childContexts.isEmpty()) {
                    EmptyText("No child contexts.")
                } else {
                    childContexts.take(DASHBOARD_CHILD_LIMIT).forEach { context ->
                        CompactContextRow(context = context, onClick = null)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopContextConnectionsView(
    state: WorkspaceExplorerState,
    onIntent: (WorkspaceExplorerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedContext = state.selectedContext()
    val parentContext = remember(state.nodes, selectedContext?.parentId) { state.contextById(selectedContext?.parentId) }
    val childContexts = remember(state.nodes, selectedContext?.id) { state.childContextsOf(selectedContext?.id) }
    val linkItems = remember(state.backlogItems) { state.backlogItems.filter { item -> item.kind == SharedBacklogItemKind.Link } }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Parent") {
                if (parentContext == null) {
                    EmptyText("This context is at the workspace root.")
                } else {
                    CompactContextRow(
                        context = parentContext,
                        onClick = { onIntent(WorkspaceExplorerIntent.ContextSelected(parentContext.id)) },
                    )
                }
            }
        }
        item {
            DashboardSection(title = "Child Contexts") {
                if (childContexts.isEmpty()) {
                    EmptyText("No child contexts.")
                } else {
                    childContexts.forEach { context ->
                        CompactContextRow(
                            context = context,
                            onClick = { onIntent(WorkspaceExplorerIntent.ContextSelected(context.id)) },
                        )
                    }
                }
            }
        }
        item {
            DashboardSection(title = "Linked Backlog Items") {
                if (linkItems.isEmpty()) {
                    EmptyText("No link items in this context backlog.")
                } else {
                    linkItems.forEach { item ->
                        CompactBacklogItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopContextInboxView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val inboxItems =
        remember(state.backlogItems) {
            state.backlogItems
                .filter { item ->
                    !item.isDone &&
                        item.kind in setOf(
                            SharedBacklogItemKind.Task,
                            SharedBacklogItemKind.Research,
                            SharedBacklogItemKind.Note,
                        )
                }
                .sortedByDescending { item -> item.sync.updatedAt }
                .take(INBOX_ITEM_LIMIT)
        }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Inbox") {
                if (inboxItems.isEmpty()) {
                    EmptyText("No open inbox-like material in this context snapshot.")
                } else {
                    inboxItems.forEach { item -> CompactBacklogItemRow(item = item) }
                }
            }
        }
    }
}

@Composable
fun DesktopContextDirectionView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val selectedContext = state.selectedContext()
    val priorityItems =
        remember(state.backlogItems) {
            state.backlogItems
                .filterNot { item -> item.isDone }
                .sortedWith(
                    compareByDescending<SharedBacklogItem> { item -> item.priority.weight }
                        .thenByDescending { item -> item.sync.updatedAt },
                )
                .take(DIRECTION_ITEM_LIMIT)
        }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Direction") {
                if (selectedContext == null) {
                    EmptyText("Select a context to inspect direction.")
                } else {
                    selectedContext.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } ?: EmptyText("No direction text yet.")
                    CompactInfoRow(label = "Status", value = selectedContext.status.title)
                    CompactInfoRow(label = "Default view", value = selectedContext.defaultView.title)
                }
            }
        }
        item {
            DashboardSection(title = "Current Priorities") {
                if (priorityItems.isEmpty()) {
                    EmptyText("No open backlog priorities.")
                } else {
                    priorityItems.forEach { item -> CompactBacklogItemRow(item = item) }
                }
            }
        }
    }
}

@Composable
fun DesktopContextJournalLogView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val journalItems =
        remember(state.backlogItems) {
            state.backlogItems
                .filter { item -> item.isDone || item.sync.updatedAt > 0L }
                .sortedByDescending { item -> item.sync.updatedAt }
                .take(JOURNAL_ITEM_LIMIT)
        }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Journal Log") {
                if (journalItems.isEmpty()) {
                    EmptyText("No journal-like history in this context snapshot.")
                } else {
                    journalItems.forEach { item -> CompactBacklogItemRow(item = item) }
                }
            }
        }
    }
}

@Composable
fun DesktopContextArtifactView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val artifactItems =
        remember(state.backlogItems) {
            state.backlogItems
                .filter { item ->
                    item.isDone ||
                        item.kind in setOf(
                            SharedBacklogItemKind.Checklist,
                            SharedBacklogItemKind.Note,
                            SharedBacklogItemKind.Link,
                        )
                }
                .sortedWith(
                    compareByDescending<SharedBacklogItem> { item -> if (item.isDone) 1 else 0 }
                        .thenByDescending { item -> item.sync.updatedAt },
                )
        }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Artifacts") {
                if (artifactItems.isEmpty()) {
                    EmptyText("No artifacts or completed outputs in this context snapshot.")
                } else {
                    artifactItems.forEach { item -> CompactBacklogItemRow(item = item) }
                }
            }
        }
    }
}

@Composable
fun DesktopContextKeyProblemsView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val problemItems =
        remember(state.backlogItems) {
            state.backlogItems
                .filter { item ->
                    !item.isDone &&
                        (
                            item.priority == SharedBacklogPriority.Critical ||
                                item.priority == SharedBacklogPriority.High ||
                                item.kind == SharedBacklogItemKind.Research
                        )
                }
                .sortedWith(
                    compareByDescending<SharedBacklogItem> { item -> item.priority.weight }
                        .thenByDescending { item -> item.sync.updatedAt },
                )
        }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Key Problems") {
                if (problemItems.isEmpty()) {
                    EmptyText("No critical or high-priority open problems.")
                } else {
                    problemItems.forEach { item -> CompactBacklogItemRow(item = item) }
                }
            }
        }
    }
}

@Composable
fun DesktopContextLogView(
    state: WorkspaceExplorerState,
    modifier: Modifier = Modifier,
) {
    val recentItems =
        remember(state.backlogItems) {
            state.backlogItems
                .sortedByDescending { item -> item.sync.updatedAt }
                .take(LOG_ITEM_LIMIT)
        }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSection(title = "Recent Context Activity") {
                if (recentItems.isEmpty()) {
                    EmptyText("No recent context activity in the desktop snapshot.")
                } else {
                    recentItems.forEach { item -> CompactBacklogItemRow(item = item) }
                }
            }
        }
    }
}

@Composable
private fun ContextOverviewPanel(selectedContext: SharedContextSummary?) {
    DashboardSection(title = "Overview") {
        if (selectedContext == null) {
            EmptyText("Select a context to inspect it.")
            return@DashboardSection
        }
        selectedContext.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } ?: EmptyText("No description.")
        CompactInfoRow(label = "Status", value = selectedContext.status.title)
        CompactInfoRow(label = "Default view", value = selectedContext.defaultView.title)
        CompactInfoRow(label = "Score", value = selectedContext.score.toString())
    }
}

@Composable
private fun DashboardSection(
    title: String,
    content: @Composable Column.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF6F1E8),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun DashboardMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE7EFED),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D7C70),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CompactContextRow(
    context: SharedContextSummary,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .background(Color(0xFF1D7C70), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Text(
                text = context.status.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = context.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = context.defaultView.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactBacklogItemRow(item: SharedBacklogItem) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${item.kind.title} / ${item.priority.title}" + if (item.isDone) " / Done" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        item.details?.takeIf { it.isNotBlank() }?.let { details ->
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun WorkspaceExplorerState.selectedContext(): SharedContextSummary? =
    nodes.firstOrNull { node -> node.context.id == selectedContextId }?.context

private fun WorkspaceExplorerState.contextById(contextId: String?): SharedContextSummary? =
    nodes.firstOrNull { node -> node.context.id == contextId }?.context

private fun WorkspaceExplorerState.childContextsOf(parentId: String?): List<SharedContextSummary> =
    nodes
        .map { node -> node.context }
        .filter { context -> context.parentId == parentId }
        .sortedBy { context -> context.name.lowercase() }

private const val DASHBOARD_CHILD_LIMIT = 6
private const val INBOX_ITEM_LIMIT = 12
private const val DIRECTION_ITEM_LIMIT = 5
private const val JOURNAL_ITEM_LIMIT = 12
private const val LOG_ITEM_LIMIT = 12
