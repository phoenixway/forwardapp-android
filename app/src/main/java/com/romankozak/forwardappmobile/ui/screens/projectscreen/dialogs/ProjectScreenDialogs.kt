package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RecentItemType
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionDialogState
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.RemindersViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.CreateCustomListDialog

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
        val remindersViewModel: RemindersViewModel = hiltViewModel()
        uiState.itemForRemindersDialog?.let {
            RemindersDialog(
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

    uiState.recordForReminderDialog?.let { record ->
        ReminderPickerDialog(
            onDismiss = viewModel::onReminderDialogDismiss,
            onSetReminder = viewModel::onSetReminder,
            onRemoveReminder = { time -> viewModel.onRemoveReminder(time) },
            currentReminderTimes = uiState.remindersForDialog.map { it.reminderTime },
        )
    }

    if (uiState.showCreateCustomListDialog) {
        CreateCustomListDialog(
            onDismiss = viewModel::onDismissCreateCustomListDialog,
            onConfirm = viewModel::onCreateCustomList,
        )
    }
}