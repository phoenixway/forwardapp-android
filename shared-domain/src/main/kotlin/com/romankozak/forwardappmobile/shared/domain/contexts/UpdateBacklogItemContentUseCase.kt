package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority

class UpdateBacklogItemContentUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(
        itemId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem? {
        if (itemId.isBlank()) {
            return null
        }
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) {
            return null
        }
        val normalizedDetails = details?.trim()?.ifBlank { null }
        return repository.updateBacklogItemContent(
            itemId = itemId,
            title = normalizedTitle,
            details = normalizedDetails,
            priority = priority,
        )
    }
}
