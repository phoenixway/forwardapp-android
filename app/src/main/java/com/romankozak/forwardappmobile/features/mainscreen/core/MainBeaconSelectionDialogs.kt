package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

data class MainBeaconSelectableItem(
    val id: String,
    val label: String,
)

@Composable
fun MainBeaconMultiSelectDialog(
    title: String,
    options: List<MainBeaconSelectableItem>,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var localSelection by remember(selectedIds) { mutableStateOf(selectedIds) }
    val filtered =
        remember(options, query) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                options
            } else {
                options.filter { it.label.contains(normalizedQuery, ignoreCase = true) }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Пошук") },
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = "Нічого не знайдено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            localSelection =
                                                if (item.id in localSelection) {
                                                    localSelection - item.id
                                                } else {
                                                    localSelection + item.id
                                                }
                                        }.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.id in localSelection,
                                    onCheckedChange = { checked ->
                                        localSelection =
                                            if (checked) {
                                                localSelection + item.id
                                            } else {
                                                localSelection - item.id
                                            }
                                    },
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(localSelection) }) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )
}
