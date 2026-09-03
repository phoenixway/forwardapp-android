package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalBacklogSelectiveImportPreviewTest {
    @Test
    fun `absent canonical field and legacy rows do not create Backlog preview`() {
        val source =
            SnapshotBundle(
                backlogItems =
                    listOf(
                        BacklogItemSnapshot("legacy", "context", "GOAL", "goal", 0L, 1L, 1L, false),
                    ),
                workspaceBacklogEntries = null,
            )

        assertTrue(source.toSelectableCanonicalBacklog().isEmpty())
    }

    @Test
    fun `canonical preview uses placement identity and keeps tombstones selectable`() {
        val live = entry("placement-live", false)
        val deleted = entry("placement-deleted", true)
        val rows = source(listOf(live, deleted)).toSelectableCanonicalBacklog()

        assertEquals(listOf("placement-live", "placement-deleted"), rows.map { it.item.entry.id })
        assertTrue(rows.all { it.isSelectable })
        assertTrue(rows.single { it.item.entry.id == deleted.id }.item.entry.isDeleted)
    }

    @Test
    fun `malformed canonical owner remains visible but cannot be selected`() {
        val row = source(listOf(entry("placement")), capabilityWorkspaceId = "other").toSelectableCanonicalBacklog().single()

        assertFalse(row.isSelectable)
        assertFalse(row.isSelected)
        assertTrue(row.changeInfo.orEmpty().contains("another Workspace"))
    }

    private fun source(
        entries: List<WorkspaceBacklogEntrySnapshot>,
        capabilityWorkspaceId: String = "workspace",
    ) =
        SnapshotBundle(
            managedSubjects = emptyList(),
            orientations = emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces =
                listOf(
                    WorkspaceEntity(
                        "workspace", "Owner", null, null, null, 0L, 1L, 1L, null, false, 1L,
                        "CANONICAL_ONLY", null,
                    ),
                ),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances =
                listOf(
                    WorkspaceCapabilityInstanceEntity(
                        "capability", capabilityWorkspaceId, "BACKLOG", "default", 0L, "ACTIVE", 1,
                        "{}", 1L, 1L, null, false, 1L,
                    ),
                ),
            savedOrientationViews = emptyList(),
            workspaceBacklogEntries = entries,
        )

    private fun entry(id: String, deleted: Boolean = false) =
        WorkspaceBacklogEntrySnapshot(
            id, "workspace", "capability", "CHECKLIST", "checklist", 0L,
            1L, 1L, 1L, deleted,
        )
}
