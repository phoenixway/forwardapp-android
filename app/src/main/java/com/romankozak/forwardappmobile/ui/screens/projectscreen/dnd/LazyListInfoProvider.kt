package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo

interface LazyListInfoProvider {
    val lazyListItemInfo: List<LazyListItemInfo>
    val viewportEndOffset: Int
    val viewportStartOffset: Int
    fun updateItemHeight(index: Int, height: Float) // Новий метод
    fun getItemHeight(index: Int): Float? // Новий метод
}
