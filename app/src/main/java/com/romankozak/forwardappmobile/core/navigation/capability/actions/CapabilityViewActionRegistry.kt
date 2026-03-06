package com.romankozak.forwardappmobile.core.navigation.capability.actions

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import javax.inject.Inject

interface CapabilityViewActionRegistry {
    fun all(): List<CapabilityViewActionEntry>

    fun forView(
        viewMode: ContextViewMode,
        enabledCapabilities: Set<CapabilityId>,
    ): List<CapabilityViewActionEntry>
}

class InMemoryCapabilityViewActionRegistry
    @Inject
    constructor(
        entries: Set<@JvmSuppressWildcards CapabilityViewActionEntry>,
    ) : CapabilityViewActionRegistry {
        private val ordered =
            entries
                .toList()
                .sortedWith(compareBy<CapabilityViewActionEntry> { it.descriptor.order }.thenBy { it.descriptor.title })

        override fun all(): List<CapabilityViewActionEntry> = ordered

        override fun forView(
            viewMode: ContextViewMode,
            enabledCapabilities: Set<CapabilityId>,
        ): List<CapabilityViewActionEntry> =
            ordered.filter { entry ->
                entry.descriptor.viewMode == viewMode &&
                    enabledCapabilities.contains(entry.descriptor.ownerCapability)
            }
    }
