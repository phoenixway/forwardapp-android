// File: com/romankozak/forwardappmobile/ui/screens/mainscreen/MainScreenHelper.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog
import com.romankozak.forwardappmobile.ui.dialogs.AddProjectDialog
import com.romankozak.forwardappmobile.ui.dialogs.GlobalSearchDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import com.romankozak.forwardappmobile.ui.screens.mainscreen.dialogs.ContextMenuDialog
import java.util.*

@Composable
fun HandleDialogs(
    dialogState: DialogState,
    viewModel: ProjectViewModel,
    listChooserFilterText: String,
    listChooserExpandedIds: Set<String>,
    filteredListHierarchyForDialog: ListHierarchyData,
) {
    val stats by viewModel.appStatistics.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    when (val state = dialogState) {
        DialogState.Hidden -> {}
        is DialogState.AddProject -> {
            AddProjectDialog(
                title = if (state.parentId == null) "Create new project" else "Create subproject",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name ->
                    val newId = UUID.randomUUID().toString()
                    viewModel.addNewProject(newId, state.parentId, name)
                    viewModel.dismissDialog()
                },
            )
        }
        is DialogState.ContextMenu -> {
            ContextMenuDialog(
                project = state.project,
                onDismissRequest = { viewModel.dismissDialog() },
                onMoveRequest = { viewModel.onMoveProjectRequest(it) },
                onAddSublistRequest = { viewModel.onAddSubprojectRequest(it) },
                onDeleteRequest = { viewModel.onDeleteRequest(it) },
                onEditRequest = { viewModel.onEditRequest(it) },
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete project?") },
                text = { Text("Are you sure you want to delete '${state.project.name}' and all its subprojects and goals? This action cannot be undone.") },
                confirmButton = { Button(onClick = { viewModel.onDeleteProjectConfirmed(state.project) }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } },
            )
        }
        is DialogState.EditProject -> {}
        is DialogState.AboutApp -> {
            AboutAppDialog(stats) { viewModel.dismissDialog() }
        }
        is DialogState.ConfirmFullImport -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Restore from backup?") },
                text = { Text("WARNING: All current data will be deleted and replaced with data from the backup file. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onFullImportConfirmed(state.uri) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete and Restore") }
                },
                dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } },
            )
        }
    }
    if (showWifiServerDialog) {
        WifiServerDialog(wifiServerAddress) { viewModel.onDismissWifiServerDialog() }
    }
    if (showWifiImportDialog) {
        val desktopAddress by viewModel.desktopAddress.collectAsState()
        WifiImportDialog(
            desktopAddress = desktopAddress,
            onAddressChange = { viewModel.onDesktopAddressChange(it) },
            onDismiss = { viewModel.onDismissWifiImportDialog() },
            onConfirm = { address -> viewModel.performWifiImport(address) },
        )
    }
    if (showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { viewModel.onDismissSearchDialog() },
            onConfirm = { query -> viewModel.onPerformGlobalSearch(query) },
        )
    }
}