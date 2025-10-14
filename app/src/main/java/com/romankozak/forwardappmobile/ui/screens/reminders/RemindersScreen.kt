package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.Goal

import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import com.romankozak.forwardappmobile.ui.screens.reminders.components.ReminderAction
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.ProjectItem
import com.romankozak.forwardappmobile.ui.screens.reminders.components.ReminderActionsDialog
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.reminders.ReminderListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    navController: NavController,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val itemToEdit by viewModel.itemToEdit.collectAsStateWithLifecycle()
    val currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showActionsDialogForItem by remember { mutableStateOf<ReminderListItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RemindersUiEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllReminders() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear all reminders")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (reminders.isEmpty()) {
            EmptyRemindersView()
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(reminders) { reminderItem ->
                    when (reminderItem) {
                        is ReminderListItem.GoalReminder -> {
                            val goal = reminderItem.goal
                            val isSnoozed = reminderItem.reminder.status == "SNOOZED"
                            val isCompleted = reminderItem.reminder.status == "COMPLETED"
                            GoalItem(
                                goal = goal,
                                obsidianVaultName = "",
                                onCheckedChange = { _ -> },
                                onItemClick = { viewModel.onEditReminder(reminderItem) },
                                onLongClick = { },
                                onTagClick = { },
                                onRelatedLinkClick = { },
                                emojiToHide = null,
                                contextMarkerToEmojiMap = emptyMap(),
                                currentTimeMillis = currentTimeMillis,
                                isSelected = false,
                                reminder = reminderItem.reminder,
                                endAction = {
                                    IconButton(onClick = { showActionsDialogForItem = reminderItem }) {
                                        Icon(Icons.Default.MoreHoriz, "...")
                                    }
                                }
                            )
                        }
                        is ReminderListItem.ProjectReminder -> {
                            val project = reminderItem.project
                            val isSnoozed = reminderItem.reminder.status == "SNOOZED"
                            val isCompleted = reminderItem.reminder.status == "COMPLETED"
                            ProjectItem(
                                project = project,
                                childProjects = emptyList(),
                                onCheckedChange = { _ -> },
                                onItemClick = { viewModel.onEditReminder(reminderItem) },
                                onLongClick = { },
                                onTagClick = { },
                                onChildProjectClick = { },
                                onRelatedLinkClick = { },
                                emojiToHide = null,
                                contextMarkerToEmojiMap = emptyMap(),
                                currentTimeMillis = currentTimeMillis,
                                isSelected = false,
                                reminder = reminderItem.reminder,
                                endAction = {
                                    IconButton(onClick = { showActionsDialogForItem = reminderItem }) {
                                        Icon(Icons.Default.MoreHoriz, "...")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    itemToEdit?.let { item ->
        ReminderPickerDialog(
            onDismiss = { viewModel.onDismissEditReminder() },
            onSetReminder = { timestamp -> viewModel.setReminder(item.reminder.id, timestamp) },
            onClearReminder = { viewModel.clearReminder(item.reminder.id) },
            currentReminderTime = item.reminder.reminderTime
        )
    }

    showActionsDialogForItem?.let { item ->
        val actions = listOf(
            ReminderAction(
                text = "Show in project",
                icon = Icons.Outlined.TravelExplore,
                onClick = {
                    viewModel.showItemInProject(item)
                    showActionsDialogForItem = null
                }
            ),
            ReminderAction(
                text = "Delete",
                icon = Icons.Outlined.Delete,
                onClick = {
                    viewModel.deleteReminder(item)
                    showActionsDialogForItem = null
                },
                color = MaterialTheme.colorScheme.error
            )
        )
        ReminderActionsDialog(
            onDismiss = { showActionsDialogForItem = null },
            actions = actions
        )
    }
}

@Composable
fun EmptyRemindersView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = "No reminders",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "No reminders yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add a reminder to a goal to see it here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
