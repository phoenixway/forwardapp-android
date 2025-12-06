package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tactical Missions") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Mission")
            }
        }
    ) { padding ->
        TacticalMissionList(
            missions = missions,
            onMissionToggled = { viewModel.toggleMissionCompleted(it) },
            onMissionDeleted = { viewModel.deleteMission(it.id) },
            modifier = Modifier.padding(padding)
        )

        if (showDialog) {
            AddMissionDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, description, deadline ->
                    viewModel.addMission(
                        title = title,
                        description = description,
                        deadline = deadline
                    )
                    showDialog = false
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
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Mission") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (timestamp)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deadlineLong = deadline.toLongOrNull() ?: System.currentTimeMillis()
                    onConfirm(title, description, deadlineLong)
                }
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
fun TacticalMissionList(
    missions: List<TacticalMission>,
    onMissionToggled: (TacticalMission) -> Unit,
    onMissionDeleted: (TacticalMission) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(missions) { mission ->
            TacticalMissionItem(
                mission = mission,
                onMissionToggled = { onMissionToggled(mission) },
                onMissionDeleted = { onMissionDeleted(mission) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun TacticalMissionItem(
    mission: TacticalMission,
    onMissionToggled: () -> Unit,
    onMissionDeleted: () -> Unit
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
            IconButton(onClick = onMissionDeleted) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Mission")
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
