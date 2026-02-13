package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.runtime.Composable

@Composable
fun ProjectChooserScreen(
    options: List<ProjectOption>,
    preselected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    LinkedTargetsPickerDialog(
        contextOptions = options,
        attachmentOptions = emptyList(),
        preselectedContextIds = preselected,
        preselectedAttachmentIds = emptySet(),
        initialTab = LinkPickerTab.CONTEXTS,
        onDismiss = onDismiss,
        onContextSelected = { id -> onConfirm(listOf(id)) },
        onAttachmentSelected = {},
    )
}
