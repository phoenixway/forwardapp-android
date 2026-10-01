package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository

class ContextSettingsActions(
    private val contextRepository: ContextRepository,
    private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
) {
    suspend fun deleteCurrentProject(contextId: String) {
        require(!SystemContexts.isSystem(ContextId(contextId))) {
            "Reserved System Workspace cannot be deleted"
        }
        require(canonicalWorkspaceRepository.hasLiveWorkspace(contextId)) {
            "V2 target deletion requires a live Workspace"
        }
        canonicalWorkspaceRepository.tombstone(contextId)
    }

    suspend fun persistContextViewMode(
        contextId: String,
        mode: ContextViewMode,
    ) {
        if (contextId.isBlank()) return
        contextRepository.updateContextViewMode(contextId, mode)
    }

    suspend fun toggleAttachmentsExpanded(contextId: String) {
        contextRepository.toggleContextAttachmentsExpanded(contextId)
    }

    suspend fun toggleProjectManagement(
        contextId: String,
        isEnabled: Boolean,
    ) {
        contextRepository.toggleContextManagement(contextId, isEnabled)
    }
}
