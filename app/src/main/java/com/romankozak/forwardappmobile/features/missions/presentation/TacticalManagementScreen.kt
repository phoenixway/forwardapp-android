package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton
import com.romankozak.forwardappmobile.ui.components.ScopeLinkItem
import com.romankozak.forwardappmobile.ui.components.ScreenScopeLinksPanel
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
    val scopeContextsExpanded by viewModel.scopeContextsExpanded.collectAsState()
    val scopeAttachmentsExpanded by viewModel.scopeAttachmentsExpanded.collectAsState()
    val showAddDialog by viewModel.isAddMissionDialogOpen.collectAsState()
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) }
    var showProjectChooser by remember { mutableStateOf(false) }
    var showAttachmentChooser by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenScopeLinksPanel(
            title = "Посилання для тактичного циклу",
            contextLinks =
                boardLinkedProjectIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = projectOptions.firstOrNull { it.id == id }?.name ?: "Контекст ${id.take(8)}",
                    )
                },
            attachmentLinks =
                boardLinkedAttachmentIds.map { id ->
                    ScopeLinkItem(
                        id = id,
                        title = attachmentOptions.firstOrNull { it.id == id }?.name ?: "Вкладення ${id.take(8)}",
                    )
                },
            onAddContextClick = { showProjectChooser = true },
            onAddAttachmentClick = { showAttachmentChooser = true },
            onContextClick = onLinkedProjectClick,
            onAttachmentClick = onLinkedAttachmentClick,
            onContextRemove = viewModel::removeBoardProjectLink,
            onAttachmentRemove = viewModel::removeBoardAttachmentLink,
            contextsExpanded = scopeContextsExpanded,
            attachmentsExpanded = scopeAttachmentsExpanded,
            onContextsExpandedChange = viewModel::setScopeContextsExpanded,
            onAttachmentsExpandedChange = viewModel::setScopeAttachmentsExpanded,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        TacticalMissionList(
            missions = missions,
            onMissionToggled = { viewModel.toggleMissionCompleted(it) },
            onMissionDeleted = { viewModel.deleteMission(it.id) },
            onMissionEdited = { editingMission = it },
            modifier = Modifier.weight(1f),
            attachmentLabel = { id ->
                attachmentOptions.firstOrNull { it.id == id }?.name ?: id
            },
            projectLabel = { id ->
                projectOptions.firstOrNull { it.id == id }?.name ?: id
            },
            onLinkedProjectClick = onLinkedProjectClick,
            onLinkedAttachmentClick = onLinkedAttachmentClick,
        )
    }

    if (showAddDialog) {
        AddMissionDialog(
            attachmentOptions = attachmentOptions,
            onDismiss = viewModel::dismissAddMissionDialog,
            onConfirm = { title, description, deadline, projects, attachments ->
                viewModel.addMission(title, description, deadline, projects, attachments)
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
                    onConfirm = { title, desc, deadline, projects, attachments ->
                        viewModel.updateMission(
                            mission.id,
                            title,
                            desc,
                            deadline,
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
            preselected = boardLinkedProjectIds.toSet(),
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
            preselected = boardLinkedAttachmentIds.toSet(),
            onDismiss = { showAttachmentChooser = false },
            onConfirm = { selected ->
                selected.forEach(viewModel::addBoardAttachmentLink)
                showAttachmentChooser = false
            },
        )
    }
}

@Composable
fun AddMissionDialog(
    attachmentOptions: List<AttachmentOption>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, List<String>, List<String>) -> Unit,
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
fun MissionDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    initialDeadline: String,
    initialProjectLinks: List<String>,
    initialAttachmentLinks: List<String>,
    attachmentOptions: List<AttachmentOption>,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, List<String>, List<String>) -> Unit,
) {
    var titleField by remember { mutableStateOf(initialTitle) }
    var descField by remember { mutableStateOf(initialDescription) }
    var deadlineField by remember { mutableStateOf(initialDeadline) }
    var deadlineLong by remember { mutableStateOf(initialDeadline.toLong()) }
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
                    onConfirm(titleField, descField, deadlineLong, projectLinks.toList(), attachmentLinks.toList())
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

@Composable
fun TacticalMissionList(
    missions: List<TacticalMission>,
    onMissionToggled: (TacticalMission) -> Unit,
    onMissionDeleted: (TacticalMission) -> Unit,
    onMissionEdited: (TacticalMission) -> Unit,
    modifier: Modifier = Modifier,
    attachmentLabel: (String) -> String,
    projectLabel: (String) -> String,
    onLinkedProjectClick: (String) -> Unit,
    onLinkedAttachmentClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
    ) {
        items(missions) { mission ->
            TacticalMissionItem(
                mission = mission,
                onMissionToggled = { onMissionToggled(mission) },
                onMissionDeleted = { onMissionDeleted(mission) },
                onMissionEdited = { onMissionEdited(mission) },
                attachmentLabel = attachmentLabel,
                projectLabel = projectLabel,
                onLinkedProjectClick = onLinkedProjectClick,
                onLinkedAttachmentClick = onLinkedAttachmentClick,
            )
        }
    }
}

@Composable
fun TacticalMissionItem(
    mission: TacticalMission,
    onMissionToggled: () -> Unit,
    onMissionDeleted: () -> Unit,
    onMissionEdited: () -> Unit,
    attachmentLabel: (String) -> String,
    projectLabel: (String) -> String,
    onLinkedProjectClick: (String) -> Unit,
    onLinkedAttachmentClick: (String) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val colorScheme = MaterialTheme.colorScheme

    val overdue =
        System.currentTimeMillis() > mission.deadline &&
            mission.status != MissionStatus.COMPLETED
    val itemState =
        when {
            mission.status == MissionStatus.COMPLETED -> UnifiedItemState.COMPLETED
            overdue -> UnifiedItemState.OVERDUE
            else -> UnifiedItemState.DEFAULT
        }
    val missionContainerColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> colorScheme.secondaryContainer.copy(alpha = 0.36f)
            UnifiedItemState.OVERDUE -> colorScheme.errorContainer.copy(alpha = 0.58f)
            UnifiedItemState.DEFAULT -> colorScheme.tertiaryContainer.copy(alpha = 0.40f)
            UnifiedItemState.SELECTED -> colorScheme.surfaceContainerHighest
            UnifiedItemState.DISABLED -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
    val missionBorderColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> colorScheme.secondary.copy(alpha = 0.35f)
            UnifiedItemState.OVERDUE -> colorScheme.error.copy(alpha = 0.50f)
            UnifiedItemState.DEFAULT -> colorScheme.tertiary.copy(alpha = 0.38f)
            UnifiedItemState.SELECTED -> colorScheme.primary.copy(alpha = 0.4f)
            UnifiedItemState.DISABLED -> colorScheme.outlineVariant.copy(alpha = 0.35f)
        }

    UnifiedListItemSurface(
        isSelected = false,
        state = itemState,
        containerColorOverride = missionContainerColor,
        borderColorOverride = missionBorderColor,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UnifiedItemCheckbox(
                checked = mission.status == MissionStatus.COMPLETED,
                onCheckedChange = { onMissionToggled() },
                style = UnifiedCheckboxStyle.Round,
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = onSurface.copy(alpha = 0.7f),
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                val titleColor by animateColorAsState(
                    when {
                        mission.status == MissionStatus.COMPLETED ->
                            onSurface.copy(alpha = 0.4f)
                        overdue ->
                            Color(0xFFFF6E6E)
                        else -> onSurface
                    },
                )

                Text(
                    mission.title,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration =
                        if (mission.status == MissionStatus.COMPLETED) {
                            TextDecoration.LineThrough
                        } else {
                            null
                        },
                )

                if (!mission.description.isNullOrBlank()) {
                    Text(
                        mission.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface.copy(alpha = 0.7f),
                        textDecoration =
                            if (mission.status == MissionStatus.COMPLETED) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    val statusItems =
                        buildList {
                            add(
                                UnifiedStatusChipSpec(
                                    icon = if (overdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                                    text = formatDate(mission.deadline),
                                    contentColor = if (overdue) MaterialTheme.colorScheme.error else onSurface.copy(alpha = 0.75f),
                                ),
                            )
                            mission.linkedProjectIds.orEmpty().forEach { projectId ->
                                add(
                                    UnifiedStatusChipSpec(
                                        icon = Icons.Outlined.Topic,
                                        text = projectLabel(projectId),
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        onClick = { onLinkedProjectClick(projectId) },
                                    ),
                                )
                            }
                            mission.linkedAttachmentIds.orEmpty().forEach { attachmentId ->
                                add(
                                    UnifiedStatusChipSpec(
                                        icon = Icons.Outlined.Attachment,
                                        text = attachmentLabel(attachmentId),
                                        contentColor = MaterialTheme.colorScheme.secondary,
                                        onClick = { onLinkedAttachmentClick(attachmentId) },
                                    ),
                                )
                            }
                        }
                    UnifiedStatusRow(items = statusItems)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                UnifiedTrailingActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    onClick = onMissionEdited,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                )
                UnifiedTrailingActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    onClick = onMissionDeleted,
                    tint = Color(0xFFFF5A5A),
                )
            }
        }
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
