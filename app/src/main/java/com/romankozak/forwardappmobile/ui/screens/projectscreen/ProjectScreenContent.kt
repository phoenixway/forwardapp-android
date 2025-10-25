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
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectManagementTab

import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectDashboardView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.AttachmentsView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.BacklogView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.InboxView
import com.romankozak.forwardappmobile.ui.dnd.DragAndDropState

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
    onEditArtifact: (com.romankozak.forwardappmobile.data.database.models.ProjectArtifact) -> Unit,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val goalList by viewModel.project.collectAsStateWithLifecycle()
    val projectLogs by viewModel.projectLogs.collectAsStateWithLifecycle()
    val projectArtifact by viewModel.projectArtifact.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val dragState: DragAndDropState by viewModel.dragState.collectAsStateWithLifecycle()

    val calculatedSwipeEnabled = !isSelectionModeActive && !dragState.dragInProgress
    Log.v(
        TAG,
        "РЕКОМПОЗИЦІЯ ЕКРАНУ: isSelectionModeActive=$isSelectionModeActive, dragState.dragInProgress=${dragState.dragInProgress}, calculatedSwipeEnabled=$calculatedSwipeEnabled",
    )

    when (uiState.currentView) {
        ProjectViewMode.BACKLOG -> {
            BacklogView(
                modifier = modifier,
                viewModel = viewModel,
                uiState = uiState,
                listState = listState,
                listContent = listContent,
                isAttachmentsExpanded = false, // This is no longer used
                swipeEnabled = calculatedSwipeEnabled,
                dragDropManager = viewModel.dragDropManager,
                dndVisualState = viewModel.dndVisualState,
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