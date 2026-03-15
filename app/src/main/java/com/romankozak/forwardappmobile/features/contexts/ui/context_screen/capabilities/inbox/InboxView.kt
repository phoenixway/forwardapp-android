package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel

@Composable
fun InboxView(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    state: InboxViewState,
    navigationManager: EnhancedNavigationManager,
) {
    Box(modifier = modifier) {
        InboxScreen(
            records = state.inboxRecords,
            onDelete = viewModel.inboxHandler::deleteInboxRecord,
            onPromoteToGoal = viewModel.inboxHandler::promoteInboxRecordToGoal,
            onRecordClick = { record ->
                navigationManager.navigate("inbox_editor_screen/${record.id}")
            },
            onCopy = { text -> viewModel.copyInboxRecordText(text) },
            listState = state.listState,
            highlightedRecordId = state.highlightedRecordId,
        )
    }
}
