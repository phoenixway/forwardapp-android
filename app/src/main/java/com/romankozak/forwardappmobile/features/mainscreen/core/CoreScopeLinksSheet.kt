package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreScopeLinksSheet(
    isVisible: Boolean,
    projectOptions: List<ProjectOption>,
    attachmentOptions: List<ScopeAttachmentOption>,
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
    val validProjectIds = linkedProjectIds.filter { it in availableProjectIds }
    val validAttachmentIds = linkedAttachmentIds.filter { it in availableAttachmentById.keys }

    val items =
        buildList {
            addAll(
                validProjectIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title = projectOptions.firstOrNull { it.id == id }?.name ?: "Контекст ${id.take(8)}",
                        type = ConnectionType.CONTEXT,
                    )
                },
            )
            addAll(
                validAttachmentIds.map { id ->
                    val option = availableAttachmentById[id]
                    val type =
                        when {
                            option?.linkType == LinkType.URL -> ConnectionType.URL
                            option?.linkType == LinkType.OBSIDIAN -> ConnectionType.OBSIDIAN_NOTE
                            option?.attachmentType == "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
                            option?.attachmentType == "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
                            option?.attachmentType == "CHECKLIST" -> ConnectionType.CHECKLIST
                            option?.attachmentType == "SCRIPT" -> ConnectionType.SCRIPT
                            else -> ConnectionType.ATTACHMENT
                        }
                    ConnectionItemUi(
                        id = id,
                        title = option?.name ?: "Вкладення ${id.take(8)}",
                        type = type,
                        vault = option?.vault,
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
            onCreateConnection = onCreateConnectionClick,
            preferActionsBesideTitleWhenWide = true,
            wrapContentHeight = true,
            onConnectionsReordered = onConnectionsReordered,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
