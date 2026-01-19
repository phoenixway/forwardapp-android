package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId

data class ContextSnapshot(
    val id: ContextId,
    val enabledCapabilities: Set<CapabilityId>
)