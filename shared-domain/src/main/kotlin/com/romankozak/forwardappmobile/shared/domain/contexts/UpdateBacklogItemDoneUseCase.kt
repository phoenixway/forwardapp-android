package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem

class UpdateBacklogItemDoneUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(
        itemId: String,
        isDone: Boolean,
    ): SharedBacklogItem? {
        if (itemId.isBlank()) {
            return null
        }
        return repository.updateBacklogItemDone(itemId = itemId, isDone = isDone)
    }
}
