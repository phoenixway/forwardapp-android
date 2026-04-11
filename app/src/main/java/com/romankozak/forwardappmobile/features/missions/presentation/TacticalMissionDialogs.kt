package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE

private data class MissionDialogUiState(
    val titleField: String,
    val descField: String,
    val deadlineLong: Long,
    val statusField: MissionStatus,
    val attachmentLinks: List<String>,
)

private data class MissionDialogCallbacks(
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onDeadlineClick: () -> Unit,
    val onDeadlineClear: () -> Unit,
    val onStatusChange: (MissionStatus) -> Unit,
    val onRemoveAttachment: (String) -> Unit,
    val onAddAttachmentClick: () -> Unit,
)

private data class MissionDialogOverlayState(
    val showDeadlinePicker: Boolean,
    val deadlineLong: Long,
    val showAttachmentChooser: Boolean,
    val attachmentOptions: List<AttachmentOption>,
    val attachmentLinks: MutableList<String>,
)

private data class MissionDialogAlertActions(
    val onDismiss: () -> Unit,
    val onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
)

@Composable
fun AddMissionDialog(
    attachmentOptions: List<AttachmentOption>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
) {
    MissionDialog(
        config =
            MissionDialogConfig(
                title = "Create Tactical Mission",
                initialTitle = "",
                initialDescription = "",
                initialDeadline = NO_DEADLINE.toString(),
                initialProjectLinks = emptyList(),
                initialAttachmentLinks = emptyList(),
                attachmentOptions = attachmentOptions,
                confirmText = "Create",
            ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun MissionDialog(
    config: MissionDialogConfig,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
) {
    var titleField by remember { mutableStateOf(config.initialTitle) }
    var descField by remember { mutableStateOf(config.initialDescription) }
    var deadlineLong by remember { mutableStateOf(config.initialDeadline.toLong()) }
    var statusField by remember { mutableStateOf(config.initialStatus) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    val projectLinks = remember { mutableStateListOf<String>().apply { addAll(config.initialProjectLinks) } }
    val attachmentLinks = remember { mutableStateListOf<String>().apply { addAll(config.initialAttachmentLinks) } }
    var showAttachmentChooser by remember { mutableStateOf(false) }
    val uiState =
        MissionDialogUiState(
            titleField = titleField,
            descField = descField,
            deadlineLong = deadlineLong,
            statusField = statusField,
            attachmentLinks = attachmentLinks,
        )
    val callbacks =
        MissionDialogCallbacks(
            onTitleChange = { titleField = it },
            onDescriptionChange = { descField = it },
            onDeadlineClick = { showDeadlinePicker = true },
            onDeadlineClear = { deadlineLong = NO_DEADLINE },
            onStatusChange = { statusField = it },
            onRemoveAttachment = { attachmentLinks.remove(it) },
            onAddAttachmentClick = { showAttachmentChooser = true },
        )
    val overlayState =
        MissionDialogOverlayState(
            showDeadlinePicker = showDeadlinePicker,
            deadlineLong = uiState.deadlineLong,
            showAttachmentChooser = showAttachmentChooser,
            attachmentOptions = config.attachmentOptions,
            attachmentLinks = attachmentLinks,
        )
    val alertActions =
        MissionDialogAlertActions(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )

    MissionDialogAlert(
        config = config,
        uiState = uiState,
        callbacks = callbacks,
        projectLinks = projectLinks,
        actions = alertActions,
    )

    MissionDialogOverlays(
        state = overlayState,
        onDeadlineDismiss = { showDeadlinePicker = false },
        onDeadlineConfirm = {
            deadlineLong = it
            showDeadlinePicker = false
        },
        onAttachmentChooserDismiss = { showAttachmentChooser = false },
    )
}

@Composable
private fun MissionDialogAlert(
    config: MissionDialogConfig,
    uiState: MissionDialogUiState,
    callbacks: MissionDialogCallbacks,
    projectLinks: List<String>,
    actions: MissionDialogAlertActions,
) {
    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(config.title, fontWeight = FontWeight.Bold) },
        text = {
            MissionDialogContent(
                uiState = uiState,
                attachmentOptions = config.attachmentOptions,
                callbacks = callbacks,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    actions.onConfirm(
                        uiState.titleField,
                        uiState.descField,
                        uiState.deadlineLong,
                        uiState.statusField,
                        projectLinks,
                        uiState.attachmentLinks,
                    )
                },
                enabled = uiState.titleField.isNotBlank(),
            ) {
                Text(config.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionDialogContent(
    uiState: MissionDialogUiState,
    attachmentOptions: List<AttachmentOption>,
    callbacks: MissionDialogCallbacks,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.titleField,
            onValueChange = callbacks.onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.descField,
            onValueChange = callbacks.onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = callbacks.onDeadlineClick, modifier = Modifier.weight(1f)) {
                Text("Deadline: ${missionDialogFormatDate(uiState.deadlineLong)}")
            }
            IconButton(onClick = callbacks.onDeadlineClear) {
                Icon(
                    imageVector = Icons.Outlined.EventBusy,
                    contentDescription = "Очистити дедлайн",
                )
            }
        }
        MissionStatusSection(
            statusField = uiState.statusField,
            onStatusChange = callbacks.onStatusChange,
        )
        HorizontalDivider()
        MissionAttachmentSection(
            attachmentLinks = uiState.attachmentLinks,
            attachmentOptions = attachmentOptions,
            onRemoveAttachment = callbacks.onRemoveAttachment,
            onAddAttachmentClick = callbacks.onAddAttachmentClick,
        )
    }
}

@Composable
private fun MissionDialogOverlays(
    state: MissionDialogOverlayState,
    onDeadlineDismiss: () -> Unit,
    onDeadlineConfirm: (Long) -> Unit,
    onAttachmentChooserDismiss: () -> Unit,
) {
    if (state.showDeadlinePicker) {
        DeadlinePickerDialog(
            initialTime = if (state.deadlineLong == NO_DEADLINE) System.currentTimeMillis() else state.deadlineLong,
            onDismiss = onDeadlineDismiss,
            onConfirm = onDeadlineConfirm,
        )
    }

    if (state.showAttachmentChooser) {
        AttachmentChooserScreen(
            options = state.attachmentOptions,
            preselected = state.attachmentLinks.toSet(),
            onDismiss = onAttachmentChooserDismiss,
            onConfirm = { selected ->
                selected.forEach { id ->
                    if (!state.attachmentLinks.contains(id)) {
                        state.attachmentLinks.add(id)
                    }
                }
                onAttachmentChooserDismiss()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionStatusSection(
    statusField: MissionStatus,
    onStatusChange: (MissionStatus) -> Unit,
) {
    Text("Status", style = MaterialTheme.typography.titleSmall)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(MissionStatus.ACTIVE, MissionStatus.INACTIVE, MissionStatus.PAUSED).forEach { status ->
            FilterChip(
                selected = statusField == status,
                onClick = { onStatusChange(status) },
                label = { Text(missionStatusLabel(status)) },
            )
        }
    }
}

@Composable
private fun MissionAttachmentSection(
    attachmentLinks: List<String>,
    attachmentOptions: List<AttachmentOption>,
    onRemoveAttachment: (String) -> Unit,
    onAddAttachmentClick: () -> Unit,
) {
    Text("Attachments", style = MaterialTheme.typography.titleSmall)
    if (attachmentLinks.isEmpty()) {
        Text(
            "No attachments linked",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            attachmentLinks.forEach { id ->
                MissionAttachmentRow(
                    label = attachmentLabel(id, attachmentOptions),
                    onRemove = { onRemoveAttachment(id) },
                )
            }
        }
    }
    Button(onClick = onAddAttachmentClick, modifier = Modifier.fillMaxWidth()) {
        Text("Add attachment")
    }
}

@Composable
private fun MissionAttachmentRow(
    label: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove attachment")
        }
    }
}

private fun missionStatusLabel(status: MissionStatus): String =
    when (status) {
        MissionStatus.ACTIVE -> "Активна"
        MissionStatus.INACTIVE -> "Неактивна"
        MissionStatus.PAUSED -> "На паузі"
        MissionStatus.COMPLETED -> "Завершена"
    }

private fun attachmentLabel(id: String, attachmentOptions: List<AttachmentOption>): String =
    attachmentOptions.firstOrNull { it.id == id }?.name ?: id
