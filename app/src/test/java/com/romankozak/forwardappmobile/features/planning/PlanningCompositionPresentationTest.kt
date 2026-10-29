package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Assert.*
import org.junit.Test

internal object CompositionUiFixture {
    fun id(key: String) = PlanningCommitmentId(key)
    fun item(key: String, kind: PlanningScopeKind = PlanningScopeKind.DAY,
        status: PlanningCommitmentStatus = PlanningCommitmentStatus.ACTIVE): PlanningParticipantSummary {
        val scope = PlanningScope(PlanningScopeId("scope-$key"), kind, PlanningScopeLifecycle.ACTIVE,
            "Horizon $key", null, null, 1, 1, null, 1, false)
        return PlanningParticipantSummary(
            PlanningCommitment(id(key), scope.id, "subject-$key", PlanningRoleCode.FOCUS, 0, null, status,
                PlanningProvenance(PlanningProvenanceKind.MANUAL), 1, 1, null, 1, false), scope,
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("subject-$key"), "Title $key", orientationKind = OrientationKind.PROJECT))
    }
    fun edge(source: String, target: String, order: Long = 0) = PlanningCommitmentRelation(
        PlanningCommitmentRelationId("$source-$target"), id(source), id(target), PlanningCommitmentRelationType.COMPONENT_OF,
        order, 1, 1, null, false, 1)
    fun frame(vararg edges: Pair<String, String>): PlanningCommitmentCompositionProjection {
        val keys = (edges.flatMap { listOf(it.first, it.second) } + "isolated").distinct()
        return PlanningCommitmentCompositionProjection(keys.map { item(it) }, edges.map { edge(it.first, it.second) })
    }
}

class PlanningCompositionPresentationTest {
    private val f = CompositionUiFixture
    @Test fun `checkbox completes active and reactivates completed participation`() {
        assertEquals(PlanningCommitmentStatus.COMPLETED, completionToggleStatus(PlanningCommitmentStatus.ACTIVE))
        assertEquals(PlanningCommitmentStatus.ACTIVE, completionToggleStatus(PlanningCommitmentStatus.COMPLETED))
    }
    @Test fun `checkbox never assigns pause`() {
        assertEquals(PlanningCommitmentStatus.COMPLETED, completionToggleStatus(PlanningCommitmentStatus.PAUSED))
        PlanningCommitmentStatus.entries.forEach { assertNotEquals(PlanningCommitmentStatus.PAUSED, completionToggleStatus(it)) }
    }
    @Test fun `Part of follows outgoing edge`() {
        assertEquals("b", f.frame("a" to "b").outgoing(f.id("a")).single().participant.commitment.id.value)
    }
    @Test fun `Components follows incoming edge`() {
        assertEquals("a", f.frame("a" to "b").incoming(f.id("b")).single().participant.commitment.id.value)
    }
    @Test fun `multiple parents remain distinct`() {
        assertEquals(2, f.frame("a" to "b", "a" to "c").outgoing(f.id("a")).size)
    }
    @Test fun `multiple children remain distinct`() {
        assertEquals(2, f.frame("a" to "b", "c" to "b").incoming(f.id("b")).size)
    }
    @Test fun `parent picker rejects self duplicates and descendants`() {
        val frame = f.frame("a" to "b", "b" to "c")
        assertEquals(listOf("isolated"), frame.relationCandidates(f.id("b"), true).map { it.commitment.id.value })
    }
    @Test fun `component picker rejects ancestors and existing child`() {
        val frame = f.frame("a" to "b", "b" to "c")
        assertEquals(listOf("isolated"), frame.relationCandidates(f.id("b"), false).map { it.commitment.id.value })
    }
    @Test fun `picker remains cross horizon and kind agnostic`() {
        val items = PlanningScopeKind.entries.map { f.item(it.name, it) }
        val frame = PlanningCommitmentCompositionProjection(items, emptyList())
        assertEquals(items.drop(1), frame.relationCandidates(items.first().commitment.id, true))
    }
    @Test fun `simple upward chain`() {
        assertEquals(listOf("b", "c"), f.frame("a" to "b", "b" to "c").trace(f.id("a"), true).map { it.participant.commitment.id.value })
    }
    @Test fun `branching DAG preserves shared ancestor on both paths`() {
        val rows = f.frame("a" to "b", "a" to "c", "b" to "d", "c" to "d").trace(f.id("a"), true)
        assertEquals(listOf("b", "d", "c", "d"), rows.map { it.participant.commitment.id.value })
        assertEquals(rows.size, rows.distinctBy { it.key }.size)
    }
    @Test fun `defensive cyclic snapshot does not recurse forever`() {
        assertEquals(listOf("b"), f.frame("a" to "b", "b" to "a").trace(f.id("a"), true).map { it.participant.commitment.id.value })
    }
    @Test fun `trace downward follows incoming direction`() {
        assertEquals(listOf("b", "a"), f.frame("a" to "b", "b" to "c").trace(f.id("c"), false).map { it.participant.commitment.id.value })
    }
    @Test fun `three levels fold and expand without new reads`() {
        val frame = f.frame("a" to "b", "b" to "c", "c" to "d", "d" to "e")
        val first = frame.trace(f.id("a"), true)
        assertEquals(3, first.size); assertTrue(first.last().hasMore)
        assertEquals(4, frame.trace(f.id("a"), true, setOf(first.last().key)).size)
    }
    @Test fun `trace has explicit size guard`() {
        val frame = f.frame(*(1..300).map { "root" to "$it" }.toTypedArray())
        assertEquals(200, frame.trace(f.id("root"), true).size)
    }
    @Test fun `deleted endpoints and relations excluded before trace`() {
        val items = listOf(f.item("a"), f.item("b"), f.item("c"))
        val frame = projectPlanningComposition(items.map { it.scope },
            items.map { if (it.commitment.id.value == "c") it.commitment.copy(isDeleted = true) else it.commitment },
            listOf(f.edge("a", "b").copy(isDeleted = true), f.edge("a", "c")), items.map { it.entity })
        assertTrue(frame.trace(f.id("a"), true).isEmpty())
    }
    @Test fun `ordering is relation local not participation order`() {
        val items = listOf("a", "b", "x", "y").map { f.item(it) }
        val frame = projectPlanningComposition(items.map { it.scope }, items.map { it.commitment },
            listOf(f.edge("a", "x", 2), f.edge("b", "x", 1), f.edge("a", "y", 1), f.edge("b", "y", 2)), items.map { it.entity })
        assertEquals(listOf("b", "a"), frame.incoming(f.id("x")).map { it.participant.commitment.id.value })
        assertEquals(listOf("a", "b"), frame.incoming(f.id("y")).map { it.participant.commitment.id.value })
    }
    @Test fun `rename and status project from new frame`() {
        val item = f.item("a").copy(entity = f.item("a").entity.copy(title = "Renamed"),
            commitment = f.item("a").commitment.copy(status = PlanningCommitmentStatus.COMPLETED))
        val frame = PlanningCommitmentCompositionProjection(listOf(item, f.item("b")), listOf(f.edge("a", "b")))
        assertEquals("Renamed", frame.incoming(f.id("b")).single().participant.entity.title)
        assertEquals("Завершено", frame.incoming(f.id("b")).single().participant.statusLabel)
    }
    @Test fun `primary mission navigation is exact participation not semantic`() {
        val item = f.item("mission", PlanningScopeKind.TACTICAL_CYCLE)
        assertEquals(NavTarget.PlanningParticipation("mission"), item.planningDestination())
    }
    @Test fun `primary Strategic navigation retains participation scope`() {
        assertEquals(NavTarget.PlanningParticipation("focus"), f.item("focus", PlanningScopeKind.STRATEGIC_ARC).planningDestination())
    }
    @Test fun `authoritative Day task opens task properties`() {
        val item = f.item("day").copy(dayTaskId = "task")
        assertEquals(NavTarget.EditTask("task"), item.planningDestination())
    }
    @Test fun `carried Day task keeps current task route rather than original provenance`() {
        val item = f.item("day").copy(dayTaskId = "tomorrow-task", commitment = f.item("day").commitment.copy(
            provenance = PlanningProvenance(PlanningProvenanceKind.CARRIED_FORWARD, "PLANNING_COMMITMENT", "yesterday")))
        assertEquals(NavTarget.EditTask("tomorrow-task"), item.planningDestination())
    }
    @Test fun `route encodes exact commitment not title`() {
        assertEquals("planning_participation/exact%2Fid", NavTargetRouter.routeOf(NavTarget.PlanningParticipation("exact/id")))
    }
    @Test fun `free form Day owner stays unavailable`() {
        assertNull(f.frame("a" to "b").forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "freeform")))
    }
    @Test fun `human role label does not expose namespaced code`() {
        val item = f.item("a").copy(commitment = f.item("a").commitment.copy(role = PlanningRoleCode.DAY_RESPONSIBILITY))
        assertTrue(item.contextLabel.contains("Відповідальність")); assertFalse(item.contextLabel.contains("DAY.RESPONSIBILITY"))
    }
    @Test fun `Mission Properties resolves canonical participation provenance`() {
        val item = f.item("a").copy(commitment = f.item("a").commitment.copy(
            provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL, "TACTICAL_MISSION", "42")))
        val frame = PlanningCommitmentCompositionProjection(listOf(item), emptyList())
        assertEquals(item, frame.forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.TACTICAL_MISSION, "42")))
        assertNull(frame.forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.ARC_QUEST, "42")))
    }
    @Test fun `Strategic Quest Properties resolves canonical participation provenance`() {
        val item = f.item("a").copy(commitment = f.item("a").commitment.copy(
            provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL, "ARC_QUEST", "quest")))
        assertEquals(item, PlanningCommitmentCompositionProjection(listOf(item), emptyList())
            .forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.ARC_QUEST, "quest")))
    }
}
