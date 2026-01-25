package com.romankozak.forwardappmobile.core.navigation.capability

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.context.ViewId

@JvmInline
value class ScreenId(val raw: String)

fun interface ScreenFactory {
    @Composable
    fun Render()
}

interface ViewResolver {
    fun resolve(viewId: ViewId): ScreenId
}

class StaticViewResolver(
    private val map: Map<ViewId, ScreenId>,
) : ViewResolver {
    override fun resolve(viewId: ViewId) = map[viewId] ?: error("Unknown view")
}
