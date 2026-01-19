package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCapabilityGate @Inject constructor(
    private val contextRepository: ContextRepository
) : CapabilityGate {

    override fun isCapabilityEnabled(
        contextId: ContextId,
        capabilityId: CapabilityId
    ): Boolean {
        val context = contextRepository.get(contextId)
            ?: return false

        return capabilityId in context.enabledCapabilities
    }
}
