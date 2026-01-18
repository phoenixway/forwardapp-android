package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFeatureGate @Inject constructor(
    private val contextRepository: ContextRepository
) : CapabilityGate {

    override fun isCapabilityEnabled(
        contextId: ContextId,
        capabilityId: CapabilityId
    ): Boolean {
        val context = contextRepository.getContext(contextId)
        return context.enabledCapabilities.contains(capabilityId)
    }
}
