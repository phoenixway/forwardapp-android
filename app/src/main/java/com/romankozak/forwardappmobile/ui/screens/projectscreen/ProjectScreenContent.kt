package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectDashboardView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectManagementTab
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.AttachmentsView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.InboxView

private const val TAG = "BACKLOG_UI_DEBUG"

@Composable
fun GoalDetailContent(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    uiState: UiState,
    listState: LazyListState,
    inboxListState: LazyListState,
    onEditLog: (com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog) -> Unit,
    onDeleteLog: (com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog) -> Unit,
    onSaveArtifact: (String) -> Unit,
    onEditArtifact: (com.romankozak.forwardappmobile.shared.features.projects.data.model.ProjectArtifact) -> Unit,
    onRemindersClick: (ListItemContent) -> Unit,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val goalList by viewModel.project.collectAsStateWithLifecycle()
    val projectLogs by viewModel.projectLogs.collectAsStateWithLifecycle()
    val projectArtifact by viewModel.projectArtifact.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()



    when (uiState.currentView) {
        ProjectViewMode.BACKLOG -> {
            val listContent by viewModel.listContent.collectAsStateWithLifecycle()
            com.romankozak.forwardappmobile.ui.features.backlog.BacklogListScreen(
                items = listContent,
                modifier = modifier,
                listState = listState,
                showCheckboxes = uiState.showCheckboxes,
                selectedItemIds = uiState.selectedItemIds,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                onMove = { from, to -> viewModel.onMove(from, to) },
                onItemClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                onLongClick = { item -> viewModel.toggleSelection(item.listItem.id) },
                onCheckedChange = { item, isChecked ->
                    when (item) {
                        is ListItemContent.GoalItem -> viewModel.itemActionHandler.toggleGoalCompletedWithState(item.goal, isChecked)
                        is ListItemContent.SublistItem -> viewModel.onSubprojectCompletedChanged(item.project, isChecked)
                        else -> {}
                    }
                },
                onDelete = { item -> viewModel.itemActionHandler.deleteItem(item) },
                onDeleteEverywhere = { item -> viewModel.onDeleteEverywhere(item) },
                onMoveToTop = { item -> viewModel.onMoveToTop(item) },
                onAddToDayPlan = { item -> viewModel.addItemToDailyPlan(item) },
                onStartTracking = { item -> viewModel.onStartTrackingRequest(item) },
                onShowGoalTransportMenu = { item -> viewModel.itemActionHandler.onGoalTransportInitiated(item) {} },
                onRelatedLinkClick = viewModel.itemActionHandler::onRelatedLinkClick,
                onRemindersClick = onRemindersClick,
                onCopyContent = viewModel.itemActionHandler::copyContentRequest,
            )
        }
        ProjectViewMode.INBOX -> {
            InboxView(
                modifier = modifier,
                viewModel = viewModel,
                inboxRecords = inboxRecords,
                listState = inboxListState,
                highlightedRecordId = uiState.inboxRecordToHighlight,
                navigationManager = viewModel.enhancedNavigationManager,
            )
        }
        ProjectViewMode.ADVANCED -> {
            ProjectDashboardView(
                modifier = modifier,
                project = goalList,
                projectLogs = projectLogs,
                projectArtifact = projectArtifact,
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onStatusUpdate = viewModel::onProjectStatusUpdate,
                projectTimeMetrics = uiState.projectTimeMetrics,
                onRecalculateTime = viewModel::onRecalculateTime,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
                onSaveArtifact = onSaveArtifact,
                onEditArtifact = onEditArtifact,
                selectedTab = uiState.selectedDashboardTab,
                onTabSelected = viewModel::onDashboardTabSelected
            )
        }
        ProjectViewMode.ATTACHMENTS -> {
            AttachmentsView(
                modifier = modifier,
                viewModel = viewModel,
                listContent = listContent
            )
        }
    }
}
