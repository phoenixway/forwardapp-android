package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations

@Composable
internal fun ArcQuestBeaconPickerDialog(
    beacons: List<MainBeaconWithRelations>,
    usedSourceIds: Set<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredBeacons =
        remember(beacons, query) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                beacons
            } else {
                beacons.filter { item ->
                    item.beacon.title.contains(normalizedQuery, ignoreCase = true) ||
                        item.beacon.description.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати орієнтир як ArcQuest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Пошук орієнтирів") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (beacons.isEmpty()) {
                    Text(
                        text = "Орієнтирів немає",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (filteredBeacons.isEmpty()) {
                    Text(
                        text = "Нічого не знайдено",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(filteredBeacons, key = { _, item -> item.beacon.id }) { _, item ->
                            val beacon = item.beacon
                            val alreadyAdded = beacon.id in usedSourceIds
                            PickerOptionRow(
                                title = beacon.title,
                                subtitle = if (alreadyAdded) "Уже є в квестах арки" else beacon.description,
                                enabled = !alreadyAdded,
                                onClick = { onSelected(beacon.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )
}

@Composable
internal fun ArcQuestBeaconGroupPickerDialog(
    groups: List<MainBeaconGroup>,
    usedSourceIds: Set<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати групу орієнтирів як ArcQuest") },
        text = {
            if (groups.isEmpty()) {
                Text(
                    text = "Груп орієнтирів немає",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(groups, key = { _, group -> group.id }) { _, group ->
                        val alreadyAdded = group.id in usedSourceIds
                        PickerOptionRow(
                            title = group.title,
                            subtitle = if (alreadyAdded) "Уже є в квестах арки" else group.description,
                            enabled = !alreadyAdded,
                            onClick = { onSelected(group.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )
}

@Composable
private fun PickerOptionRow(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
        }
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = contentColor,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
