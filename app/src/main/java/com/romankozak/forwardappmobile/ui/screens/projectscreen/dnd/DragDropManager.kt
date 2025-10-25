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
            delay(100)
            val draggedItemInfo = lazyListInfoProvider.lazyListItemInfo.firstOrNull { it.index == index }
            
            val draggedItemHeight = try {
                draggedItemInfo?.size?.let { size ->
                    val heightField = size::class.java.getDeclaredField("height")
                    heightField.isAccessible = true
                    (heightField.get(size) as Int).toFloat()
                } ?: 0f
            } catch (e: Exception) {
                Log.e("DND_DEBUG", "Error getting height: ", e)
                0f
            }
            android.util.Log.d("DND_DEBUG", "onDragStart: calculated height=$draggedItemHeight")
            
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
            if (draggedItemIndex != null && targetItemIndex != null && 
                draggedItemIndex != targetItemIndex) {
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
        val draggedItem = listInfo.firstOrNull { 
            it.index == draggedItemIndex 
        } ?: return -1

        val draggedItemCenter = draggedItem.offset + (try {
            draggedItem.size.let { size ->
                val heightField = size::class.java.getDeclaredField("height")
                heightField.isAccessible = true
                (heightField.get(size) as Int) / 2
            }
        } catch (e: Exception) {
            0
        })
        val dragPosition = draggedItemCenter + dragAmount.y

        val itemsToSearch = if (dragAmount.y < 0) listInfo.asReversed() else listInfo

        return itemsToSearch.minByOrNull {
            val itemCenter = it.offset + (try {
                it.size.let { size ->
                    val heightField = size::class.java.getDeclaredField("height")
                    heightField.isAccessible = true
                    (heightField.get(size) as Int) / 2
                }
            } catch (e: Exception) {
                0
            })
            kotlin.math.abs(itemCenter - dragPosition)
        }?.index ?: -1
    }
}
