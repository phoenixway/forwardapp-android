package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues

private data class UpdateStatusDialogContentState(
    val selectedStatus: String,
    val statusText: String,
    val isDropdownExpanded: Boolean,
    val statuses: List<String>,
)

private data class UpdateStatusDialogActions(
    val onExpandedChange: (Boolean) -> Unit,
    val onDismissDropdown: () -> Unit,
    val onStatusSelected: (String) -> Unit,
    val onStatusTextChange: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateStatusDialog(
    currentStatus: String,
    currentStatusText: String,
    onDismissRequest: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var statusText by remember { mutableStateOf(currentStatusText) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val statuses =
        listOf(
            ContextStatusValues.NO_PLAN,
            ContextStatusValues.PLANNING,
            ContextStatusValues.IN_PROGRESS,
            ContextStatusValues.ON_HOLD,
            ContextStatusValues.PAUSED,
            ContextStatusValues.COMPLETED,
        )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Оновити статус проекту") },
        text = {
            UpdateStatusDialogContent(
                state =
                    UpdateStatusDialogContentState(
                        selectedStatus = selectedStatus,
                        statusText = statusText,
                        isDropdownExpanded = isDropdownExpanded,
                        statuses = statuses,
                    ),
                actions =
                    UpdateStatusDialogActions(
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                        onDismissDropdown = { isDropdownExpanded = false },
                        onStatusSelected = { status ->
                            selectedStatus = status
                            isDropdownExpanded = false
                        },
                        onStatusTextChange = { statusText = it },
                    ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedStatus, statusText.takeIf { it.isNotBlank() })
                },
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Скасувати")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateStatusDialogContent(
    state: UpdateStatusDialogContentState,
    actions: UpdateStatusDialogActions,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusDropdownField(
            state = state,
            actions = actions,
        )
        StatusTextField(
            statusText = state.statusText,
            onStatusTextChange = actions.onStatusTextChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdownField(
    state: UpdateStatusDialogContentState,
    actions: UpdateStatusDialogActions,
) {
    ExposedDropdownMenuBox(
        expanded = state.isDropdownExpanded,
        onExpandedChange = actions.onExpandedChange,
    ) {
        OutlinedTextField(
            value = ContextStatusValues.getDisplayName(state.selectedStatus),
            onValueChange = {},
            readOnly = true,
            label = { Text("Статус") },
            trailingIcon = {
                val rotation by animateFloatAsState(
                    targetValue = if (state.isDropdownExpanded) 180f else 0f,
                    label = "dropdownIconRotation",
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Вибрати статус",
                    Modifier.rotate(rotation),
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = state.isDropdownExpanded,
            onDismissRequest = actions.onDismissDropdown,
        ) {
            state.statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(ContextStatusValues.getDisplayName(status)) },
                    onClick = { actions.onStatusSelected(status) },
                )
            }
        }
    }
}

@Composable
private fun StatusTextField(
    statusText: String,
    onStatusTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = statusText,
        onValueChange = onStatusTextChange,
        label = { Text("Якісний опис (опційно)") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 5,
    )
}
