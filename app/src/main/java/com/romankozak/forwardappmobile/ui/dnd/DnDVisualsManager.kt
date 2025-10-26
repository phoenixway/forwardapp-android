package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListItemInfo
import android.util.Log

private const val TAG = "DnDVisualsManager"

class DnDVisualsManager(
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val dragDropManager: DragDropManager
) {

    fun calculateDnDVisualState(
        dragAndDropState: DragAndDropState
    ): DnDVisualState {
        val draggedItemIndex = dragAndDropState.draggedItemIndex
        val dragAmount = dragAndDropState.dragAmount
        val dragInProgress = dragAndDropState.dragInProgress

        if (!dragInProgress || draggedItemIndex == null) {
            return DnDVisualState(isDragging = false)
        }

        // Calculate the new target index
        val newTargetIndex = lazyListInfoProvider.lazyListItemInfo
            .firstOrNull { item ->
                val itemTop = item.offset
                val itemBottom = item.offset + item.size
                val draggedItemCenter = dragAndDropState.dragAmount.y
                draggedItemCenter > itemTop && draggedItemCenter < itemBottom && item.index != draggedItemIndex
            }?.index

        if (newTargetIndex != dragAndDropState.targetItemIndex) {
            dragDropManager.setTargetItemIndex(newTargetIndex)
        }

        val targetItemIndex = newTargetIndex ?: dragAndDropState.targetItemIndex

        val itemOffsets = mutableMapOf<Int, Float>()
        var draggedItemHeight = 0f

        val draggedItemInfo = lazyListInfoProvider.lazyListItemInfo.firstOrNull { it.index == draggedItemIndex }
        if (draggedItemInfo != null) {
            draggedItemHeight = draggedItemInfo.size.toFloat()
            
            val currentItemOffset = draggedItemInfo.offset.toFloat()
            val calculatedOffset = dragAmount.y - currentItemOffset - dragAndDropState.dragOffsetInItem
            
            Log.d(TAG, "Dragged Item ($draggedItemIndex) | " +
                "fingerY=${dragAmount.y.toInt()}, " +
                "currentItemOffset=${currentItemOffset.toInt()}, " +
                "dragOffsetInItem=${dragAndDropState.dragOffsetInItem.toInt()}, " +
                "→ translationY=${calculatedOffset.toInt()}"
            )
            
            itemOffsets[draggedItemIndex] = calculatedOffset
        }

        if (targetItemIndex != null && targetItemIndex >= 0) {
            lazyListInfoProvider.lazyListItemInfo.forEach { item ->
                val itemIndex = item.index
                if (itemIndex == draggedItemIndex) {
                    // Already handled
                } else {
                    val offset = when {
                        draggedItemIndex < itemIndex && itemIndex <= targetItemIndex -> -draggedItemHeight
                        targetItemIndex <= itemIndex && itemIndex < draggedItemIndex -> draggedItemHeight
                        else -> 0f
                    }
                    itemOffsets[itemIndex] = offset
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
