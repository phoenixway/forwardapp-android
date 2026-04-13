package com.romankozak.forwardappmobile.shared.application.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkspaceRecoveryStore(
    private val gateway: WorkspaceRecoveryGateway,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(WorkspaceRecoveryState())
    val state: StateFlow<WorkspaceRecoveryState> = _state.asStateFlow()

    init {
        scope.launch {
            refresh()
        }
    }

    fun onImportPathChanged(path: String) {
        _state.update { current ->
            current.copy(
                importPath = path,
                importInspection = null,
            )
        }
    }

    fun selectBackup(backupId: String) {
        _state.update { current ->
            current.copy(selectedBackupId = backupId)
        }
    }

    fun requestRestoreLatest() {
        _state.update { current ->
            current.copy(pendingAction = WorkspaceRecoveryAction.RestoreLatest)
        }
    }

    fun requestRestoreSelected() {
        val selectedBackup = state.value.selectedBackup ?: return
        _state.update { current ->
            current.copy(
                pendingAction =
                    WorkspaceRecoveryAction.RestoreSelected(
                        backupId = selectedBackup.id,
                        backupLabel = selectedBackup.label,
                    ),
            )
        }
    }

    fun requestImportSnapshot() {
        val importPath = state.value.importPath.trim()
        if (importPath.isBlank()) {
            return
        }
        _state.update { current ->
            current.copy(
                pendingAction = WorkspaceRecoveryAction.ImportSnapshot(sourcePath = importPath),
            )
        }
    }

    fun dismissPendingAction() {
        _state.update { current -> current.copy(pendingAction = null) }
    }

    fun previewImport() {
        val importPath = state.value.importPath.trim()
        if (importPath.isBlank()) {
            setMessage("No import file selected.")
            return
        }
        scope.launch {
            val inspection = gateway.inspectImport(importPath)
            _state.update { current ->
                current.copy(importInspection = inspection)
            }
            setMessage(inspection.toStatusMessage())
        }
    }

    fun exportSnapshot() {
        scope.launch {
            val exportPath = gateway.defaultExportPath()
            gateway.exportSnapshot(exportPath)
            refreshBackups()
            setMessage("Snapshot exported to $exportPath.")
        }
    }

    fun confirmPendingAction() {
        val action = state.value.pendingAction ?: return
        dismissPendingAction()
        scope.launch {
            when (action) {
                WorkspaceRecoveryAction.RestoreLatest -> {
                    val restored = gateway.restoreLatestBackup()
                    refreshBackups()
                    setMessage(
                        if (restored) {
                            "Latest backup restored into current workspace snapshot."
                        } else {
                            "No backups available to restore."
                        },
                    )
                }

                is WorkspaceRecoveryAction.RestoreSelected -> {
                    val restored = gateway.restoreBackup(action.backupId)
                    refreshBackups()
                    setMessage(
                        if (restored) {
                            "Selected backup restored into current workspace snapshot."
                        } else {
                            "Select a valid backup file before restore."
                        },
                    )
                }

                is WorkspaceRecoveryAction.ImportSnapshot -> {
                    val result = gateway.importSnapshot(action.sourcePath)
                    refreshBackups()
                    setMessage(result.toStatusMessage())
                }
            }
        }
    }

    suspend fun refresh() {
        val backups = gateway.listBackups()
        val workspacePath = gateway.workspacePath()
        val defaultExportPath = gateway.defaultExportPath()
        _state.update { current ->
            current.copy(
                workspacePath = workspacePath,
                defaultExportPath = defaultExportPath,
                backups = backups,
                selectedBackupId = current.selectedBackupId.takeIf { selected -> backups.any { it.id == selected } } ?: backups.firstOrNull()?.id,
            )
        }
    }

    private suspend fun refreshBackups() {
        val backups = gateway.listBackups()
        _state.update { current ->
            current.copy(
                backups = backups,
                selectedBackupId = backups.firstOrNull()?.id,
            )
        }
    }

    private fun setMessage(message: String) {
        _state.update { current -> current.copy(lastActionMessage = message) }
    }
}

data class WorkspaceRecoveryState(
    val workspacePath: String = "",
    val defaultExportPath: String = "",
    val backups: List<WorkspaceBackupEntry> = emptyList(),
    val selectedBackupId: String? = null,
    val importPath: String = "",
    val importInspection: WorkspaceImportInspection? = null,
    val pendingAction: WorkspaceRecoveryAction? = null,
    val lastActionMessage: String = "No recovery action executed yet.",
) {
    val backupCount: Int get() = backups.size
    val selectedBackup: WorkspaceBackupEntry? get() = backups.firstOrNull { it.id == selectedBackupId }
    val canImport: Boolean get() = importInspection is WorkspaceImportInspection.Valid
}

data class WorkspaceBackupEntry(
    val id: String,
    val label: String,
    val lastModifiedLabel: String,
    val sizeLabel: String,
)

sealed interface WorkspaceImportInspection {
    val sourcePath: String

    data class Valid(
        override val sourcePath: String,
        val format: WorkspaceSnapshotFormat,
        val contextsCount: Int,
        val backlogItemsCount: Int,
    ) : WorkspaceImportInspection

    data class FileNotFound(
        override val sourcePath: String,
    ) : WorkspaceImportInspection

    data class Invalid(
        override val sourcePath: String,
    ) : WorkspaceImportInspection
}

sealed interface WorkspaceImportResult {
    val sourcePath: String

    data class Success(
        override val sourcePath: String,
        val format: WorkspaceSnapshotFormat,
    ) : WorkspaceImportResult

    data class FileNotFound(
        override val sourcePath: String,
    ) : WorkspaceImportResult

    data class Invalid(
        override val sourcePath: String,
    ) : WorkspaceImportResult
}

sealed interface WorkspaceRecoveryAction {
    val title: String
    val message: String
    val confirmLabel: String

    data object RestoreLatest : WorkspaceRecoveryAction {
        override val title: String = "Restore Latest Backup"
        override val message: String = "Current workspace snapshot will be replaced by the most recent backup. A fresh backup of the current state will be created first."
        override val confirmLabel: String = "Restore"
    }

    data class RestoreSelected(
        val backupId: String,
        val backupLabel: String,
    ) : WorkspaceRecoveryAction {
        override val title: String = "Restore Selected Backup"
        override val message: String = "Current workspace snapshot will be replaced by $backupLabel. A fresh backup of the current state will be created first."
        override val confirmLabel: String = "Restore Selected"
    }

    data class ImportSnapshot(
        val sourcePath: String,
    ) : WorkspaceRecoveryAction {
        override val title: String = "Import Snapshot"
        override val message: String = "Current workspace snapshot will be replaced by $sourcePath. The import file has already passed validation, and a fresh backup of the current state will be created first."
        override val confirmLabel: String = "Import"
    }
}

interface WorkspaceRecoveryGateway {
    suspend fun workspacePath(): String

    suspend fun defaultExportPath(): String

    suspend fun listBackups(): List<WorkspaceBackupEntry>

    suspend fun inspectImport(sourcePath: String): WorkspaceImportInspection

    suspend fun importSnapshot(sourcePath: String): WorkspaceImportResult

    suspend fun restoreLatestBackup(): Boolean

    suspend fun restoreBackup(backupId: String): Boolean

    suspend fun exportSnapshot(targetPath: String)
}

private fun WorkspaceImportInspection.toStatusMessage(): String =
    when (this) {
        is WorkspaceImportInspection.Valid ->
            "Import preview ready: $contextsCount contexts, $backlogItemsCount backlog items from ${format.title}."

        is WorkspaceImportInspection.FileNotFound ->
            "Import file was not found: $sourcePath."

        is WorkspaceImportInspection.Invalid ->
            "Import file is not a supported desktop or Android snapshot: $sourcePath."
    }

private fun WorkspaceImportResult.toStatusMessage(): String =
    when (this) {
        is WorkspaceImportResult.Success ->
            "Snapshot imported from $sourcePath (${format.title})."

        is WorkspaceImportResult.FileNotFound ->
            "Import file was not found: $sourcePath."

        is WorkspaceImportResult.Invalid ->
            "Import file is not a supported desktop or Android snapshot: $sourcePath."
    }
