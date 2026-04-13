package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

class CreateContextUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(
        parentId: String?,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
    ): SharedContextSummary? {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return null
        }
        val normalizedDescription = description?.trim()?.ifBlank { null }
        return repository.createContext(
            parentId = parentId?.trim()?.ifBlank { null },
            name = normalizedName,
            description = normalizedDescription,
            status = status,
            defaultView = defaultView,
        )
    }
}
