package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.TacticalMissionList
import com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.TacticalScopeLinksSheet
import com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.dialogs.TacticalAddObsidianDialog
import com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.dialogs.TacticalAddUrlDialog
import com.romankozak.forwardappmobile.ui.components.orderToken
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalManagementScreen(
    onLinkedProjectClick: (String) -> Unit = {},
    onLinkedAttachmentClick: (String) -> Unit = {},
    viewModel: TacticalMissionViewModel = hiltViewModel(),
) {
    val missions by viewModel.missions.collectAsState()
    val attachmentOptions by viewModel.attachmentOptions.collectAsState()
    val projectOptions by viewModel.projectOptions.collectAsState()
    val boardLinkedProjectIds by viewModel.boardLinkedProjectIds.collectAsState()
    val boardLinkedAttachmentIds by viewModel.boardLinkedAttachmentIds.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    val showAddDialog by viewModel.isAddMissionDialogOpen.collectAsState()
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) }
    var actionMenuMission by remember { mutableStateOf<TacticalMission?>(null) }
    var showProjectChooser by remember { mutableStateOf(false) }
    var showAttachmentChooser by remember { mutableStateOf(false) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddObsidianDialog by remember { mutableStateOf(false) }
    var selectedMissionIds by remember { mutableStateOf(setOf<Long>()) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val selectionMode = selectedMissionIds.isNotEmpty()

    LaunchedEffect(missions) {
        val existingIds = missions.map { it.id }.toSet()
        selectedMissionIds = selectedMissionIds.filter { it in existingIds }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectionMode) {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Вибрано: ${selectedMissionIds.size}",
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Box {
                        FilledTonalButton(onClick = { statusMenuExpanded = true }) {
                            Text("Змінити статус")
                        }
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false },
                        ) {
                            listOf(
                                MissionStatus.ACTIVE to "Активна",
                                MissionStatus.INACTIVE to "Неактивна",
                                MissionStatus.PAUSED to "На паузі",
                            ).forEach { (status, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        missions
                                            .filter { it.id in selectedMissionIds }
                                            .forEach { mission ->
                                                viewModel.updateMission(mission.copy(status = status))
                                            }
                                        selectedMissionIds = emptySet()
                                        statusMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            missions
                                .filter { it.id in selectedMissionIds }
                                .forEach { mission ->
                                    viewModel.updateMission(mission.copy(status = MissionStatus.COMPLETED))
                                }
                            selectedMissionIds = emptySet()
                        },
                    ) {
                        Text("Виконані")
                    }

                    OutlinedButton(
                        onClick = {
                            missions
                                .filter { it.id in selectedMissionIds }
                                .forEach { mission ->
                                    viewModel.updateMission(mission.copy(status = MissionStatus.ACTIVE))
                                }
                            selectedMissionIds = emptySet()
                        },
                    ) {
                        Text("Невиконані")
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        onClick = {
                            selectedMissionIds.forEach { id ->
                                viewModel.deleteMission(id)
                            }
                            selectedMissionIds = emptySet()
                        },
                    ) {
                        Text("Видалити", color = MaterialTheme.colorScheme.onErrorContainer)
                    }

                    TextButton(onClick = { selectedMissionIds = emptySet() }) {
                        Text("Скасувати")
                    }
                }
            }
        }

        TacticalMissionList(
            missions = missions,
            projectOptions = projectOptions,
            attachmentOptions = attachmentOptions,
            selectedMissionIds = selectedMissionIds,
            selectionMode = selectionMode,
            onMissionToggled = { viewModel.toggleMissionCompleted(it) },
            onMissionSelectionToggle = { mission ->
                selectedMissionIds =
                    if (mission.id in selectedMissionIds) {
                        selectedMissionIds - mission.id
                    } else {
                        selectedMissionIds + mission.id
                    }
            },
            onMissionClick = { mission ->
                if (!selectionMode) {
                    editingMission = mission
                }
            },
            onMissionLongPress = { mission ->
                selectedMissionIds =
                    if (mission.id in selectedMissionIds) {
                        selectedMissionIds
                    } else {
                        selectedMissionIds + mission.id
                    }
            },
            onMissionMoreClick = { mission -> actionMenuMission = mission },
            onMissionsReordered = viewModel::reorderMissions,
            modifier = Modifier.weight(1f),
        )

    }

    actionMenuMission?.let { mission ->
        ModalBottomSheet(
            onDismissRequest = { actionMenuMission = null },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                HorizontalDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.Edit,
                    text = "Редагувати",
                    onClick = {
                        editingMission = mission
                        actionMenuMission = null
                    },
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.CheckCircle,
                    text = if (mission.status == MissionStatus.COMPLETED) "Позначити невиконаною" else "Позначити виконаною",
                    onClick = {
                        val nextStatus =
                            if (mission.status == MissionStatus.COMPLETED) {
                                MissionStatus.ACTIVE
                            } else {
                                MissionStatus.COMPLETED
                            }
                        viewModel.updateMission(mission.copy(status = nextStatus))
                        actionMenuMission = null
                    },
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.DeleteOutline,
                    text = "Видалити",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.deleteMission(mission.id)
                        actionMenuMission = null
                    },
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.Close,
                    text = "Скасувати",
                    onClick = { actionMenuMission = null },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    val availableProjectIds = projectOptions.map { it.id }.toSet()
    val availableAttachmentIds = attachmentOptions.map { it.id }.toSet()
    val validBoardLinkedProjectIds = boardLinkedProjectIds.filter { it in availableProjectIds }
    val validBoardLinkedAttachmentIds = boardLinkedAttachmentIds.filter { it in availableAttachmentIds }

    TacticalScopeLinksSheet(
        isVisible = isScopeLinksSheetVisible,
        projectOptions = projectOptions,
        attachmentOptions = attachmentOptions,
        linkedProjectIds = boardLinkedProjectIds,
        linkedAttachmentIds = boardLinkedAttachmentIds,
        onDismiss = viewModel::dismissScopeLinksSheet,
        onAddContextClick = { showProjectChooser = true },
        onAddAttachmentClick = { showAttachmentChooser = true },
        onAddExternalClick = { showAddUrlDialog = true },
        onAddObsidianClick = { showAddObsidianDialog = true },
        onContextClick = onLinkedProjectClick,
        onAttachmentClick = onLinkedAttachmentClick,
        onContextRemove = viewModel::removeBoardProjectLink,
        onAttachmentRemove = viewModel::removeBoardAttachmentLink,
        connectionOrder = connectionsOrder,
        onConnectionsReordered = { reordered ->
            viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
        },
    )

    if (showAddDialog) {
        AddMissionDialog(
            attachmentOptions = attachmentOptions,
            onDismiss = viewModel::dismissAddMissionDialog,
            onConfirm = { title, description, deadline, status, projects, attachments ->
                viewModel.addMission(title, description, deadline, status, projects, attachments)
                viewModel.dismissAddMissionDialog()
            },
        )
    }

    editingMission?.let { mission ->
        Dialog(
            onDismissRequest = { editingMission = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                MissionEditorScreen(
                    mission = mission,
                    attachmentOptions = attachmentOptions,
                    projectOptions = projectOptions,
                    onDismiss = { editingMission = null },
                    onConfirm = { title, desc, deadline, status, projects, attachments ->
                        viewModel.updateMission(
                            mission.id,
                            title,
                            desc,
                            deadline,
                            status,
                            projects,
                            attachments,
                        )
                        editingMission = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showProjectChooser) {
        ProjectChooserScreen(
            options = projectOptions,
            preselected = validBoardLinkedProjectIds.toSet(),
            onDismiss = { showProjectChooser = false },
            onConfirm = { selected ->
                selected.forEach(viewModel::addBoardProjectLink)
                showProjectChooser = false
            },
        )
    }

    if (showAttachmentChooser) {
        AttachmentChooserScreen(
            options = attachmentOptions,
            preselected = validBoardLinkedAttachmentIds.toSet(),
            onDismiss = { showAttachmentChooser = false },
            onConfirm = { selected ->
                selected.forEach(viewModel::addBoardAttachmentLink)
                showAttachmentChooser = false
            },
        )
    }

    if (showAddUrlDialog) {
        TacticalAddUrlDialog(
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url, name ->
                viewModel.addBoardUrlLink(url, name)
                showAddUrlDialog = false
            },
        )
    }

    if (showAddObsidianDialog) {
        TacticalAddObsidianDialog(
            onDismiss = { showAddObsidianDialog = false },
            onConfirm = { noteName, displayName ->
                viewModel.addBoardObsidianLink(noteName, displayName)
                showAddObsidianDialog = false
            },
        )
    }

}

@Composable
fun AddMissionDialog(
    attachmentOptions: List<AttachmentOption>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
) {
    MissionDialog(
        title = "Create Tactical Mission",
        initialTitle = "",
        initialDescription = "",
        initialDeadline = System.currentTimeMillis().toString(),
        confirmText = "Create",
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        initialProjectLinks = emptyList(),
        initialAttachmentLinks = emptyList(),
        attachmentOptions = attachmentOptions,
    )
}

@Composable
private fun MissionActionSheetItem(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = textColor.copy(alpha = 0.9f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }

}

@Composable
private fun SubtleActionDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
    )
}

@Composable
fun MissionDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    initialDeadline: String,
    initialProjectLinks: List<String>,
    initialAttachmentLinks: List<String>,
    attachmentOptions: List<AttachmentOption>,
    initialStatus: MissionStatus = MissionStatus.ACTIVE,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
) {
    var titleField by remember { mutableStateOf(initialTitle) }
    var descField by remember { mutableStateOf(initialDescription) }
    var deadlineField by remember { mutableStateOf(initialDeadline) }
    var deadlineLong by remember { mutableStateOf(initialDeadline.toLong()) }
    var statusField by remember { mutableStateOf(initialStatus) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    val projectLinks = remember { mutableStateListOf<String>().apply { addAll(initialProjectLinks) } }
    val attachmentLinks = remember { mutableStateListOf<String>().apply { addAll(initialAttachmentLinks) } }
    var showAttachmentChooser by remember { mutableStateOf(false) }

    fun attachmentLabel(id: String): String {
        return attachmentOptions.firstOrNull { it.id == id }?.name ?: id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleField,
                    onValueChange = { titleField = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = descField,
                    onValueChange = { descField = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { showDeadlinePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Select Deadline: ${formatDate(deadlineLong)}")
                }

                Text("Status", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(MissionStatus.ACTIVE, MissionStatus.INACTIVE, MissionStatus.PAUSED).forEach { status ->
                        FilterChip(
                            selected = statusField == status,
                            onClick = { statusField = status },
                            label = {
                                Text(
                                    when (status) {
                                        MissionStatus.ACTIVE -> "Активна"
                                        MissionStatus.INACTIVE -> "Неактивна"
                                        MissionStatus.PAUSED -> "На паузі"
                                        MissionStatus.COMPLETED -> "Завершена"
                                    },
                                )
                            },
                        )
                    }
                }

                Divider()

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
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    attachmentLabel(id),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                IconButton(onClick = { attachmentLinks.remove(id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove attachment",
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAttachmentChooser = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add attachment")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        titleField,
                        descField,
                        deadlineLong,
                        statusField,
                        projectLinks.toList(),
                        attachmentLinks.toList(),
                    )
                },
                enabled = titleField.isNotBlank(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showDeadlinePicker) {
        DeadlinePickerDialog(
            initialTime = deadlineLong,
            onDismiss = { showDeadlinePicker = false },
            onConfirm = {
                deadlineLong = it
                showDeadlinePicker = false
            },
        )
    }

    if (showAttachmentChooser) {
        AttachmentChooserScreen(
            options = attachmentOptions,
            preselected = attachmentLinks.toSet(),
            onDismiss = { showAttachmentChooser = false },
            onConfirm = { selected ->
                selected.forEach { id ->
                    if (!attachmentLinks.contains(id)) {
                        attachmentLinks.add(id)
                    }
                }
                showAttachmentChooser = false
            },
        )
    }
}

private fun formatDate(ts: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinePickerDialog(
    initialTime: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val initialCalendar =
        remember(initialTime) {
            Calendar.getInstance().apply { timeInMillis = initialTime }
        }

    var selectedDate by remember { mutableStateOf(initialCalendar.timeInMillis) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePickerDialog(
            initialCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate },
            onDismiss = onDismiss,
            onConfirm = { hour, minute ->
                val calendar =
                    Calendar.getInstance().apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }
                onConfirm(calendar.timeInMillis)
            },
        )
        return
    }

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialCalendar.timeInMillis,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Deadline") },
        text = {
            DatePicker(state = datePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDate =
                        datePickerState.selectedDateMillis ?: initialCalendar.timeInMillis

                    showTimePicker = true
                },
            ) { Text("Next") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialCalendar: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCalendar.get(Calendar.MINUTE),
            is24Hour = true,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
