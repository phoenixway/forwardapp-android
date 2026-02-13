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
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScopeLinksSheet(
    isVisible: Boolean,
    uiState: DayPlanUiState,
    onDismiss: () -> Unit,
    onAddContextClick: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onAddExternalClick: () -> Unit,
    onAddObsidianClick: () -> Unit,
    onContextClick: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    onContextRemove: (String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    connectionOrder: List<String>,
    onConnectionsReordered: (List<ConnectionItemUi>) -> Unit,
) {
    if (!isVisible) return

    val availableProjectIds = uiState.availableProjects.map { it.id }.toSet()
    val availableAttachmentById = uiState.availableAttachments.associateBy { it.id }
    val availableAttachmentIds = availableAttachmentById.keys
    val planLinkedProjectIds = uiState.dayPlan?.linkedProjectIds.orEmpty().filter { it in availableProjectIds }
    val planLinkedAttachmentIds = uiState.dayPlan?.linkedAttachmentIds.orEmpty().filter { it in availableAttachmentIds }
    val planLinkedUrlIds =
        planLinkedAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType == LinkType.URL
        }
    val planLinkedObsidianIds =
        planLinkedAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType == LinkType.OBSIDIAN
        }
    val planLinkedGeneralAttachmentIds =
        planLinkedAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType !in setOf(LinkType.URL, LinkType.OBSIDIAN)
        }
    val items =
        buildList {
            addAll(
                planLinkedProjectIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = uiState.availableProjects.firstOrNull { it.id == id }?.name ?: "Контекст ${id.take(8)}",
                        type = ConnectionType.CONTEXT,
                    )
                },
            )
            addAll(
                planLinkedGeneralAttachmentIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = uiState.availableAttachments.firstOrNull { it.id == id }?.name ?: "Вкладення ${id.take(8)}",
                        type = ConnectionType.ATTACHMENT,
                    )
                },
            )
            addAll(
                planLinkedUrlIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = uiState.availableAttachments.firstOrNull { it.id == id }?.name ?: "URL ${id.take(8)}",
                        type = ConnectionType.URL,
                    )
                },
            )
            addAll(
                planLinkedObsidianIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = uiState.availableAttachments.firstOrNull { it.id == id }?.name ?: "Obsidian ${id.take(8)}",
                        type = ConnectionType.OBSIDIAN_NOTE,
                    )
                },
            )
        }
    val sortedItems = sortConnectionsByOrder(items, connectionOrder)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        ConnectionsPanel(
            items = sortedItems,
            onConnectionClick = { item ->
                if (item.type == ConnectionType.CONTEXT) {
                    onContextClick(item.id)
                } else {
                    onAttachmentClick(item.id)
                }
            },
            onConnectionRemove = { item ->
                if (item.type == ConnectionType.CONTEXT) {
                    onContextRemove(item.id)
                } else {
                    onAttachmentRemove(item.id)
                }
            },
            onAddConnection = { type ->
                when (type) {
                    AddConnectionType.CONTEXT -> onAddContextClick()
                    AddConnectionType.ATTACHMENT -> onAddAttachmentClick()
                    AddConnectionType.EXTERNAL_LINK -> onAddExternalClick()
                    AddConnectionType.OBSIDIAN_NOTE -> onAddObsidianClick()
                }
            },
            onCreateConnection = { type ->
                when (type) {
                    CreateConnectionType.CONTEXT -> onAddContextClick()
                    CreateConnectionType.NOTE_DOCUMENT -> onAddAttachmentClick()
                    CreateConnectionType.CHECKLIST -> onAddAttachmentClick()
                    CreateConnectionType.EXTERNAL_LINK -> onAddExternalClick()
                    CreateConnectionType.OBSIDIAN_NOTE -> onAddObsidianClick()
                }
            },
            onConnectionsReordered = onConnectionsReordered,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
