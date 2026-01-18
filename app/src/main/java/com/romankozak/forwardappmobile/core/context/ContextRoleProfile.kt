package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilitySet

data class ContextRoleProfile(
    val code: String,
    val defaultFeatures: Set<CapabilityId>,
    val defaultViews: Set<ViewId>,
    val startView: ViewId
)

fun ContextRoleProfile.instantiate(
    contextId: ContextId
): ContextState = object : ContextState {
    override val id = contextId
    override val features = CapabilitySet(defaultFeatures)
    override val views = ViewSet(defaultViews, startView)
}
