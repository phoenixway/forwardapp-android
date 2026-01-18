package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.capability.CapabilitySet

@JvmInline
value class ContextId(val raw: String)



@JvmInline
value class ViewId(val raw: String)


data class ViewSet(
    val available: Set<ViewId>,
    val start: ViewId
)

interface ContextState {
    val id: ContextId
    val features: CapabilitySet
    val views: ViewSet
}
