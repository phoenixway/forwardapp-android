package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository

class ContextSettingsActions(
    private val contextRepository: ContextRepository,
) {
    suspend fun deleteCurrentProject(contextId: String) {
        contextRepository.deleteContextsByIds(listOf(contextId))
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
