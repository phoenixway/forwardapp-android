package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId

// core/context/ContextConfiguration.kt
data class ContextConfiguration(
    val contextId: ContextId,
    val baseRoleCode: String,
    val activeCapabilities: Set<CapabilityId>,
    val activeViews: Set<ViewId>
)