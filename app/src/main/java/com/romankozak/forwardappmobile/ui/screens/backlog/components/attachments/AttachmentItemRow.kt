package com.romankozak.forwardappmobile.ui.screens.backlog.components.attachments

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.ui.screens.backlog.components.SwipeableListItem

@Composable
fun AttachmentItemRow(
    onDelete: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeableListItem(
        modifier = modifier,
        isDragging = false,
        isAnyItemDragging = false,
        swipeEnabled = true,
        isAnotherItemSwiped = false,
        resetTrigger = 0,
        onSwipeStart = { },
        onDelete = onDelete,
        onMoreActionsRequest = { },
        onGoalTransportRequest = { },
        //onCopyContentRequest = { },
        onStartTrackingRequest = { },
        backgroundColor = backgroundColor,
        content = content,
        onAddToDayPlanRequest = {},
    )
}
