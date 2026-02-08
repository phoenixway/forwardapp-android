package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextViewPolicy
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType

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
    currentViewMode: ContextViewMode? = null,
    enabledCapabilities: Set<CapabilityId> = emptySet(),
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars,
) {
    val topPadding = windowInsets.asPaddingValues().calculateTopPadding()
    val availableViews =
        if (enabledCapabilities.isNotEmpty()) {
            ContextViewPolicy.availableViews(enabledCapabilities)
        } else {
            emptyList()
        }
    val displayViewMode =
        currentViewMode?.takeIf { availableViews.isEmpty() || it in availableViews }

    Column(modifier = modifier.padding(top = topPadding)) {
        if (isSelectionModeActive) {
            ListTitleBar(
                project = project?.copy(isContextManagementEnabled = false),
                currentViewMode = displayViewMode,
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
                currentViewMode = displayViewMode,
                onInboxClick = onInboxClick,
            )
        }
    }
}
