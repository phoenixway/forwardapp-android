package com.romankozak.forwardappmobile.shared.domain.contexts

class DeleteBacklogItemUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(itemId: String): Boolean {
        if (itemId.isBlank()) {
            return false
        }
        return repository.deleteBacklogItem(itemId)
    }
}
