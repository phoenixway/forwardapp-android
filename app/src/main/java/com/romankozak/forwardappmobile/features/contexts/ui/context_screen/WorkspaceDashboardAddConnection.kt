package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.features.missions.presentation.*

/** Reuses Connections' picker and commands, with no dashboard-specific writer. */
@Composable
internal fun WorkspaceDashboardAddConnection(viewModel: ContextScreenViewModel) {
    var picking by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { picking = true }) {
        Icon(Icons.Default.Add, "Додати Connection", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
    if (!picking) return
    val contexts by viewModel.allContextsForPicker.collectAsStateWithLifecycle()
    val attachments by viewModel.pickerAttachmentOptions.collectAsStateWithLifecycle()
    val current by viewModel.contextAttachments.collectAsStateWithLifecycle()
    val linked by viewModel.attachmentItems.collectAsStateWithLifecycle()
    LinkedTargetsPickerDialog(
        contextOptions = contexts.distinctBy { it.id }.map { ProjectOption(it.id, it.name) },
        attachmentOptions = attachments,
        preselectedContextIds = linked.filterIsInstance<BacklogItemContent.LinkItem>()
            .mapNotNull { it.link.linkData.target.takeIf { _ -> it.link.linkData.type == LinkType.CONTEXT } }.toSet(),
        preselectedAttachmentIds = current.map { it.attachment.id }.toSet(),
        initialTab = LinkPickerTab.ATTACHMENTS,
        onDismiss = { picking = false },
        onContextSelected = { viewModel.onPickerContextSelected(it); picking = false },
        onAttachmentSelected = { viewModel.onPickerAttachmentSelected(it); picking = false },
        onCreateRootContext = viewModel::createRootContextForPicker,
        onCreateDocument = viewModel::createAttachmentForPicker,
    )
}
