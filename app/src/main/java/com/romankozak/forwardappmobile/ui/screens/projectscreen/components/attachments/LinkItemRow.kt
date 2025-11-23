package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.attachments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SwipeableListItem

@Composable
fun LinkItemRow(
    linkItem: ListItemContent.LinkItem,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onCopyContentRequest: (ListItemContent) -> Unit,
    modifier: Modifier = Modifier,
    endAction: @Composable () -> Unit = {},
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        animationSpec = spring(),
        label = "link_item_background",
    )

    val link = linkItem.link.linkData

    SwipeableListItem(
        modifier = modifier,
        isDragging = false,
        isAnyItemDragging = false,
        swipeEnabled = true,
        isAnotherItemSwiped = false,
        resetTrigger = 0,
        onSwipeStart = {},
        onDelete = onDelete,
        onMoreActionsRequest = {},
        onGoalTransportRequest = {},
        onCopyContentRequest = { onCopyContentRequest(linkItem) },
        onStartTrackingRequest = {},
        backgroundColor = backgroundColor,
        content = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(onClick, onLongClick) {
                            detectTapGestures(
                                onLongPress = { onLongClick() },
                                onTap = { onClick() },
                            )
                        }.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconImageVector =
                    when (link.type) {
                        LinkType.PROJECT -> Icons.AutoMirrored.Filled.ListAlt
                        LinkType.URL -> Icons.Default.Language
                        LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
                        null -> Icons.Default.BrokenImage
                        else -> Icons.Default.BrokenImage
                    }
                Icon(
                    imageVector = iconImageVector,
                    contentDescription = "Link icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.displayName ?: link.target,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            when (link.type) {
                                LinkType.PROJECT -> "Посилання на список"
                                LinkType.URL -> link.target
                                LinkType.OBSIDIAN -> "Нотатка Obsidian"
                                null -> "Broken"
                                else -> "Broken"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                endAction()
            }
        },
        onAddToDayPlanRequest = {},
        onMoveToTopRequest = {},
        onToggleCompleted = {},
    )
}
