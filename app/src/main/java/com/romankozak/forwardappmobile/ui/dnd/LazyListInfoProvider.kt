package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo

interface LazyListInfoProvider {
    val lazyListItemInfo: List<LazyListItemInfo>
    val viewportEndOffset: Int
    val viewportStartOffset: Int
    val firstVisibleItemIndex: Int
    val firstVisibleItemScrollOffset: Int
    val viewportSize: androidx.compose.ui.unit.IntSize
}
