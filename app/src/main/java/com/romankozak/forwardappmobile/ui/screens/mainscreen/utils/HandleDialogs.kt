package com.romankozak.forwardappmobile.ui.screens.mainscreen.utils

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog
import com.romankozak.forwardappmobile.ui.dialogs.AddProjectDialog
import com.romankozak.forwardappmobile.ui.dialogs.GlobalSearchDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import com.romankozak.forwardappmobile.ui.screens.mainscreen.dialogs.ContextMenuDialog
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DialogState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState

/**
 * A central composable to handle displaying different dialogs based on the DialogState from the UiState.
 * It uses the onEvent lambda to send user actions back to the ViewModel.
 */
@Composable
fun HandleDialogs(
    uiState: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    // Handle state-based dialogs from the DialogState object
    when (val state = uiState.dialogState) {
        is DialogState.Hidden -> { /* Do nothing */ }
        is DialogState.AddProject -> {
            AddProjectDialog(
                title = if (state.parentId == null) "Create new project" else "Create subproject",
                onDismiss = { onEvent(MainScreenEvent.DismissDialog) },
                onConfirm = { name ->
                    // In a full implementation, you would create a dedicated event
                    // onEvent(MainScreenEvent.AddProjectConfirm(UUID.randomUUID().toString(), state.parentId, name))
                    onEvent(MainScreenEvent.DismissDialog) // For now, just dismiss
                },
            )
        }
        // FIXED: Use the correct state name 'ProjectMenu'
        is DialogState.ProjectMenu -> {
            ContextMenuDialog(
                project = state.project,
                onDismissRequest = { onEvent(MainScreenEvent.DismissDialog) },
                onMoveRequest = { onEvent(MainScreenEvent.MoveRequest(it)) },
                onAddSubprojectRequest = { onEvent(MainScreenEvent.AddSubprojectRequest(it)) },
                onDeleteRequest = { onEvent(MainScreenEvent.DeleteRequest(it)) },
                onEditRequest = { onEvent(MainScreenEvent.EditRequest(it)) },
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { onEvent(MainScreenEvent.DismissDialog) },
                title = { Text("Delete project?") },
                text = { Text("Are you sure you want to delete '${state.project.name}' and all its contents? This action cannot be undone.") },
                confirmButton = { Button(onClick = { onEvent(MainScreenEvent.DeleteConfirm(state.project)) }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { onEvent(MainScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }
        // FIXED: Use the correct state name 'About'
        is DialogState.About -> {
            AboutAppDialog(
                stats = uiState.appStatistics,
                onDismiss = { onEvent(MainScreenEvent.DismissDialog) }
            )
        }
        // FIXED: Use the correct state name 'ConfirmImport'
        is DialogState.ConfirmImport -> {
            AlertDialog(
                onDismissRequest = { onEvent(MainScreenEvent.DismissDialog) },
                title = { Text("Restore from backup?") },
                text = { Text("WARNING: All current data will be deleted and replaced with data from the backup file. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        // This now resolves correctly because 'state' is smart-cast to ConfirmImport
                        onClick = { onEvent(MainScreenEvent.FullImportConfirm(state.uri)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete and Restore") }
                },
                dismissButton = { TextButton(onClick = { onEvent(MainScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }
        // Handle other states to make the 'when' exhaustive
        is DialogState.EditProject -> {
            // Here you would navigate to an edit screen or show an edit dialog
            // For now, we can just dismiss to handle the state
            onEvent(MainScreenEvent.DismissDialog)
        }
        is DialogState.WifiImport -> { /* Handled by boolean flag below */ }
        is DialogState.WifiServer -> { /* Handled by boolean flag below */ }
    }

    // Handle boolean-flag based dialogs
    if (uiState.showWifiServerDialog) {
        WifiServerDialog(
            address = uiState.wifiServerAddress,
            onDismiss = { onEvent(MainScreenEvent.DismissWifiServerDialog) }
        )
    }
    if (uiState.showWifiImportDialog) {
        WifiImportDialog(
            desktopAddress = uiState.desktopAddress,
            onAddressChange = { onEvent(MainScreenEvent.DesktopAddressChange(it)) },
            onDismiss = { onEvent(MainScreenEvent.DismissWifiImportDialog) },
            onConfirm = { onEvent(MainScreenEvent.PerformWifiImport(it)) },
        )
    }
    if (uiState.showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { onEvent(MainScreenEvent.DismissSearchDialog) },
            onConfirm = { onEvent(MainScreenEvent.GlobalSearchPerform(it)) },
        )
    }
}