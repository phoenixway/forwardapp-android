package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.features.backlog.isCompleted
import com.romankozak.forwardappmobile.ui.features.backlog.withCompletedAtEnd
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogListScreen(
    items: List<ListItemContent>,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    showCheckboxes: Boolean,
    selectedItemIds: Set<String>,
    contextMarkerToEmojiMap: Map<String, String>,
    swipedItemId: String?,
    swipeResetCounter: Int,
    onMove: (from: Int, to: Int) -> Unit,
    onItemClick: (ListItemContent) -> Unit,
    onLongClick: (ListItemContent) -> Unit,
    onCheckedChange: (ListItemContent, Boolean) -> Unit,
    onDelete: (ListItemContent) -> Unit,
    onDeleteEverywhere: (ListItemContent) -> Unit,
    onMoveToTop: (ListItemContent) -> Unit,
    onAddToDayPlan: (ListItemContent) -> Unit,
    onStartTracking: (ListItemContent) -> Unit,
    onShowGoalTransportMenu: (ListItemContent) -> Unit,
    onRelatedLinkClick: (com.romankozak.forwardappmobile.data.database.models.RelatedLink) -> Unit,
    onRemindersClick: (ListItemContent) -> Unit,
    onCopyContent: (ListItemContent) -> Unit,
    onResetSwipe: (String) -> Unit,
) {
    val reorderableState = rememberReorderableLazyListState(listState) { from, to -> onMove(from.index, to.index) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<ListItemContent?>(null) }
    val sortedItems = remember(items) { items.withCompletedAtEnd() }
    val completedStartIndex = remember(sortedItems) { sortedItems.indexOfFirst { it.isCompleted() } }
    val completedCount = remember(sortedItems) {
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

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        itemsIndexed(sortedItems, key = { _, item -> item.listItem.id }) { index, item ->
            val showCompletedHeader = completedStartIndex != -1 && index == completedStartIndex
            Column {
                if (showCompletedHeader) {
                    CompletedSectionHeader(completedCount = completedCount)
                }
                ReorderableItem(reorderableState, key = item.listItem.id) { isDragging ->
                    val isSelected = item.listItem.id in selectedItemIds
                    SwipeableBacklogItem(
                        item = item,
                        reorderableScope = this,
                        showCheckboxes = showCheckboxes,
                        isDragging = isDragging,
                        isSelected = isSelected,
                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        onRequestCloseOthers = { onResetSwipe(item.listItem.id) },
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
                        onMoveToTop = { onMoveToTop(item) },
                        onAddToDayPlan = { onAddToDayPlan(item) },
                        onStartTracking = { onStartTracking(item) },
                        onShowGoalTransportMenu = { onShowGoalTransportMenu(item) },
                        onRelatedLinkClick = onRelatedLinkClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(completedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Виконані",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (completedCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = completedCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
