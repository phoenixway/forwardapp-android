package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
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

data class RoleOption(
    val code: String?,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    title: String,
    roleOptions: List<RoleOption> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val availableRoles =
        remember(roleOptions) {
            val source =
                if (roleOptions.isNotEmpty()) {
                    roleOptions
                } else {
                    ContextRoleRegistry
                        .getReservedBaseRoleDefinitions()
                        .map { RoleOption(code = it.code, label = it.label) }
                }
            listOf(RoleOption(code = null, label = "No role")) + source
        }
    var roleExpanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(availableRoles.first()) }

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Context name") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedRole.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false },
                    ) {
                        availableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                },
                            )
                        }
                    }
                }
            }
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
