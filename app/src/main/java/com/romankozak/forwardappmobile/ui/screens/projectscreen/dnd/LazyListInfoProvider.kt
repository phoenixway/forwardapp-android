package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo

interface LazyListInfoProvider {
    val visibleItemsInfo: List<LazyListItemInfo>
}
