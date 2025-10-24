package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import androidx.compose.foundation.lazy.LazyListItemInfo

data class DragState(
    val initialIndex: Int,
    val currentIndex: Int,
    val targetIndex: Int,
    val draggedDistance: Float = 0f,
    val draggedItemLayoutInfo: LazyListItemInfo?
)
