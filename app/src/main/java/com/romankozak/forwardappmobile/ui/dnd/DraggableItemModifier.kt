package com.romankozak.forwardappmobile.ui.dnd

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.draggableItem(
    dragDropManager: DragDropManager,
    index: Int
): Modifier = this.pointerInput(dragDropManager, index) {
    detectDragGesturesAfterLongPress(
        onDragStart = {
            offset ->
            dragDropManager.onDragStart(offset, index)
        },
        onDrag = {
            change, dragAmount ->
            change.consume()
            dragDropManager.onDrag(dragAmount)
        },
        onDragEnd = {
            dragDropManager.onDragEnd()
        },
        onDragCancel = {
            dragDropManager.onDragEnd()
        }
    )
}