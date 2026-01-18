package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId

interface CapabilityGate {
    fun isCapabilityEnabled(
        contextId: ContextId,
        capabilityId: CapabilityId
    ): Boolean
}
