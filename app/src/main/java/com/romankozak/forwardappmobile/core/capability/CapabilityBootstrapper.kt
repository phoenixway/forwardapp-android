package com.romankozak.forwardappmobile.core.capability

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityBootstrapper
    @Inject
    constructor(
        capabilities: Set<@JvmSuppressWildcards Capability>,
        runtime: CapabilityRuntime,
    ) {
        init {
            capabilities.forEach { it.register(runtime) }
        }
    }
