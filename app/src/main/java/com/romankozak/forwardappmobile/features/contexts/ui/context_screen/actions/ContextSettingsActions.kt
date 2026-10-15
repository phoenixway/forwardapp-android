package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspacePresentationPreferencesRepository

class ContextSettingsActions(
    private val contextRepository: ContextRepository,
    private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
    private val workspacePresentationPreferencesRepository: WorkspacePresentationPreferencesRepository,
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
        workspacePresentationPreferencesRepository.setDefaultViewMode(
            workspaceId = contextId,
            defaultViewModeName = mode.name,
        )
    }

    suspend fun toggleAttachmentsExpanded(contextId: String) {
        if (contextId.isBlank()) return
        workspacePresentationPreferencesRepository.toggleAttachmentsExpanded(contextId)
    }

    suspend fun toggleProjectManagement(
        contextId: String,
        isEnabled: Boolean,
    ) {
        contextRepository.toggleContextManagement(contextId, isEnabled)
    }
}
