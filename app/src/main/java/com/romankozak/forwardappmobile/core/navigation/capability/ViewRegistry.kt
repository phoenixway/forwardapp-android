package com.romankozak.forwardappmobile.core.navigation.capability

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ViewId

// Файл: core/navigation/ViewRegistry.kt

interface ViewRegistry {
    fun get(id: ViewId): ViewDescriptor?
    fun getForCapability(capId: CapabilityId): List<ViewDescriptor>
}

class InMemoryViewRegistry(
    descriptors: Set<ViewDescriptor>
) : ViewRegistry {
    private val map = descriptors.associateBy { it.id }

    override fun get(id: ViewId) = map[id]

    override fun getForCapability(capId: CapabilityId) =
        map.values.filter { it.ownerCapability == capId }
}