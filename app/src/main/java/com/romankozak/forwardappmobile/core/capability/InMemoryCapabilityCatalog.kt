package com.romankozak.forwardappmobile.core.capability

class InMemoryCapabilityCatalog(
    private val descriptors: Set<CapabilityDescriptor>,
) : CapabilityCatalog {

    override fun all(): Set<CapabilityDescriptor> = descriptors

    override fun get(id: CapabilityId): CapabilityDescriptor? =
        descriptors.firstOrNull { it.id == id }
}

