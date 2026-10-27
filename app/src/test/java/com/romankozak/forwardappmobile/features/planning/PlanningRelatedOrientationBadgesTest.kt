package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Assert.*
import org.junit.Test

class PlanningRelatedOrientationBadgesTest {
    private val a = CanonicalEntitySummary(CanonicalEntityRef.Orientation("a"), "Alpha")
    private val b = CanonicalEntitySummary(CanonicalEntityRef.Orientation("b"), "Beta")
    private val catalog = listOf(a, b, CanonicalEntitySummary(CanonicalEntityRef.Workspace("a"), "Not Alpha"))
    private fun row(id: String = "a", kind: String = "DAY_TASK", owner: String = "task") =
        PlanningReferenceEntity("$kind:$owner:$id", kind, owner, "ORIENTATION", id, "RELATED", 1, 1, null, 1, false)

    @Test fun `multiple related Orientations render deterministically for Mission and Day owners`() {
        listOf(PlanningReferenceOwnerKind.TACTICAL_MISSION to "42", PlanningReferenceOwnerKind.DAY_TASK to "task").forEach { (kind, owner) ->
            val rows = listOf(row("b", kind.name, owner), row("a", kind.name, owner))
            assertEquals(listOf(a, b), projectPlanningRelatedOrientations(rows, catalog, kind).getValue(owner))
            assertEquals(listOf(a, b), projectPlanningRelatedOrientations(rows.reversed(), catalog.reversed(), kind).getValue(owner))
        }
    }

    @Test fun `SOURCE unknown roles other target classes tombstones and missing targets do not become RELATED`() {
        listOf(row().copy(role = "SOURCE"), row().copy(role = "UNKNOWN"), row().copy(targetKind = "WORKSPACE"),
            row().copy(targetKind = "ASPECT"), row().copy(isDeleted = true), row("missing")).forEach {
            assertTrue(projectPlanningRelatedOrientations(listOf(it), catalog, PlanningReferenceOwnerKind.DAY_TASK).isEmpty())
        }
    }

    @Test fun `typed identity excludes same raw ID Workspace when Orientation is gone`() {
        assertTrue(projectPlanningRelatedOrientations(listOf(row()), catalog.filterNot { it.ref == a.ref }, PlanningReferenceOwnerKind.DAY_TASK).isEmpty())
    }

    @Test fun `duplicate links merge but different owners remain isolated`() {
        val result = projectPlanningRelatedOrientations(listOf(row(), row().copy(id = "duplicate"), row(owner = "other")), catalog, PlanningReferenceOwnerKind.DAY_TASK)
        assertEquals(mapOf("task" to listOf(a), "other" to listOf(a)), result)
    }

    @Test fun `invalid Mission owners are not reinterpreted and Day owner strings remain exact`() {
        listOf("", "0", "-1", "01", "+1", "other").forEach {
            assertTrue(projectPlanningRelatedOrientations(listOf(row(kind = "TACTICAL_MISSION", owner = it)), catalog, PlanningReferenceOwnerKind.TACTICAL_MISSION).isEmpty())
        }
        assertEquals(setOf("01"), projectPlanningRelatedOrientations(listOf(row(owner = "01")), catalog, PlanningReferenceOwnerKind.DAY_TASK).keys)
    }
}
