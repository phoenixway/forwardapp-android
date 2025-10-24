package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

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

private const val TAG = "DND_DEBUG"

@Stable
class SimpleDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
) {
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggedItemLayoutInfo by mutableStateOf<LazyListItemInfo?>(null)
    private var autoScrollJob: Job? = null

    var draggedItemIndex by mutableIntStateOf(-1)
        private set

    val isDragging: Boolean
        get() = draggedItemIndex != -1

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return

        val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == item.listItem.id }
        if (itemInfo != null) {
            draggedItemLayoutInfo = itemInfo
            draggedItemIndex = itemInfo.index
            Log.d(TAG, "▶️ [onDragStart] START. ItemId: ${item.listItem.id}, Index: ${itemInfo.index}")
        } else {
            Log.w(TAG, "[onDragStart] WARNING: Could not find layout info for dragged item: ${item.listItem.id}")
        }
    }

    fun onDrag(dragAmount: Float) {
        if (!isDragging) return

        draggedDistance += dragAmount
        Log.d(TAG, "[onDrag] Drag amount: $dragAmount, Total distance: $draggedDistance")

        val draggedItem = draggedItemLayoutInfo
        if (draggedItem == null) {
            Log.w(TAG, "[onDrag] SKIPPING: draggedItemLayoutInfo is null.")
            return
        }

        val startOffset = draggedItem.offset + draggedDistance
        val endOffset = startOffset + draggedItem.size

        val hoveredItem =
            state.layoutInfo.visibleItemsInfo.find { item ->
                if (item.key == draggedItem.key) return@find false

                val delta = (startOffset + endOffset) / 2f
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                delta.toInt() in itemStart..itemEnd
            }

        if (hoveredItem != null && hoveredItem.index != draggedItemIndex) {
            val fromIndex = draggedItemIndex
            val toIndex = hoveredItem.index
            Log.d(TAG, "[onDrag] Attempting to move from $fromIndex to $toIndex")
            onMove(fromIndex, toIndex)
            draggedItemIndex = toIndex
            Log.d(TAG, "[onDrag] Item moved to index: $toIndex")
        }
    }

    fun onDragEnd() {
        Log.d(TAG, "⏹️ [onDragEnd] END.")
        reset()
    }

    fun reset() {
        Log.d(TAG, "[reset] Resetting drag state.")
        draggedDistance = 0f
        draggedItemLayoutInfo = null
        draggedItemIndex = -1
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    fun getItemOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f

        val draggedItemKey = draggedItemLayoutInfo?.key ?: return 0f

        if (item.listItem.id == draggedItemKey) {
            Log.d(TAG, "[getItemOffset] Applying offset $draggedDistance to item ${item.listItem.id}")
            return draggedDistance
        }

        return 0f
    }
}
