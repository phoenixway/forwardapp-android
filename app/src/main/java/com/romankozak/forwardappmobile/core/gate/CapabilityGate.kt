package com.romankozak.forwardappmobile.core.gate

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextId

// core/gate/CapabilityGate.kt
class CapabilityGate(
    private val registry: CapabilityRegistry //
) {
    fun isAvailable(context: Context, capId: CapabilityId): Boolean {
        // Логіка: чи є ця можливість у списку активних для цього контексту
        return context.config.activeCapabilities.contains(capId)
    }
}