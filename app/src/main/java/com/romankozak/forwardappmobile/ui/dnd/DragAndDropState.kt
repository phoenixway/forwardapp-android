package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset

data class DragAndDropState(
    val dragInProgress: Boolean = false,
    val dragAmount: Offset = Offset.Zero,
    val draggedItemIndex: Int? = null,
    val targetItemIndex: Int? = null,
    val initialItemOffset: Int = 0,
    val dragOffsetInItem: Float = 0f
)