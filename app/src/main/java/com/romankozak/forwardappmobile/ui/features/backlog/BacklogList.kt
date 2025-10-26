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
import androidx.compose.ui.input.pointer.pointerInput
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogListScreen(
    items: List<ListItemContent>,
    modifier: Modifier = Modifier,
    onMove: (from: Int, to: Int) -> Unit,
    onItemClick: (ListItemContent) -> Unit,
    onLongClick: (ListItemContent) -> Unit,
    onCheckedChange: (ListItemContent, Boolean) -> Unit,
    onDelete: (ListItemContent) -> Unit,
    onMoveToTop: (ListItemContent) -> Unit,
    onAddToDayPlan: (ListItemContent) -> Unit,
    onShowGoalTransportMenu: (ListItemContent) -> Unit,
    onStartTracking: (ListItemContent) -> Unit,
    onCopyContent: (ListItemContent) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to -> onMove(from.index, to.index) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<ListItemContent?>(null) }

    if (showBottomSheet && selectedItemForActions != null) {
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onDelete = { onDelete(selectedItemForActions!!) },
            onMoveToTop = { onMoveToTop(selectedItemForActions!!) },
            onAddToDayPlan = { onAddToDayPlan(selectedItemForActions!!) },
            onShowGoalTransportMenu = { onShowGoalTransportMenu(selectedItemForActions!!) },
            onStartTracking = { onStartTracking(selectedItemForActions!!) },
            onCopyContent = { onCopyContent(selectedItemForActions!!) },
        )
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
         //   .reorderable(reorderableState)
           // .detectReorderAfterLongPress(reorderableState)
    ) {
        items(items, key = { it.listItem.id }) { item ->
            ReorderableItem(reorderableState, key = item.listItem.id) { isDragging ->
                BacklogItem(
                    item = item,
                    modifier = Modifier,
                    onItemClick = { onItemClick(item) },
                    onLongClick = { onLongClick(item) },
                    onMoreClick = { 
                        selectedItemForActions = item
                        showBottomSheet = true 
                    },
                    onCheckedChange = { isChecked -> onCheckedChange(item, isChecked) },
                    showCheckbox = true, // Or get from state
                    isSelected = isDragging
                )
            }
        }
    }
}
