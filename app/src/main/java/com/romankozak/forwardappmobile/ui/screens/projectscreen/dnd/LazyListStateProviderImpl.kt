package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState

class LazyListStateProviderImpl : LazyListInfoProvider {
    var lazyListState: LazyListState? = null

    override val visibleItemsInfo: List<LazyListItemInfo>
        get() = lazyListState?.layoutInfo?.visibleItemsInfo ?: emptyList()
}
