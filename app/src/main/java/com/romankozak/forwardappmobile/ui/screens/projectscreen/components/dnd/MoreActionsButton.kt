package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.geometry.Offset

@Composable
fun MoreActionsButton(
    onMoreClick: () -> Unit,
    dragDropState: SimpleDragDropState,
    item: ListItemContent,
    modifier: Modifier = Modifier
) {
    val icon = if (dragDropState.isDragging) {
        Icons.Default.DragHandle
    } else {
        Icons.Default.MoreVert
    }

    Icon(
        imageVector = icon,
        contentDescription = "More actions",
        modifier = modifier
            .pointerInput(dragDropState, item, onMoreClick) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragDropState.onDragStart(item) },
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        change.consume()
                        dragDropState.onDrag(dragAmount.y)
                    },
                    onDragEnd = { dragDropState.onDragEnd() },
                    onDragCancel = { dragDropState.onDragEnd() }
                )
            }
            .pointerInput(onMoreClick) { detectTapGestures(onTap = { onMoreClick() }) }
    )
}
