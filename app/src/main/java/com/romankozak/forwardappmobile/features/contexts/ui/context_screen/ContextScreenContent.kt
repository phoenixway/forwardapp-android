package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.connections.ConnectionsView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.direction.DirectionView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox.InboxView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox.InboxViewState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.keyproblems.KeyProblemsView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.LogContent
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
    onRemindersClick: (BacklogItemContent) -> Unit,
    onShowProjectProperties: () -> Unit,
    onSwitchView: (ContextViewMode) -> Unit,
    onLinkDirectionRequest: (String) -> Unit,
    onUnlinkDirectionRequest: (String) -> Unit,
    onOpenLinkedDirectionContext: (String) -> Unit,
    linkedContextNames: Map<String, String>,
    dashboardSemanticContent: @Composable (() -> Unit)? = null,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val attachmentItems by viewModel.attachmentItems.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val inboxSelectionMode by viewModel.inboxHandler.isSelectionMode.collectAsStateWithLifecycle()
    val inboxSelectedRecordIds by viewModel.inboxHandler.selectedRecordIds.collectAsStateWithLifecycle()
    val canPasteIntoInbox by viewModel.itemActionHandler.canPasteIntoCurrentInbox.collectAsStateWithLifecycle()
    val projectPresentation by viewModel.projectPresentation.collectAsStateWithLifecycle()
    val projectLogs = uiState.logs
    val keyProblemsData by viewModel.keyProblemsData.collectAsStateWithLifecycle()
    val allContexts by viewModel.allContextsForPicker.collectAsStateWithLifecycle()
    val pickerAttachmentOptions by viewModel.pickerAttachmentOptions.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val tacticalBacklogItemIds by viewModel.tacticalBacklogItemIds.collectAsStateWithLifecycle()
    val tacticalBacklogMissionStreamIds by viewModel.tacticalBacklogMissionStreamIds.collectAsStateWithLifecycle()
    val missionStreams by viewModel.missionStreams.collectAsStateWithLifecycle()
    val missionStreamTitleById = missionStreams.associate { it.id to it.title }
    val enableKeyProblems = CapabilityId("key_problems") in enabledCapabilities
    val localSearchQuery = uiState.localSearchQuery.trim()

    when (currentViewMode) {
        ContextViewMode.RELATIONS -> {
            com.romankozak.forwardappmobile.features.workspace.WorkspaceRelationsView(
                modifier = modifier,
                workspaceTitle = uiState.presentation?.name ?: "Workspace",
                onOpen = { viewModel.enhancedNavigationManager.navigate(it) },
            )
        }
        ContextViewMode.BACKLOG -> {
            val listContent by viewModel.listContent.collectAsStateWithLifecycle()
            val filteredBacklogItems =
                remember(listContent, localSearchQuery) {
                    if (localSearchQuery.isBlank()) {
                        listContent
                    } else {
                        listContent.filter { it.matchesLocalSearch(localSearchQuery) }
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
                                    item.legacyProject?.let { legacyProject ->
                                        viewModel.onSubprojectCompletedChanged(
                                            legacyProject,
                                            isChecked,
                                        )
                                    }

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
                        onDragStopped = { reorderedItems ->
                            if (localSearchQuery.isBlank()) {
                                viewModel.onBacklogDragStopped(reorderedItems)
                            }
                        },
                    ),
            )
        }
        ContextViewMode.INBOX -> {
            InboxView(
                modifier = modifier,
                viewModel = viewModel,
                state =
                    InboxViewState(
                        inboxRecords =
                            remember(inboxRecords, localSearchQuery) {
                                inboxRecords.filter { it.text.matchesLocalSearch(localSearchQuery) }
                            },
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
                localSearchQuery = localSearchQuery,
            )
        }
        ContextViewMode.DIRECTION -> {
            val filteredDirectionItems =
                remember(uiState.directionItems, linkedContextNames, localSearchQuery) {
                    uiState.directionItems.filter { item ->
                        item.text.matchesLocalSearch(localSearchQuery) ||
                            item.linkedContextId
                                ?.let(linkedContextNames::get)
                                .matchesLocalSearch(localSearchQuery)
                    }
                }
            DirectionView(
                items = filteredDirectionItems,
                modifier = modifier,
                onAddItem = viewModel::addDirectionItem,
                onEditItem = viewModel::updateDirectionItemText,
                onDeleteItem = viewModel::deleteDirectionItem,
                onMove = { from, to ->
                    val fromId = filteredDirectionItems.getOrNull(from)?.id
                    val toId = filteredDirectionItems.getOrNull(to)?.id
                    val originalFrom = uiState.directionItems.indexOfFirst { it.id == fromId }
                    val originalTo = uiState.directionItems.indexOfFirst { it.id == toId }
                    if (originalFrom >= 0 && originalTo >= 0) {
                        viewModel.onMoveDirectionItem(originalFrom, originalTo)
                    }
                },
                onLinkRequest = onLinkDirectionRequest,
                onUnlinkRequest = onUnlinkDirectionRequest,
                onOpenLinkedContext = onOpenLinkedDirectionContext,
                onCopyItem = viewModel::copyDirectionItem,
                onCutItem = viewModel::cutDirectionItem,
                linkedContextNames = linkedContextNames,
            )
        }
        ContextViewMode.DASHBOARD -> {
            WorkspaceDashboardOverview(
                entityNavigation = dashboardSemanticContent,
                connectionAction = { WorkspaceDashboardAddConnection(viewModel) },
                modifier = modifier,
                backlogCount = listContent.size,
                inboxCount = inboxRecords.size,
                directionCount = uiState.directionItems.size,
                attachments = attachmentItems,
                onAttachmentClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                currentViewMode = currentViewMode,
                onSwitchView = onSwitchView,
                enabledCapabilities = enabledCapabilities,
            )
        }
        ContextViewMode.LOG -> {
            LogContent(
                modifier = modifier,
                logs = projectLogs,
                externalSearchQuery = localSearchQuery,
                isManagementEnabled = true,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
            )
        }
        ContextViewMode.KEY_PROBLEMS -> {
            val pickerContextOptions =
                allContexts
                    .filter { it.id != (uiState.presentation?.id ?: uiState.context?.id) }
                    .map { context ->
                        ProjectOption(
                            id = context.id,
                            name = context.name,
                        )
                    }
            KeyProblemsView(
                modifier = modifier,
                issues = keyProblemsData.issues,
                searchQuery = localSearchQuery,
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


private fun BacklogItemContent.matchesLocalSearch(query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = query.lowercase(Locale.getDefault())
    return searchableTexts().any { it.lowercase(Locale.getDefault()).contains(normalizedQuery) }
}

private fun String?.matchesLocalSearch(query: String): Boolean =
    query.isBlank() || this?.contains(query, ignoreCase = true) == true

private fun BacklogItemContent.searchableTexts(): List<String> =
    when (this) {
        is BacklogItemContent.GoalItem -> listOfNotNull(goal.text, goal.description)
        is BacklogItemContent.ContextLinkItem -> listOfNotNull(project.name, project.description, project.roleCode)
        is BacklogItemContent.LinkItem -> listOfNotNull(link.linkData.displayName, link.linkData.target)
        is BacklogItemContent.NoteItem -> listOfNotNull(note.title, note.content)
        is BacklogItemContent.NoteDocumentItem -> listOfNotNull(document.name, document.content)
        is BacklogItemContent.ChecklistItem -> listOfNotNull(checklist.name)
        is BacklogItemContent.MusicNoteItem -> listOfNotNull(musicNote.name, musicNote.content)
    }
