package com.romankozak.forwardappmobile.shared.application.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

sealed interface WorkspaceExplorerIntent {
    data class QueryChanged(
        val query: String,
    ) : WorkspaceExplorerIntent

    data class ContextSelected(
        val contextId: String,
    ) : WorkspaceExplorerIntent

    data object StartCreatingContext : WorkspaceExplorerIntent

    data object StartEditingContext : WorkspaceExplorerIntent

    data object CancelContextEditing : WorkspaceExplorerIntent

    data class ContextDraftNameChanged(
        val name: String,
    ) : WorkspaceExplorerIntent

    data class ContextDraftDescriptionChanged(
        val description: String,
    ) : WorkspaceExplorerIntent

    data class ContextDraftStatusChanged(
        val status: SharedContextStatus,
    ) : WorkspaceExplorerIntent

    data class ContextDraftViewChanged(
        val view: SharedContextView,
    ) : WorkspaceExplorerIntent

    data class ContextDraftCapabilityToggled(
        val capabilityId: String,
        val isEnabled: Boolean,
    ) : WorkspaceExplorerIntent

    data object SaveContext : WorkspaceExplorerIntent

    data object DeleteContext : WorkspaceExplorerIntent

    data object StartCreatingBacklogItem : WorkspaceExplorerIntent

    data class StartEditingBacklogItem(
        val itemId: String,
    ) : WorkspaceExplorerIntent

    data object CancelBacklogEditing : WorkspaceExplorerIntent

    data class BacklogDraftTitleChanged(
        val title: String,
    ) : WorkspaceExplorerIntent

    data class BacklogDraftDetailsChanged(
        val details: String,
    ) : WorkspaceExplorerIntent

    data class BacklogDraftPriorityChanged(
        val priority: SharedBacklogPriority,
    ) : WorkspaceExplorerIntent

    data object SaveBacklogItem : WorkspaceExplorerIntent

    data class ToggleBacklogItemDone(
        val itemId: String,
        val isDone: Boolean,
    ) : WorkspaceExplorerIntent

    data class DeleteBacklogItem(
        val itemId: String,
    ) : WorkspaceExplorerIntent
}
