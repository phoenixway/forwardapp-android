package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog
import com.romankozak.forwardappmobile.ui.dialogs.AddProjectDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import com.romankozak.forwardappmobile.ui.screens.mainscreen.dialogs.ContextMenuDialog
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DialogState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenUiState


@Composable
fun HandleProjectHierarchyDialogs(
    uiState: ProjectHierarchyScreenUiState,
    onEvent: (ProjectHierarchyScreenEvent) -> Unit,
) {
    
    when (val state = uiState.dialogState) {
        is DialogState.Hidden -> { }
        is DialogState.AddProject -> {
            AddProjectDialog(
                title = if (state.parentId == null) "Створити новий проект" else "Створити підпроект",
                onDismiss = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) },
                
                onConfirm = { name ->
                    onEvent(ProjectHierarchyScreenEvent.AddProjectConfirm(name, state.parentId))
                },
            )
        }
        
        is DialogState.ProjectMenu -> {
            ContextMenuDialog(
                project = state.project,
                onDismissRequest = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) },
                onMoveRequest = { project -> onEvent(ProjectHierarchyScreenEvent.MoveRequest(project)) },
                onAddSubprojectRequest = { project -> onEvent(ProjectHierarchyScreenEvent.AddSubprojectRequest(project)) },
                onDeleteRequest = { project -> onEvent(ProjectHierarchyScreenEvent.DeleteRequest(project)) },
                onEditRequest = { project -> onEvent(ProjectHierarchyScreenEvent.EditRequest(project)) },
                onAddToDayPlanRequest = { project -> onEvent(ProjectHierarchyScreenEvent.AddToDayPlanRequest(project)) },
                onSetReminderRequest = { project -> onEvent(ProjectHierarchyScreenEvent.SetReminderRequest(project)) },
                onFocusRequest = { project -> onEvent(ProjectHierarchyScreenEvent.FocusProject(project)) },
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) },
                title = { Text("Delete project?") },
                text = {
                    Text(
                        "Are you sure you want to delete '${state.project.name}' and all its contents? This action cannot be undone.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(ProjectHierarchyScreenEvent.DeleteConfirm(state.project)) }) {
                        Text("Delete")
                    }
                },
                dismissButton = { TextButton(onClick = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }
        
        is DialogState.About -> {
            AboutAppDialog(
                stats = uiState.appStatistics,
                onDismiss = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) },
            )
        }
        
        is DialogState.ConfirmImport -> {
            AlertDialog(
                onDismissRequest = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) },
                title = { Text("Restore from backup?") },
                text = {
                    Text(
                        "WARNING: All current data will be deleted and replaced with data from the backup file. This action cannot be undone.",
                    )
                },
                confirmButton = {
                    Button(
                        
                        onClick = { onEvent(ProjectHierarchyScreenEvent.FullImportConfirm(state.uri)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete and Restore") }
                },
                dismissButton = { TextButton(onClick = { onEvent(ProjectHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }
        
        is DialogState.EditProject -> {
            
            
            onEvent(ProjectHierarchyScreenEvent.DismissDialog)
        }
        is DialogState.WifiImport -> { }
        is DialogState.WifiServer -> { }
    }

    
    if (uiState.showWifiServerDialog && uiState.featureToggles[com.romankozak.forwardappmobile.config.FeatureFlag.WifiSync] == true) {
        WifiServerDialog(
            address = uiState.wifiServerAddress,
            onDismiss = { onEvent(ProjectHierarchyScreenEvent.DismissWifiServerDialog) },
        )
    }
    if (uiState.showWifiImportDialog && uiState.featureToggles[com.romankozak.forwardappmobile.config.FeatureFlag.WifiSync] == true) {
        WifiImportDialog(
            desktopAddress = uiState.desktopAddress,
            onAddressChange = { onEvent(ProjectHierarchyScreenEvent.DesktopAddressChange(it)) },
            onDismiss = { onEvent(ProjectHierarchyScreenEvent.DismissWifiImportDialog) },
            onConfirm = { onEvent(ProjectHierarchyScreenEvent.PerformWifiImport(it)) },
        )
    }
    if (uiState.showSearchDialog) {
        LaunchedEffect(uiState.showSearchDialog) {
            onEvent(ProjectHierarchyScreenEvent.SearchQueryChanged(TextFieldValue("")))
            onEvent(ProjectHierarchyScreenEvent.DismissSearchDialog)
        }
    }
}
