package com.romankozak.forwardappmobile.core.capability

class InMemoryCapabilityRegistry(
    features: Set<CapabilityDescriptor>
) : CapabilityRegistry {

    private val map = features.associateBy { it.id }

    override fun all() = map.values.toSet()
    override fun get(id: CapabilityId) = map[id]
}
