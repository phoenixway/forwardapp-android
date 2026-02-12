package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.attachments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel

@Composable
fun AttachmentsView(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    attachmentItems: List<BacklogItemContent>,
) {
    val attachments =
        attachmentItems.filter {
            it is BacklogItemContent.LinkItem ||
                it is BacklogItemContent.NoteDocumentItem ||
                it is BacklogItemContent.ChecklistItem
        }

    val items =
        attachments.mapNotNull { item ->
            val id = item.connectionId()
            val title = item.connectionTitle()
            val type = item.connectionType()
            if (title.isBlank()) null else ConnectionItemUi(id = id, title = title, type = type)
        }

    val attachmentByConnectionId =
        remember(attachments) {
            attachments.associateBy { it.connectionId() }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = 8.dp),
    ) {
        ConnectionsPanel(
            items = items,
            onConnectionClick = { item ->
                attachmentByConnectionId[item.id]?.let { viewModel.itemActionHandler.onItemClick(it) }
            },
            onConnectionRemove = { item ->
                attachmentByConnectionId[item.id]?.let { viewModel.onDeleteEverywhere(it) }
            },
            onAddConnection = { type ->
                when (type) {
                    AddConnectionType.CONTEXT -> viewModel.inputHandler.onAddListLinkRequest()
                    AddConnectionType.ATTACHMENT -> viewModel.onShowCreateNoteDocumentDialog()
                    AddConnectionType.EXTERNAL_LINK -> viewModel.onShowAddWebLinkDialog()
                    AddConnectionType.OBSIDIAN_NOTE -> viewModel.onShowAddObsidianLinkDialog()
                }
            },
        )
    }
}

private fun BacklogItemContent.connectionId(): String = "backlog:${backlogItem.id}"

private fun BacklogItemContent.connectionTitle(): String =
    when (this) {
        is BacklogItemContent.LinkItem -> link.linkData.displayName?.ifBlank { link.linkData.target } ?: link.linkData.target
        is BacklogItemContent.NoteDocumentItem -> document.name
        is BacklogItemContent.ChecklistItem -> checklist.name
        else -> ""
    }

private fun BacklogItemContent.connectionType(): ConnectionType =
    when (this) {
        is BacklogItemContent.LinkItem ->
            when (link.linkData.type) {
                LinkType.CONTEXT -> ConnectionType.CONTEXT
                LinkType.URL -> ConnectionType.URL
                LinkType.OBSIDIAN -> ConnectionType.OBSIDIAN_NOTE
                null -> ConnectionType.ATTACHMENT
            }
        else -> ConnectionType.ATTACHMENT
    }
