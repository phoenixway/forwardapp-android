package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.connections

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
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionPanelMode
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType

@Composable
fun ConnectionsView(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    attachmentItems: List<BacklogItemContent>,
) {
    var activePickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    val groupedContexts by viewModel.subprojectChildren.collectAsState()
    val pickerAttachmentOptions by viewModel.pickerAttachmentOptions.collectAsState()
    val contextAttachments by viewModel.contextAttachments.collectAsState()

    val attachments =
        attachmentItems.filter {
            it is BacklogItemContent.LinkItem ||
                it is BacklogItemContent.NoteDocumentItem ||
                it is BacklogItemContent.MusicNoteItem ||
                it is BacklogItemContent.ChecklistItem
        }

    val scriptAttachmentById =
        remember(contextAttachments) {
            contextAttachments
                .map { it.attachment }
                .filter { it.attachmentType == BacklogItemTypeValues.SCRIPT }
                .associateBy { it.id }
        }
    val attachmentNameById = remember(pickerAttachmentOptions) { pickerAttachmentOptions.associateBy({ it.id }, { it.name }) }
    val scriptItems =
        remember(scriptAttachmentById, attachmentNameById) {
            scriptAttachmentById.values.map { attachment ->
                val title = attachmentNameById[attachment.id] ?: "Script ${attachment.id.takeLast(4)}"
                ConnectionItemUi(
                    id = connectionIdForAttachment(attachment.id),
                    title = title,
                    type = ConnectionType.SCRIPT,
                )
            }
        }
    val items =
        remember(attachments, scriptItems) {
            val baseItems =
                attachments.mapNotNull { item ->
                    val id = item.connectionId()
                    val title = item.connectionTitle()
                    val type = item.connectionType()
                    if (title.isBlank()) null else ConnectionItemUi(id = id, title = title, type = type)
                }
            val baseIds = baseItems.map { it.id }.toSet()
            baseItems + scriptItems.filterNot { it.id in baseIds }
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
    val preselectedAttachmentIds = remember(contextAttachments) { contextAttachments.map { it.attachment.id }.toSet() }
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
            mode = ConnectionPanelMode.NORMAL,
            onConnectionClick = { item ->
                val directItem = attachmentByConnectionId[item.id]
                if (directItem != null) {
                    viewModel.itemActionHandler.onItemClick(directItem)
                } else {
                    val attachmentId = attachmentIdFromConnectionId(item.id) ?: return@ConnectionsPanel
                    val scriptAttachment = scriptAttachmentById[attachmentId] ?: return@ConnectionsPanel
                    viewModel.openScriptAttachment(scriptAttachment.entityId)
                }
            },
            onConnectionRemove = { item ->
                val directItem = attachmentByConnectionId[item.id]
                if (directItem != null) {
                    viewModel.onDeleteEverywhere(directItem)
                } else {
                    val attachmentId = attachmentIdFromConnectionId(item.id) ?: return@ConnectionsPanel
                    viewModel.deleteAttachmentEverywhereById(attachmentId)
                }
            },
            onConnectionCopy = { item ->
                val directItem = attachmentByConnectionId[item.id]
                if (directItem != null) {
                    viewModel.itemActionHandler.copyAttachmentItem(directItem)
                } else {
                    val attachmentId = attachmentIdFromConnectionId(item.id) ?: return@ConnectionsPanel
                    viewModel.itemActionHandler.copyAttachmentById(attachmentId)
                }
            },
            onConnectionCut = { item ->
                val directItem = attachmentByConnectionId[item.id]
                if (directItem != null) {
                    viewModel.itemActionHandler.cutAttachmentItem(directItem)
                } else {
                    val attachmentId = attachmentIdFromConnectionId(item.id) ?: return@ConnectionsPanel
                    viewModel.itemActionHandler.cutAttachmentById(attachmentId)
                }
            },
            onAddButtonClick = {
                pendingCreateAction = null
                activePickerTab = LinkPickerTab.CONTEXTS
            },
            onAddConnection = { _ ->
                pendingCreateAction = null
                activePickerTab = LinkPickerTab.CONTEXTS
            },
            onCreateConnection = { type ->
                if (type == CreateConnectionType.SCRIPT) {
                    activePickerTab = null
                    pendingCreateAction = null
                    viewModel.openScriptEditorForCurrentContext()
                    return@ConnectionsPanel
                }
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
            preselectedAttachmentIds = preselectedAttachmentIds,
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
        CreateConnectionType.MUSIC_NOTE -> PickerCreateAction.MUSIC_NOTE
        CreateConnectionType.CHECKLIST -> PickerCreateAction.CHECKLIST
        CreateConnectionType.SCRIPT -> PickerCreateAction.NOTE
        CreateConnectionType.EXTERNAL_LINK -> PickerCreateAction.WEB_LINK
        CreateConnectionType.OBSIDIAN_NOTE -> PickerCreateAction.OBSIDIAN
    }

private fun BacklogItemContent.connectionId(): String = "backlog:${backlogItem.id}"
private fun connectionIdForAttachment(attachmentId: String): String = "backlog:$attachmentId"
private fun attachmentIdFromConnectionId(connectionId: String): String? =
    connectionId.takeIf { it.startsWith("backlog:") }?.removePrefix("backlog:")

private fun BacklogItemContent.connectionTitle(): String =
    when (this) {
        is BacklogItemContent.LinkItem -> link.linkData.displayName?.ifBlank { link.linkData.target } ?: link.linkData.target
        is BacklogItemContent.NoteDocumentItem -> document.name
        is BacklogItemContent.MusicNoteItem -> musicNote.name
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
        is BacklogItemContent.NoteDocumentItem -> ConnectionType.NOTE_DOCUMENT
        is BacklogItemContent.MusicNoteItem -> ConnectionType.MUSIC_NOTE
        is BacklogItemContent.ChecklistItem -> ConnectionType.CHECKLIST
        else -> ConnectionType.ATTACHMENT
    }

private fun BacklogItemContent.contextTargetIdOrNull(): String? =
    when (this) {
        is BacklogItemContent.LinkItem -> link.linkData.target.takeIf { link.linkData.type == LinkType.CONTEXT }
        else -> null
    }
