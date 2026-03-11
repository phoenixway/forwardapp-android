package com.romankozak.forwardappmobile.features.missions.presentation.scopelinks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalScopeLinksSheet(
    isVisible: Boolean,
    projectOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    linkedProjectIds: List<String>,
    linkedAttachmentIds: List<String>,
    onDismiss: () -> Unit,
    onAddContextClick: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onAddExternalClick: () -> Unit,
    onAddObsidianClick: () -> Unit,
    onCreateConnectionClick: (CreateConnectionType) -> Unit,
    onContextClick: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    onContextRemove: (String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    connectionOrder: List<String>,
    onConnectionsReordered: (List<ConnectionItemUi>) -> Unit,
) {
    if (!isVisible) return

    val availableProjectIds = projectOptions.map { it.id }.toSet()
    val availableAttachmentById = attachmentOptions.associateBy { it.id }
    val availableAttachmentIds = availableAttachmentById.keys
    val validLinkedProjectIds = linkedProjectIds.filter { it in availableProjectIds }
    val validLinkedAttachmentIds = linkedAttachmentIds.filter { it in availableAttachmentIds }
    val validLinkedUrlIds =
        validLinkedAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType == LinkType.URL
        }
    val validLinkedObsidianIds =
        validLinkedAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType == LinkType.OBSIDIAN
        }
    val validLinkedGeneralAttachmentIds =
        validLinkedAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType !in setOf(LinkType.URL, LinkType.OBSIDIAN)
        }
    val items =
        buildList {
            addAll(
                validLinkedProjectIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = projectOptions.firstOrNull { it.id == id }?.name ?: "Контекст ${id.take(8)}",
                        type = ConnectionType.CONTEXT,
                    )
                },
            )
            addAll(
                validLinkedGeneralAttachmentIds.map { id ->
                    val option = attachmentOptions.firstOrNull { it.id == id }
                    ConnectionItemUi(
                        id = id,
                        title = option?.name ?: "Вкладення ${id.take(8)}",
                        type =
                            when (option?.attachmentType) {
                                "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
                                "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
                                "CHECKLIST" -> ConnectionType.CHECKLIST
                                "SCRIPT" -> ConnectionType.SCRIPT
                                else -> ConnectionType.ATTACHMENT
                            },
                    )
                },
            )
            addAll(
                validLinkedUrlIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = attachmentOptions.firstOrNull { it.id == id }?.name ?: "URL ${id.take(8)}",
                        type = ConnectionType.URL,
                    )
                },
            )
            addAll(
                validLinkedObsidianIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = attachmentOptions.firstOrNull { it.id == id }?.name ?: "Obsidian ${id.take(8)}",
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
                onCreateConnectionClick(type)
            },
            preferActionsBesideTitleWhenWide = true,
            onConnectionsReordered = onConnectionsReordered,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
