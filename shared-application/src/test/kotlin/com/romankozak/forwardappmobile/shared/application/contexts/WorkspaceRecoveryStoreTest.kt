package com.romankozak.forwardappmobile.shared.application.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceRecoveryStoreTest {
    @Test
    fun previewImportAndConfirmImportFlowUsesGatewayAndRefreshesState() =
        runTest {
            val gateway = FakeWorkspaceRecoveryGateway()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val store = WorkspaceRecoveryStore(gateway = gateway, scope = TestScope(dispatcher))

            advanceUntilIdle()
            store.onImportPathChanged("/tmp/android-backup.json")
            store.previewImport()
            advanceUntilIdle()

            val previewState = store.state.value
            assertTrue(previewState.importInspection is WorkspaceImportInspection.Valid)
            assertTrue(previewState.lastActionMessage.contains("Import preview ready"))

            store.requestImportSnapshot()
            assertTrue(store.state.value.pendingAction is WorkspaceRecoveryAction.ImportSnapshot)

            store.confirmPendingAction()
            advanceUntilIdle()

            val finalState = store.state.value
            assertEquals(1, gateway.importCalls)
            assertEquals(2, finalState.backupCount)
            assertTrue(finalState.lastActionMessage.contains("Snapshot imported"))
        }

    private class FakeWorkspaceRecoveryGateway : WorkspaceRecoveryGateway {
        var importCalls: Int = 0

        override suspend fun workspacePath(): String = "/workspace/current.json"

        override suspend fun defaultExportPath(): String = "/workspace/export.json"

        override suspend fun listBackups(): List<WorkspaceBackupEntry> =
            listOf(
                WorkspaceBackupEntry("backup-1", "backup-1.json", "2026-01-01 10:00:00", "10 KB"),
                WorkspaceBackupEntry("backup-2", "backup-2.json", "2026-01-01 09:00:00", "8 KB"),
            )

        override suspend fun inspectImport(sourcePath: String): WorkspaceImportInspection =
            WorkspaceImportInspection.Valid(
                sourcePath = sourcePath,
                format = WorkspaceSnapshotFormat.AndroidSnapshotBundleV2,
                contextsCount = 3,
                backlogItemsCount = 5,
            )

        override suspend fun importSnapshot(sourcePath: String): WorkspaceImportResult {
            importCalls += 1
            return WorkspaceImportResult.Success(
                sourcePath = sourcePath,
                format = WorkspaceSnapshotFormat.AndroidSnapshotBundleV2,
            )
        }

        override suspend fun restoreLatestBackup(): Boolean = true

        override suspend fun restoreBackup(backupId: String): Boolean = true

        override suspend fun exportSnapshot(targetPath: String) = Unit
    }
}
