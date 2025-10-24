package com.romankozak.forwardappmobile.ui.screens.projectscreen.dnd

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "DND_DEBUG"

class DragDropManager(
    private val scope: CoroutineScope,
    private val listInfoProvider: LazyListInfoProvider,
    private val onMove: (Int, Int) -> Unit,
) {
    private val _dragState = MutableStateFlow<DragState?>(null)
    val dragState: StateFlow<DragState?> = _dragState.asStateFlow()

    private var autoScrollJob: Job? = null

    val isDragging: Boolean
        get() = _dragState.value != null

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return

        listInfoProvider.visibleItemsInfo
            .find { it.key == item.listItem.id }
            ?.also { info ->
                _dragState.value = DragState(
                    initialIndex = info.index,
                    currentIndex = info.index,
                    targetIndex = info.index,
                    draggedItemLayoutInfo = info
                )
                Log.d(TAG, "▶️ [onDragStart] START. ItemId: ${item.listItem.id}, Index: ${info.index}")
            } ?: Log.w(TAG, "[onDragStart] WARNING: Could not find layout info for dragged item: ${item.listItem.id}")
    }

    fun onDrag(dragAmount: Float) {
        _dragState.value?.let {
            _dragState.value = it.copy(draggedDistance = it.draggedDistance + dragAmount)
            val newTarget = findHoveredItemIndex()
            if (newTarget != null && newTarget != it.targetIndex) {
                _dragState.value = it.copy(targetIndex = newTarget)
                Log.d(TAG, "[onDrag] New target index: $newTarget")
            }
        }
    }

    private fun findHoveredItemIndex(): Int? {
        val currentDragState = _dragState.value ?: return null
        val draggedItem = currentDragState.draggedItemLayoutInfo ?: return null

        val startOffset = draggedItem.offset + currentDragState.draggedDistance
        val endOffset = startOffset + draggedItem.size

        return listInfoProvider.visibleItemsInfo.find { item ->
            if (item.index == currentDragState.initialIndex) return@find false
            val delta = (startOffset + endOffset) / 2f
            delta.toInt() in item.offset..(item.offset + item.size)
        }?.index
    }

    fun onDragEnd() {
        Log.d(TAG, "⏹️ [onDragEnd] END. ===========================")
        _dragState.value?.let {
            if (it.initialIndex != -1 && it.targetIndex != -1 && it.initialIndex != it.targetIndex) {
                Log.i(TAG, "[onDragEnd] Executing move: ${it.initialIndex} -> ${it.targetIndex}")
                onMove(it.initialIndex, it.targetIndex)
            }
        }
        scope.launch {
            reset()
        }
        Log.d(TAG, "[onDragEnd] ===================================")
    }

    fun reset() {
        Log.d(TAG, "[reset] Resetting drag state.")
        _dragState.value = null
        autoScrollJob?.cancel()
        autoScrollJob = null
    }
}
