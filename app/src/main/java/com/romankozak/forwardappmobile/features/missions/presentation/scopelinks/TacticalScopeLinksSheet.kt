package com.romankozak.forwardappmobile.features.missions.presentation.scopelinks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.ScopeLinkItem
import com.romankozak.forwardappmobile.ui.components.ScreenScopeLinksPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalScopeLinksSheet(
    isVisible: Boolean,
    projectOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    linkedProjectIds: List<String>,
    linkedAttachmentIds: List<String>,
    contextsExpanded: Boolean,
    attachmentsExpanded: Boolean,
    onDismiss: () -> Unit,
    onAddContextClick: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onAddUrlClick: () -> Unit,
    onAddObsidianClick: () -> Unit,
    onContextClick: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    onContextRemove: (String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onContextsExpandedChange: (Boolean) -> Unit,
    onAttachmentsExpandedChange: (Boolean) -> Unit,
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        ScreenScopeLinksPanel(
            title = "Посилання для тактичного циклу",
            contextLinks =
                validLinkedProjectIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = projectOptions.firstOrNull { it.id == id }?.name ?: "Контекст ${id.take(8)}",
                    )
                },
            attachmentLinks =
                validLinkedGeneralAttachmentIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = attachmentOptions.firstOrNull { it.id == id }?.name ?: "Вкладення ${id.take(8)}",
                    )
                },
            urlLinks =
                validLinkedUrlIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = attachmentOptions.firstOrNull { it.id == id }?.name ?: "URL ${id.take(8)}",
                    )
                },
            obsidianLinks =
                validLinkedObsidianIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = attachmentOptions.firstOrNull { it.id == id }?.name ?: "Obsidian ${id.take(8)}",
                    )
                },
            onAddContextClick = onAddContextClick,
            onAddAttachmentClick = onAddAttachmentClick,
            onAddUrlClick = onAddUrlClick,
            onAddObsidianClick = onAddObsidianClick,
            onContextClick = onContextClick,
            onAttachmentClick = onAttachmentClick,
            onContextRemove = onContextRemove,
            onAttachmentRemove = onAttachmentRemove,
            contextsExpanded = contextsExpanded,
            attachmentsExpanded = attachmentsExpanded,
            onContextsExpandedChange = onContextsExpandedChange,
            onAttachmentsExpandedChange = onAttachmentsExpandedChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
