package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.keyproblems

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.repository.ContextKeyProblemsRepository
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurfaceLayout
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyProblemsView(
    modifier: Modifier = Modifier,
    issues: List<ContextKeyProblemsRepository.IssueItem>,
    searchQuery: String = "",
    allContexts: List<Context>,
    pickerContextOptions: List<ProjectOption>,
    pickerAttachmentOptions: List<AttachmentOption>,
    onSaveIssue: (ContextKeyProblemsRepository.IssueItem) -> Unit,
    onDeleteIssue: (String) -> Unit,
    onReorderIssues: (List<String>) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var uiItems by remember(issues) { mutableStateOf(issues.sortedBy { it.order }) }
    val lazyListState = rememberLazyListState()
    var editingIssue by remember { mutableStateOf<ContextKeyProblemsRepository.IssueItem?>(null) }
    var issueForActions by remember { mutableStateOf<ContextKeyProblemsRepository.IssueItem?>(null) }
    val visibleItems =
        remember(uiItems, searchQuery, allContexts, pickerAttachmentOptions) {
            if (searchQuery.isBlank()) {
                uiItems
            } else {
                val contextNames = allContexts.associate { it.id to it.name }
                val attachmentNames = pickerAttachmentOptions.associate { it.id to it.name }
                uiItems.filter { issue ->
                    issue.title.contains(searchQuery, ignoreCase = true) ||
                        issue.description.contains(searchQuery, ignoreCase = true) ||
                        issue.status.name.contains(searchQuery, ignoreCase = true) ||
                        issue.relatedContextIds.any { id ->
                            contextNames[id]?.contains(searchQuery, ignoreCase = true) == true
                        } ||
                        issue.relatedAttachmentIds.any { id ->
                            attachmentNames[id]?.contains(searchQuery, ignoreCase = true) == true
                        }
                }
            }
        }

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            if (searchQuery.isNotBlank() || uiItems.isEmpty()) return@rememberReorderableLazyListState
            val safeFromIndex = from.index.coerceIn(0, uiItems.lastIndex)
            val safeToIndex = to.index.coerceIn(0, uiItems.lastIndex)
            if (safeFromIndex == safeToIndex) return@rememberReorderableLazyListState
            uiItems =
                uiItems.toMutableList().apply {
                    add(safeToIndex, removeAt(safeFromIndex))
                }
            onReorderIssues(uiItems.map { it.id })
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (visibleItems.isEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Issues ще немає",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Додай першу issue і опиши контексти, документи, дату та поточний статус.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(visibleItems, key = { it.id }) { issue ->
                    ReorderableItem(reorderableState, key = issue.id) {
                        IssueCard(
                            issue = issue,
                            contextNames = allContexts.associate { it.id to it.name },
                            attachmentNames = pickerAttachmentOptions.associate { it.id to it.name },
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            dragHandleModifier =
                                with(this@ReorderableItem) {
                                            if (searchQuery.isBlank()) Modifier.longPressDraggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                            ) else Modifier
                                },
                            onEdit = { editingIssue = issue },
                            onMoreClick = { issueForActions = issue },
                        )
                    }
                }
            }
        }
    }

    editingIssue?.let { issue ->
        IssueEditorSheet(
            issue = issue,
            contextOptions = pickerContextOptions,
            attachmentOptions = pickerAttachmentOptions,
            allContexts = allContexts,
            onDismiss = { editingIssue = null },
            onSave = { updatedIssue ->
                onSaveIssue(updatedIssue)
                editingIssue = null
            },
            onDelete = {
                if (issues.any { existing -> existing.id == issue.id }) {
                    onDeleteIssue(issue.id)
                }
                editingIssue = null
            },
        )
    }

    issueForActions?.let { issue ->
        IssueActionsBottomSheet(
            issueTitle = issue.title.ifBlank { "Без назви" },
            onDismiss = { issueForActions = null },
            onDelete = {
                onDeleteIssue(issue.id)
                issueForActions = null
            },
        )
    }
}

@Composable
private fun IssueCard(
    issue: ContextKeyProblemsRepository.IssueItem,
    contextNames: Map<String, String>,
    attachmentNames: Map<String, String>,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val visualState = rememberIssueVisualState(issue)
    UnifiedListItemSurface(
        isSelected = false,
        state = visualState.itemState,
        layout =
            UnifiedListItemSurfaceLayout(
                modifier =
                    modifier.combinedClickable(
                        onClick = onEdit,
                        onLongClick = onEdit,
                    ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ),
        colors =
            UnifiedListItemColors(
                container = visualState.containerColor,
                border = visualState.borderColor,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnifiedTrailingActionButton(
                icon = Icons.Default.DragHandle,
                contentDescription = "Перетягнути",
                onClick = {},
                modifier = dragHandleModifier,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = issue.title.ifBlank { "Без назви" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = visualState.titleColor,
                    textDecoration = if (visualState.itemState == UnifiedItemState.COMPLETED) TextDecoration.LineThrough else null,
                )
                issue.description
                    .takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (visualState.itemState == UnifiedItemState.COMPLETED) TextDecoration.LineThrough else null,
                        )
                    }
                UnifiedStatusRow(
                    items =
                        buildIssueMetaItems(
                            issue = issue,
                            contextNames = contextNames,
                            attachmentNames = attachmentNames,
                        ),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            UnifiedTrailingActionButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "Дії",
                onClick = onMoreClick,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueActionsBottomSheet(
    issueTitle: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Дії з issue",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = issueTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Видалити",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class IssueVisualState(
    val itemState: UnifiedItemState,
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
)

@Composable
private fun rememberIssueVisualState(issue: ContextKeyProblemsRepository.IssueItem): IssueVisualState {
    val colorScheme = MaterialTheme.colorScheme
    val itemState =
        when (issue.status) {
            ContextKeyProblemsRepository.IssueStatus.CLOSED,
            ContextKeyProblemsRepository.IssueStatus.RESOLVED,
            -> UnifiedItemState.COMPLETED
            ContextKeyProblemsRepository.IssueStatus.BLOCKED -> UnifiedItemState.OVERDUE
            else -> UnifiedItemState.DEFAULT
        }
    val borderColor =
        when (issue.status) {
            ContextKeyProblemsRepository.IssueStatus.OPEN -> colorScheme.primary.copy(alpha = 0.42f)
            ContextKeyProblemsRepository.IssueStatus.IN_PROGRESS -> colorScheme.tertiary.copy(alpha = 0.48f)
            ContextKeyProblemsRepository.IssueStatus.BLOCKED -> colorScheme.error.copy(alpha = 0.52f)
            ContextKeyProblemsRepository.IssueStatus.RESOLVED -> colorScheme.secondary.copy(alpha = 0.42f)
            ContextKeyProblemsRepository.IssueStatus.CLOSED -> colorScheme.outline.copy(alpha = 0.34f)
        }
    val containerColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> borderColor.copy(alpha = 0.08f)
            UnifiedItemState.OVERDUE -> borderColor.copy(alpha = 0.12f)
            else -> borderColor.copy(alpha = 0.10f)
        }
    val titleColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> colorScheme.onSurface.copy(alpha = 0.45f)
            UnifiedItemState.OVERDUE -> colorScheme.error
            else -> colorScheme.onSurface
        }

    return IssueVisualState(
        itemState = itemState,
        containerColor = containerColor,
        borderColor = borderColor,
        titleColor = titleColor,
    )
}

@Composable
private fun buildIssueMetaItems(
    issue: ContextKeyProblemsRepository.IssueItem,
    contextNames: Map<String, String>,
    attachmentNames: Map<String, String>,
): List<UnifiedStatusChipSpec> =
    buildList {
        add(
            UnifiedStatusChipSpec(
                icon =
                    if (issue.status == ContextKeyProblemsRepository.IssueStatus.BLOCKED) {
                        Icons.Outlined.Warning
                    } else {
                        Icons.Outlined.Schedule
                    },
                text = issueStatusLabel(issue.status),
                contentColor = issueStatusColor(issue.status),
            ),
        )
        issue.relatedContextIds
            .take(2)
            .forEach { id ->
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Outlined.AccountTree,
                        text = contextNames[id] ?: id,
                    ),
                )
            }
        issue.relatedAttachmentIds
            .take(2)
            .forEach { id ->
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Outlined.AttachFile,
                        text = attachmentNames[id] ?: id,
                    ),
                )
            }
        val remainingLinks = (issue.relatedContextIds.size - 2).coerceAtLeast(0) + (issue.relatedAttachmentIds.size - 2).coerceAtLeast(0)
        if (remainingLinks > 0) {
            add(UnifiedStatusChipSpec(text = "+$remainingLinks ще"))
        }
    }

private fun issueStatusLabel(status: ContextKeyProblemsRepository.IssueStatus): String =
    when (status) {
        ContextKeyProblemsRepository.IssueStatus.OPEN -> "Open"
        ContextKeyProblemsRepository.IssueStatus.IN_PROGRESS -> "In progress"
        ContextKeyProblemsRepository.IssueStatus.BLOCKED -> "Blocked"
        ContextKeyProblemsRepository.IssueStatus.RESOLVED -> "Resolved"
        ContextKeyProblemsRepository.IssueStatus.CLOSED -> "Closed"
    }

@Composable
private fun issueStatusColor(status: ContextKeyProblemsRepository.IssueStatus): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (status) {
        ContextKeyProblemsRepository.IssueStatus.OPEN -> colorScheme.primary
        ContextKeyProblemsRepository.IssueStatus.IN_PROGRESS -> colorScheme.tertiary
        ContextKeyProblemsRepository.IssueStatus.BLOCKED -> colorScheme.error
        ContextKeyProblemsRepository.IssueStatus.RESOLVED -> colorScheme.secondary
        ContextKeyProblemsRepository.IssueStatus.CLOSED -> colorScheme.onSurfaceVariant
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueEditorSheet(
    issue: ContextKeyProblemsRepository.IssueItem,
    contextOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    allContexts: List<Context>,
    onDismiss: () -> Unit,
    onSave: (ContextKeyProblemsRepository.IssueItem) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(issue.id) { mutableStateOf(issue) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    val attachmentNames = remember(attachmentOptions) { attachmentOptions.associate { it.id to it.name } }
    val contextNames = remember(allContexts) { allContexts.associate { it.id to it.name } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (issue.title.isBlank()) "Нова issue" else "Редагування issue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                placeholder = { Text("Коротка назва проблеми") },
            )

            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 4,
                placeholder = { Text("Деталі, симптоми, контекст, next steps") },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ContextKeyProblemsRepository.IssueStatus.entries.forEach { status ->
                        FilterChip(
                            selected = draft.status == status,
                            onClick = { draft = draft.copy(status = status) },
                            label = { Text(status.name.replace('_', ' ')) },
                        )
                    }
                }
            }

            HorizontalDivider()

            IssueLinksEditorSection(
                title = "Related contexts",
                values = draft.relatedContextIds,
                labels = contextNames,
                onAdd = { showContextPicker = true },
                onRemove = { contextId ->
                    draft = draft.copy(relatedContextIds = draft.relatedContextIds.filterNot { it == contextId })
                },
            )

            IssueLinksEditorSection(
                title = "Related documents",
                values = draft.relatedAttachmentIds,
                labels = attachmentNames,
                onAdd = { showAttachmentPicker = true },
                onRemove = { attachmentId ->
                    draft = draft.copy(relatedAttachmentIds = draft.relatedAttachmentIds.filterNot { it == attachmentId })
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Видалити")
                }
                Button(
                    onClick = {
                        onSave(
                            draft.copy(
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Зберегти")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showContextPicker) {
        LinkedTargetsPickerDialog(
            contextOptions = contextOptions,
            attachmentOptions = emptyList(),
            preselectedContextIds = draft.relatedContextIds.toSet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { showContextPicker = false },
            onContextSelected = { id ->
                draft = draft.copy(relatedContextIds = (draft.relatedContextIds + id).distinct())
                showContextPicker = false
            },
            onAttachmentSelected = {},
            onCreateRootContext = null,
            onCreateDocument = null,
        )
    }

    if (showAttachmentPicker) {
        LinkedTargetsPickerDialog(
            contextOptions = emptyList(),
            attachmentOptions = attachmentOptions,
            preselectedContextIds = emptySet(),
            preselectedAttachmentIds = draft.relatedAttachmentIds.toSet(),
            initialTab = LinkPickerTab.ATTACHMENTS,
            allowedTabs = setOf(LinkPickerTab.ATTACHMENTS),
            onDismiss = { showAttachmentPicker = false },
            onContextSelected = {},
            onAttachmentSelected = { id ->
                draft = draft.copy(relatedAttachmentIds = (draft.relatedAttachmentIds + id).distinct())
                showAttachmentPicker = false
            },
            onCreateRootContext = null,
            onCreateDocument = null,
        )
    }
}

@Composable
private fun IssueLinksEditorSection(
    title: String,
    values: List<String>,
    labels: Map<String, String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
        if (values.isEmpty()) {
            Text(
                text = "Ще не додано",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                values.forEach { id ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = labels[id] ?: id,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .background(Color.Transparent)
                                        .clickable { onRemove(id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Прибрати")
                            }
                        },
                    )
                }
            }
        }
    }
}
