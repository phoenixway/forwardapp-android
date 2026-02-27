package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.dialogs.ContextMenuDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog
import com.romankozak.forwardappmobile.ui.dialogs.AddProjectDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog

@Composable
fun HandleProjectHierarchyDialogs(
    uiState: ProjectHierarchyScreenUiState,
    focusedContextIds: Set<String>,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
) {
    when (val state = uiState.dialogState) {
        is DialogState.Hidden -> { }
        is DialogState.AddProject -> {
            AddProjectDialog(
                title = if (state.parentId == null) "Create new context" else "Add child context",
                roleOptions =
                    uiState.availableContextRoles.map { role ->
                        com.romankozak.forwardappmobile.ui.dialogs.RoleOption(
                            code = role.code,
                            label = role.label,
                        )
                    },
                preferredRoleCode = if (state.parentId != null) "others" else null,
                onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                onConfirm = { name, roleCode ->
                    onEvent(ContextHierarchyScreenEvent.AddContextConfirm(name, state.parentId, roleCode))
                },
            )
        }

        is DialogState.ProjectMenu -> {
            ContextMenuDialog(
                project = state.project,
                isUserFocused = focusedContextIds.contains(state.project.id),
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                onMoveRequest = { project -> onEvent(ContextHierarchyScreenEvent.MoveRequest(project)) },
                onAddSubprojectRequest = { project -> onEvent(ContextHierarchyScreenEvent.AddSubprojectRequest(project)) },
                onDeleteRequest = { project -> onEvent(ContextHierarchyScreenEvent.DeleteRequest(project)) },
                onEditRequest = { project -> onEvent(ContextHierarchyScreenEvent.EditRequest(project)) },
                onAddToDayPlanRequest = { project -> onEvent(ContextHierarchyScreenEvent.AddToDayPlanRequest(project)) },
                onSetReminderRequest = { project -> onEvent(ContextHierarchyScreenEvent.SetReminderRequest(project)) },
                onToggleUserFocusRequest = { project -> onEvent(ContextHierarchyScreenEvent.ToggleUserFocusContext(project)) },
                onCopyContextLinkRequest = { project -> onEvent(ContextHierarchyScreenEvent.CopyContextLink(project)) },
                onCutContextLinkRequest = { project -> onEvent(ContextHierarchyScreenEvent.CutContextLink(project)) },
                onPasteContextLinkRequest = { project -> onEvent(ContextHierarchyScreenEvent.PasteContextLink(project)) },
                canPasteContextLinks = state.canPasteContextLinks,
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                title = { Text("Delete project?") },
                text = {
                    Text(
                        "Are you sure you want to delete '${state.project.name}' and all its contents? This action cannot be undone.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DeleteConfirm(state.project)) }) {
                        Text("Delete")
                    }
                },
                dismissButton = { TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }

        is DialogState.About -> {
            AboutAppDialog(
                stats = uiState.appStatistics,
                onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
            )
        }

        is DialogState.ConfirmImport -> {
            AlertDialog(
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                title = { Text("Restore from backup?") },
                text = {
                    Text(
                        "WARNING: All current data will be deleted and replaced with data from the backup file. This action cannot be undone.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onEvent(ContextHierarchyScreenEvent.FullImportConfirm(state.uri)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete and Restore") }
                },
                dismissButton = { TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }

        is DialogState.ImportChoiceDialog -> {
            AlertDialog(
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                title = { Text("Choose Import Version") },
                text = { Text("Would you like to import a V1 (legacy) or V2 (snapshot) backup file?") },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = { onEvent(ContextHierarchyScreenEvent.FullImportConfirm(state.uri)) }) {
                            Text("Import V1")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onEvent(ContextHierarchyScreenEvent.FullImportConfirmV2(state.uri)) }) {
                            Text("Import V2")
                        }
                    }
                },
                dismissButton = { TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }

        is DialogState.ExportChoiceDialog -> {
            AlertDialog(
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                title = { Text("Choose Export Version") },
                text = { Text("Would you like to export a V1 (legacy) or V2 (snapshot) backup file?") },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = { onEvent(ContextHierarchyScreenEvent.ExportToFile) }) {
                            Text("Export V1")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onEvent(ContextHierarchyScreenEvent.ExportToFileV2) }) {
                            Text("Export V2")
                        }
                    }
                },
                dismissButton = { TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }

        is DialogState.EditProject -> {
            onEvent(ContextHierarchyScreenEvent.DismissDialog)
        }
        is DialogState.WifiImport -> { }
        is DialogState.WifiServer -> { }
    }

    if (uiState.showWifiServerDialog && uiState.featureToggles[FeatureFlag.WifiSync] == true) {
        WifiServerDialog(
            address = uiState.wifiServerAddress,
            onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissWifiServerDialog) },
        )
    }
    if (uiState.showWifiImportDialog && uiState.featureToggles[FeatureFlag.WifiSync] == true) {
        WifiImportDialog(
            desktopAddress = uiState.desktopAddress,
            onAddressChange = { onEvent(ContextHierarchyScreenEvent.DesktopAddressChange(it)) },
            onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissWifiImportDialog) },
            onConfirm = { onEvent(ContextHierarchyScreenEvent.PerformWifiImport(it)) },
        )
    }
    if (uiState.showSearchDialog) {
        LaunchedEffect(uiState.showSearchDialog) {
            onEvent(ContextHierarchyScreenEvent.SearchQueryChanged(TextFieldValue("")))
            onEvent(ContextHierarchyScreenEvent.DismissSearchDialog)
        }
    }
}
