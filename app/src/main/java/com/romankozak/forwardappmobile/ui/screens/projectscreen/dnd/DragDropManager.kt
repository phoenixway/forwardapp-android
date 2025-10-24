package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

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
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val onMove: (Int, Int) -> Unit
) {
    private val _dragState = MutableStateFlow(DragState())
    val dragState: StateFlow<DragState> = _dragState.asStateFlow()

    private var dragJob: Job? = null

    fun onDragStart(offset: Offset, index: Int) {
        dragJob?.cancel()
        dragJob = scope.launch {
            _dragState.update {
                it.copy(
                    dragInProgress = true,
                    dragAmount = offset,
                    draggedItemIndex = index,
                    targetItemIndex = index
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
            if (newTargetIndex != _dragState.value.targetItemIndex) {
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
            if (draggedItemIndex != null && targetItemIndex != null && draggedItemIndex != targetItemIndex) {
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
        val listInfo = lazyListInfoProvider.lazyListItemInfo
        val draggedItem = listInfo.firstOrNull { it.index == draggedItemIndex } ?: return -1

        val draggedItemCenter = draggedItem.offset + draggedItem.size / 2
        val dragPosition = draggedItemCenter.toFloat() + dragAmount.y

        return listInfo.minByOrNull {
            val itemCenter = it.offset + it.size / 2
            kotlin.math.abs(itemCenter - dragPosition)
        }?.index ?: -1
    }
}
