package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType

import androidx.compose.animation.AnimatedVisibilityScope

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
    onInboxClick: () -> Unit,
    currentViewMode: ProjectViewMode? = null,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars,
) {
    val topPadding = windowInsets.asPaddingValues().calculateTopPadding()

    Column(modifier = modifier.padding(top = topPadding)) {
        if (isSelectionModeActive) {
            ListTitleBar(
                project = project?.copy(isProjectManagementEnabled = false),
                currentViewMode = currentViewMode,
                onInboxClick = onInboxClick,
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
        } else {
            ListTitleBar(
                project = project,
                currentViewMode = currentViewMode,
                onInboxClick = onInboxClick,
            )
        }
    }
}
