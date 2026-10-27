package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId

/** Intrinsic read/navigation surfaces for every live canonical Workspace. No content ownership. */
object WorkspaceCoreViews {
    val capabilities = setOf(CapabilityId("dashboard"), CapabilityId("relations"))
    fun isRequired(key: String): Boolean = CapabilityId(key) in capabilities
    fun overrides(): Map<CapabilityId, Boolean> = capabilities.associateWith { true }
}
