package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState

class LazyListStateProviderImpl(private val state: LazyListState) : LazyListInfoProvider {
    override val lazyListItemInfo: List<LazyListItemInfo>
        get() = state.layoutInfo.visibleItemsInfo

    override val viewportEndOffset: Int
        get() = state.layoutInfo.viewportEndOffset

    override val viewportStartOffset: Int
        get() = state.layoutInfo.viewportStartOffset
}
