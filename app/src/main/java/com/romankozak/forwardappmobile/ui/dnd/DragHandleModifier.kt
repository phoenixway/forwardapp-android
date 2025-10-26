package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

fun Modifier.dragHandle(
    dragDropManager: DragDropManager,
    itemIndex: Int,
    lazyListState: LazyListState,
    scope: CoroutineScope,
    onDragStateChanged: (Boolean) -> Unit
): Modifier = this.pointerInput(itemIndex) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onDragStateChanged(true)
        
        var totalDrag = Offset.Zero
        var dragStarted = false
        val dragThreshold = 15f
        
        try {
            val dragResult = drag(down.id) { change ->
                totalDrag += change.positionChange()
                
                if (!dragStarted && totalDrag.getDistance() > dragThreshold) {
                    dragStarted = true
                    
                    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == itemIndex }
                    
                    if (itemInfo != null) {
                        val initialItemOffset = itemInfo.offset
                        val dragOffsetInItem = change.position.y - initialItemOffset
                        val itemHeight = itemInfo.size.toFloat()
                        
                        dragDropManager.onDragStart(
                            offset = change.position,
                            index = itemInfo.index,
                            initialItemOffset = initialItemOffset,
                            dragOffsetInItem = dragOffsetInItem,
                            itemHeight = itemHeight
                        )
                    }
                }
                
                if (dragStarted) {
                    change.consume()
                    dragDropManager.onDrag(change.position)
                }
            }
            
            if (dragStarted) {
                dragDropManager.onDragEnd()
            }
        } finally {
            onDragStateChanged(false)
        }
    }
}