package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.feature.FeatureId

@JvmInline
value class ContextId(val raw: String)



@JvmInline
value class ViewId(val raw: String)

data class FeatureSet(
    val active: Set<FeatureId>
)

data class ViewSet(
    val available: Set<ViewId>,
    val start: ViewId
)

