package com.romankozak.forwardappmobile.core.capability


@JvmInline
value class CapabilityId(val raw: String)

interface Capability {
    val id: CapabilityId

    fun register(registry: CapabilityRuntime)
}


data class CapabilitySet(
    val active: Set<CapabilityId>
)

interface CapabilityDescriptor {
    val id: CapabilityId
}

