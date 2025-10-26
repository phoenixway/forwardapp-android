package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Data class representing the current state of a drag-and-drop operation.
 */
data class DragAndDropState(
    val dragInProgress: Boolean = false,
    val draggedItemIndex: Int? = null,
    val targetItemIndex: Int? = null,
    val dragAmount: Offset = Offset.Zero,
    val initialItemOffset: Int = 0,
    val dragOffsetInItem: Float = 0f,
    val totalScrollAmount: Float = 0f,
    val draggedItemHeight: Float = 0f,
    val ghostScreenY: Float = 0f
)

/**
 * A state holder class that manages the logic and state for a reorderable list.
 *
 * This class will centralize all drag-and-drop logic, making it more modular and testable.
 */
class ReorderableState(
    private val onMove: (Int, Int) -> Unit,
    private val scope: CoroutineScope,
    private val scrollBy: suspend (Float) -> Unit,
    private val lazyListInfoProvider: LazyListInfoProvider,
    private val hotZonePercentage: Float = 0.2f,
    private val maxScrollSpeed: Float = 100f
) {
    var dndState by mutableStateOf(DragAndDropState())
        private set

    private var autoScrollJob: Job? = null

    fun onDragStart(
        offset: Offset,
        index: Int,
        initialItemOffset: Int,
        dragOffsetInItem: Float,
        itemHeight: Float
    ) {
        dndState = dndState.copy(
            dragInProgress = true,
            draggedItemIndex = index,
            dragAmount = offset,
            initialItemOffset = initialItemOffset,
            dragOffsetInItem = dragOffsetInItem,
            draggedItemHeight = itemHeight,
            ghostScreenY = offset.y - dragOffsetInItem,
            totalScrollAmount = 0f
        )

        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            while (true) {
                delay(16)
                val fingerY = dndState.dragAmount.y
                val viewportHeight = lazyListInfoProvider.viewportSize.height.toFloat()
                val hotZone = viewportHeight * hotZonePercentage

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
                    scope.launch { 
                        scrollBy(scrollAmount) 
                        dndState = dndState.copy(totalScrollAmount = dndState.totalScrollAmount + scrollAmount)
                    }
                }
            }
        }
    }

    fun onDrag(position: Offset) {
        dndState = dndState.copy(
            dragAmount = position,
            ghostScreenY = position.y - dndState.dragOffsetInItem
        )
        calculateTargetIndex()
    }

    fun onDragEnd() {
        autoScrollJob?.cancel()
        val draggedItemIndex = dndState.draggedItemIndex
        val targetItemIndex = dndState.targetItemIndex

        if (draggedItemIndex != null && targetItemIndex != null && draggedItemIndex != targetItemIndex) {
            onMove(draggedItemIndex, targetItemIndex)
        }

        dndState = dndState.copy(
            dragInProgress = false,
            draggedItemIndex = null,
            targetItemIndex = null,
            dragAmount = Offset.Zero
        )
    }

    private fun calculateTargetIndex() {
        val draggedItemIndex = dndState.draggedItemIndex
        val ghostScreenY = dndState.ghostScreenY

        if (!dndState.dragInProgress || draggedItemIndex == null) return

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

        if (newTargetIndex != dndState.targetItemIndex) {
            dndState = dndState.copy(targetItemIndex = newTargetIndex)
        }
    }
}
