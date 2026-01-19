// core/capability/InMemoryCapabilityCatalog.kt
package com.romankozak.forwardappmobile.core.capability

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryCapabilityCatalog @Inject constructor(
    capabilities: Set<@JvmSuppressWildcards Capability>
) : CapabilityCatalog {

    private val descriptors: Map<CapabilityId, CapabilityDescriptor> =
        capabilities
            .map { it.descriptor }
            .associateBy { it.id }

    override fun all(): Set<CapabilityDescriptor> =
        descriptors.values.toSet()

    override fun get(id: CapabilityId): CapabilityDescriptor? =
        descriptors[id]
}
