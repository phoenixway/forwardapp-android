package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogListScreen(
    items: List<BacklogItemContent>,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    showCheckboxes: Boolean,
    selectedItemIds: Set<String>,
    contextMarkerToEmojiMap: Map<String, String>,
    swipedItemId: String?,
    swipeResetCounter: Int,
    onMove: (from: Int, to: Int) -> Unit,
    onItemClick: (BacklogItemContent) -> Unit,
    onLongClick: (BacklogItemContent) -> Unit,
    onCheckedChange: (BacklogItemContent, Boolean) -> Unit,
    onDelete: (BacklogItemContent) -> Unit,
    onDeleteEverywhere: (BacklogItemContent) -> Unit,
    onAddToDayPlan: (BacklogItemContent) -> Unit,
    onStartTracking: (BacklogItemContent) -> Unit,
    onShowGoalTransportMenu: (BacklogItemContent) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    onRemindersClick: (BacklogItemContent) -> Unit,
    onCopyContent: (BacklogItemContent) -> Unit,
    onResetSwipe: (String) -> Unit,
) {
    val sortedItems = remember(items) { items.withCompletedAtEnd() }
    val reorderableState =
        rememberReorderableLazyListState(listState) { from, to ->
            if (sortedItems.isEmpty()) return@rememberReorderableLazyListState
            val safeFromIndex = from.index.coerceIn(0, sortedItems.lastIndex)
            val safeToIndex = to.index.coerceIn(0, sortedItems.lastIndex)
            val fromItem = sortedItems[safeFromIndex]
            val toItem = sortedItems[safeToIndex]
            val originalFrom = items.indexOfFirst { it.backlogItem.id == fromItem.backlogItem.id }
            val originalTo = items.indexOfFirst { it.backlogItem.id == toItem.backlogItem.id }
            if (originalFrom >= 0 && originalTo >= 0) {
                onMove(originalFrom, originalTo)
            }
        }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<BacklogItemContent?>(null) }
    val completedStartIndex = remember(sortedItems) { sortedItems.indexOfFirst { it.isCompleted() } }
    val completedCount =
        remember(sortedItems) {
            if (completedStartIndex == -1) 0 else sortedItems.size - completedStartIndex
        }

    if (showBottomSheet && selectedItemForActions != null) {
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onCopyContent = { onCopyContent(selectedItemForActions!!) },
            onRemindersClick = { onRemindersClick(selectedItemForActions!!) },
            onDeleteEverywhere = { onDeleteEverywhere(selectedItemForActions!!) },
        )
    }

    if (sortedItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "Беклог порожній",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Додай першу ціль або посилання на контекст через панель вводу нижче",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        itemsIndexed(sortedItems, key = { _, item -> item.backlogItem.id }) { index, item ->
            val showCompletedHeader = completedStartIndex != -1 && index == completedStartIndex
            Column {
                if (showCompletedHeader) {
                    CompletedSectionHeader(completedCount = completedCount)
                }
                ReorderableItem(reorderableState, key = item.backlogItem.id) { isDragging ->
                    val isSelected = item.backlogItem.id in selectedItemIds
                    SwipeableBacklogItem(
                        item = item,
                        reorderableScope = this,
                        showCheckboxes = showCheckboxes,
                        isDragging = isDragging,
                        isSelected = isSelected,
                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        onRequestCloseOthers = { onResetSwipe(item.backlogItem.id) },
                        swipedItemId = swipedItemId,
                        resetCounter = swipeResetCounter,
                        onItemClick = { onItemClick(item) },
                        onLongClick = { onLongClick(item) },
                        onMoreClick = {
                            selectedItemForActions = item
                            showBottomSheet = true
                        },
                        onCheckedChange = onCheckedChange,
                        onDelete = { onDelete(item) },
                        onRemindersClick = { onRemindersClick(item) },
                        onAddToDayPlan = { onAddToDayPlan(item) },
                        onStartTracking = { onStartTracking(item) },
                        onShowGoalTransportMenu = { onShowGoalTransportMenu(item) },
                        onRelatedLinkClick = onRelatedLinkClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(completedCount: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Виконані",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (completedCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = completedCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
