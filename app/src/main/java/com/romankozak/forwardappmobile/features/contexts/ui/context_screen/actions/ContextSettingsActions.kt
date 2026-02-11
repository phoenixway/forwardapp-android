package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.repository.ContextRepository

class ContextSettingsActions(
    private val contextRepository: ContextRepository,
) {
    suspend fun deleteCurrentProject(contextId: String) {
        contextRepository.getContextById(contextId)?.let { project ->
            contextRepository.deleteContextsAndSubContexts(listOf(project))
        }
    }

    suspend fun persistContextViewMode(
        contextId: String,
        mode: ContextViewMode,
    ) {
        if (contextId.isBlank()) return
        contextRepository.updateContextViewMode(contextId, mode)
    }

    suspend fun toggleAttachmentsExpanded(context: Context) {
        contextRepository.updateContext(
            context.copy(isAttachmentsExpanded = !context.isAttachmentsExpanded),
        )
    }

    suspend fun toggleProjectManagement(
        contextId: String,
        isEnabled: Boolean,
    ) {
        contextRepository.toggleContextManagement(contextId, isEnabled)
    }
}
