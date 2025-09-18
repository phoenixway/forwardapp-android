package com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd

import android.util.Log
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DND_DEBUG"

enum class DropPosition { Top, Bottom }

data class DropIndicatorState(
    val show: Boolean,
    val position: DropPosition = DropPosition.Bottom,
)

fun shouldShowDropIndicator(
    currentIndex: Int,
    draggedIndex: Int,
    targetIndex: Int,
): DropIndicatorState {
    if (draggedIndex == -1 || targetIndex == -1 || draggedIndex == targetIndex) {
        return DropIndicatorState(show = false)
    }

    return when {
        draggedIndex < targetIndex && currentIndex == targetIndex -> DropIndicatorState(show = true, position = DropPosition.Top)
        draggedIndex > targetIndex && currentIndex == targetIndex -> DropIndicatorState(show = true, position = DropPosition.Top)
        else -> DropIndicatorState(show = false)
    }
}

@Stable
class SimpleDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
) {
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggedItemLayoutInfo by mutableStateOf<LazyListItemInfo?>(null)
    private var autoScrollJob: Job? = null

    var initialIndexOfDraggedItem by mutableIntStateOf(-1)
        private set

    var draggedItemIndex by mutableIntStateOf(-1)
        private set

    var targetIndexOfDraggedItem by mutableIntStateOf(-1)
        private set

    val isDragging: Boolean
        get() = draggedItemIndex != -1

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return

        val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == item.listItem.id }
        if (itemInfo != null) {
            if (isDragging) {
                reset()
            }
            draggedItemLayoutInfo = itemInfo
            initialIndexOfDraggedItem = itemInfo.index
            draggedItemIndex = itemInfo.index
            targetIndexOfDraggedItem = itemInfo.index
            Log.d(TAG, "▶️ [onDragStart] START. ItemId: ${item.listItem.id}, Index: ${itemInfo.index}")
        } else {
            Log.w(TAG, "[onDragStart] WARNING: Could not find layout info for dragged item: ${item.listItem.id}")
        }
    }

    fun onDrag(dragAmount: Float) {
        if (!isDragging) return

        draggedDistance += dragAmount

        val draggedItem = draggedItemLayoutInfo
        if (draggedItem == null) {
            Log.w(TAG, "[onDrag] SKIPPING: draggedItemLayoutInfo is null.")
            return
        }

        val startOffset = draggedItem.offset + draggedDistance
        val endOffset = startOffset + draggedItem.size

        val hoveredItem =
            state.layoutInfo.visibleItemsInfo.find { item ->
                if (item.index == initialIndexOfDraggedItem) return@find false

                val delta = (startOffset + endOffset) / 2f
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                delta.toInt() in itemStart..itemEnd
            }

        if (hoveredItem != null && hoveredItem.index != targetIndexOfDraggedItem) {
            targetIndexOfDraggedItem = hoveredItem.index
            Log.d(TAG, "[onDrag] New target index: ${hoveredItem.index}")
        }
    }

    fun onDragEnd() {
        Log.d(TAG, "⏹️ [onDragEnd] END. ===========================")
        val fromIndex = initialIndexOfDraggedItem
        val toIndex = targetIndexOfDraggedItem

        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            Log.i(TAG, "[onDragEnd] Executing move: $fromIndex -> $toIndex")
            onMove(fromIndex, toIndex)

            scope.launch {
                delay(100)
                val isTargetVisible = state.layoutInfo.visibleItemsInfo.any { it.index == toIndex }
                if (!isTargetVisible) {
                    state.animateScrollToItem(toIndex)
                    Log.d(TAG, "[onDragEnd] Target item was not visible. Scrolled to new index: $toIndex")
                } else {
                    Log.d(TAG, "[onDragEnd] Target item is already visible. Skipping scroll.")
                }
            }
        } else {
            Log.d(TAG, "[onDragEnd] SKIPPED: No change in position or invalid state.")
        }

        reset()
        Log.d(TAG, "[onDragEnd] ===================================")
    }

    fun reset() {
        Log.d(TAG, "[reset] Resetting drag state.")
        draggedDistance = 0f
        draggedItemLayoutInfo = null
        initialIndexOfDraggedItem = -1
        draggedItemIndex = -1
        targetIndexOfDraggedItem = -1
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    fun getItemOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f

        val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == item.listItem.id }
        val itemIndex = itemInfo?.index ?: return 0f

        val draggedIndex = initialIndexOfDraggedItem
        val targetIndex = targetIndexOfDraggedItem

        if (draggedIndex == -1 || targetIndex == -1) return 0f

        val draggedItemSize = (draggedItemLayoutInfo?.size ?: 0).toFloat()

        val offset =
            when {
                itemIndex == draggedIndex -> draggedDistance
                draggedIndex < targetIndex && itemIndex in (draggedIndex + 1)..targetIndex -> -draggedItemSize
                draggedIndex > targetIndex && itemIndex in targetIndex until draggedIndex -> draggedItemSize
                else -> 0f
            }

        return offset
    }
}
