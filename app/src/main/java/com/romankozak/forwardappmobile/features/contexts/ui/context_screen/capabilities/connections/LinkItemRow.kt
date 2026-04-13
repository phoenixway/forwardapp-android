package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.connections

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.SwipeableListItem
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemLayout

@Suppress("LongParameterList")
@Composable
fun LinkItemRow(
    linkItem: BacklogItemContent.LinkItem,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onCopyContentRequest: (BacklogItemContent) -> Unit,
    modifier: Modifier = Modifier,
    endAction: @Composable () -> Unit = {},
) {
    val backgroundColor = rememberLinkItemBackgroundColor(isSelected = isSelected, isHighlighted = isHighlighted)
    val visualState = rememberLinkItemVisualState(linkItem)

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
            LinkItemMainContent(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(onClick, onLongClick) {
                            detectTapGestures(
                                onLongPress = { onLongClick() },
                                onTap = { onClick() },
                            )
                        },
                visualState = visualState,
                endAction = endAction,
            )
        },
        onAddToDayPlanRequest = {},
        onMoveToTopRequest = {},
        onToggleCompleted = {},
    )
}

private data class LinkItemVisualState(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
private fun rememberLinkItemBackgroundColor(
    isSelected: Boolean,
    isHighlighted: Boolean,
): Color {
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
    return backgroundColor
}

@Composable
private fun rememberLinkItemVisualState(
    linkItem: BacklogItemContent.LinkItem,
): LinkItemVisualState {
    val link = linkItem.link.linkData
    return LinkItemVisualState(
        title = link.displayName ?: link.target,
        subtitle = resolveLinkSubtitle(link.type, link.target),
        icon = resolveLinkIcon(link.type),
    )
}

@Composable
private fun LinkItemMainContent(
    modifier: Modifier,
    visualState: LinkItemVisualState,
    endAction: @Composable () -> Unit,
) {
    UnifiedListItemLayout(
        modifier = modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        leading = {
            Icon(
                imageVector = visualState.icon,
                contentDescription = "Link icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        },
        trailing = endAction,
        main = {
            Text(
                text = visualState.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = visualState.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

private fun resolveLinkIcon(type: LinkType?) =
    when (type) {
        LinkType.CONTEXT -> Icons.AutoMirrored.Filled.ListAlt
        LinkType.NOTE_DOCUMENT -> Icons.AutoMirrored.Filled.Note
        LinkType.CHECKLIST -> Icons.AutoMirrored.Filled.ListAlt
        LinkType.MUSIC_NOTE -> Icons.AutoMirrored.Filled.Note
        LinkType.URL -> Icons.Default.Language
        LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
        null -> Icons.Default.BrokenImage
    }

private fun resolveLinkSubtitle(
    type: LinkType?,
    target: String,
): String =
    when (type) {
        LinkType.CONTEXT -> "Посилання на список"
        LinkType.NOTE_DOCUMENT -> "Документ"
        LinkType.CHECKLIST -> "Чекліст"
        LinkType.MUSIC_NOTE -> "Музичні ноти"
        LinkType.URL -> target
        LinkType.OBSIDIAN -> "Нотатка Obsidian"
        null -> "Broken"
    }
