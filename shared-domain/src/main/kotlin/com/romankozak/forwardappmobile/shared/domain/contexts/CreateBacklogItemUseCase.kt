package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority

class CreateBacklogItemUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(
        contextId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem? {
        val normalizedContextId = contextId.trim()
        val normalizedTitle = title.trim()
        if (normalizedContextId.isBlank() || normalizedTitle.isBlank()) {
            return null
        }
        val normalizedDetails = details?.trim()?.ifBlank { null }
        return repository.createBacklogItem(
            contextId = normalizedContextId,
            title = normalizedTitle,
            details = normalizedDetails,
            priority = priority,
        )
    }
}
