package com.romankozak.forwardappmobile.features.activitytracker.entities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType

@Composable
fun ActivityEntityLinksEditor(
    selectedLinks: List<ActivityEntityLink>,
    options: List<ActivityEntityDescriptor>,
    onLinksChanged: (List<ActivityEntityLink>) -> Unit,
) {
    var pickerVisible by remember { mutableStateOf(false) }
    val descriptors = remember(options) { options.associateBy { it.link.identityKey() } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Пов’язані сутності")
        if (selectedLinks.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedLinks.forEach { link ->
                    val descriptor = descriptors[link.identityKey()]
                    FilterChip(
                        selected = true,
                        onClick = { onLinksChanged(selectedLinks.filterNot { it.identityKey() == link.identityKey() }) },
                        label = { Text(descriptor?.title ?: link.entityType.displayName(), maxLines = 1) },
                    )
                }
            }
        }
        OutlinedButton(onClick = { pickerVisible = true }) {
            Text(if (selectedLinks.isEmpty()) "Додати зв’язки" else "Змінити зв’язки")
        }
    }

    if (pickerVisible) {
        ActivityEntityLinkPicker(
            selectedLinks = selectedLinks,
            options = options,
            onLinksChanged = onLinksChanged,
            onDismiss = { pickerVisible = false },
        )
    }
}

@Composable
private fun ActivityEntityLinkPicker(
    selectedLinks: List<ActivityEntityLink>,
    options: List<ActivityEntityDescriptor>,
    onLinksChanged: (List<ActivityEntityLink>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val selectedKeys = selectedLinks.mapTo(mutableSetOf()) { it.identityKey() }
    val filtered =
        remember(options, query) {
            options.filter { option ->
                query.isBlank() || option.title.contains(query, ignoreCase = true) ||
                    option.typeLabel.contains(query, ignoreCase = true)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Зв’язки запису") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Пошук") },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(filtered, key = { "${it.link.entityType}:${it.link.entityId}" }) { option ->
                        val key = option.link.identityKey()
                        val selected = key in selectedKeys
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = {
                                    val next =
                                        if (selected) {
                                            selectedLinks.filterNot { link -> link.identityKey() == key }
                                        } else {
                                            selectedLinks + option.link
                                        }
                                    onLinksChanged(next)
                                },
                            )
                            Column {
                                Text(option.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(option.typeLabel)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

fun ActivityEntityLink.identityKey(): Pair<ActivityEntityType, String> = entityType to entityId
