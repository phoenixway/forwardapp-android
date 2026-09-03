package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSectionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalBacklogSelectiveImportViewModelProjectionTest {
    @Test
    fun `canonical Backlog preview and selection use placement identity`() {
        val placement =
            WorkspaceBacklogEntrySnapshot(
                "placement-id", "workspace", "capability", "ORIENTATION", "subject-id",
                0L, 1L, 2L, 3L, false,
            )
        val content =
            SelectableDatabaseContent(
                workspaceBacklogEntries =
                    listOf(
                        SelectableDiffItem(
                            CanonicalBacklogPreviewRow(placement, "Goal", "Workspace · ORIENTATION"),
                            DiffStatus.NEW,
                            isSelected = true,
                        ),
                    ),
            )

        val section =
            content.toWorkspaceImportPreviewModel().sections.single {
                it.kind == WorkspaceImportPreviewSectionKind.Backlog
            }

        assertEquals(WorkspaceImportPreviewSectionKind.Backlog, section.kind)
        assertEquals("placement-id", section.items.single().id)
        assertEquals(
            setOf("placement-id"),
            content.toWorkspaceSelectiveImportSelection().selectedWorkspaceBacklogEntryIds,
        )
        assertEquals(1, content.toWorkspaceImportPreviewSummary().totalSelectedCount)
    }

    @Test
    fun `absent canonical Backlog rows do not produce preview section`() {
        val sections = SelectableDatabaseContent().toWorkspaceImportPreviewModel().sections

        assertTrue(sections.none { it.kind == WorkspaceImportPreviewSectionKind.Backlog })
    }
}
