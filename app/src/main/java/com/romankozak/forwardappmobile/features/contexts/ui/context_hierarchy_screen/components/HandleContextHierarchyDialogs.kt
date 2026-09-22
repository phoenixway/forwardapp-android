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
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.dialogs.ContextMigrationDialog
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
                    onEvent(
                        ContextHierarchyScreenEvent.AddContextConfirm(
                            name = name,
                            parentId = state.parentId,
                            parentPlacementId = state.parentPlacementId,
                            roleCode = roleCode,
                        ),
                    )
                },
            )
        }

        is DialogState.ProjectMenu -> {
            ContextMenuDialog(
                projectId = state.projectId,
                projectName = state.projectName,
                isUserFocused = focusedContextIds.contains(state.projectId),
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                onMoveRequest = {
                    onEvent(ContextHierarchyScreenEvent.MoveRequest(state.projectId, state.occurrence))
                },
                onAddSubprojectRequest = {
                    onEvent(
                        ContextHierarchyScreenEvent.AddSubprojectRequest(
                            parentProjectId = state.projectId,
                            parentOccurrence = state.occurrence,
                        ),
                    )
                },
                onDeleteRequest = {
                    onEvent(ContextHierarchyScreenEvent.DeleteRequest(state.projectId, state.occurrence))
                },
                onEditRequest = {
                    onEvent(ContextHierarchyScreenEvent.EditRequest(state.projectId))
                },
                onOpenContextRequest = {
                    onEvent(ContextHierarchyScreenEvent.OpenContextRequest(state.projectId))
                },
                onAddToDayPlanRequest = {
                    onEvent(ContextHierarchyScreenEvent.AddToDayPlanRequest(state.projectId))
                },
                onAddToDayFocusRequest = {
                    onEvent(ContextHierarchyScreenEvent.AddToDayFocusRequest(state.projectId))
                },
                onSetReminderRequest = {
                    onEvent(ContextHierarchyScreenEvent.SetReminderRequest(state.projectId))
                },
                onToggleUserFocusRequest = {
                    onEvent(ContextHierarchyScreenEvent.ToggleUserFocusContext(state.projectId))
                },
                onCopyWorkspaceRequest = {
                    onEvent(ContextHierarchyScreenEvent.CopyWorkspace(state.projectId, state.occurrence))
                },
                onCutWorkspaceRequest = {
                    onEvent(ContextHierarchyScreenEvent.CutWorkspace(state.projectId, state.occurrence))
                },
                onPasteWorkspaceRequest = {
                    onEvent(ContextHierarchyScreenEvent.PasteWorkspace(state.projectId, state.occurrence))
                },
                onCopyContextLinkRequest = {
                    onEvent(ContextHierarchyScreenEvent.CopyContextLink(state.projectId, state.occurrence))
                },
                onCutContextLinkRequest = {
                    onEvent(ContextHierarchyScreenEvent.CutContextLink(state.projectId, state.occurrence))
                },
                onPasteContextLinkRequest = {
                    onEvent(ContextHierarchyScreenEvent.PasteContextLink(state.projectId, destinationOccurrence = state.occurrence))
                },
                onAddContextAppearanceRequest = {
                    onEvent(ContextHierarchyScreenEvent.AddContextAppearanceHere(state.projectId, state.occurrence))
                },
                onAddNoteDocumentRequest = {
                    onEvent(ContextHierarchyScreenEvent.AddNoteDocumentToContextRequest(state.projectId))
                },
                onAddChecklistRequest = {
                    onEvent(ContextHierarchyScreenEvent.AddChecklistToContextRequest(state.projectId))
                },
                onMigrateRequest = {
                    onEvent(ContextHierarchyScreenEvent.MigrateRequest(state.projectId))
                },
                availability = state.availability,
            )
        }
        is DialogState.ContextMigration -> {
            ContextMigrationDialog(
                state = state,
                onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                onChoice = { onEvent(ContextHierarchyScreenEvent.MigrationChoiceSelected(it)) },
                onOrientationKind = { onEvent(ContextHierarchyScreenEvent.MigrationOrientationKindSelected(it)) },
                onExistingAspect = { onEvent(ContextHierarchyScreenEvent.MigrationExistingAspectSelected(it)) },
                onExistingOrientation = { onEvent(ContextHierarchyScreenEvent.MigrationExistingOrientationSelected(it)) },
                onRequestConfirmation = { onEvent(ContextHierarchyScreenEvent.MigrationConfirmationRequested) },
                onExecute = { onEvent(ContextHierarchyScreenEvent.MigrationExecute) },
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                title = { Text("Delete project?") },
                text = {
                    Text(
                        "Are you sure you want to delete '${state.projectName}' and all its contents? This action cannot be undone.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DeleteConfirm(state.projectId)) }) {
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

        is DialogState.ConfirmRestore -> {
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
                        onClick = { onEvent(ContextHierarchyScreenEvent.RestoreConfirm(state.uri)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete and Restore") }
                },
                dismissButton = { TextButton(onClick = { onEvent(ContextHierarchyScreenEvent.DismissDialog) }) { Text("Cancel") } },
            )
        }

        is DialogState.ImportModeChoice -> {
            AlertDialog(
                onDismissRequest = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                title = { Text("Import backup") },
                text = {
                    Text(
                        "Restore replaces all current data with the backup. " +
                            "Merge keeps current data and applies supported data from the backup.",
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                onEvent(
                                    ContextHierarchyScreenEvent.RestoreImportRequest(state.uri),
                                )
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text("Restore")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onEvent(ContextHierarchyScreenEvent.MergeConfirm(state.uri))
                            },
                        ) {
                            Text("Merge")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(ContextHierarchyScreenEvent.DismissDialog) },
                    ) {
                        Text("Cancel")
                    }
                },
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
