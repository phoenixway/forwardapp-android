package com.romankozak.forwardappmobile.ui.screens.backlog.components.projectrealization

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
import com.romankozak.forwardappmobile.data.database.models.ProjectStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateStatusDialog(
    currentStatus: ProjectStatus,
    currentStatusText: String,
    onDismissRequest: () -> Unit,
    onSave: (ProjectStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var statusText by remember { mutableStateOf(currentStatusText) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Оновити статус проекту") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dropdown для вибору статусу
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Статус") },
                        trailingIcon = {
                            val rotation by animateFloatAsState(targetValue = if (isDropdownExpanded) 180f else 0f, label = "dropdownIconRotation")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Вибрати статус",
                                Modifier.rotate(rotation)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        ProjectStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayName) },
                                onClick = {
                                    selectedStatus = status
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Текстове поле для опису
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("Якісний опис (опційно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedStatus, statusText.takeIf { it.isNotBlank() })
                }
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Скасувати")
            }
        }
    )
}
