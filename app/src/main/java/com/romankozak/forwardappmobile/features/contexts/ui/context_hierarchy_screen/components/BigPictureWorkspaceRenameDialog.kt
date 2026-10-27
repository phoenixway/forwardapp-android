package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureWorkspaceRenameState

@Composable
internal fun BigPictureWorkspaceRenameDialog(
    state: BigPictureWorkspaceRenameState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state.target == null) return
    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text("Перейменувати Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Назва зміниться для всіх появ цього робочого простору.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = state.name, onValueChange = onNameChange, singleLine = true,
                    label = { Text("Назва") }, enabled = !state.busy,
                    isError = state.name.isBlank() || state.error != null)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !state.busy && state.name.isNotBlank()) { Text("Зберегти") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Скасувати") } },
    )
}
