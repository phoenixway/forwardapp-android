

package com.romankozak.forwardappmobile.ui.screens.projectscreen.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.InboxScreen

@Composable
fun InboxView(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    inboxRecords: List<InboxRecord>,
    listState: LazyListState,
    highlightedRecordId: String?,
) {
    Box(modifier = modifier) {
        InboxScreen(
            records = inboxRecords,
            
            onDelete = viewModel.inboxHandler::deleteInboxRecord,
            onPromoteToGoal = viewModel.inboxHandler::promoteInboxRecordToGoal,
            onPromoteToAnotherList = viewModel.inboxHandler::onPromoteToAnotherList,
            onRecordClick = viewModel.inboxHandler::onInboxRecordEditRequest,
            
            onCopy = { text -> viewModel.copyInboxRecordText(text) },
            listState = listState,
            highlightedRecordId = highlightedRecordId,
        )
    }
}
