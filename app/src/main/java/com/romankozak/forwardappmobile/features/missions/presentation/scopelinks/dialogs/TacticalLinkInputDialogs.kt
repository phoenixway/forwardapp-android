package com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.dialogs

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog

@Composable
fun TacticalAddUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String) -> Unit,
) {
    AddWebLinkDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun TacticalAddObsidianDialog(
    onDismiss: () -> Unit,
    onConfirm: (noteName: String, displayName: String, vault: String) -> Unit,
) {
    AddObsidianLinkDialog(
        onDismiss = onDismiss,
        onConfirm = { url, name, vault -> onConfirm(url, name, vault) },
    )
}
