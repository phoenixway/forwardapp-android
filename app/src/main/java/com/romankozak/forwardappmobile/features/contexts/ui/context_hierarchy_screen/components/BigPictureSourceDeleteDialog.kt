package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureSourceDeleteState

@Composable
internal fun BigPictureSourceDeleteDialog(state: BigPictureSourceDeleteState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val target = state.pending ?: return
    AlertDialog(
        onDismissRequest = { if (!state.busy) onCancel() },
        title = { Text(target.confirmationTitle) },
        text = { Column { Text(target.confirmationText); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = !state.busy, onClick = onConfirm) {
            if (state.busy) CircularProgressIndicator() else Text("Видалити джерело", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = onCancel) { Text("Скасувати") } },
    )
}
