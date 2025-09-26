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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RecentItemType
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionDialogState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType

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
        )
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
        ModalBottomSheet(onDismissRequest = { viewModel.inputHandler.onDismissRecentLists() }) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Нещодавно відкриті",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (recentItems.isEmpty()) {
                    Text(
                        text = "Історія порожня.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    val pinnedItems = recentItems.filter { it.isPinned }
                    val regularItems = recentItems.filter { !it.isPinned }

                    LazyColumn {
                        if (pinnedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Закріплені",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(pinnedItems, key = { "pinned-${it.id}" }) { item ->
                                RecentItemEntry(viewModel = viewModel, item = item)
                            }
                        }

                        if (regularItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Недавні",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(regularItems, key = { "recent-${it.id}" }) { item ->
                                RecentItemEntry(viewModel = viewModel, item = item)
                            }
                        }
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
            onClearReminder =
                if (record.reminderTime != null) {
                    { viewModel.onClearReminder() }
                } else {
                    null
                },
            currentReminderTime = record.reminderTime,
        )
    }
}

@Composable
private fun RecentItemEntry(viewModel: BacklogViewModel, item: com.romankozak.forwardappmobile.data.database.models.RecentItem) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(item.displayName) },
        leadingContent = {
            val icon = when (item.type) {
                RecentItemType.PROJECT -> Icons.Outlined.Folder
                RecentItemType.NOTE -> Icons.Outlined.Note
                RecentItemType.CUSTOM_LIST -> Icons.Outlined.List
                RecentItemType.OBSIDIAN_LINK -> Icons.Outlined.Link
            }
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (item.isPinned) "Відкріпити" else "Закріпити") },
                        onClick = {
                            viewModel.onPinRecentItem(item)
                            menuExpanded = false
                        }
                    )
                }
            }
        },
        modifier = Modifier.clickable { viewModel.inputHandler.onRecentListSelected(item) },
    )
}