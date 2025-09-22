// file: ui/screens/backlog/ProjectScreenContent.kt

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
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.project_dashboard.ProjectDashboardView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.BacklogView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.InboxView

private const val TAG = "BACKLOG_UI_DEBUG"

@Composable
fun GoalDetailContent(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel, // ВИПРАВЛЕНО: Правильний тип для ViewModel
    uiState: UiState,
    listState: LazyListState,
    inboxListState: LazyListState,
    dragDropState: SimpleDragDropState,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    // ВИПРАВЛЕНО: Звертаємось до inboxRecords через inboxHandler
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val goalList by viewModel.project.collectAsStateWithLifecycle()
    val projectLogs by viewModel.projectLogs.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    val displayList = remember(listContent, goalList?.isAttachmentsExpanded) {
        val attachmentItems = listContent.filter { it is ListItemContent.LinkItem || it is ListItemContent.NoteItem }
        val draggableItems = listContent.filterNot { it is ListItemContent.LinkItem || it is ListItemContent.NoteItem }

        if (goalList?.isAttachmentsExpanded == true) {
            attachmentItems + draggableItems
        } else {
            draggableItems
        }
    }

    val calculatedSwipeEnabled = !isSelectionModeActive && !dragDropState.isDragging
    Log.v(
        TAG,
        "РЕКОМПОЗИЦІЯ ЕКРАНУ: isSelectionModeActive=$isSelectionModeActive, dragDropState.isDragging=${dragDropState.isDragging}, calculatedSwipeEnabled=$calculatedSwipeEnabled",
    )

    when (uiState.currentView) {
        ProjectViewMode.BACKLOG -> {
            BacklogView(
                modifier = modifier,
                viewModel = viewModel,
                uiState = uiState,
                listState = listState,
                dragDropState = dragDropState,
                listContent = listContent,
                isAttachmentsExpanded = goalList?.isAttachmentsExpanded == true,
                swipeEnabled = calculatedSwipeEnabled
            )
        }
        ProjectViewMode.INBOX -> {
            InboxView(
                modifier = modifier,
                viewModel = viewModel,
                inboxRecords = inboxRecords,
                listState = inboxListState,
                highlightedRecordId = uiState.inboxRecordToHighlight
            )
        }
        ProjectViewMode.DASHBOARD -> {
            ProjectDashboardView(
                modifier = modifier,
                project = goalList,
                projectLogs = projectLogs,
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onStatusUpdate = viewModel::onProjectStatusUpdate,
                projectTimeMetrics = uiState.projectTimeMetrics,
                onRecalculateTime = viewModel::onRecalculateTime,
            )
        }
    }
}