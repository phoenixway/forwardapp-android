package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListItemInfo

class DnDVisualsManager(
    private val lazyListInfoProvider: LazyListInfoProvider
) {

    fun calculateDnDVisualState(
        dragAndDropState: DragAndDropState
    ): DnDVisualState {
        val draggedItemIndex = dragAndDropState.draggedItemIndex
        val targetItemIndex = dragAndDropState.targetItemIndex
        val dragAmount = dragAndDropState.dragAmount
        val dragInProgress = dragAndDropState.dragInProgress

        if (!dragInProgress || draggedItemIndex == null) {
            return DnDVisualState(isDragging = false)
        }

        val itemOffsets = mutableMapOf<Int, Float>()
        var draggedItemHeight = 0f

        val draggedItemInfo = lazyListInfoProvider.lazyListItemInfo.firstOrNull { it.index == draggedItemIndex }
        if (draggedItemInfo != null) {
            draggedItemHeight = draggedItemInfo.size.toFloat()
        }

        if (targetItemIndex != null && targetItemIndex >= 0) {
            lazyListInfoProvider.lazyListItemInfo.forEach { item ->
                val itemIndex = item.index
                itemOffsets[itemIndex] = when {
                    itemIndex == draggedItemIndex -> dragAmount.y
                    draggedItemIndex < itemIndex && itemIndex <= targetItemIndex -> -draggedItemHeight
                    targetItemIndex <= itemIndex && itemIndex < draggedItemIndex -> draggedItemHeight
                    else -> 0f
                }
            }
        }

        return DnDVisualState(
            itemOffsets = itemOffsets,
            draggedItemHeight = draggedItemHeight,
            isDragging = dragInProgress
        )
    }
}