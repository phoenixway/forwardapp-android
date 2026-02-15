package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.attachments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType

@Composable
fun AttachmentsView(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    attachmentItems: List<BacklogItemContent>,
) {
    var activePickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    val groupedContexts by viewModel.subprojectChildren.collectAsState()

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
    val contextOptions =
        remember(groupedContexts) {
            groupedContexts
                .values
                .flatten()
                .distinctBy { it.id }
                .map { context -> ProjectOption(id = context.id, name = context.name, parentId = context.parentId) }
        }
    val pickerAttachmentOptions =
        remember(attachments) {
            attachments.mapNotNull { it.toAttachmentOptionOrNull() }
        }
    val preselectedContextIds =
        remember(attachments) {
            attachments.mapNotNull { it.contextTargetIdOrNull() }.toSet()
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
            onConnectionCopy = { item ->
                attachmentByConnectionId[item.id]?.let { viewModel.itemActionHandler.copyAttachmentItem(it) }
            },
            onConnectionCut = { item ->
                attachmentByConnectionId[item.id]?.let { viewModel.itemActionHandler.cutAttachmentItem(it) }
            },
            onAddConnection = { type ->
                pendingCreateAction = null
                when (type) {
                    AddConnectionType.CONTEXT -> activePickerTab = LinkPickerTab.CONTEXTS
                    AddConnectionType.ATTACHMENT -> activePickerTab = LinkPickerTab.ATTACHMENTS
                    AddConnectionType.EXTERNAL_LINK -> activePickerTab = LinkPickerTab.ATTACHMENTS
                    AddConnectionType.OBSIDIAN_NOTE -> activePickerTab = LinkPickerTab.ATTACHMENTS
                }
            },
            onCreateConnection = { type ->
                pendingCreateAction = type.toPickerCreateAction()
                activePickerTab =
                    if (type == CreateConnectionType.CONTEXT) {
                        LinkPickerTab.CONTEXTS
                    } else {
                        LinkPickerTab.ATTACHMENTS
                    }
            },
        )
    }

    activePickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions = contextOptions,
            attachmentOptions = pickerAttachmentOptions,
            preselectedContextIds = preselectedContextIds,
            preselectedAttachmentIds = pickerAttachmentOptions.map { it.id }.toSet(),
            initialTab = initialTab,
            initialCreateAction = pendingCreateAction,
            onDismiss = {
                activePickerTab = null
                pendingCreateAction = null
            },
            onContextSelected = { id ->
                viewModel.onPickerContextSelected(id)
                activePickerTab = null
                pendingCreateAction = null
            },
            onAttachmentSelected = { id ->
                viewModel.onPickerAttachmentSelected(id)
                activePickerTab = null
                pendingCreateAction = null
            },
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = { draft -> viewModel.createAttachmentForPicker(draft) },
        )
    }
}

private fun CreateConnectionType.toPickerCreateAction(): PickerCreateAction =
    when (this) {
        CreateConnectionType.CONTEXT -> PickerCreateAction.CONTEXT
        CreateConnectionType.NOTE_DOCUMENT -> PickerCreateAction.NOTE
        CreateConnectionType.CHECKLIST -> PickerCreateAction.CHECKLIST
        CreateConnectionType.EXTERNAL_LINK -> PickerCreateAction.WEB_LINK
        CreateConnectionType.OBSIDIAN_NOTE -> PickerCreateAction.OBSIDIAN
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

private fun BacklogItemContent.toAttachmentOptionOrNull(): AttachmentOption? =
    when (this) {
        is BacklogItemContent.NoteDocumentItem ->
            AttachmentOption(
                id = backlogItem.id,
                name = document.name,
                attachmentType = "NOTE_DOCUMENT",
                entityId = document.id,
            )
        is BacklogItemContent.ChecklistItem ->
            AttachmentOption(
                id = backlogItem.id,
                name = checklist.name,
                attachmentType = "CHECKLIST",
                entityId = checklist.id,
            )
        is BacklogItemContent.LinkItem ->
            AttachmentOption(
                id = backlogItem.id,
                name = link.linkData.displayName?.ifBlank { link.linkData.target } ?: link.linkData.target,
                linkType = link.linkData.type,
                attachmentType = backlogItem.itemType,
                entityId = backlogItem.entityId,
                target = link.linkData.target,
            )
        else -> null
    }

private fun BacklogItemContent.contextTargetIdOrNull(): String? =
    when (this) {
        is BacklogItemContent.LinkItem -> link.linkData.target.takeIf { link.linkData.type == LinkType.CONTEXT }
        else -> null
    }
