package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextViewPolicy
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.connections.ConnectionsView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.direction.DirectionView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox.InboxView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox.InboxViewState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog.JournalLogView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.keyproblems.KeyProblemsView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ArtifactContent
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ContextManagementTab
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.LogContent
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ProjectDashboardView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import java.util.Locale

private const val TAG = "BACKLOG_UI_DEBUG"

private fun ContextViewMode.displayName(): String {
    return this.name.lowercase(Locale.ROOT)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        .replace("_", " ")
}

@Composable
fun GoalDetailContent(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    uiState: ContextUiState,
    currentViewMode: ContextViewMode,
    enabledCapabilities: Set<CapabilityId>,
    listState: LazyListState,
    inboxListState: LazyListState,
    onEditLog: (ContextLog) -> Unit,
    onDeleteLog: (ContextLog) -> Unit,
    onSaveArtifact: (String, String) -> Unit,
    onEditArtifact: (ContextArtifact) -> Unit,
    onRemindersClick: (BacklogItemContent) -> Unit,
    onShowProjectProperties: () -> Unit,
    onSwitchView: (ContextViewMode) -> Unit,
    onLinkDirectionRequest: (String) -> Unit,
    onUnlinkDirectionRequest: (String) -> Unit,
    onOpenLinkedDirectionContext: (String) -> Unit,
    linkedContextNames: Map<String, String>,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val attachmentItems by viewModel.attachmentItems.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val inboxSelectionMode by viewModel.inboxHandler.isSelectionMode.collectAsStateWithLifecycle()
    val inboxSelectedRecordIds by viewModel.inboxHandler.selectedRecordIds.collectAsStateWithLifecycle()
    val canPasteIntoInbox by viewModel.itemActionHandler.canPasteIntoCurrentInbox.collectAsStateWithLifecycle()
    val goalList by viewModel.project.collectAsStateWithLifecycle()
    val projectLogs = uiState.logs
    val projectArtifact by viewModel.contextArtifact.collectAsStateWithLifecycle()
    val keyProblemsData by viewModel.keyProblemsData.collectAsStateWithLifecycle()
    val journalLogDocument by viewModel.journalLogDocument.collectAsStateWithLifecycle()
    val allContexts by viewModel.allContextsForPicker.collectAsStateWithLifecycle()
    val pickerAttachmentOptions by viewModel.pickerAttachmentOptions.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val tacticalBacklogItemIds by viewModel.tacticalBacklogItemIds.collectAsStateWithLifecycle()
    val tacticalBacklogMissionStreamIds by viewModel.tacticalBacklogMissionStreamIds.collectAsStateWithLifecycle()
    val missionStreams by viewModel.missionStreams.collectAsStateWithLifecycle()
    val missionStreamTitleById = missionStreams.associate { it.id to it.title }
    val enableKeyProblems = uiState.experimentalCapabilityIds.contains(CapabilityId("key_problems"))

    when (currentViewMode) {
        ContextViewMode.BACKLOG -> {
            val listContent by viewModel.listContent.collectAsStateWithLifecycle()
            val searchQuery = uiState.localSearchQuery.trim()
            val currentContextId = goalList?.id
            val filteredBacklogItems =
                remember(listContent, searchQuery, currentContextId) {
                    val backlogItemsWithoutAutoChildContexts =
                        if (currentContextId.isNullOrBlank()) {
                            listContent
                        } else {
                            listContent.filterNot { item ->
                                item is BacklogItemContent.ContextLinkItem &&
                                    item.project.parentId == currentContextId
                            }
                        }

                    if (searchQuery.isBlank()) {
                        backlogItemsWithoutAutoChildContexts
                    } else {
                        backlogItemsWithoutAutoChildContexts.filter { it.matchesLocalSearch(searchQuery) }
                    }
                }
            BacklogListScreen(
                modifier = modifier,
                state =
                    BacklogListState(
                        items = filteredBacklogItems,
                        listState = listState,
                        showCheckboxes = uiState.showCheckboxes,
                        selectedItemIds = uiState.selectedItemIds,
                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        editingGoalId = uiState.goalToEditInline?.id,
                        missionStreams = missionStreams,
                        tacticalPriorityStreamIdByItemId = tacticalBacklogMissionStreamIds,
                        tacticalPriorityStreamTitleByItemId =
                            tacticalBacklogMissionStreamIds.mapValues { (_, streamId) ->
                                missionStreamTitleById[streamId] ?: streamId
                            },
                    ),
                actions =
                    BacklogListActions(
                        onItemClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                        onTagClick = viewModel::onTagClicked,
                        onLongClick = { item -> viewModel.toggleSelection(item.backlogItem.id) },
                        onCheckedChange = { item, isChecked ->
                            when (item) {
                                is BacklogItemContent.GoalItem ->
                                    viewModel.itemActionHandler.toggleGoalCompletedWithState(
                                        item.goal,
                                        isChecked,
                                    )

                                is BacklogItemContent.ContextLinkItem ->
                                    viewModel.onSubprojectCompletedChanged(
                                        item.project,
                                        isChecked,
                                    )

                                else -> {}
                            }
                        },
                        onDelete = { item -> viewModel.itemActionHandler.deleteItem(item) },
                        onDeleteEverywhere = { item -> viewModel.onDeleteEverywhere(item) },
                        onAddToDayPlan = { item -> viewModel.addItemToDailyPlan(item) },
                        isTacticalPriority = { item -> item.backlogItem.id in tacticalBacklogItemIds },
                        onToggleTacticalPriority = { item, streamId ->
                            viewModel.toggleItemTacticalPriority(item, streamId)
                        },
                        onStartTracking = { item -> viewModel.onStartTrackingRequest(item) },
                        onCopyTransport = { item -> viewModel.itemActionHandler.onTransportCopyRequested(item) },
                        onCutTransport = { item -> viewModel.itemActionHandler.onTransportCutRequested(item) },
                        onRelatedLinkClick = viewModel.itemActionHandler::onRelatedLinkClick,
                        onRemindersClick = onRemindersClick,
                        onCopyContent = viewModel.itemActionHandler::copyContentRequest,
                        onOpenGoalProperties = viewModel::openGoalProperties,
                        onGoalInlineEditSave = viewModel::onSaveGoalInlineEditor,
                        onGoalInlineEditCancel = viewModel::onDismissGoalInlineEditor,
                        onDragStopped = viewModel::onBacklogDragStopped,
                    ),
            )
        }
        ContextViewMode.INBOX -> {
            InboxView(
                modifier = modifier,
                viewModel = viewModel,
                state =
                    InboxViewState(
                        inboxRecords = inboxRecords,
                        listState = inboxListState,
                        highlightedRecordId = uiState.inboxRecordToHighlight,
                        isSelectionMode = inboxSelectionMode,
                        selectedRecordIds = inboxSelectedRecordIds,
                        canPaste = canPasteIntoInbox,
                        onTagClick = viewModel::onTagClicked,
                    ),
                navigationManager = viewModel.enhancedNavigationManager,
            )
        }
        ContextViewMode.ADVANCED -> Unit
        ContextViewMode.CONNECTIONS -> {
            ConnectionsView(
                modifier = modifier,
                viewModel = viewModel,
                attachmentItems = attachmentItems,
            )
        }
        ContextViewMode.DIRECTION -> {
            DirectionView(
                items = uiState.directionItems,
                modifier = modifier,
                onAddItem = viewModel::addDirectionItem,
                onEditItem = viewModel::updateDirectionItemText,
                onDeleteItem = viewModel::deleteDirectionItem,
                onMove = viewModel::onMoveDirectionItem,
                onLinkRequest = onLinkDirectionRequest,
                onUnlinkRequest = onUnlinkDirectionRequest,
                onOpenLinkedContext = onOpenLinkedDirectionContext,
                onCopyItem = viewModel::copyDirectionItem,
                onCutItem = viewModel::cutDirectionItem,
                linkedContextNames = linkedContextNames,
            )
        }
        ContextViewMode.DASHBOARD -> {
            DashboardOverview(
                modifier = modifier,
                project = goalList,
                backlogCount = listContent.size,
                inboxCount = inboxRecords.size,
                directionCount = uiState.directionItems.size,
                attachments = attachmentItems,
                onAttachmentClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                onShowProperties = onShowProjectProperties,
                currentViewMode = currentViewMode,
                onSwitchView = onSwitchView,
                enabledCapabilities = enabledCapabilities,
                enableDashboard = uiState.enableDashboard,
                enableAttachments = uiState.enableAttachments,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
                enableKeyProblems = enableKeyProblems,
            )
        }
        ContextViewMode.LOG -> {
            LogContent(
                modifier = modifier,
                logs = projectLogs,
                isManagementEnabled = true,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
            )
        }
        ContextViewMode.JOURNAL_LOG -> {
            JournalLogView(
                modifier = modifier,
                document = journalLogDocument,
                onUpdateLine = viewModel::updateJournalLogLine,
                onDeleteLine = viewModel::deleteJournalLogLine,
                onReorderLines = viewModel::replaceJournalLogLines,
            )
        }
        ContextViewMode.ARTIFACT -> {
            val editableArtifact =
                projectArtifact
                    ?: goalList?.id?.let { contextId ->
                        ContextArtifact(
                            id = "draft-artifact-$contextId",
                            contextId = contextId,
                            content = "",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        )
                    }
            ArtifactContent(
                modifier = modifier,
                artifact = editableArtifact,
                isManagementEnabled = true,
                onEditArtifact = onEditArtifact,
            )
        }
        ContextViewMode.KEY_PROBLEMS -> {
            val pickerContextOptions =
                allContexts
                    .filter { it.id != uiState.context?.id }
                    .map { context ->
                        ProjectOption(
                            id = context.id,
                            name = context.name,
                            parentId = context.parentId,
                        )
                    }
            KeyProblemsView(
                modifier = modifier,
                issues = keyProblemsData.issues,
                allContexts = allContexts,
                pickerContextOptions = pickerContextOptions,
                pickerAttachmentOptions = pickerAttachmentOptions,
                onSaveIssue = viewModel::saveIssueTrackerIssue,
                onDeleteIssue = viewModel::deleteIssueTrackerIssue,
                onReorderIssues = viewModel::reorderIssueTrackerIssues,
            )
        }
        ContextViewMode.NOTES, ContextViewMode.VET_CASE -> Unit
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DashboardOverview(
    modifier: Modifier = Modifier,
    project: Context?,
    backlogCount: Int,
    inboxCount: Int,
    directionCount: Int,
    attachments: List<BacklogItemContent>,
    onAttachmentClick: (BacklogItemContent) -> Unit,
    onShowProperties: () -> Unit,
    currentViewMode: ContextViewMode,
    onSwitchView: (ContextViewMode) -> Unit,
    enabledCapabilities: Set<CapabilityId>,
    enableDashboard: Boolean,
    enableAttachments: Boolean,
    enableLog: Boolean,
    enableArtifact: Boolean,
    enableKeyProblems: Boolean,
) {
    val countsByMode =
        remember(backlogCount, inboxCount, directionCount, attachments.size) {
            mapOf(
                ContextViewMode.BACKLOG to backlogCount,
                ContextViewMode.INBOX to inboxCount,
                ContextViewMode.DIRECTION to directionCount,
                ContextViewMode.CONNECTIONS to attachments.size,
            )
        }
    val activeViews =
        remember(enabledCapabilities, countsByMode) {
            ContextViewPolicy.availableViews(enabledCapabilities)
                .map { mode ->
                    DashboardViewItem(
                        mode = mode,
                        label = mode.dashboardLabelWithCount(countsByMode[mode]),
                        icon = mode.dashboardIcon(),
                    )
                }
        }
    val contextTitle = project?.name?.trim().orEmpty().ifBlank { "Контекст" }
    val roleBadge = project?.roleCode.toRoleBadgeText()
    val previewAttachments = attachments.take(6)

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                ),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Context Dashboard",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = contextTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onShowProperties) {
                        Icon(Icons.Default.Settings, contentDescription = "Context properties")
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!roleBadge.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text(roleBadge) },
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text("Видів: ${activeViews.size}") },
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Вкладень: ${attachments.size}") },
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Активні види",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Обери, який режим контексту відкрити зараз",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activeViews.forEach { viewItem ->
                        FilterChip(
                            selected = currentViewMode == viewItem.mode,
                            onClick = { onSwitchView(viewItem.mode) },
                            label = {
                                Text(
                                    text = viewItem.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = viewItem.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        if (enableAttachments) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Connections",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (attachments.isEmpty()) {
                        Text(
                            text = "Поки немає зв'язків у цьому контексті",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        previewAttachments.forEach { item ->
                            AttachmentRowSummary(
                                item = item,
                                onClick = { onAttachmentClick(item) },
                            )
                        }
                        if (attachments.size > previewAttachments.size) {
                            Text(
                                text = "Ще ${attachments.size - previewAttachments.size} елемент(ів) у зв'язках",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardViewItem(
    val mode: ContextViewMode,
    val label: String,
    val icon: ImageVector,
)

private fun ContextViewMode.dashboardLabel(): String =
    when (this) {
        ContextViewMode.BACKLOG -> "Беклог"
        ContextViewMode.INBOX -> "Інбокс"
        ContextViewMode.DIRECTION -> "Напрямок"
        ContextViewMode.CONNECTIONS -> "Connections"
        ContextViewMode.DASHBOARD -> "Дашборд"
        ContextViewMode.LOG -> "Лог"
        ContextViewMode.JOURNAL_LOG -> "Journal Log"
        ContextViewMode.ARTIFACT -> "Артефакт"
        ContextViewMode.KEY_PROBLEMS -> "Issues"
        ContextViewMode.ADVANCED,
        ContextViewMode.NOTES,
        ContextViewMode.VET_CASE,
        -> "Недоступно"
    }

private fun ContextViewMode.dashboardLabelWithCount(count: Int?): String {
    if (count == null) return dashboardLabel()
    return "${dashboardLabel()} ($count)"
}

private fun ContextViewMode.dashboardIcon(): ImageVector =
    when (this) {
        ContextViewMode.BACKLOG -> Icons.AutoMirrored.Outlined.List
        ContextViewMode.INBOX -> Icons.Outlined.Inbox
        ContextViewMode.DIRECTION -> Icons.Outlined.AccountTree
        ContextViewMode.CONNECTIONS -> Icons.Default.Attachment
        ContextViewMode.DASHBOARD -> Icons.Default.Dashboard
        ContextViewMode.LOG -> Icons.Outlined.History
        ContextViewMode.JOURNAL_LOG -> Icons.Outlined.MenuBook
        ContextViewMode.ARTIFACT -> Icons.Outlined.Inventory2
        ContextViewMode.KEY_PROBLEMS -> Icons.Outlined.Checklist
        ContextViewMode.ADVANCED,
        ContextViewMode.NOTES,
        ContextViewMode.VET_CASE,
        -> Icons.Outlined.Description
    }

private fun String?.toRoleBadgeText(): String? {
    val code = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return "role: $code"
}

private fun BacklogItemContent.matchesLocalSearch(query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = query.lowercase(Locale.getDefault())
    return searchableTexts().any { it.lowercase(Locale.getDefault()).contains(normalizedQuery) }
}

private fun BacklogItemContent.searchableTexts(): List<String> =
    when (this) {
        is BacklogItemContent.GoalItem -> listOfNotNull(goal.text, goal.description)
        is BacklogItemContent.ContextLinkItem -> listOfNotNull(project.name, project.description, project.roleCode)
        is BacklogItemContent.LinkItem -> listOfNotNull(link.linkData.displayName, link.linkData.target)
        is BacklogItemContent.NoteItem -> listOfNotNull(note.title, note.content)
        is BacklogItemContent.NoteDocumentItem -> listOfNotNull(document.name, document.content)
        is BacklogItemContent.JournalDocumentItem -> listOfNotNull(document.name, document.content)
        is BacklogItemContent.ChecklistItem -> listOfNotNull(checklist.name)
        is BacklogItemContent.MusicNoteItem -> listOfNotNull(musicNote.name, musicNote.content)
    }

@Composable
private fun AttachmentRowSummary(
    item: BacklogItemContent,
    onClick: () -> Unit,
) {
    val title =
        when (item) {
            is BacklogItemContent.LinkItem ->
                item.link.linkData.displayName?.takeIf { it.isNotBlank() }
                    ?: item.link.linkData.target
            is BacklogItemContent.NoteDocumentItem -> item.document.name.ifBlank { "Document" }
            is BacklogItemContent.JournalDocumentItem -> item.document.name.ifBlank { "Journal" }
            is BacklogItemContent.MusicNoteItem -> item.musicNote.name.ifBlank { "Music note" }
            is BacklogItemContent.ChecklistItem -> item.checklist.name ?: "Checklist"
            else -> "Attachment"
        }
    Row(
        modifier =
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val icon =
            when (item) {
                is BacklogItemContent.LinkItem -> Icons.Outlined.Link
                is BacklogItemContent.NoteDocumentItem -> Icons.Outlined.Description
                is BacklogItemContent.JournalDocumentItem -> Icons.Outlined.Description
                is BacklogItemContent.MusicNoteItem -> Icons.Outlined.MusicNote
                is BacklogItemContent.ChecklistItem -> Icons.Outlined.Checklist
                else -> Icons.Default.Attachment
            }
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
