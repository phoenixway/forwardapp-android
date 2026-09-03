package com.romankozak.forwardappmobile.features.sync.selectiveimport

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
