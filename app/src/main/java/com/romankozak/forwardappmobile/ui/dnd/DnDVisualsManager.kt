package com.romankozak.forwardappmobile.ui.dnd

import android.util.Log



private const val TAG = "DnDVisualsManager"

class DnDVisualsManager(
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val dragDropManager: DragDropManager
) {
    fun calculateTargetIndex(dragAndDropState: DragAndDropState) {
        val draggedItemIndex = dragAndDropState.draggedItemIndex
        val ghostScreenY = dragAndDropState.ghostScreenY

        if (!dragAndDropState.dragInProgress || draggedItemIndex == null) return

        val newTargetIndex = lazyListInfoProvider.lazyListItemInfo
            .firstOrNull { item ->
                val itemTop = item.offset
                val itemBottom = item.offset + item.size
                ghostScreenY > itemTop && ghostScreenY < itemBottom && item.index != draggedItemIndex
            }?.index ?: run {
                val firstVisible = lazyListInfoProvider.lazyListItemInfo.firstOrNull()?.index
                val lastVisible = lazyListInfoProvider.lazyListItemInfo.lastOrNull()?.index
                when {
                    ghostScreenY < (lazyListInfoProvider.lazyListItemInfo.firstOrNull()?.offset ?: 0) -> firstVisible
                    ghostScreenY > (lazyListInfoProvider.lazyListItemInfo.lastOrNull()?.offset ?: 0) -> lastVisible
                    else -> null
                }
            }

        if (newTargetIndex != dragAndDropState.targetItemIndex) {
            Log.d(TAG, "Target changed: ${dragAndDropState.targetItemIndex} -> $newTargetIndex")
            dragDropManager.setTargetItemIndex(newTargetIndex)
        }
    }
}