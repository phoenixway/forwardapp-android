package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.GoalActionType

@Composable
fun AdaptiveTopBar(
    isSelectionModeActive: Boolean,
    project: Context?,
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
