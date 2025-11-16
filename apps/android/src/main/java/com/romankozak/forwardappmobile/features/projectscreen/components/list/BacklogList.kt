package com.romankozak.forwardappmobile.features.projectscreen.components.list

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
import com.romankozak.forwardappmobile.features.projectscreen.UiState
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun BacklogView(
    listContent: List<ListItem>,
    listState: LazyListState,
    viewModel: ProjectScreenViewModel,
    uiState: UiState,
    onRemindersClick: (ListItem) -> Unit
) {
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            // viewModel.itemActionHandler.onItemMove(from.index, to.index)
        }
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderableState)
    ) {
        items(
            items = listContent,
            key = { it.id }
        ) { item ->
            ReorderableItem(reorderableState, key = item.id) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation_animation")
                ListItemView(
                    item = item,
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation.value)
                        .detectReorderAfterLongPress(reorderableState),
                    onRemindersClick = { onRemindersClick(item) }
                )
            }
        }
    }
}

@Composable
private fun ListItemView(
    item: ListItem,
    viewModel: ProjectScreenViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier,
    onRemindersClick: () -> Unit
) {
    when (item.type) {
        "goal" -> GoalItem(
            goal = item,
            viewModel = viewModel,
            uiState = uiState,
            modifier = modifier,
            onRemindersClick = onRemindersClick
        )
        "link" -> LinkItem(
            link = item,
            viewModel = viewModel,
            uiState = uiState,
            modifier = modifier
        )
    }
}

@Composable
private fun GoalItem(
    goal: ListItem,
    viewModel: ProjectScreenViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier,
    onRemindersClick: () -> Unit
) {
    // TODO: Implement GoalItem
}

@Composable
private fun LinkItem(
    link: ListItem,
    viewModel: ProjectScreenViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    // TODO: Implement LinkItem
}
