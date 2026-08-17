package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

class UpdateContextUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(
        contextId: String,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String> = emptyList(),
        experimentalCapabilityIds: List<String> = emptyList(),
    ): SharedContextSummary? {
        val normalizedContextId = contextId.trim()
        val normalizedName = name.trim()
        if (normalizedContextId.isBlank() || normalizedName.isBlank()) {
            return null
        }
        val normalizedDescription = description?.trim()?.ifBlank { null }
        return repository.updateContext(
            contextId = normalizedContextId,
            name = normalizedName,
            description = normalizedDescription,
            status = status,
            defaultView = defaultView,
            enabledCapabilityIds = enabledCapabilityIds.normalizeCapabilityIds(defaultView),
            experimentalCapabilityIds = experimentalCapabilityIds.normalizeCapabilityIds(),
        )
    }
}

private fun List<String>.normalizeCapabilityIds(defaultView: SharedContextView? = null): List<String> =
    (
        this +
            listOfNotNull(defaultView?.let { view -> SharedContextCapabilityCatalog.capabilityIdFor(view) })
    ).let { capabilityIds -> SharedContextCapabilityCatalog.normalizeCapabilityIds(capabilityIds) }
