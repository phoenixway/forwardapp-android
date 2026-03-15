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

data class AdaptiveTopBarState(
    val isSelectionModeActive: Boolean,
    val project: Context?,
    val selectedCount: Int,
    val areAllSelected: Boolean,
    val currentViewMode: ContextViewMode?,
    val enabledCapabilities: Set<CapabilityId>,
)

data class AdaptiveTopBarActions(
    val onClearSelection: () -> Unit,
    val onSelectAll: () -> Unit,
    val onDelete: () -> Unit,
    val onMarkAsComplete: () -> Unit,
    val onMarkAsIncomplete: () -> Unit,
    val onMoreActions: (GoalActionType) -> Unit,
    val onPaste: (() -> Unit)?,
    val onInboxClick: () -> Unit,
)

@Composable
fun AdaptiveTopBar(
    state: AdaptiveTopBarState,
    actions: AdaptiveTopBarActions,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars,
) {
    val topPadding = windowInsets.asPaddingValues().calculateTopPadding()
    val availableViews =
        if (state.enabledCapabilities.isNotEmpty()) {
            ContextViewPolicy.availableViews(state.enabledCapabilities)
        } else {
            emptyList()
        }
    val displayViewMode =
        state.currentViewMode?.takeIf { availableViews.isEmpty() || it in availableViews }

    Column(modifier = modifier.padding(top = topPadding)) {
        if (state.isSelectionModeActive) {
            ListTitleBar(
                project = state.project?.copy(isContextManagementEnabled = false),
                currentViewMode = displayViewMode,
                onPasteClick = actions.onPaste,
                onInboxClick = actions.onInboxClick,
            )
            MultiSelectTopAppBar(
                selectedCount = state.selectedCount,
                areAllSelected = state.areAllSelected,
                onClearSelection = actions.onClearSelection,
                onSelectAll = actions.onSelectAll,
                onDelete = actions.onDelete,
                onMoreActions = actions.onMoreActions,
                onMarkAsComplete = actions.onMarkAsComplete,
                onMarkAsIncomplete = actions.onMarkAsIncomplete,
            )
        } else {
            ListTitleBar(
                project = state.project,
                currentViewMode = displayViewMode,
                onPasteClick = actions.onPaste,
                onInboxClick = actions.onInboxClick,
            )
        }
    }
}
