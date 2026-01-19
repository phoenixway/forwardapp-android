// core/capability/InMemoryCapabilityCatalog.kt
package com.romankozak.forwardappmobile.core.capability

import javax.inject.Inject
import javax.inject.Singleton

// core/capability/CapabilityRegistry.kt
class InMemoryCapabilityRegistry @Inject constructor(
    private val availableCapabilities: Set<CapabilityDescriptor>
) : CapabilityRegistry {

    private val map = availableCapabilities.associateBy { it.id }

    override fun all() = map.values.toSet()
    override fun get(id: CapabilityId) = map[id]
}