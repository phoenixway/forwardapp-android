package com.romankozak.forwardappmobile.desktop.features.settings

import com.romankozak.forwardappmobile.desktop.data.contexts.DesktopWorkspaceFileStore
import com.romankozak.forwardappmobile.desktop.data.contexts.ImportResult
import com.romankozak.forwardappmobile.desktop.data.contexts.SnapshotInspectionResult
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceBackupEntry
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceImportInspection
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceImportResult
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceRecoveryGateway
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime

class DesktopWorkspaceRecoveryGateway(
    private val fileStore: DesktopWorkspaceFileStore,
) : WorkspaceRecoveryGateway {
    override suspend fun workspacePath(): String = fileStore.workspacePath().toString()

    override suspend fun defaultExportPath(): String = fileStore.defaultExportPath().toString()

    override suspend fun listBackups(): List<WorkspaceBackupEntry> =
        fileStore.listBackups().map { backup ->
            WorkspaceBackupEntry(
                id = backup.toString(),
                label = backup.fileName.toString(),
                lastModifiedLabel = backup.getLastModifiedTimeSafe(),
                sizeLabel = backup.fileSizeSafe(),
            )
        }

    override suspend fun inspectImport(sourcePath: String): WorkspaceImportInspection {
        val result = fileStore.inspectSnapshot(Paths.get(sourcePath))
        return when (result) {
            is SnapshotInspectionResult.Valid ->
                WorkspaceImportInspection.Valid(
                    sourcePath = result.sourceFile.toString(),
                    format = result.format,
                    contextsCount = result.contextsCount,
                    backlogItemsCount = result.backlogItemsCount,
                )

            is SnapshotInspectionResult.FileNotFound ->
                WorkspaceImportInspection.FileNotFound(result.sourceFile.toString())

            is SnapshotInspectionResult.Invalid ->
                WorkspaceImportInspection.Invalid(result.sourceFile.toString())
        }
    }

    override suspend fun importSnapshot(sourcePath: String): WorkspaceImportResult {
        val result = fileStore.importSnapshot(Paths.get(sourcePath))
        return when (result) {
            is ImportResult.Success ->
                WorkspaceImportResult.Success(
                    sourcePath = result.sourceFile.toString(),
                    format = result.format,
                )

            is ImportResult.FileNotFound ->
                WorkspaceImportResult.FileNotFound(result.sourceFile.toString())

            is ImportResult.InvalidSnapshot ->
                WorkspaceImportResult.Invalid(result.sourceFile.toString())
        }
    }

    override suspend fun restoreLatestBackup(): Boolean = fileStore.restoreLatestBackup()

    override suspend fun restoreBackup(backupId: String): Boolean = fileStore.restoreBackup(Paths.get(backupId))

    override suspend fun exportSnapshot(targetPath: String) {
        fileStore.exportSnapshot(Paths.get(targetPath))
    }
}

private fun Path.getLastModifiedTimeSafe(): String =
    runCatching {
        val fileTime: FileTime = getLastModifiedTime()
        BACKUP_TIME_FORMATTER.format(fileTime.toInstant().atZone(ZoneId.systemDefault()))
    }.getOrElse { "Unknown time" }

private fun Path.fileSizeSafe(): String =
    runCatching {
        val bytes = fileSize()
        when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }.getOrElse { "Unknown size" }

private val BACKUP_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
