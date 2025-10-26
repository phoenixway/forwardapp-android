package com.romankozak.forwardappmobile.ui.dnd

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
    private val onMove: (Int, Int) -> Unit,
    private val scrollBy: suspend (Float) -> Unit,
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val hotZonePercentage: Float = 0.2f,
    private val maxScrollSpeed: Float = 100f
) {
    init {
        Log.d("DragDropManager", "DragDropManager created")
    }
    private val _dragState = MutableStateFlow(DragAndDropState())
    val dragState: StateFlow<DragAndDropState> = _dragState.asStateFlow()

    private var autoScrollJob: Job? = null

    fun onDragStart(
        offset: Offset,
        index: Int,
        initialItemOffset: Int,
        dragOffsetInItem: Float,
        itemHeight: Float
    ) {
        Log.d("DragDropManager", "onDragStart called with offset: $offset, index: $index, initialItemOffset: $initialItemOffset, dragOffsetInItem: $dragOffsetInItem, itemHeight: $itemHeight")
        _dragState.update {
            it.copy(
                dragInProgress = true,
                draggedItemIndex = index,
                dragAmount = offset,
                initialItemOffset = initialItemOffset,
                dragOffsetInItem = dragOffsetInItem,
                draggedItemHeight = itemHeight,
                ghostScreenY = offset.y - dragOffsetInItem,
                totalScrollAmount = 0f
            )
        }

        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            Log.d("DragDropManager", "autoScrollJob started")
            while (true) {
                delay(16)
                val fingerY = _dragState.value.dragAmount.y
                val viewportHeight = lazyListInfoProvider.viewportSize.height.toFloat()
                val hotZone = viewportHeight * hotZonePercentage

                Log.d("DragDropManager", "fingerY: $fingerY, viewportHeight: $viewportHeight, hotZone: $hotZone")

                val scrollAmount = when {
                    fingerY < hotZone -> {
                        val distance = hotZone - fingerY
                        -(distance / hotZone) * maxScrollSpeed
                    }
                    fingerY > viewportHeight - hotZone -> {
                        val distance = fingerY - (viewportHeight - hotZone)
                        (distance / hotZone) * maxScrollSpeed
                    }
                    else -> 0f
                }

                if (scrollAmount != 0f) {
                    Log.d("DragDropManager", "scrolling by $scrollAmount")
                    scope.launch { 
                        scrollBy(scrollAmount) 
                        _dragState.update { it.copy(totalScrollAmount = it.totalScrollAmount + scrollAmount) }
                    }
                }
            }
        }
    }

    fun onDrag(position: Offset) {
        Log.d("DragDropManager", "onDrag called with position: $position")
        _dragState.update {
            it.copy(
                dragAmount = position,
                ghostScreenY = position.y - it.dragOffsetInItem
            )
        }
    }

    fun onDragEnd() {
        Log.d("DragDropManager", "onDragEnd called")
        autoScrollJob?.cancel()
        val draggedItemIndex = _dragState.value.draggedItemIndex
        val targetItemIndex = _dragState.value.targetItemIndex

        Log.d("DragDropManager", "onDragEnd called. draggedItemIndex: $draggedItemIndex, targetItemIndex: $targetItemIndex")

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