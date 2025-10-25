package com.romankozak.forwardappmobile.ui.dnd

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DRAG_HANDLE_DEBUG"

private var autoScrollJob: Job? = null

private fun handleAutoScroll(
    scope: CoroutineScope,
    lazyListState: LazyListState,
    containerHeight: Float,
    pointerY: Float
) {
    autoScrollJob?.cancel()
    
    val topThreshold = containerHeight * 0.15f
    val bottomThreshold = containerHeight * 0.85f
    
    when {
        pointerY < topThreshold -> {
            val scrollSpeed = ((topThreshold - pointerY) / topThreshold * 30f).coerceIn(5f, 40f)
            autoScrollJob = scope.launch {
                while (true) {
                    lazyListState.scrollBy(-scrollSpeed)
                    delay(16)
                }
            }
        }
        pointerY > bottomThreshold -> {
            val scrollSpeed = ((pointerY - bottomThreshold) / (containerHeight - bottomThreshold) * 30f).coerceIn(5f, 40f)
            autoScrollJob = scope.launch {
                while (true) {
                    lazyListState.scrollBy(scrollSpeed)
                    delay(16)
                }
            }
        }
        else -> {
            autoScrollJob?.cancel()
            autoScrollJob = null
        }
    }
}

fun Modifier.dragHandle(
    dragDropManager: DragDropManager,
    itemIndex: Int,
    lazyListState: LazyListState,
    scope: CoroutineScope,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this.pointerInput(itemIndex) {
    Log.d(TAG, "DragHandle pointerInput registered for item $itemIndex")
    
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        Log.d(TAG, "✅ DOWN detected on handle at index $itemIndex, position: ${down.position}")
        
        onDragStateChanged(true)
        
        var totalDrag = Offset.Zero
        var dragStarted = false
        val dragThreshold = 15f
        
        try {
            val dragResult = drag(down.id) { change ->
                totalDrag += change.positionChange()
                
                if (!dragStarted && totalDrag.getDistance() > dragThreshold) {
                    Log.d(TAG, "🚀 DRAG threshold exceeded! Starting drag for item $itemIndex")
                    dragStarted = true
                    
                    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == itemIndex }
                    
                    if (itemInfo != null) {
                        val initialItemOffset = itemInfo.offset
                        val dragOffsetInItem = change.position.y - initialItemOffset
                        
                        dragDropManager.onDragStart(
                            offset = change.position,
                            index = itemInfo.index,
                            initialItemOffset = initialItemOffset,
                            dragOffsetInItem = dragOffsetInItem
                        )
                    } else {
                        Log.e(TAG, "❌ ItemInfo not found for index $itemIndex")
                    }
                }
                
                if (dragStarted) {
                    change.consume()
                    dragDropManager.onDrag(change.position)
                    
                    handleAutoScroll(
                        scope = scope,
                        lazyListState = lazyListState,
                        containerHeight = size.height.toFloat(),
                        pointerY = change.position.y
                    )
                }
            }
            
            if (dragStarted) {
                Log.d(TAG, "Drag gesture completed for item $itemIndex")
                dragDropManager.onDragEnd()
            } else {
                Log.d(TAG, "Gesture ended without reaching drag threshold (was tap or small movement)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in drag gesture: ${e.message}", e)
        } finally {
            onDragStateChanged(false)
            autoScrollJob?.cancel()
            Log.d(TAG, "DragHandle state reset for item $itemIndex")
        }
    }
}
