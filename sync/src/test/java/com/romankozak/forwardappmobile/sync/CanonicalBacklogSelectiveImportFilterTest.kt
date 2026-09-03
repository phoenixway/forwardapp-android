package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.ChecklistSnapshot
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
        assertEquals(listOf("workspace-root", "workspace-owner"), filtered.workspaces?.map { it.id })
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
        WorkspaceEntity(
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
