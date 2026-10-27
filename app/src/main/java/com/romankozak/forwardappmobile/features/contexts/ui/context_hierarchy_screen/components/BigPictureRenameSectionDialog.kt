package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal fun BigPictureRenameSectionDialog(
    containerId: String,
    initialTitle: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var title by rememberSaveable(containerId) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейменувати групу / секцію") },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true,
                enabled = enabled, label = { Text("Назва") }, isError = title.isBlank())
        },
        confirmButton = { TextButton(enabled = enabled && title.isNotBlank(), onClick = { onRename(title.trim()) }) {
            Text("Зберегти")
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
