package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogPasteMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionDialogState
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.features.reminders.dialogs.RemindersDialog
import com.romankozak.forwardappmobile.features.reminders.viewmodel.ReminderViewModel
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailDialogs(viewModel: ContextScreenViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val goalActionState by viewModel.itemActionHandler.goalActionDialogState.collectAsStateWithLifecycle()
    val showGoalTransportMenu by viewModel.itemActionHandler.showGoalTransportMenu.collectAsStateWithLifecycle()
    val showPasteModeDialog by viewModel.itemActionHandler.showPasteModeDialog.collectAsStateWithLifecycle()
    val showAttachmentPasteDialog by viewModel.itemActionHandler.showAttachmentPasteDialog.collectAsStateWithLifecycle()
    val recentItems = uiState.recentItems

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
            onOpenRemindersDialog = {
                viewModel.onOpenRemindersDialog(itemContent)
            },
        )
    }

    if (uiState.showRemindersDialog) {
        val remindersViewModel: ReminderViewModel = hiltViewModel()
        uiState.itemForRemindersDialog?.let {
            RemindersDialog(
                viewModel = remindersViewModel,
                item = it,
                onDismiss = { viewModel.onDismissRemindersDialog() },
            )
        }
    }

    GoalTransportMenu(
        isVisible = showGoalTransportMenu,
        onDismiss = { viewModel.itemActionHandler.onDismissGoalTransportMenu() },
        onCopyRequest = { viewModel.itemActionHandler.onTransportCopyRequested() },
        onCutRequest = { viewModel.itemActionHandler.onTransportCutRequested() },
    )

    if (showPasteModeDialog) {
        var selectedMode by remember { mutableStateOf(BacklogPasteMode.AS_LINK) }
        var addSourceContextLink by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { viewModel.itemActionHandler.onDismissPasteModeDialog() },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.itemActionHandler.onPasteModeSelected(
                            mode = selectedMode,
                            addSourceContextLink = selectedMode == BacklogPasteMode.AS_LINK && addSourceContextLink,
                        )
                    },
                ) {
                    Text("Вставити")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.itemActionHandler.onDismissPasteModeDialog() },
                ) {
                    Text("Скасувати")
                }
            },
            title = { Text("Режим вставки") },
            text = {
                Column {
                    Text("Вставити цілі як посилання чи створити клон?")
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMode == BacklogPasteMode.AS_LINK,
                            onClick = { selectedMode = BacklogPasteMode.AS_LINK },
                        )
                        Text("Як посилання")
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedMode == BacklogPasteMode.AS_CLONE,
                            onClick = {
                                selectedMode = BacklogPasteMode.AS_CLONE
                                addSourceContextLink = false
                            },
                        )
                        Text("Клонувати")
                    }
                    if (selectedMode == BacklogPasteMode.AS_LINK) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(
                                checked = addSourceContextLink,
                                onCheckedChange = { addSourceContextLink = it },
                            )
                            Text("Додати посилання на вихідний контекст")
                        }
                    }
                }
            },
        )
    }

    if (showAttachmentPasteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.itemActionHandler.onDismissAttachmentPasteDialog() },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.itemActionHandler.onAttachmentPasteDecision(includeAttachments = true) },
                ) {
                    Text("Так, як вкладення")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.itemActionHandler.onAttachmentPasteDecision(includeAttachments = false) },
                ) {
                    Text("Ні, пропустити")
                }
            },
            title = { Text("Вставка вкладень") },
            text = { Text("У буфері є вкладення. Додати їх у секцію вкладень поточного контексту?") },
        )
    }

    if (uiState.showRecentProjectsSheet) {
        NewRecentListsSheet(
            showSheet = uiState.showRecentProjectsSheet,
            recentItems = recentItems,
            onDismiss = { viewModel.inputHandler.onDismissRecentLists() },
            onItemClick = { viewModel.onRecentItemClick(it) },
            onPinClick = { viewModel.onPinRecentItem(it) },
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
            onDismiss = viewModel::onDismissImportBacklogFromMarkdownDialog,
            onConfirm = viewModel::onImportBacklogFromMarkdown,
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
        ReminderPropertiesDialog(
            onDismiss = viewModel::onDismissRemindersDialog,
            onSetReminder = viewModel::onSetReminder,
            onRemoveReminder = viewModel::onRemoveReminder,
            currentReminders = uiState.remindersForDialog,
        )
    }
}
