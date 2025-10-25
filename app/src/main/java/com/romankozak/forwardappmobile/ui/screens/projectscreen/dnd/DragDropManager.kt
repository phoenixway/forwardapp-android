package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import android.util.Log
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
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
    private val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    private val _dragState = MutableStateFlow(DragState())
    val dragState: StateFlow<DragState> = _dragState.asStateFlow()

    private var dragJob: Job? = null

    fun onDragStart(offset: Offset, index: Int) {
        val draggedItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return

        _dragState.update {
            it.copy(
                dragInProgress = true,
                draggedItemIndex = index,
                draggedItemHeight = draggedItem.size.toFloat(),
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

            if (newTargetIndex != _dragState.value.targetItemIndex && newTargetIndex >= 0) {
                _dragState.update { it.copy(targetItemIndex = newTargetIndex) }
            }

            // Auto-scroll logic
            val dragAbsoluteY = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == draggedItemIndex }?.offset?.toFloat()?.plus(currentDragPosition.y) ?: 0f

            val viewportHeight = lazyListState.layoutInfo.viewportSize.height
            val hotZone = viewportHeight * 0.1f

            if (dragAbsoluteY < hotZone) {
                lazyListState.scrollBy(-20f)
            } else if (dragAbsoluteY > viewportHeight - hotZone) {
                lazyListState.scrollBy(20f)
            }
        }
    }

    private fun calculateTargetIndex(dragAmount: Offset, draggedItemIndex: Int): Int {
        val draggedItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggedItemIndex } ?: return -1
        val dragAbsoluteY = draggedItem.offset + dragAmount.y

        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.minByOrNull {
            val itemCenter = it.offset + it.size / 2
            kotlin.math.abs(itemCenter - dragAbsoluteY)
        }

        return targetItem?.index ?: -1
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
                dragAmount = Offset.Zero,
                draggedItemHeight = 0f
            )
        }
    }
}
