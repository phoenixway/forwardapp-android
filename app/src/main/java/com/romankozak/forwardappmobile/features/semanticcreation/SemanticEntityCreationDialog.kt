package com.romankozak.forwardappmobile.features.semanticcreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semantic.creatableOrientationKinds
import com.romankozak.forwardappmobile.features.semantic.displayLabel
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

@Composable
fun SemanticEntityCreationDialog(
    state: SemanticEntityCreationUiState,
    onNameChange: (String) -> Unit,
    onEntityClassChange: (SemanticEntityClass) -> Unit,
    onOrientationKindChange: (OrientationKind) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text("Нова семантична сутність") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Назва") },
                    singleLine = true,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                SemanticEntityTypeFields(
                    selectedClass = state.entityClass,
                    selectedKind = state.orientationKind,
                    enabled = !state.isSaving,
                    onClassChange = onEntityClassChange,
                    onKindChange = onOrientationKindChange,
                )
                state.errorMessage?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = state.canCreate, onClick = onCreate) {
                if (state.isSaving) {
                    CircularProgressIndicator()
                } else {
                    Text("Створити")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !state.isSaving, onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )
}

@Composable
fun SemanticEntityTypeFields(
    selectedClass: SemanticEntityClass?,
    selectedKind: OrientationKind?,
    enabled: Boolean,
    onClassChange: (SemanticEntityClass) -> Unit,
    onKindChange: (OrientationKind) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Сутність")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SemanticEntityClass.entries.forEach { entityClass ->
                FilterChip(
                    selected = selectedClass == entityClass,
                    onClick = { onClassChange(entityClass) },
                    enabled = enabled,
                    label = { Text(entityClass.displayLabel()) },
                )
            }
        }
        if (selectedClass == SemanticEntityClass.ORIENTATION) {
            OrientationKindSelector(selectedKind, enabled, onKindChange)
        }
    }
}

@Composable
private fun OrientationKindSelector(
    selected: OrientationKind?,
    enabled: Boolean,
    onSelected: (OrientationKind) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Тип орієнтира")
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.displayLabel() ?: "Оберіть тип")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            creatableOrientationKinds.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(kind.displayLabel()) },
                    onClick = {
                        expanded = false
                        onSelected(kind)
                    },
                )
            }
        }
    }
}
