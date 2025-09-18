// file: ui/screens/backlog/GoalDetailDialogs.kt

package com.romankozak.forwardappmobile.ui.screens.backlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.*

// ВИПРАВЛЕНО: Додано необхідний імпорт

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailDialogs(viewModel: BacklogViewModel) { // ВИПРАВЛЕНО: Правильний тип ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ВИПРАВЛЕНО: Звертаємось до recordToEdit через inboxHandler
    val recordToEdit by viewModel.inboxHandler.recordToEdit.collectAsStateWithLifecycle()
    val goalActionState by viewModel.itemActionHandler.goalActionDialogState.collectAsStateWithLifecycle()
    val showGoalTransportMenu by viewModel.itemActionHandler.showGoalTransportMenu.collectAsStateWithLifecycle()
    val itemForTransportMenu by viewModel.itemActionHandler.itemForTransportMenu.collectAsStateWithLifecycle()
    val showRecentListsSheet = uiState.showRecentProjectsSheet
    val recentLists by viewModel.recentProjects.collectAsStateWithLifecycle()

    if (uiState.showAddWebLinkDialog) {
        AddWebLinkDialog(
            onDismiss = { viewModel.inputHandler.onDismissLinkDialogs() },
            onConfirm = { url, name -> viewModel.inputHandler.onAddWebLinkConfirm(url, name) },
        )
    }

    if (uiState.showAddObsidianLinkDialog) {
        AddObsidianLinkDialog(
            onDismiss = { viewModel.inputHandler.onDismissLinkDialogs() },
            onConfirm = { noteName -> viewModel.inputHandler.onAddObsidianLinkConfirm(noteName) },
        )
    }

    recordToEdit?.let { record ->
        EditInboxRecordDialog(
            record = record,
            // ВИПРАВЛЕНО: Викликаємо методи з inboxHandler
            onDismiss = { viewModel.inboxHandler.onInboxRecordEditDismiss() },
            onConfirm = { newText -> viewModel.inboxHandler.onInboxRecordEditConfirm(newText) },
        )
    }

    if (goalActionState is GoalActionDialogState.AwaitingActionChoice) {
        val itemContent = (goalActionState as GoalActionDialogState.AwaitingActionChoice).itemContent
        GoalActionChoiceDialog(
            itemContent = itemContent,
            onDismiss = { viewModel.itemActionHandler.onDismissGoalActionDialogs() },
            onActionSelected = { actionType ->
                viewModel.itemActionHandler.onGoalActionSelected(actionType, itemContent)
            },
        )
    }

    GoalTransportMenu(
        isVisible = showGoalTransportMenu,
        onDismiss = { viewModel.itemActionHandler.onDismissGoalTransportMenu() },
        onCreateInstanceRequest = { viewModel.itemActionHandler.onTransportActionSelected(GoalActionType.CreateInstance) },
        onMoveInstanceRequest = { viewModel.itemActionHandler.onTransportActionSelected(GoalActionType.MoveInstance) },
        onCopyGoalRequest = { viewModel.itemActionHandler.onTransportActionSelected(GoalActionType.CopyGoal) },
        isGoalItem = itemForTransportMenu is ListItemContent.GoalItem
    )

    if (showRecentListsSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.inputHandler.onDismissRecentLists() }) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.recent_lists),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn {
                    items(recentLists, key = { it.id }) { list: Project ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            modifier = Modifier.clickable { viewModel.inputHandler.onRecentListSelected(list.id) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showImportFromMarkdownDialog) {
        ImportMarkdownDialog(
            onDismiss = viewModel::onImportFromMarkdownDismiss,
            onConfirm = viewModel::onImportFromMarkdownConfirm,
        )
    }

    if (uiState.showImportBacklogFromMarkdownDialog) {
        ImportMarkdownDialog(
            onDismiss = viewModel::onImportBacklogFromMarkdownDismiss,
            onConfirm = viewModel::onImportBacklogFromMarkdownConfirm,
        )
    }

    uiState.recordForReminderDialog?.let { record ->
        ReminderPickerDialog(
            onDismiss = viewModel::onReminderDialogDismiss,
            onSetReminder = viewModel::onSetReminder,
            onClearReminder = if (record.reminderTime != null) { { viewModel.onClearReminder() } } else { null },
            currentReminderTime = record.reminderTime,
        )
    }
}