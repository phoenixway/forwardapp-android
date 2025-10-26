package com.romankozak.forwardappmobile.ui.dnd

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
    private val onMove: (Int, Int) -> Unit,
    private val scrollBy: suspend (Float) -> Unit
) {
    private val _dragState = MutableStateFlow(DragAndDropState())
    val dragState: StateFlow<DragAndDropState> = _dragState.asStateFlow()

    private var dragJob: Job? = null
    private var autoScrollJob: Job? = null

    fun onDragStart(offset: Offset, index: Int, initialItemOffset: Int, dragOffsetInItem: Float) {
        _dragState.update {
            it.copy(
                dragInProgress = true,
                draggedItemIndex = index,
                dragAmount = offset,
                initialItemOffset = initialItemOffset,
                dragOffsetInItem = dragOffsetInItem
            )
        }

        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            while (true) {
                delay(16)
                val fingerY = _dragState.value.dragAmount.y
                // This is a hack, I don't have access to the viewport height here.
                // I will assume a fixed height for now.
                val viewportHeight = 2000f
                val hotZone = viewportHeight * 0.2f

                val scrollAmount =
                    if (fingerY < hotZone) {
                        val distance = hotZone - fingerY
                        -(distance / hotZone) * 100f
                    } else if (fingerY > viewportHeight - hotZone) {
                        val distance = fingerY - (viewportHeight - hotZone)
                        (distance / hotZone) * 100f
                    } else {
                        0f
                    }

                if (scrollAmount != 0f) {
                    scope.launch {
                        scrollBy(scrollAmount)
                    }
                }
            }
        }
    }

    fun onDrag(position: Offset) {
        _dragState.update { it.copy(dragAmount = position) }
    }

    fun onDragEnd() {
        autoScrollJob?.cancel()
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

    fun setTargetItemIndex(index: Int?) {
        _dragState.update { it.copy(targetItemIndex = index) }
    }
}