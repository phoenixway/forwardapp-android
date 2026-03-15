package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry

private data class AddProjectDialogFieldsState(
    val text: String,
    val focusRequester: FocusRequester,
    val roleExpanded: Boolean,
    val selectedRole: RoleOption,
    val availableRoles: List<RoleOption>,
)

private data class AddProjectDialogFieldsActions(
    val onTextChange: (String) -> Unit,
    val onRoleExpandedChange: (Boolean) -> Unit,
    val onRoleSelected: (RoleOption) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    title: String,
    roleOptions: List<RoleOption> = emptyList(),
    preferredRoleCode: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val availableRoles = remember(roleOptions) { resolveAvailableRoles(roleOptions) }
    var roleExpanded by remember { mutableStateOf(false) }
    val initialRole =
        remember(availableRoles, preferredRoleCode) {
            resolveInitialRole(availableRoles, preferredRoleCode)
        }
    var selectedRole by remember(availableRoles, preferredRoleCode) { mutableStateOf(initialRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Create context and optionally assign a role",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            AddProjectDialogFields(
                state =
                    AddProjectDialogFieldsState(
                        text = text,
                        focusRequester = focusRequester,
                        roleExpanded = roleExpanded,
                        selectedRole = selectedRole,
                        availableRoles = availableRoles,
                    ),
                actions =
                    AddProjectDialogFieldsActions(
                        onTextChange = { text = it },
                        onRoleExpandedChange = { roleExpanded = it },
                        onRoleSelected = { selectedRole = it },
                    ),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim(), selectedRole.code) },
            ) { Text("Створити") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 4.dp),
            ) { Text("Скасувати") }
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun resolveAvailableRoles(roleOptions: List<RoleOption>): List<RoleOption> {
    val source =
        if (roleOptions.isNotEmpty()) {
            roleOptions
        } else {
            ContextRoleRegistry
                .getReservedBaseRoleDefinitions()
                .map { RoleOption(code = it.code, label = it.label) }
        }
    return listOf(RoleOption(code = null, label = "No role")) + source
}

private fun resolveInitialRole(
    availableRoles: List<RoleOption>,
    preferredRoleCode: String?,
): RoleOption {
    val preferred = preferredRoleCode?.trim()?.takeIf { it.isNotEmpty() } ?: return availableRoles.first()

    return availableRoles.firstOrNull { it.code?.equals(preferred, ignoreCase = true) == true }
        ?: availableRoles.firstOrNull {
            preferred.equals("others", ignoreCase = true) &&
                (
                    it.code?.equals("other", ignoreCase = true) == true ||
                        it.code?.equals("others", ignoreCase = true) == true
                )
        }
        ?: availableRoles.first()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProjectDialogFields(
    state: AddProjectDialogFieldsState,
    actions: AddProjectDialogFieldsActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.text,
            onValueChange = actions.onTextChange,
            label = { Text("Context name") },
            singleLine = true,
            modifier =
                Modifier
                    .focusRequester(state.focusRequester)
                    .fillMaxWidth(),
        )

        RoleSelector(
            expanded = state.roleExpanded,
            onExpandedChange = actions.onRoleExpandedChange,
            selectedRole = state.selectedRole,
            availableRoles = state.availableRoles,
            onRoleSelected = actions.onRoleSelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleSelector(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedRole: RoleOption,
    availableRoles: List<RoleOption>,
    onRoleSelected: (RoleOption) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) },
    ) {
        OutlinedTextField(
            value = selectedRole.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Role") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            availableRoles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.label) },
                    onClick = {
                        onRoleSelected(role)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}
