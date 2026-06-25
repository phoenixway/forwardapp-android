package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
internal fun StrategicArcContextPickerHost(
    visible: Boolean,
    contextOptions: List<ProjectOption>,
    preselectedContextIds: Set<String>,
    onDismiss: () -> Unit,
    onContextSelected: (String) -> Unit,
    onCreateRootContext: (suspend (String) -> String?)?,
) {
    if (!visible) return

    LinkedTargetsPickerDialog(
        contextOptions = contextOptions,
        attachmentOptions = emptyList<AttachmentOption>(),
        preselectedContextIds = preselectedContextIds,
        preselectedAttachmentIds = emptySet(),
        initialTab = LinkPickerTab.CONTEXTS,
        allowedTabs = setOf(LinkPickerTab.CONTEXTS),
        onDismiss = onDismiss,
        onContextSelected = onContextSelected,
        onAttachmentSelected = {},
        onCreateRootContext = onCreateRootContext,
        onCreateDocument = null,
    )
}
