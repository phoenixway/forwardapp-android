package com.romankozak.forwardappmobile.ui.dnd

import android.util.Log

private const val TAG = "DnDVisualsManager"

class DnDVisualsManager(
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val dragDropManager: DragDropManager
) {
    fun calculateTargetIndex(dragAndDropState: DragAndDropState) {
        val draggedItemIndex = dragAndDropState.draggedItemIndex
        val dragAmount = dragAndDropState.dragAmount

        if (!dragAndDropState.dragInProgress || draggedItemIndex == null) return

        val newTargetIndex = lazyListInfoProvider.lazyListItemInfo
            .firstOrNull { item ->
                val itemTop = item.offset
                val itemBottom = item.offset + item.size
                val fingerY = dragAmount.y
                fingerY > itemTop && fingerY < itemBottom && item.index != draggedItemIndex
            }?.index

        if (newTargetIndex != dragAndDropState.targetItemIndex) {
            Log.d(TAG, "Target changed: ${dragAndDropState.targetItemIndex} -> $newTargetIndex")
            dragDropManager.setTargetItemIndex(newTargetIndex)
        }
    }
}