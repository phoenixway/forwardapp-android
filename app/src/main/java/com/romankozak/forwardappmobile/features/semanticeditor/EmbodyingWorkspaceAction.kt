package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EmbodyingWorkspaceAction(
    model: SemanticEntityEditorModel, state: EmbodyingWorkspaceState, onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null, deleteLabel: String = "Видалити сутність…", blocked: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(enabled = !state.busy && !blocked, onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "Дії сутності") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Створити embodying workspace") },
            enabled = model.canCreateEmbodyingWorkspace && !state.busy && !blocked,
            onClick = { expanded = false; onOpen() })
        onDelete?.let { delete ->
            DropdownMenuItem(text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
                enabled = !blocked && !state.busy && !model.isSaving && !model.isRelationMutationInFlight,
                onClick = { expanded = false; delete() })
        }
    }
}

@Composable
internal fun EmbodyingWorkspaceDialog(state: EmbodyingWorkspaceState, viewModel: EmbodyingWorkspaceViewModel) {
    val request = state.request ?: return
    AlertDialog(
        onDismissRequest = viewModel::cancel,
        title = { Text("Створити embodying workspace") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Новий робочий простір буде пов’язаний із цією сутністю через EMBODIES. Сутність не змінюється. Поява в Big Picture не створюється.")
                OutlinedTextField(value = request.name, onValueChange = viewModel::setName, enabled = !state.busy,
                    singleLine = true, label = { Text("Назва робочого простору") })
                OutlinedTextField(value = request.description.orEmpty(), onValueChange = viewModel::setDescription, enabled = !state.busy,
                    minLines = 3, maxLines = 8, label = { Text("Опис (Markdown)") })
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !state.busy && request.name.isNotBlank(), onClick = viewModel::confirm) {
            if (state.busy) CircularProgressIndicator() else Text("Створити й відкрити")
        } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = viewModel::cancel) { Text("Скасувати") } },
    )
}
