package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem

class ObserveBacklogUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(contextId: String): List<SharedBacklogItem> {
        if (contextId.isBlank()) {
            return emptyList()
        }
        return repository.getBacklogItems(contextId)
            .sortedWith(
                compareByDescending<SharedBacklogItem> { it.priority.weight }
                    .thenBy { it.title.lowercase() },
            )
    }
}
