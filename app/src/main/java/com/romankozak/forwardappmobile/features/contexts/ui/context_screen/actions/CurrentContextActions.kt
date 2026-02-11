package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ActivityManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager

class CurrentContextActions(
    private val stateManager: ContextStateManager,
    private val activityManager: ActivityManager,
    private val contextSettingsActions: ContextSettingsActions,
) {
    fun setReminderForOngoingActivity(activity: ActivityRecord?) {
        activity ?: return
        stateManager.updateState { uiState -> uiState.copy(recordForReminderDialog = activity) }
    }

    fun startTrackingCurrentProject(projectId: String?) {
        projectId?.let(activityManager::startActivity)
    }

    suspend fun toggleAttachmentsExpanded(context: Context?) {
        context?.let { contextSettingsActions.toggleAttachmentsExpanded(it) }
    }

    suspend fun toggleProjectManagement(
        contextId: String,
        isEnabled: Boolean,
    ) {
        contextSettingsActions.toggleProjectManagement(contextId, isEnabled)
    }
}
