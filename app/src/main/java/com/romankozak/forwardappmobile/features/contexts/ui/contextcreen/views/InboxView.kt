package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord
import com.romankozak.forwardappmobile.features.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.BacklogViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.InboxScreen

@Composable
fun InboxView(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    inboxRecords: List<InboxRecord>,
    listState: LazyListState,
    highlightedRecordId: String?,
    navigationManager: EnhancedNavigationManager,
) {
    Box(modifier = modifier) {
        InboxScreen(
            records = inboxRecords,
            
            onDelete = viewModel.inboxHandler::deleteInboxRecord,
            onPromoteToGoal = viewModel.inboxHandler::promoteInboxRecordToGoal,
            onRecordClick = { record ->
                navigationManager.navigate("inbox_editor_screen/${record.id}")
            },
            
            onCopy = { text -> viewModel.copyInboxRecordText(text) },
            listState = listState,
            highlightedRecordId = highlightedRecordId,
        )
    }
}
