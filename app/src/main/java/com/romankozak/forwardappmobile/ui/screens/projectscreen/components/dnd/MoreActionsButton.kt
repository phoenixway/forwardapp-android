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
    isDragging: Boolean,
    isDraggable: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (isDragging && isDraggable) {
        Icons.Default.DragHandle
    } else {
        Icons.Default.MoreVert
    }

    var updatedModifier = modifier.pointerInput(onMoreClick) { detectTapGestures(onTap = { onMoreClick() }) }

    if (isDraggable) {
        updatedModifier = updatedModifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> onDragStart(offset) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                },
                onDragEnd = onDragEnd,
                onDragCancel = onDragEnd
            )
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = "More actions",
        modifier = updatedModifier
    )
}
