package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.data.planning.PlanningCommitmentCompositionProjection
import com.romankozak.forwardappmobile.features.planning.CompositionUiFixture
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import org.junit.Assert.*
import org.junit.Test

class StrategicArcOverviewTest {
    private val f = CompositionUiFixture
    private val scope = f.item("arc", PlanningScopeKind.STRATEGIC_ARC).scope
    private fun quest(id: String) = ArcQuestEntity(id = id, arcKey = "2026-10", title = id)
    private fun item(id: String) = f.item(id, PlanningScopeKind.STRATEGIC_ARC).let { it.copy(scope = scope,
        commitment = it.commitment.copy(scopeId = scope.id, provenance = PlanningProvenance(
            PlanningProvenanceKind.MANUAL, sourceType = "ARC_QUEST", sourceId = id))) }
    private fun frame(vararg edges: Pair<String, String>): PlanningCommitmentCompositionProjection {
        val keys = (edges.flatMap { listOf(it.first, it.second) } + "standalone").distinct()
        return PlanningCommitmentCompositionProjection(keys.map(::item), edges.map { f.edge(it.first, it.second) })
    }
    private fun ids(frame: PlanningCommitmentCompositionProjection) = strategicArcComponentQuestIds(
        frame.participants.map { quest(it.commitment.provenance.sourceId!!) }, frame)

    @Test fun `standalone quests remain in overview`() { assertTrue(ids(frame()).isEmpty()) }
    @Test fun `same Arc component is excluded but root stays`() { assertEquals(setOf("goal"), ids(frame("goal" to "focus"))) }
    @Test fun `nested components exclude every non root without expanding a tree`() {
        assertEquals(setOf("goal", "project"), ids(frame("goal" to "project", "project" to "focus")))
    }
    @Test fun `multiple parents hide one component only once`() {
        assertEquals(setOf("goal"), ids(frame("goal" to "a", "goal" to "b")))
    }
    @Test fun `Goal Project and ongoing kinds use participation composition not semantic kind`() {
        listOf(com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind.GOAL,
            com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind.PROJECT,
            com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind.ONGOING_STANDARD).forEach { kind ->
            val frame = frame("goal" to "focus").let { p -> p.copy(participants = p.participants.map {
                it.copy(entity = it.entity.copy(orientationKind = kind)) }) }
            assertEquals(setOf("goal"), ids(frame))
        }
    }
    @Test fun `parent in another Arc does not hide current participation`() {
        val frame = frame("goal" to "focus").let { p -> p.copy(participants = p.participants.map {
            if (it.commitment.provenance.sourceId == "focus") it.copy(scope = it.scope.copy(id = PlanningScopeId("other")),
                commitment = it.commitment.copy(scopeId = PlanningScopeId("other"))) else it }) }
        assertTrue(ids(frame).isEmpty())
    }
    @Test fun `parent in Tactical does not hide Strategic participation`() {
        val frame = frame("goal" to "focus").let { p -> p.copy(participants = p.participants.map {
            if (it.commitment.provenance.sourceId == "focus") it.copy(scope = it.scope.copy(kind = PlanningScopeKind.TACTICAL_CYCLE)) else it }) }
        assertTrue(ids(frame).isEmpty())
    }
    @Test fun `missing parent carrier in list never hides an otherwise reachable quest`() {
        val frame = frame("goal" to "focus")
        assertTrue(strategicArcComponentQuestIds(listOf(quest("goal")), frame).isEmpty())
    }
    @Test fun `relation removal returns component to overview without changing participation`() {
        val frame = frame("goal" to "focus")
        assertEquals(setOf("goal"), ids(frame))
        assertTrue(ids(frame.copy(relations = frame.relations.map { it.copy(isDeleted = true) })).isEmpty())
        assertEquals(3, frame.participants.size)
    }
    @Test fun `deleted endpoint does not suppress live quest`() {
        val frame = frame("goal" to "focus").let { p -> p.copy(participants = p.participants.map {
            if (it.commitment.provenance.sourceId == "focus") it.copy(commitment = it.commitment.copy(isDeleted = true)) else it }) }
        assertTrue(ids(frame).isEmpty())
    }
    @Test fun `orientation raw ID or title equality is not participation ownership`() {
        val frame = frame("goal" to "focus").let { p -> p.copy(participants = p.participants.map {
            it.copy(commitment = it.commitment.copy(provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL))) }) }
        assertTrue(strategicArcComponentQuestIds(listOf("goal", "focus", "standalone").map(::quest), frame).isEmpty())
    }
    @Test fun `filtered DnD preserves hidden slots and passes complete quest set`() {
        val all = listOf("a", "component", "b", "nested", "c").map(::quest)
        val result = mergeStrategicArcOverviewOrder(all, listOf(all[4], all[0], all[2]))
        assertEquals(listOf("c", "component", "a", "nested", "b"), result.map { it.id })
        assertEquals(all[1], result[1]); assertEquals(all[3], result[3])
        assertEquals(all.map { it.id }.toSet(), result.map { it.id }.toSet())
    }
    @Test fun `all participant reorder remains normal and uses latest row data`() {
        val all = listOf(quest("a"), quest("b"))
        assertEquals(listOf(all[1], all[0]), mergeStrategicArcOverviewOrder(all, all.reversed().map { it.copy(title = "stale") }))
    }
    @Test fun `invalid duplicate or foreign reordered identities rejected`() {
        val all = listOf(quest("a"), quest("b"))
        assertTrue(runCatching { mergeStrategicArcOverviewOrder(all, listOf(all[0], all[0])) }.isFailure)
        assertTrue(runCatching { mergeStrategicArcOverviewOrder(all, listOf(quest("unknown"))) }.isFailure)
    }
}
