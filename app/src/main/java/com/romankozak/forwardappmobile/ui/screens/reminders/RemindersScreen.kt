package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ReminderStatusValues
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import com.romankozak.forwardappmobile.ui.screens.reminders.components.ReminderAction
import com.romankozak.forwardappmobile.ui.screens.reminders.components.ReminderActionsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    navController: NavController,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val goalToEdit by viewModel.goalToEdit.collectAsStateWithLifecycle()
    val currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showActionsDialogForGoal by remember { mutableStateOf<Goal?>(null) }

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
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(reminders) { reminderItem ->
                val goal = reminderItem.goal
                val isSnoozed = reminderItem.reminderInfo?.reminderStatus == ReminderStatusValues.SNOOZED
                GoalItem(
                    goal = goal,
                    obsidianVaultName = "",
                    onCheckedChange = { _ -> },
                    onItemClick = { viewModel.onEditReminder(goal) },
                    onLongClick = { },
                    onTagClick = { },
                    onRelatedLinkClick = { },
                    emojiToHide = null,
                    contextMarkerToEmojiMap = emptyMap(),
                    currentTimeMillis = currentTimeMillis,
                    isSelected = false,
                    isSnoozed = isSnoozed,
                    endAction = {
                        IconButton(onClick = { showActionsDialogForGoal = goal }) {
                            Icon(Icons.Default.MoreHoriz, "...")
                        }
                    }
                )
            }
        }
    }

    goalToEdit?.let { goal ->
        ReminderPickerDialog(
            onDismiss = { viewModel.onDismissEditReminder() },
            onSetReminder = { timestamp -> viewModel.setReminderForGoal(goal.id, timestamp) },
            onClearReminder = { viewModel.clearReminderForGoal(goal.id) },
            currentReminderTime = goal.reminderTime
        )
    }

    showActionsDialogForGoal?.let { goal ->
        val actions = listOf(
            ReminderAction(
                text = "Show in project",
                icon = Icons.Outlined.TravelExplore,
                onClick = {
                    viewModel.showGoalInProject(goal)
                    showActionsDialogForGoal = null
                }
            )
        )
        ReminderActionsDialog(
            onDismiss = { showActionsDialogForGoal = null },
            actions = actions
        )
    }
}