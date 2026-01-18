package com.romankozak.forwardappmobile.core.capability


@JvmInline
value class CapabilityId(val raw: String)

data class CapabilitySet(
    val active: Set<CapabilityId>
)

interface CapabilityDescriptor {
    val id: CapabilityId
}

