package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
internal fun TacticsContextPickerHost(
    visible: Boolean,
    projectOptions: List<ProjectOption>,
    onDismiss: () -> Unit,
    onContextSelected: (String) -> Unit,
) {
    if (!visible) return

    LinkedTargetsPickerDialog(
        contextOptions = projectOptions,
        attachmentOptions = emptyList<AttachmentOption>(),
        preselectedContextIds = emptySet(),
        preselectedAttachmentIds = emptySet(),
        initialTab = LinkPickerTab.CONTEXTS,
        allowedTabs = setOf(LinkPickerTab.CONTEXTS),
        onDismiss = onDismiss,
        onContextSelected = onContextSelected,
        onAttachmentSelected = {},
        onCreateRootContext = null,
        onCreateDocument = null,
    )
}
