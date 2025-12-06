package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.features.missions.data.model.TacticalMission
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalManagementScreen(
    viewModel: TacticalMissionViewModel = hiltViewModel()
) {
    val missions by viewModel.missions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) }

    Scaffold(
        // topBar = {
        //     TopAppBar(
        //         title = { Text("🎯 Tactical Missions", fontWeight = FontWeight.Bold) }
        //     )
        // },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Mission")
            }
        }
    ) { padding ->
Text("🎯 Tactical Missions", fontWeight = FontWeight.Bold)

        TacticalMissionList(
            missions = missions,
            onMissionToggled = { viewModel.toggleMissionCompleted(it) },
            onMissionDeleted = { viewModel.deleteMission(it.id) },
            onMissionEdited = { editingMission = it },
            modifier = Modifier.padding(padding)
        )

        if (showAddDialog) {
            AddMissionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description, deadline ->
                    viewModel.addMission(title, description, deadline)
                    showAddDialog = false
                }
            )
        }

        editingMission?.let { mission ->
            EditMissionDialog(
                mission = mission,
                onDismiss = { editingMission = null },
                onConfirm = { title, desc, deadline ->
                    viewModel.updateMission(mission.id, title, desc, deadline)
                    editingMission = null
                }
            )
        }
    }
}


@Composable
fun AddMissionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    MissionDialog(
        title = "Create Tactical Mission",
        initialTitle = "",
        initialDescription = "",
        initialDeadline = System.currentTimeMillis().toString(),
        confirmText = "Create",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun EditMissionDialog(
    mission: TacticalMission,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    MissionDialog(
        title = "Edit Mission",
        initialTitle = mission.title,
        initialDescription = mission.description ?: "",
        initialDeadline = mission.deadline.toString(),
        confirmText = "Save",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun MissionDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    initialDeadline: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    var titleField by remember { mutableStateOf(initialTitle) }
    var descField by remember { mutableStateOf(initialDescription) }
    var deadlineField by remember { mutableStateOf(initialDeadline) }
var deadlineLong by remember { mutableStateOf(initialDeadline.toLong()) }
var showDeadlinePicker by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = titleField,
                    onValueChange = { titleField = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descField,
                    onValueChange = { descField = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

Button(
    onClick = { showDeadlinePicker = true },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Select Deadline: ${formatDate(deadlineLong)}")
}

            }
        },
        confirmButton = {
    Button(
        onClick = {
            onConfirm(titleField, descField, deadlineLong)
        },
        enabled = titleField.isNotBlank()
    ) {
        Text(confirmText)
    }
}
,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDeadlinePicker) {
    DeadlinePickerDialog(
        initialTime = deadlineLong,
        onDismiss = { showDeadlinePicker = false },
        onConfirm = {
            deadlineLong = it
            showDeadlinePicker = false
        }
    )
}

}

@Composable
fun TacticalMissionList(
    missions: List<TacticalMission>,
    onMissionToggled: (TacticalMission) -> Unit,
    onMissionDeleted: (TacticalMission) -> Unit,
    onMissionEdited: (TacticalMission) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(missions) { mission ->
            TacticalMissionItem(
                mission = mission,
                onMissionToggled = { onMissionToggled(mission) },
                onMissionDeleted = { onMissionDeleted(mission) },
                onMissionEdited = { onMissionEdited(mission) }
            )
        }
    }
}

@Composable
fun TacticalMissionItem(
    mission: TacticalMission,
    onMissionToggled: () -> Unit,
    onMissionDeleted: () -> Unit,
    onMissionEdited: () -> Unit
) {
    val overdue = System.currentTimeMillis() > mission.deadline && mission.status != MissionStatus.COMPLETED

    val bgColor by animateColorAsState(
        when {
            mission.status == MissionStatus.COMPLETED ->
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)

            overdue ->
                Color(0x44FF5555)

            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = mission.status == MissionStatus.COMPLETED,
                onCheckedChange = { onMissionToggled() }
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    mission.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textDecoration = if (mission.status == MissionStatus.COMPLETED)
                        TextDecoration.LineThrough else null
                )

                if (!mission.description.isNullOrBlank()) {
                    Text(
                        mission.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                        textDecoration = if (mission.status == MissionStatus.COMPLETED)
                            TextDecoration.LineThrough else null
                    )
                }

                Text(
                    "⏳ " + formatDate(mission.deadline),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            IconButton(onClick = onMissionEdited) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onMissionDeleted) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
    onConfirm: (Long) -> Unit
) {
    val initialCalendar = remember(initialTime) {
        Calendar.getInstance().apply { timeInMillis = initialTime }
    }

    var selectedDate by remember { mutableStateOf(initialCalendar.timeInMillis) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePickerDialog(
            initialCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate },
            onDismiss = onDismiss,
            onConfirm = { hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                onConfirm(calendar.timeInMillis)
            }
        )
        return
    }

    // 🎯 FIX: створюємо state тут — у Composable, а не в onClick
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialCalendar.timeInMillis
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
                }
            ) { Text("Next") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialCalendar: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(Calendar.MINUTE),
        is24Hour = true
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
        }
    )
}
