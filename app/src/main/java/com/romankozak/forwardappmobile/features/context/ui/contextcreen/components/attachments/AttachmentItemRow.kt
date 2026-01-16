package com.romankozak.forwardappmobile.features.context.ui.contextcreen.components.attachments

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.features.context.ui.contextcreen.components.backlogitems.SwipeableListItem

@Composable
fun AttachmentItemRow(
    onDelete: () -> Unit,
    backgroundColor: Color,
    onCopyContentRequest: () -> Unit,
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
        onCopyContentRequest = onCopyContentRequest,
        onStartTrackingRequest = { },
        backgroundColor = backgroundColor,
        content = content,
        onAddToDayPlanRequest = {},
        onMoveToTopRequest = {},
        onToggleCompleted = {},
    )
}
