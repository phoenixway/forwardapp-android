package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository

class ContextSettingsActions(
    private val contextRepository: ContextRepository,
    private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
) {
    suspend fun deleteCurrentProject(contextId: String) {
        if (canonicalWorkspaceRepository.hasLiveWorkspace(contextId)) {
            canonicalWorkspaceRepository.tombstoneSubtree(contextId)
        } else {
            contextRepository.deleteContextsByIds(listOf(contextId))
        }
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
