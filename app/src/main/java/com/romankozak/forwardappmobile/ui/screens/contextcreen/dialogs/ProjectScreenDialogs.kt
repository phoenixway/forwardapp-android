package com.romankozak.forwardappmobile.ui.screens.contextcreen.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

import com.romankozak.forwardappmobile.ui.screens.contextcreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.contextcreen.GoalActionDialogState
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.screens.contextcreen.GoalActionType



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailDialogs(viewModel: BacklogViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val goalActionState by viewModel.itemActionHandler.goalActionDialogState.collectAsStateWithLifecycle()
    val showGoalTransportMenu by viewModel.itemActionHandler.showGoalTransportMenu.collectAsStateWithLifecycle()
    val itemForTransportMenu by viewModel.itemActionHandler.itemForTransportMenu.collectAsStateWithLifecycle()
    val recentItems by viewModel.recentItems.collectAsStateWithLifecycle()

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
            onCreateNew = { noteName -> viewModel.inputHandler.onAddObsidianLinkAndCreateNewConfirm(noteName) },
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
            onSetReminder = {
                viewModel.onSetReminderForItem(itemContent)
            },
            onOpenRemindersDialog = {
                viewModel.onOpenRemindersDialog(itemContent)
            }
        )
    }

    if (uiState.showRemindersDialog) {
        val remindersViewModel: com.romankozak.forwardappmobile.ui.reminders.viewmodel.ReminderViewModel = hiltViewModel()
        uiState.itemForRemindersDialog?.let {
            com.romankozak.forwardappmobile.ui.reminders.dialogs.RemindersDialog(
                viewModel = remindersViewModel,
                item = it,
                onDismiss = { viewModel.onDismissRemindersDialog() }
            )
        }
    }

    GoalTransportMenu(
        isVisible = showGoalTransportMenu,
        onDismiss = { viewModel.itemActionHandler.onDismissGoalTransportMenu() },
        onCreateInstanceRequest = {
            itemForTransportMenu?.let { item ->
                viewModel.itemActionHandler.onItemActionSelected(GoalActionType.CreateInstance, item)
            }
        },
        onMoveInstanceRequest = {
            itemForTransportMenu?.let { item ->
                viewModel.itemActionHandler.onItemActionSelected(GoalActionType.MoveInstance, item)
            }
        },
        onCopyGoalRequest = {
            itemForTransportMenu?.let { item ->
                viewModel.itemActionHandler.onItemActionSelected(GoalActionType.CopyGoal, item)
            }
        },
        onCopyContentToClipboardRequest = viewModel.itemActionHandler.onCopyContentToClipboard.collectAsStateWithLifecycle().value,
        isGoalItem = itemForTransportMenu is ListItemContent.GoalItem,
    )

    if (uiState.showRecentProjectsSheet) {
        NewRecentListsSheet(
            showSheet = uiState.showRecentProjectsSheet,
            recentItems = recentItems,
            onDismiss = { viewModel.inputHandler.onDismissRecentLists() },
            onItemClick = { viewModel.onRecentItemClick(it) },
            onPinClick = { viewModel.onPinRecentItem(it) }
        )
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

    var showReminderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.recordForReminderDialog) {
        showReminderDialog = false
        if (uiState.recordForReminderDialog != null) {
            withFrameNanos { }
            showReminderDialog = true
        }
    }

    if (showReminderDialog && uiState.recordForReminderDialog != null) {
        com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog(
            onDismiss = viewModel::onReminderDialogDismiss,
            onSetReminder = viewModel::onSetReminder,
            onRemoveReminder = viewModel::onRemoveReminder,
            currentReminders = uiState.remindersForDialog,
        )
    }


}
