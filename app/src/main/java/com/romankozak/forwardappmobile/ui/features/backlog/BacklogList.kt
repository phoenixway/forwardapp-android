package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogListScreen(
    items: List<ListItemContent>,
    modifier: Modifier = Modifier,
    showCheckboxes: Boolean,
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
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to -> onMove(from.index, to.index) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<ListItemContent?>(null) }

    if (showBottomSheet && selectedItemForActions != null) {
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onCopyContent = { },
            onRemindersClick = { onRemindersClick(selectedItemForActions!!) },
        )
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        items(items, key = { it.listItem.id }) { item ->
            ReorderableItem(reorderableState, key = item.listItem.id) { isDragging ->
                SwipeableBacklogItem(
                    item = item,
                    reorderableScope = this,
                    showCheckboxes = showCheckboxes,
                    isDragging = isDragging,
                    onItemClick = { onItemClick(item) },
                    onLongClick = { onLongClick(item) },
                    onMoreClick = { 
                        selectedItemForActions = item
                        showBottomSheet = true 
                    },
                    onCheckedChange = onCheckedChange,
                    onDelete = { onDelete(item) },
                    onDeleteEverywhere = { onDeleteEverywhere(item) },
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
