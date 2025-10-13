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

@Composable
fun MoreActionsButton(
    onMoreClick: () -> Unit,
    dragDropState: SimpleDragDropState,
    item: ListItemContent,
    modifier: Modifier = Modifier
) {
    var isLongPressed by remember { mutableStateOf(false) }

    val icon = if (isLongPressed || dragDropState.initialIndexOfDraggedItem != -1) {
        Icons.Default.DragHandle
    } else {
        Icons.Default.MoreVert
    }

    Icon(
        imageVector = icon,
        contentDescription = "More actions",
        modifier = modifier
            .pointerInput(dragDropState, item) {
                detectTapGestures(
                    onLongPress = {
                        isLongPressed = true
                        dragDropState.onDragStart(item)
                    },
                    onTap = { onMoreClick() }
                )
            }
    )
}
