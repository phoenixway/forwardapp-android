package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import android.util.Log
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "DND_DEBUG"

@Stable
class SimpleDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
) {
    private var dragState by mutableStateOf<DragState?>(null)
    private var autoScrollJob: Job? = null

    val isDragging: Boolean
        get() = dragState != null

    val initialIndexOfDraggedItem: Int
        get() = dragState?.initialIndex ?: -1

    val draggedItemIndex: Int
        get() = dragState?.currentIndex ?: -1

    val targetIndexOfDraggedItem: Int
        get() = dragState?.targetIndex ?: -1

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return

        state.layoutInfo.visibleItemsInfo
            .find { it.key == item.listItem.id }
            ?.also { info ->
                dragState = DragState(
                    initialIndex = info.index,
                    currentIndex = info.index,
                    targetIndex = info.index,
                    draggedItemLayoutInfo = info
                )
                Log.d(TAG, "▶️ [onDragStart] START. ItemId: ${item.listItem.id}, Index: ${info.index}")
            } ?: Log.w(TAG, "[onDragStart] WARNING: Could not find layout info for dragged item: ${item.listItem.id}")
    }

    fun onDrag(dragAmount: Float) {
        dragState?.let {
            dragState = it.copy(draggedDistance = it.draggedDistance + dragAmount)
            val newTarget = findHoveredItemIndex()
            if (newTarget != null && newTarget != it.targetIndex) {
                dragState = it.copy(targetIndex = newTarget)
                Log.d(TAG, "[onDrag] New target index: $newTarget")
            }
        }
    }

    private fun findHoveredItemIndex(): Int? {
        val currentDragState = dragState ?: return null
        val draggedItem = currentDragState.draggedItemLayoutInfo ?: return null

        val startOffset = draggedItem.offset + currentDragState.draggedDistance
        val endOffset = startOffset + draggedItem.size

        return state.layoutInfo.visibleItemsInfo.find { item ->
            if (item.index == currentDragState.initialIndex) return@find false
            val delta = (startOffset + endOffset) / 2f
            delta.toInt() in item.offset..(item.offset + item.size)
        }?.index
    }

    fun onDragEnd() {
        Log.d(TAG, "⏹️ [onDragEnd] END. ===========================")
        dragState?.let {
            if (it.initialIndex != -1 && it.targetIndex != -1 && it.initialIndex != it.targetIndex) {
                Log.i(TAG, "[onDragEnd] Executing move: ${it.initialIndex} -> ${it.targetIndex}")
                onMove(it.initialIndex, it.targetIndex)
            }
        }
        scope.launch {
            // No delay needed anymore, reset is now safer
            reset()
        }
        Log.d(TAG, "[onDragEnd] ===================================")
    }

    fun reset() {
        Log.d(TAG, "[reset] Resetting drag state.")
        dragState = null
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    fun getItemOffset(item: ListItemContent): Float {
        val currentDragState = dragState ?: return 0f
        val itemInfo = state.layoutInfo.visibleItemsInfo.find { it.key == item.listItem.id }
        val itemIndex = itemInfo?.index ?: return 0f

        val draggedIndex = currentDragState.initialIndex
        val targetIndex = currentDragState.targetIndex
        val draggedItemSize = (currentDragState.draggedItemLayoutInfo?.size ?: 0).toFloat()

        return when {
            itemIndex == draggedIndex -> currentDragState.draggedDistance
            draggedIndex < targetIndex && itemIndex in (draggedIndex + 1)..targetIndex -> -draggedItemSize
            draggedIndex > targetIndex && itemIndex in targetIndex until draggedIndex -> draggedItemSize
            else -> 0f
        }
    }

    private data class DragState(
        val initialIndex: Int,
        val currentIndex: Int,
        val targetIndex: Int,
        val draggedDistance: Float = 0f,
        val draggedItemLayoutInfo: LazyListItemInfo?
    )
}
