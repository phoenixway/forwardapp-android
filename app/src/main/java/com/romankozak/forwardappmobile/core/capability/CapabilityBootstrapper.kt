package com.romankozak.forwardappmobile.core.capability

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityBootstrapper @Inject constructor(
    registry: CapabilityRegistry,
    runtime: CapabilityRuntime
) {
    init {
        registry.capabilities.forEach {
            it.register(runtime)
        }
    }
}
