package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox

import androidx.compose.foundation.lazy.LazyListState
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord

data class InboxViewState(
    val inboxRecords: List<InboxRecord>,
    val listState: LazyListState,
    val highlightedRecordId: String?,
    val isSelectionMode: Boolean,
    val selectedRecordIds: Set<String>,
    val canPaste: Boolean,
    val onTagClick: (String) -> Unit,
)
