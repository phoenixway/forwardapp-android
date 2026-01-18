package com.romankozak.forwardappmobile.core.capability

interface CapabilityRegistry {
    fun all(): Set<CapabilityDescriptor>
    fun get(id: CapabilityId): CapabilityDescriptor?
}

