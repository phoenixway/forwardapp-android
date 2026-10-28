package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import org.junit.Assert.*
import org.junit.Test

class OrientationPlanningProjectionTest {
    private val f = CompositionUiFixture
    private fun subject(item: PlanningParticipantSummary, id: String) = item.copy(
        commitment = item.commitment.copy(orientationId = id), entity = item.entity.copy(ref = CanonicalEntityRef.Orientation(id)))
    @Test fun `unplanned Orientation remains available without participation`() {
        val entity = f.item("x").entity
        val frame = projectPlanningComposition(emptyList(), emptyList(), emptyList(), listOf(entity))
        assertEquals(listOf(entity), frame.orientations); assertTrue(frame.forOrientation(entity.ref.id).isEmpty())
    }
    @Test fun `same Orientation groups all horizons without identity collapse`() {
        val items = PlanningScopeKind.entries.map { subject(f.item(it.name, it), "subject") }
        val frame = PlanningCommitmentCompositionProjection(items, emptyList())
        assertEquals(items, frame.forOrientation("subject")); assertEquals(3, items.map { it.commitment.id }.distinct().size)
    }
    @Test fun `multiple scopes of same horizon remain visible`() {
        val items = listOf(subject(f.item("a"), "subject"), subject(f.item("b"), "subject"))
        assertEquals(2, PlanningCommitmentCompositionProjection(items, emptyList()).forOrientation("subject").size)
    }
    @Test fun `Aspect cannot open Orientation Planning`() {
        assertNull(orientationPlanningTarget(CanonicalEntityRef.Aspect("same")))
        assertEquals(NavTarget.OrientationPlanning("same"), orientationPlanningTarget(CanonicalEntityRef.Orientation("same")))
    }
    @Test fun `Workspace identity is not a Planning target`() {
        assertNull(orientationPlanningTarget(CanonicalEntityRef.Workspace("same")))
    }
    @Test fun `Orientation route preserves exact identity`() {
        assertEquals("orientation_planning/id%2Fexact", NavTargetRouter.routeOf(NavTarget.OrientationPlanning("id/exact")))
    }
    @Test fun `unplanned backlog item has no badge`() {
        assertNull(f.frame("a" to "b").planningBadgesByOrientation()["unplanned"])
    }
    @Test fun `own participation produces exact navigation badge`() {
        val item = f.item("a", PlanningScopeKind.TACTICAL_CYCLE)
        val badge = PlanningCommitmentCompositionProjection(listOf(item), emptyList()).planningBadgesByOrientation()[item.commitment.orientationId]!!.single()
        assertTrue(badge.label.startsWith("Tactical ·")); assertEquals(item, badge.participant)
    }
    @Test fun `Strategic parent badge navigates to larger participation`() {
        val source = f.item("a")
        val focus = f.item("b", PlanningScopeKind.STRATEGIC_ARC)
        val frame = PlanningCommitmentCompositionProjection(listOf(source, focus), listOf(f.edge("a", "b")))
        val badge = frame.planningBadgesByOrientation()[source.commitment.orientationId]!!.single()
        assertEquals("Strategic · Title b", badge.label); assertEquals(focus, badge.participant)
        assertEquals(NavTarget.PlanningParticipation("b"), badge.participant.planningDestination())
    }
    @Test fun `Tactical parent badge navigates to exact Mission context`() {
        val source = f.item("a")
        val mission = f.item("b", PlanningScopeKind.TACTICAL_CYCLE)
        val badge = PlanningCommitmentCompositionProjection(listOf(source, mission), listOf(f.edge("a", "b")))
            .planningBadgesByOrientation()[source.commitment.orientationId]!!.single()
        assertEquals("Tactical · Title b", badge.label); assertEquals(mission, badge.participant)
    }
    @Test fun `multiple parents produce independent badges`() {
        assertEquals(2, f.frame("a" to "b", "a" to "c").planningBadgesByOrientation()["subject-a"]!!.size)
    }
    @Test fun `two participations pointing at same parent deduplicate badge not participation`() {
        val a = subject(f.item("a"), "shared")
        val c = subject(f.item("c"), "shared")
        val frame = PlanningCommitmentCompositionProjection(listOf(a, c, f.item("b")), listOf(f.edge("a", "b"), f.edge("c", "b")))
        assertEquals(1, frame.planningBadgesByOrientation()["shared"]!!.size)
        assertEquals(2, frame.forOrientation("shared").size)
    }
    @Test fun `removed relation falls back to actual own participation badge`() {
        val frame = f.frame("a" to "b")
        val updated = frame.copy(relations = emptyList())
        assertEquals("a", updated.planningBadgesByOrientation()["subject-a"]!!.single().participant.commitment.id.value)
    }
    @Test fun `closed scope does not claim current planning in backlog`() {
        val item = f.item("a").copy(scope = f.item("a").scope.copy(lifecycle = PlanningScopeLifecycle.CLOSED))
        assertTrue(PlanningCommitmentCompositionProjection(listOf(item), emptyList()).planningBadgesByOrientation().isEmpty())
    }
    @Test fun `completed participation hidden from active backlog summary`() {
        val item = f.item("a", status = PlanningCommitmentStatus.COMPLETED)
        assertTrue(PlanningCommitmentCompositionProjection(listOf(item), emptyList()).planningBadgesByOrientation().isEmpty())
    }
    @Test fun `source tombstone removes badge through live projection`() {
        val a = f.item("a")
        val frame = projectPlanningComposition(listOf(a.scope), listOf(a.commitment.copy(isDeleted = true)), emptyList(), listOf(a.entity))
        assertTrue(frame.planningBadgesByOrientation().isEmpty())
    }
    @Test fun `carried participation adds current badge without hiding history in full context`() {
        val old = subject(f.item("old"), "shared").let { it.copy(scope = it.scope.copy(lifecycle = PlanningScopeLifecycle.CLOSED)) }
        val next = subject(f.item("next"), "shared")
        val frame = PlanningCommitmentCompositionProjection(listOf(old, next), emptyList())
        assertEquals(2, frame.forOrientation("shared").size)
        assertEquals("next", frame.planningBadgesByOrientation()["shared"]!!.single().participant.commitment.id.value)
    }
    @Test fun `derived count responds to live link projection without inline tree`() {
        val frame = f.frame("a" to "b", "c" to "b")
        assertEquals(2, frame.incoming(f.id("b")).size)
        assertEquals(1, frame.copy(relations = frame.relations.take(1)).incoming(f.id("b")).size)
    }
}
