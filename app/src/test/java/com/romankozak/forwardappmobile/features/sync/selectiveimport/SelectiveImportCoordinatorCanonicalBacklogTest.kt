package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.sync.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectiveImportCoordinatorCanonicalBacklogTest {
    @Test
    fun `coordinator forwards canonical placement selection to SnapshotBundle filter`() = runBlocking {
        val repository = mockk<SyncRepository>()
        val capturedSelection = slot<WorkspaceSelectiveImportSelection>()
        val source = SnapshotBundle(version = 2)
        val filtered = SnapshotBundle(version = 2, workspaceBacklogEntries = listOf(placement()))
        every {
            repository.filterSnapshotBundleForSelectiveImport(source, capture(capturedSelection))
        } returns filtered
        coEvery { repository.importSelectedSnapshotBundle(filtered) } returns Result.success("imported")

        val result = SelectiveImportCoordinator(repository).importSelection(state(source))

        assertTrue(result.isSuccess)
        assertEquals(setOf("placement"), capturedSelection.captured.selectedWorkspaceBacklogEntryIds)
        coVerify(exactly = 1) { repository.importSelectedSnapshotBundle(filtered) }
    }

    @Test
    fun `selected System owned Script is forwarded without selecting a legacy Context shell`() =
        runBlocking {
            val repository = mockk<SyncRepository>()
            val capturedSelection = slot<WorkspaceSelectiveImportSelection>()
            val source = SnapshotBundle(version = 2)

            every {
                repository.filterSnapshotBundleForSelectiveImport(
                    source,
                    capture(capturedSelection),
                )
            } returns source
            coEvery {
                repository.importSelectedSnapshotBundle(source)
            } returns Result.success("imported")

            val script =
                ScriptEntity(
                    id = "script-system",
                    contextId = SystemContexts.INBOX.raw,
                    name = "System script",
                    content = "echo test",
                )
            val state =
                SelectiveImportState(
                    backupContent =
                        SelectableDatabaseContent(
                            scripts =
                                listOf(
                                    SelectableDiffItem(
                                        item = script,
                                        status = DiffStatus.NEW,
                                        isSelected = true,
                                    ),
                                ),
                        ),
                    sourceSnapshotBundle = source,
                )

            val result = SelectiveImportCoordinator(repository).importSelection(state)

            assertTrue(result.isSuccess)
            assertEquals(
                setOf("script-system"),
                capturedSelection.captured.selectedScriptIds,
            )
            assertTrue(capturedSelection.captured.selectedContextIds.isEmpty())
        }

    @Test
    fun `coordinator returns filtering failure without invoking import`() = runBlocking {
        val repository = mockk<SyncRepository>()
        val source = SnapshotBundle(version = 2)
        val failure = IllegalArgumentException("missing canonical placement")
        every {
            repository.filterSnapshotBundleForSelectiveImport(source, any())
        } throws failure

        val result = SelectiveImportCoordinator(repository).importSelection(state(source))

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        coVerify(exactly = 0) { repository.importSelectedSnapshotBundle(any()) }
    }

    private fun state(source: SnapshotBundle) =
        SelectiveImportState(
            backupContent =
                SelectableDatabaseContent(
                    workspaceBacklogEntries =
                        listOf(
                            SelectableDiffItem(
                                item = CanonicalBacklogPreviewRow(placement(), "Target", "Workspace · CHECKLIST"),
                                status = DiffStatus.NEW,
                                isSelected = true,
                            ),
                        ),
                ),
            sourceSnapshotBundle = source,
        )

    private fun placement() =
        WorkspaceBacklogEntrySnapshot(
            id = "placement",
            workspaceId = "workspace",
            capabilityInstanceId = "backlog-capability",
            targetKind = "CHECKLIST",
            targetId = "checklist",
            order = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            version = 1L,
            isDeleted = false,
        )
}
