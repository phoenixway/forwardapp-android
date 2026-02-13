package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.runtime.Composable

@Composable
fun AttachmentChooserScreen(
    options: List<AttachmentOption>,
    preselected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    LinkedTargetsPickerDialog(
        contextOptions = emptyList(),
        attachmentOptions = options,
        preselectedContextIds = emptySet(),
        preselectedAttachmentIds = preselected,
        initialTab = LinkPickerTab.ATTACHMENTS,
        onDismiss = onDismiss,
        onContextSelected = {},
        onAttachmentSelected = { id -> onConfirm(listOf(id)) },
    )
}
