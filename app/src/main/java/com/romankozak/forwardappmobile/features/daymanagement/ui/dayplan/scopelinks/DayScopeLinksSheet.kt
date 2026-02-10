package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.scopelinks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanUiState
import com.romankozak.forwardappmobile.ui.components.ScopeLinkItem
import com.romankozak.forwardappmobile.ui.components.ScreenScopeLinksPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScopeLinksSheet(
    isVisible: Boolean,
    uiState: DayPlanUiState,
    onDismiss: () -> Unit,
    onAddContextClick: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onContextClick: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    onContextRemove: (String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onContextsExpandedChange: (Boolean) -> Unit,
    onAttachmentsExpandedChange: (Boolean) -> Unit,
) {
    if (!isVisible) return

    val availableProjectIds = uiState.availableProjects.map { it.id }.toSet()
    val availableAttachmentIds = uiState.availableAttachments.map { it.id }.toSet()
    val planLinkedProjectIds = uiState.dayPlan?.linkedProjectIds.orEmpty().filter { it in availableProjectIds }
    val planLinkedAttachmentIds = uiState.dayPlan?.linkedAttachmentIds.orEmpty().filter { it in availableAttachmentIds }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        ScreenScopeLinksPanel(
            title = "Посилання для денного циклу",
            contextLinks =
                planLinkedProjectIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = uiState.availableProjects.firstOrNull { it.id == id }?.name ?: "Контекст ${id.take(8)}",
                    )
                },
            attachmentLinks =
                planLinkedAttachmentIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = uiState.availableAttachments.firstOrNull { it.id == id }?.name ?: "Вкладення ${id.take(8)}",
                    )
                },
            onAddContextClick = onAddContextClick,
            onAddAttachmentClick = onAddAttachmentClick,
            onContextClick = onContextClick,
            onAttachmentClick = onAttachmentClick,
            onContextRemove = onContextRemove,
            onAttachmentRemove = onAttachmentRemove,
            contextsExpanded = uiState.scopeContextsExpanded,
            attachmentsExpanded = uiState.scopeAttachmentsExpanded,
            onContextsExpandedChange = onContextsExpandedChange,
            onAttachmentsExpandedChange = onAttachmentsExpandedChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
