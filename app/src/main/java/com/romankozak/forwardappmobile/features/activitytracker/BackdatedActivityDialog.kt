package com.romankozak.forwardappmobile.features.activitytracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityDescriptor
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityLinksEditor
import com.romankozak.forwardappmobile.features.reminders.components.DateTimePickerDialog
import java.text.DateFormat
import java.util.Date

data class BackdatedActivityDraft(
    val text: String,
    val durationMinutes: Long = 30,
    val entityLinks: List<ActivityEntityLink> = emptyList(),
)

@Composable
fun BackdatedActivityDialog(
    draft: BackdatedActivityDraft,
    entityOptions: List<ActivityEntityDescriptor>,
    onDismiss: () -> Unit,
    onConfirm: (text: String, startTime: Long, endTime: Long, links: List<ActivityEntityLink>) -> Unit,
) {
    var text by remember(draft) { mutableStateOf(draft.text) }
    var durationText by remember(draft) { mutableStateOf(draft.durationMinutes.toString()) }
    var endTime by remember(draft) { mutableStateOf(System.currentTimeMillis()) }
    var entityLinks by remember(draft) { mutableStateOf(draft.entityLinks) }
    var showEndPicker by remember { mutableStateOf(false) }
    val duration = durationText.toLongOrNull()
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати минулу активність") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Що робили") },
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { value ->
                        if (value.isBlank() || value.all(Char::isDigit)) durationText = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Тривалість, хв") },
                    singleLine = true,
                )
                OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Text(" Завершено: ${formatter.format(Date(endTime))}")
                }
                if (duration != null && duration > 0) {
                    Text("Початок: ${formatter.format(Date(endTime - duration * 60_000L))}")
                }
                ActivityEntityLinksEditor(
                    selectedLinks = entityLinks,
                    options = entityOptions,
                    onLinksChanged = { entityLinks = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = duration ?: return@Button
                    onConfirm(text.trim(), endTime - minutes * 60_000L, endTime, entityLinks)
                },
                enabled = text.isNotBlank() && duration != null && duration > 0,
            ) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )

    if (showEndPicker) {
        DateTimePickerDialog(
            initialDateTime = endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                endTime = it
                showEndPicker = false
            },
            enablePastValues = true,
            title = "Завершення активності",
            summaryLabel = "Обраний час",
        )
    }
}
