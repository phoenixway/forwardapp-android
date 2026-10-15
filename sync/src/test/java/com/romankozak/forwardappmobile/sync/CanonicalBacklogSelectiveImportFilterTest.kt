package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalBacklogSelectiveImportFilterTest {
    private val filter = SnapshotBundleSelectiveImportFilter()

    @Test
    fun `no selected placement keeps canonical BACKLOG absent`() {
        val filtered = filter.filter(checklistSource(), WorkspaceSelectiveImportSelection(selectedGoalIds = setOf("goal")))

        assertNull(filtered.workspaceBacklogEntries)
        assertTrue(filtered.backlogItems.isEmpty())
        assertTrue(filtered.backlogOrders.isEmpty())
    }

    @Test
    fun `selection uses placement identity and includes minimal owner and checklist closure`() {
        val a = entry("placement-a")
        val b = entry("placement-b", order = 1L)
        val source = checklistSource(entries = listOf(a, b))

        val filtered = filter.filter(source, selection("placement-a"))

        assertEquals(listOf("placement-a"), filtered.workspaceBacklogEntries?.map { it.id })
        assertTrue(filtered.workspaceBacklogEntries.orEmpty().isNotEmpty())
        assertEquals(listOf("workspace-owner"), filtered.workspaces?.map { it.id })
        val owner = requireNotNull(filtered.workspaces).single()
        assertNull(owner.parentWorkspaceId)
        assertEquals(0L, owner.workspaceOrder)
        assertEquals(listOf("backlog-owner"), filtered.workspaceCapabilityInstances?.map { it.id })
        assertEquals(listOf("checklist-1"), filtered.checklists.map { it.id })
        assertEquals(listOf("checklist-item"), filtered.checklistItems.map { it.id })
        assertTrue(filtered.backlogItems.isEmpty())
        assertTrue(filtered.backlogOrders.isEmpty())
    }

    @Test
    fun `requested placement fails when canonical source field or id is absent`() {
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(checklistSource(entries = null), selection("placement-a"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(checklistSource(), selection("missing-placement"))
        }
    }

    @Test
    fun `malformed owner capability and unresolved live target fail closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(
                checklistSource().copy(workspaces = listOf(workspace("workspace-root"))),
                selection("placement-a"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(
                checklistSource(capabilities = emptyList()),
                selection("placement-a"),
            )
        }
        val wrongCapability = capability().copy(workspaceId = "workspace-root")
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(
                checklistSource(capabilities = listOf(wrongCapability)),
                selection("placement-a"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(
                checklistSource(checklists = emptyList()),
                selection("placement-a"),
            )
        }
    }

    @Test
    fun `selected tombstone preserves identity and does not require a live target`() {
        val tombstone = entry("placement-deleted", targetId = "missing", deleted = true)
        val filtered = filter.filter(checklistSource(entries = listOf(tombstone), checklists = emptyList()), selection(tombstone.id))

        val selected = requireNotNull(filtered.workspaceBacklogEntries).single()
        assertEquals(tombstone.id, selected.id)
        assertEquals(tombstone.version, selected.version)
        assertTrue(selected.isDeleted)
    }

    @Test
    fun `ORIENTATION placement pulls a validator coherent canonical target graph`() {
        val source = orientationSource()

        val filtered = filter.filter(source, selection("orientation-placement"))

        assertEquals(listOf("orientation-placement"), filtered.workspaceBacklogEntries?.map { it.id })
        assertEquals(listOf("subject-1"), filtered.managedSubjects?.map { it.id })
        assertEquals(listOf("subject-1"), filtered.orientations?.map { it.subjectId })
        assertEquals(listOf("subject-1"), filtered.orientationAssessments?.map { it.orientationId })
        assertEquals(listOf("revision-1"), filtered.orientationAssessmentRevisions?.map { it.id })
        assertEquals(listOf("mapping-1"), filtered.legacySubjectMappings?.map { it.id })
        assertNotNull(filtered.aspects)
        assertNotNull(filtered.orientationRelations)
        assertNotNull(filtered.aspectOrientationRefs)
        assertNotNull(filtered.workspaceBindings)
        assertNotNull(filtered.savedOrientationViews)
    }

    @Test
    fun `target selection alone never selects canonical placement`() {
        val filtered = filter.filter(orientationSource(), WorkspaceSelectiveImportSelection(selectedGoalIds = setOf("goal-1")))

        assertNull(filtered.workspaceBacklogEntries)
    }

    @Test
    fun `FK10B3b selected Note placement includes content without historical Context`() {
        val note = legacyNote("note-a", "absent-historical-context")
        val placement = entry("note-placement", targetKind = "LEGACY_NOTE", targetId = note.id)
        val source =
            checklistSource(entries = listOf(placement))
                .copy(notes = listOf(note, legacyNote("unselected-note", note.contextId)))

        val filtered = filter.filter(source, selection(placement.id))

        assertEquals(listOf(placement.id), filtered.workspaceBacklogEntries?.map { it.id })
        assertEquals(listOf(note.id), filtered.notes.map { it.id })
        assertEquals(note.contextId, filtered.notes.single().contextId)
        assertTrue(filtered.contexts.isEmpty())
        assertTrue(filtered.managedSubjects.orEmpty().isEmpty())
        assertTrue(filtered.orientations.orEmpty().isEmpty())
    }

    @Test
    fun `FK10B3b multi placement Note has one identity and independently selected occurrences`() {
        val note = legacyNote("shared-note", "missing-context")
        val first = entry("note-a", targetKind = "LEGACY_NOTE", targetId = note.id)
        val second =
            entry("note-b", targetKind = "LEGACY_NOTE", targetId = note.id)
                .copy(workspaceId = "workspace-b", capabilityInstanceId = "backlog-b")

        val source =
            checklistSource(
                entries = listOf(first, second),
                capabilities = listOf(capability(), capability("backlog-b", "workspace-b")),
            ).copy(
                notes = listOf(note),
                workspaces = listOf(workspace("workspace-owner"), workspace("workspace-b")),
            )

        val single = filter.filter(source, selection(first.id))

        assertEquals(listOf(note.id), single.notes.map { it.id })
        assertEquals(listOf(first.id), single.workspaceBacklogEntries?.map { it.id })
        assertEquals(listOf("workspace-owner"), single.workspaces?.map { it.id })

        val both =
            filter.filter(
                source,
                WorkspaceSelectiveImportSelection(
                    selectedWorkspaceBacklogEntryIds = setOf(first.id, second.id),
                ),
            )

        assertEquals(1, both.notes.count { it.id == note.id })
        assertEquals(setOf(first.id, second.id), both.workspaceBacklogEntries?.map { it.id }?.toSet())
        assertEquals(setOf("workspace-owner", "workspace-b"), both.workspaces?.map { it.id }?.toSet())
    }

    @Test
    fun `FK10B3b live placement requires live Note but tombstone does not resurrect target`() {
        val placement = entry("live-note", targetKind = "LEGACY_NOTE", targetId = "target-note")
        val missing = checklistSource(entries = listOf(placement)).copy(notes = emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(missing, selection(placement.id))
        }

        val deletedNote = legacyNote("target-note", "missing-context", deleted = true)
        val source = missing.copy(notes = listOf(deletedNote))

        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(source, selection(placement.id))
        }

        val deletedPlacement = placement.copy(isDeleted = true)
        val filtered =
            filter.filter(
                source.copy(workspaceBacklogEntries = listOf(deletedPlacement)),
                selection(deletedPlacement.id),
            )

        assertEquals(listOf(deletedPlacement.id), filtered.workspaceBacklogEntries?.map { it.id })
        assertTrue(filtered.workspaceBacklogEntries.orEmpty().all { it.isDeleted })
        assertTrue(filtered.notes.isEmpty())
    }

    @Test
    fun `FK10B3b Workspace selection does not pull unplaced historical Note`() {
        val note = legacyNote("standalone", "workspace-owner")
        val source = checklistSource(entries = emptyList()).copy(notes = listOf(note))

        val filtered =
            filter.filter(
                source,
                WorkspaceSelectiveImportSelection(selectedContextIds = setOf("workspace-owner")),
            )

        assertTrue(filtered.notes.isEmpty())
        assertNull(filtered.workspaceBacklogEntries)
    }

    private fun legacyNote(
        id: String,
        contextId: String?,
        deleted: Boolean = false,
    ) = LegacyNoteSnapshot(
        id = id,
        contextId = contextId,
        title = "Title-$id",
        content = "Body-$id",
        createdAt = 10L,
        updatedAt = 20L,
        isDeleted = deleted,
        version = 2L,
    )

    private fun selection(id: String) =
        WorkspaceSelectiveImportSelection(selectedWorkspaceBacklogEntryIds = setOf(id))

    private fun checklistSource(
        entries: List<WorkspaceBacklogEntrySnapshot>? = listOf(entry("placement-a")),
        capabilities: List<WorkspaceCapabilityInstanceEntity> = listOf(capability()),
        checklists: List<ChecklistSnapshot> = listOf(checklist()),
    ) =
        canonicalSource(
            entries = entries,
            capabilities = capabilities,
            checklists = checklists,
            checklistItems = listOf(checklistItem()),
        )

    private fun orientationSource(): SnapshotBundle {
        val assessment = emptyAssessment()
        return canonicalSource(
            entries = listOf(entry("orientation-placement", targetKind = "ORIENTATION", targetId = "subject-1")),
            managedSubjects =
                listOf(
                    ManagedSubjectEntity(
                        "subject-1", "ORIENTATION", "Goal", null, 1L, 2L, null, false, 1L,
                    ),
                ),
            orientations = listOf(OrientationEntity("subject-1", "GOAL", "ACTIVE", "EXPLICIT")),
            assessments = listOf(assessmentEntity()),
            revisions =
                listOf(
                    OrientationAssessmentRevisionEntity(
                        "revision-1", "subject-1", 1L, 1L, "USER", null,
                        Gson().toJson(assessment), 1L, 2L, null, false, 1L,
                    ),
                ),
            mappings =
                listOf(
                    LegacySubjectMappingEntity(
                        "mapping-1", "GOAL", "goal-1", "subject-1", 1, "CUT_OVER",
                        1L, 2L, null, false, 1L,
                    ),
                ),
        )
    }

    private fun canonicalSource(
        entries: List<WorkspaceBacklogEntrySnapshot>?,
        capabilities: List<WorkspaceCapabilityInstanceEntity> = listOf(capability()),
        checklists: List<ChecklistSnapshot> = emptyList(),
        checklistItems: List<ChecklistItemSnapshot> = emptyList(),
        managedSubjects: List<ManagedSubjectEntity> = emptyList(),
        orientations: List<OrientationEntity> = emptyList(),
        assessments: List<OrientationAssessmentEntity> = emptyList(),
        revisions: List<OrientationAssessmentRevisionEntity> = emptyList(),
        mappings: List<LegacySubjectMappingEntity> = emptyList(),
    ) =
        SnapshotBundle(
            version = 2,
            checklists = checklists,
            checklistItems = checklistItems,
            managedSubjects = managedSubjects,
            orientations = orientations,
            aspects = emptyList(),
            orientationAssessments = assessments,
            orientationAssessmentRevisions = revisions,
            legacySubjectMappings = mappings,
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces = listOf(workspace("workspace-root"), workspace("workspace-owner", "workspace-root"), workspace("unrelated")),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = capabilities + capability("unrelated-capability", "unrelated"),
            savedOrientationViews = emptyList(),
            workspaceBacklogEntries = entries,
        )

    private fun entry(
        id: String,
        order: Long = 0L,
        targetKind: String = "CHECKLIST",
        targetId: String = "checklist-1",
        deleted: Boolean = false,
    ) =
        WorkspaceBacklogEntrySnapshot(
            id, "workspace-owner", "backlog-owner", targetKind, targetId, order,
            1L, 2L, 3L, deleted,
        )

    private fun workspace(id: String, parentId: String? = null) =
        WorkspaceSnapshot(
            id, id, null, parentId, null, 0L, 1L, 2L, null, false, 1L,
            "CANONICAL_ONLY", null,
        )

    private fun capability(id: String = "backlog-owner", workspaceId: String = "workspace-owner") =
        WorkspaceCapabilityInstanceEntity(
            id, workspaceId, "BACKLOG", "default", 0L, "ACTIVE", 1, "{}",
            1L, 2L, null, false, 1L,
        )

    private fun checklist() =
        ChecklistSnapshot("checklist-1", "Checklist", null, 1L, 2L, 1L, false)

    private fun checklistItem() =
        ChecklistItemSnapshot("checklist-item", "checklist-1", "Item", false, 0, 2L, 1L, false)

    private fun emptyAssessment() =
        OrientationAssessment(
            importance = AxisAssessment(null, ValueOrigin.UNSET),
            impact = AxisAssessment(null, ValueOrigin.UNSET),
            breadth = AxisAssessment(null, ValueOrigin.UNSET),
            expectedSpan = AxisAssessment(null, ValueOrigin.UNSET),
            targetWindow = AxisAssessment(null, ValueOrigin.UNSET),
            attentionTier = AxisAssessment(null, ValueOrigin.UNSET),
            commitment = AxisAssessment(null, ValueOrigin.UNSET),
            confidence = AxisAssessment(null, ValueOrigin.UNSET),
        )

    private fun assessmentEntity() =
        OrientationAssessmentEntity(
            "subject-1", "revision-1",
            null, "UNSET", null, "UNSET", null, "UNSET", null, "UNSET",
            null, "UNSET", null, "UNSET", null, "UNSET", null, "UNSET",
            "[]", 1L, 2L, null, false, 1L,
        )
}
