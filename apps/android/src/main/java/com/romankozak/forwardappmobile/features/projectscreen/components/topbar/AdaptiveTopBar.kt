package com.romankozak.forwardappmobile.features.projectscreen.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode

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
    // onMoreActions: (GoalActionType) -> Unit, // TODO: Re-implement if needed
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
                // onMoreActions = onMoreActions, // TODO: Re-implement if needed
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
