package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.semanticcreation.SemanticEntityTypeFields
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

internal const val WORKSPACE_SEMANTIC_BOOTSTRAP_ACTION = "Створити семантичну сутність"

@Composable
internal fun WorkspaceSemanticBootstrapDialog(
    state: WorkspaceSemanticBootstrapState,
    model: WorkspaceSemanticBootstrapViewModel,
) {
    var bindingMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = model::cancel,
        title = { Text("Семантична сутність на основі Workspace") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Буде створено нову сутність і явний зв’язок із цим робочим простором. Сам Workspace не зміниться.")
                SemanticEntityTypeFields(state.entityClass, state.kind, !state.saving, model::chooseClass, model::chooseKind)
                OutlinedTextField(
                    value = state.title, onValueChange = { model.edit(title = it) }, label = { Text("Назва") },
                    enabled = !state.saving, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.description, onValueChange = { model.edit(description = it) }, label = { Text("Опис (Markdown)") },
                    enabled = !state.saving, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    OutlinedButton(onClick = { bindingMenu = true }, enabled = !state.saving) {
                        Text(state.bindingType?.bootstrapLabel() ?: "Оберіть тип зв’язку з Workspace")
                    }
                    DropdownMenu(expanded = bindingMenu, onDismissRequest = { bindingMenu = false }) {
                        WorkspaceBindingType.entries.forEach { type ->
                            DropdownMenuItem(text = { Text(type.bootstrapLabel()) }, onClick = {
                                bindingMenu = false
                                model.chooseBinding(type)
                            })
                        }
                    }
                }
                state.error?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = state.canSubmit, onClick = model::submit) {
                if (state.saving) CircularProgressIndicator() else Text("Створити й пов’язати")
            }
        },
        dismissButton = { TextButton(enabled = !state.saving, onClick = model::cancel) { Text("Скасувати") } },
    )
}

private fun WorkspaceBindingType.bootstrapLabel() = when (this) {
    WorkspaceBindingType.EMBODIES -> "EMBODIES — основний робочий простір"
    WorkspaceBindingType.REALIZES -> "REALIZES — безпосередня реалізація"
    WorkspaceBindingType.SUPPORTS -> "SUPPORTS — підтримка"
    WorkspaceBindingType.MONITORS -> "MONITORS — спостереження / огляд"
}
