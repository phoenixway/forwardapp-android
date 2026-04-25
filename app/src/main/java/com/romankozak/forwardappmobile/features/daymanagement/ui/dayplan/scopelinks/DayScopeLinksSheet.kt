package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.scopelinks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.LinkOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder

private const val ID_PREVIEW_LENGTH = 8

data class DayScopeLinksActions(
    val onDismiss: () -> Unit,
    val onAddContextClick: () -> Unit,
    val onAddAttachmentClick: () -> Unit,
    val onAddExternalClick: () -> Unit,
    val onAddObsidianClick: () -> Unit,
    val onCreateConnectionClick: (CreateConnectionType) -> Unit,
    val onContextClick: (String) -> Unit,
    val onAttachmentClick: (String) -> Unit,
    val onContextRemove: (String) -> Unit,
    val onAttachmentRemove: (String) -> Unit,
    val onConnectionsReordered: (List<ConnectionItemUi>) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScopeLinksSheet(
    isVisible: Boolean,
    uiState: DayPlanUiState,
    actions: DayScopeLinksActions,
    connectionOrder: List<String>,
) {
    if (!isVisible) return

    val items = buildConnectionItems(uiState)
    val sortedItems = sortConnectionsByOrder(items, connectionOrder)

    ModalBottomSheet(onDismissRequest = actions.onDismiss) {
        ConnectionsPanel(
            items = sortedItems,
            onConnectionClick = { item ->
                if (item.type == ConnectionType.CONTEXT) {
                    actions.onContextClick(item.id)
                } else {
                    actions.onAttachmentClick(item.id)
                }
            },
            onConnectionRemove = { item ->
                if (item.type == ConnectionType.CONTEXT) {
                    actions.onContextRemove(item.id)
                } else {
                    actions.onAttachmentRemove(item.id)
                }
            },
            onAddConnection = { type ->
                when (type) {
                    AddConnectionType.CONTEXT -> actions.onAddContextClick()
                    AddConnectionType.ATTACHMENT -> actions.onAddAttachmentClick()
                    AddConnectionType.EXTERNAL_LINK -> actions.onAddExternalClick()
                    AddConnectionType.OBSIDIAN_NOTE -> actions.onAddObsidianClick()
                }
            },
            onAddButtonClick = actions.onAddContextClick,
            onCreateConnection = actions.onCreateConnectionClick,
            preferActionsBesideTitleWhenWide = true,
            wrapContentHeight = true,
            onConnectionsReordered = actions.onConnectionsReordered,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun buildConnectionItems(uiState: DayPlanUiState): List<ConnectionItemUi> {
    val availableProjectById = uiState.availableProjects.associateBy { it.id }
    val availableAttachmentById = uiState.availableAttachments.associateBy { it.id }
    val availableProjectIds = availableProjectById.keys
    val availableAttachmentIds = availableAttachmentById.keys
    val planLinkedProjectIds = uiState.todayScopeLinkedProjectIds.filter { it in availableProjectIds }
    val planLinkedAttachmentIds =
        uiState.todayScopeLinkedAttachmentIds.filter { it in availableAttachmentIds }

    return buildList {
        addAll(planLinkedProjectIds.map { id -> createContextItem(id, availableProjectById[id]?.name) })
        addAll(
            planLinkedAttachmentIds.mapNotNull { id ->
                createAttachmentItem(id, availableAttachmentById[id])
            },
        )
    }
}

private fun createContextItem(id: String, title: String?): ConnectionItemUi =
    ConnectionItemUi(
        id = id,
        title = title ?: "Контекст ${id.take(ID_PREVIEW_LENGTH)}",
        type = ConnectionType.CONTEXT,
    )

private fun createAttachmentItem(
    id: String,
    option: LinkOption?,
): ConnectionItemUi? {
    val type = option.toConnectionType() ?: return null
    val defaultTitle =
        when (type) {
            ConnectionType.URL -> "URL ${id.take(ID_PREVIEW_LENGTH)}"
            ConnectionType.OBSIDIAN_NOTE -> "Obsidian ${id.take(ID_PREVIEW_LENGTH)}"
            else -> "Вкладення ${id.take(ID_PREVIEW_LENGTH)}"
        }

    return ConnectionItemUi(
        id = id,
        title = option?.name ?: defaultTitle,
        type = type,
        vault = option?.vault,
    )
}

private fun LinkOption?.toConnectionType(): ConnectionType? =
    when {
        this == null -> null
        linkType == LinkType.URL -> ConnectionType.URL
        linkType == LinkType.OBSIDIAN -> ConnectionType.OBSIDIAN_NOTE
        attachmentType == "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
        attachmentType == "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
        attachmentType == "CHECKLIST" -> ConnectionType.CHECKLIST
        attachmentType == "SCRIPT" -> ConnectionType.SCRIPT
        else -> ConnectionType.ATTACHMENT
    }
