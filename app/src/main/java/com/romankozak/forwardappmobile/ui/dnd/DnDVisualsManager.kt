package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListItemInfo
import android.util.Log
import kotlin.math.abs

private const val TAG = "DnDVisualsManager"

class DnDVisualsManager(
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val dragDropManager: DragDropManager
) {

    fun calculateTargetIndex(dragAndDropState: DragAndDropState) {
        val draggedItemIndex = dragAndDropState.draggedItemIndex
        val dragAmount = dragAndDropState.dragAmount

        if (!dragAndDropState.dragInProgress || draggedItemIndex == null) return

        val fingerY = dragAndDropState.dragAmount.y
        val newTarget = lazyListInfoProvider.lazyListItemInfo
            .filter { it.index != draggedItemIndex }
            .minByOrNull { abs((it.offset + it.size / 2) - fingerY) }

        val newTargetIndex = newTarget?.index
        val finalTargetIndex = newTargetIndex ?: dragAndDropState.targetItemIndex

        if (finalTargetIndex != dragAndDropState.targetItemIndex) {
            Log.d(TAG, "New target index: $finalTargetIndex")
            dragDropManager.setTargetItemIndex(finalTargetIndex)
        }
    }
}