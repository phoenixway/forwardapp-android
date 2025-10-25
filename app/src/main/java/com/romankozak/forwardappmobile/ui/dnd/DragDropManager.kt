package com.romankozak.forwardappmobile.ui.dnd

import android.util.Log
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DragDropManager(
    private val scope: CoroutineScope,
    private val lazyListInfoProvider: LazyListInfoProvider, // Changed
    private val onMove: (Int, Int) -> Unit,
    private val scrollBy: suspend (Float) -> Unit // Added for testability
) {
    private val _dragState = MutableStateFlow(DragAndDropState())
    val dragState: StateFlow<DragAndDropState> = _dragState.asStateFlow()

    private var dragJob: Job? = null

    fun onDragStart(offset: Offset, index: Int) {
        val draggedItem = lazyListInfoProvider.lazyListItemInfo.firstOrNull { it.index == index } ?: return

        _dragState.update {
            it.copy(
                dragInProgress = true,
                draggedItemIndex = index,
                dragAmount = offset
            )
        }
    }

    fun onDrag(offset: Offset) {
        dragJob?.cancel()
        dragJob = scope.launch {
            _dragState.update { it.copy(dragAmount = it.dragAmount + offset) }

            val currentDragPosition = _dragState.value.dragAmount
            val draggedItemIndex = _dragState.value.draggedItemIndex ?: return@launch

            val newTargetIndex = calculateTargetIndex(currentDragPosition, draggedItemIndex)

            _dragState.update {
                it.copy(
                    targetItemIndex = newTargetIndex
                )
            }

            // Auto-scroll logic
            val draggedItem = lazyListInfoProvider.lazyListItemInfo.firstOrNull { it.index == draggedItemIndex }
            if (draggedItem != null) {
                val viewportTopOffset = (lazyListInfoProvider.lazyListItemInfo.firstOrNull()?.offset ?: 0) - lazyListInfoProvider.firstVisibleItemScrollOffset
                val fingerYInList = draggedItem.offset + _dragState.value.dragAmount.y
                val fingerYInViewport = fingerYInList - viewportTopOffset

                val viewportHeight = lazyListInfoProvider.viewportSize.height
                val hotZone = viewportHeight * 0.1f

                if (fingerYInViewport < hotZone) {
                    scrollBy(-50f)
                } else if (fingerYInViewport > viewportHeight - hotZone) {
                    scrollBy(50f)
                }
            }
        }
    }

    private fun calculateTargetIndex(dragAmount: Offset, draggedItemIndex: Int): Int? {
        val draggedItem = lazyListInfoProvider.lazyListItemInfo.firstOrNull { it.index == draggedItemIndex } ?: return null
        val dragAbsoluteY = draggedItem.offset + dragAmount.y + lazyListInfoProvider.firstVisibleItemScrollOffset

        return lazyListInfoProvider.lazyListItemInfo
            .filter { it.index != draggedItemIndex }
            .minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2 + lazyListInfoProvider.firstVisibleItemScrollOffset
                kotlin.math.abs(itemCenter - dragAbsoluteY)
            }?.index
    }

    fun onDragEnd() {
        val draggedItemIndex = _dragState.value.draggedItemIndex
        val targetItemIndex = _dragState.value.targetItemIndex

        if (draggedItemIndex != null && targetItemIndex != null && draggedItemIndex != targetItemIndex) {
            onMove(draggedItemIndex, targetItemIndex)
        }

        _dragState.update {
            it.copy(
                dragInProgress = false,
                draggedItemIndex = null,
                targetItemIndex = null,
                dragAmount = Offset.Zero
            )
        }
    }
}
