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
    onConvert: (() -> Unit)? = null,
    onChangeKind: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(enabled = !state.busy && !blocked, onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "Дії сутності") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        onChangeKind?.let { change ->
            DropdownMenuItem(text = { Text("Змінити тип орієнтира…") },
                enabled = !blocked && !state.busy && !model.isDirty && !model.isSaving && !model.isRelationMutationInFlight,
                onClick = { expanded = false; change() })
        }
        onConvert?.let { convert ->
            DropdownMenuItem(text = { Text("Перетворити на орієнтир…") },
                enabled = !blocked && !state.busy && !model.isDirty && !model.isSaving && !model.isRelationMutationInFlight,
                onClick = { expanded = false; convert() })
        }
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
    EmbodyingWorkspaceDialogContent(state, viewModel::setName, viewModel::setDescription, viewModel::confirm, viewModel::cancel)
}

@Composable
internal fun EmbodyingWorkspaceDialogContent(
    state: EmbodyingWorkspaceState,
    onName: (String) -> Unit,
    onDescription: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmLabel: String = "Створити й відкрити",
) {
    val request = state.request ?: return
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Створити embodying workspace") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Новий робочий простір буде пов’язаний із цією сутністю через EMBODIES. Сутність не змінюється. Поява в Navigation не створюється.")
                OutlinedTextField(value = request.name, onValueChange = onName, enabled = !state.busy,
                    singleLine = true, label = { Text("Назва робочого простору") })
                OutlinedTextField(value = request.description.orEmpty(), onValueChange = onDescription, enabled = !state.busy,
                    minLines = 3, maxLines = 8, label = { Text("Опис (Markdown)") })
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !state.busy && request.name.isNotBlank(), onClick = onConfirm) {
            if (state.busy) CircularProgressIndicator() else Text(confirmLabel)
        } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = onCancel) { Text("Скасувати") } },
    )
}
