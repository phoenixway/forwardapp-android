package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) } // State for editing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tactical Missions") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Mission")
            }
        }
    ) { padding ->
        TacticalMissionList(
            missions = missions,
            onMissionToggled = { viewModel.toggleMissionCompleted(it) },
            onMissionDeleted = { viewModel.deleteMission(it.id) },
            onMissionEdited = { mission -> editingMission = mission }, // Pass editing lambda
            modifier = Modifier.padding(padding)
        )

        if (showAddDialog) {
            AddMissionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description, deadline ->
                    viewModel.addMission(
                        title = title,
                        description = description,
                        deadline = deadline
                    )
                    showAddDialog = false
                }
            )
        }

        editingMission?.let { mission ->
            EditMissionDialog(
                mission = mission,
                onDismiss = { editingMission = null },
                onConfirm = { updatedTitle, updatedDescription, updatedDeadline ->
                    viewModel.updateMission(
                        id = mission.id,
                        title = updatedTitle,
                        description = updatedDescription,
                        deadline = updatedDeadline
                    )
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
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(System.currentTimeMillis().toString()) } // Default to current time

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Mission") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (timestamp)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deadlineLong = deadline.toLongOrNull() ?: System.currentTimeMillis()
                    onConfirm(title, description, deadlineLong)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditMissionDialog(
    mission: TacticalMission,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf(mission.title) }
    var description by remember { mutableStateOf(mission.description ?: "") }
    var deadline by remember { mutableStateOf(mission.deadline.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Mission") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (timestamp)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deadlineLong = deadline.toLongOrNull() ?: System.currentTimeMillis()
                    onConfirm(title, description, deadlineLong)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TacticalMissionList(
    missions: List<TacticalMission>,
    onMissionToggled: (TacticalMission) -> Unit,
    onMissionDeleted: (TacticalMission) -> Unit,
    onMissionEdited: (TacticalMission) -> Unit, // New parameter
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(missions) { mission ->
            TacticalMissionItem(
                mission = mission,
                onMissionToggled = { onMissionToggled(mission) },
                onMissionDeleted = { onMissionDeleted(mission) },
                onMissionEdited = { onMissionEdited(mission) } // Pass through
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun TacticalMissionItem(
    mission: TacticalMission,
    onMissionToggled: () -> Unit,
    onMissionDeleted: () -> Unit,
    onMissionEdited: () -> Unit // New parameter
) {
    val isOverdue = System.currentTimeMillis() > mission.deadline && mission.status != MissionStatus.COMPLETED
    val itemColor = if (isOverdue) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = mission.status == MissionStatus.COMPLETED,
                onCheckedChange = { onMissionToggled() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textDecoration = if (mission.status == MissionStatus.COMPLETED) TextDecoration.LineThrough else null
                )
                mission.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (mission.status == MissionStatus.COMPLETED) TextDecoration.LineThrough else null
                    )
                }
                Text(
                    text = "Deadline: ${formatDate(mission.deadline)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Row { // Use a Row for multiple action icons
                IconButton(onClick = onMissionEdited) { // Edit button
                    Icon(Icons.Default.Edit, contentDescription = "Edit Mission")
                }
                IconButton(onClick = onMissionDeleted) { // Delete button
                    Icon(Icons.Default.Delete, contentDescription = "Delete Mission")
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
