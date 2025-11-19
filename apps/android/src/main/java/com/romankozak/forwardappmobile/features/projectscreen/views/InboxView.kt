package com.romankozak.forwardappmobile.features.projectscreen.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.features.projectscreen.ProjectScreenViewModel
import com.romankozak.forwardappmobile.features.projectscreen.components.InboxScreen
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import androidx.navigation.NavController

@Composable
fun InboxView(
    modifier: Modifier = Modifier,
    viewModel: ProjectScreenViewModel,
    inboxRecords: List<InboxRecord>,
    listState: LazyListState,
    highlightedRecordId: String?,
    navController: NavController,
) {
    Box(modifier = modifier) {
        InboxScreen(
            records = inboxRecords,
            
            onDelete = { recordId ->
                viewModel.onEvent(ProjectScreenViewModel.InboxEvent.DeleteRecord(recordId))
            },
            onPromoteToGoal = { record ->
                viewModel.onEvent(ProjectScreenViewModel.InboxEvent.PromoteToGoal(record))
            },
            onRecordClick = { record ->
                navController.navigate("inbox_editor_screen/${record.id}")
            },
            
            onCopy = { text ->
                viewModel.onEvent(ProjectScreenViewModel.InboxEvent.CopyRecordText(text))
            },
            listState = listState,
            highlightedRecordId = highlightedRecordId,
        )
    }
}
