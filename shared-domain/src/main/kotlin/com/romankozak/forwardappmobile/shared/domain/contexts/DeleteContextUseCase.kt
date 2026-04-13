package com.romankozak.forwardappmobile.shared.domain.contexts

class DeleteContextUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(contextId: String): Boolean {
        if (contextId.isBlank()) {
            return false
        }
        return repository.deleteContext(contextId)
    }
}
