package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType

@Composable
fun AdaptiveTopBar(
    isSelectionModeActive: Boolean,
    project: Project?,
    selectedCount: Int,
    areAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsComplete: () -> Unit,
    onMarkAsIncomplete: () -> Unit,
    onMoreActions: (GoalActionType) -> Unit,
    currentViewMode: ProjectViewMode? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = if (isSelectionModeActive) 4.dp else 1.dp,
        modifier = modifier.statusBarsPadding(),
    ) {
        if (isSelectionModeActive) {
            Column(modifier = Modifier.statusBarsPadding()) {
                ListTitleBar(
                    project = project?.copy(isProjectManagementEnabled = false),
                    currentViewMode = currentViewMode
                )
                MultiSelectTopAppBar(
                    selectedCount = selectedCount,
                    areAllSelected = areAllSelected,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll,
                    onDelete = onDelete,
                    onMoreActions = onMoreActions,
                    onMarkAsComplete = onMarkAsComplete,
                    onMarkAsIncomplete = onMarkAsIncomplete,
                )
            }
        } else {
            ListTitleBar(
                project = project,
                currentViewMode = currentViewMode
            )
        }
    }
}