package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import android.util.Log
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DragDropManager(
    private val scope: CoroutineScope,
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val onMove: (Int, Int) -> Unit
) {
    private val _dragState = MutableStateFlow(DragState())
    val dragState: StateFlow<DragState> = _dragState.asStateFlow()

    private var dragJob: Job? = null

    fun onDragStart(offset: Offset, index: Int) {
        dragJob?.cancel()
        dragJob = scope.launch {
            // Використовуємо збережену висоту замість LazyListItemInfo.size
            val draggedItemHeight = lazyListInfoProvider.getItemHeight(index) ?: run {
                Log.w("DND_DEBUG", "Height not found for index $index, using fallback")
                80f // Fallback значення
            }

            Log.d("DND_DEBUG", "onDragStart: index=$index, height=$draggedItemHeight, initial offset=$offset")

            _dragState.update {
                it.copy(
                    dragInProgress = true,
                    dragAmount = offset,
                    draggedItemIndex = index,
                    targetItemIndex = index,
                    draggedItemHeight = draggedItemHeight
                )
            }
        }
    }

    fun onDrag(offset: Offset) {
        dragJob?.cancel()
        dragJob = scope.launch {
            _dragState.update {
                it.copy(dragAmount = it.dragAmount + offset)
            }
            val currentPosition = _dragState.value.dragAmount
            val draggedItemIndex = _dragState.value.draggedItemIndex ?: return@launch
            val newTargetIndex = getTargetIndex(currentPosition, draggedItemIndex)

            Log.d("DND_DEBUG", "onDrag: offset=$offset, totalDragAmount=${_dragState.value.dragAmount}, newTargetIndex=$newTargetIndex")

            if (newTargetIndex != _dragState.value.targetItemIndex && newTargetIndex >= 0) {
                _dragState.update {
                    it.copy(targetItemIndex = newTargetIndex)
                }
            }
        }
    }

    fun onDragEnd() {
        dragJob?.cancel()
        dragJob = scope.launch {
            val draggedItemIndex = _dragState.value.draggedItemIndex
            val targetItemIndex = _dragState.value.targetItemIndex
            Log.d("DND_DEBUG", "onDragEnd: draggedIndex=$draggedItemIndex, targetIndex=$targetItemIndex")
            if (draggedItemIndex != null && targetItemIndex != null &&
                draggedItemIndex != targetItemIndex && targetItemIndex >= 0) {
                Log.d("DND_DEBUG", "onDragEnd: Calling onMove($draggedItemIndex, $targetItemIndex)")
                onMove(draggedItemIndex, targetItemIndex)
            }
            _dragState.update {
                it.copy(
                    dragInProgress = false,
                    dragAmount = Offset.Zero,
                    draggedItemIndex = null,
                    targetItemIndex = null
                )
            }
        }
    }

    private fun getTargetIndex(dragAmount: Offset, draggedItemIndex: Int): Int {
        Log.d("DND_DEBUG", "getTargetIndex: START. dragAmount=$dragAmount, draggedItemIndex=$draggedItemIndex")

        val listInfo = lazyListInfoProvider.lazyListItemInfo
        Log.d("DND_DEBUG", "getTargetIndex: Visible items count: ${listInfo.size}. Indices: ${listInfo.map { it.index }}")
        if (listInfo.isEmpty()) {
            Log.w("DND_DEBUG", "getTargetIndex: listInfo is empty, returning -1")
            return -1
        }

        val draggedItem = listInfo.firstOrNull { it.index == draggedItemIndex }
        if (draggedItem == null) {
            Log.w("DND_DEBUG", "getTargetIndex: Dragged item with index $draggedItemIndex not found in visible items. Returning -1")
            return -1
        }

        // Використовуємо збережену висоту
        val draggedItemHeight = lazyListInfoProvider.getItemHeight(draggedItemIndex) ?: 80f
        val draggedItemCenter = draggedItem.offset + (draggedItemHeight / 2)
        val dragPosition = draggedItemCenter + dragAmount.y

        Log.d("DND_DEBUG", "getTargetIndex: draggedItem.offset=${draggedItem.offset}, draggedItemHeight=$draggedItemHeight")
        Log.d("DND_DEBUG", "getTargetIndex: draggedItemCenter=$draggedItemCenter, dragAmount.y=${dragAmount.y}, dragPosition=$dragPosition")


        val targetItem = listInfo.minByOrNull { item ->
            val itemHeight = lazyListInfoProvider.getItemHeight(item.index) ?: 80f
            val itemCenter = item.offset + (itemHeight / 2)
            val distance = kotlin.math.abs(itemCenter - dragPosition)

            Log.d("DND_DEBUG", "getTargetIndex: Checking item index=${item.index}, item.offset=${item.offset}, itemHeight=$itemHeight, itemCenter=$itemCenter, distance=$distance")
            distance
        }

        val resultIndex = targetItem?.index ?: -1
        Log.d("DND_DEBUG", "getTargetIndex: END. Found target item index: ${targetItem?.index}. Returning: $resultIndex")
        return resultIndex
    }
}