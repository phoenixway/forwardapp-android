package com.romankozak.forwardappmobile.core.capability

interface Capability {
    val descriptor: CapabilityDescriptor

    fun register(runtime: CapabilityRuntime)
}
