package com.romankozak.forwardappmobile.core.navigation

import com.romankozak.forwardappmobile.core.context.ViewId

@JvmInline
value class ScreenId(val raw: String)

interface ViewResolver {
    fun resolve(viewId: ViewId): ScreenId
}

class StaticViewResolver(
    private val map: Map<ViewId, ScreenId>
) : ViewResolver {

    override fun resolve(viewId: ViewId) =
        map[viewId] ?: error("Unknown view")
}
