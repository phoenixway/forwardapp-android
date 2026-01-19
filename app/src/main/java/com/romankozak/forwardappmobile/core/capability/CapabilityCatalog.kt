package com.romankozak.forwardappmobile.core.capability

interface CapabilityCatalog {
    fun all(): Set<CapabilityDescriptor>
    fun get(id: CapabilityId): CapabilityDescriptor?
}
