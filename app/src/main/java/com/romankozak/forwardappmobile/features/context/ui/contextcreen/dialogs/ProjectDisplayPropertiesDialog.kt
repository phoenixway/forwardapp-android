package com.romankozak.forwardappmobile.features.context.ui.contextcreen.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProjectDisplayPropertiesDialog(
    isProjectManagementEnabled: Boolean,
    onToggleProjectManagement: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showCheckboxes by remember { mutableStateOf(true) }
    var isAdvancedModeEnabled by remember { mutableStateOf(isProjectManagementEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Властивості відображення") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Display", modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Просунутий режим проєкту")
                    Switch(
                        checked = isAdvancedModeEnabled,
                        onCheckedChange = { isAdvancedModeEnabled = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Показувати чекбокси")
                    Switch(
                        checked = showCheckboxes,
                        onCheckedChange = { showCheckboxes = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onToggleProjectManagement(isAdvancedModeEnabled)
                onDismiss()
            }) {
                Text("Готово")
            }
        }
    )
}
