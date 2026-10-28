package com.romankozak.forwardappmobile.shared.core.domain.planning

import kotlin.test.*

class PlanningCompositionContractTest {
    private fun id(value: String) = PlanningCommitmentId(value)
    private fun edge(a: String, b: String, order: Long = 0) = PlanningCommitmentRelation(
        PlanningCommitmentRelationId("$a:$b"), id(a), id(b), PlanningCommitmentRelationType.COMPONENT_OF, order, 1, 1, null, false, 1)
    private val live = setOf("a", "b", "c", "d").mapTo(hashSetOf(), ::id)
    @Test fun dagAllowsManyParents() { assertTrue(validatePlanningCommitmentRelations(listOf(edge("a", "b"), edge("a", "c"), edge("d", "b")), live).isEmpty()) }
    @Test fun selfLinkRejected() { assertTrue(validatePlanningCommitmentRelations(listOf(edge("a", "a")), live).isNotEmpty()) }
    @Test fun transitiveCycleRejected() { assertTrue(validatePlanningCommitmentRelations(listOf(edge("a", "b"), edge("b", "c"), edge("c", "a")), live).isNotEmpty()) }
    @Test fun tombstoneDoesNotContributeToCycle() { assertTrue(validatePlanningCommitmentRelations(listOf(edge("a", "b"), edge("b", "a").copy(isDeleted = true)), live).isEmpty()) }
    @Test fun missingEndpointRejected() { assertTrue(validatePlanningCommitmentRelations(listOf(edge("a", "missing")), live).isNotEmpty()) }
    @Test fun duplicateLiveTupleRejected() { assertTrue(validatePlanningCommitmentRelations(listOf(edge("a", "b"), edge("a", "b").copy(id = PlanningCommitmentRelationId("other"))), live).isNotEmpty()) }
    @Test fun sourceOnlyCarryKeepsTarget() { assertEquals(listOf(id("c") to id("b")), remapPlanningComposition(listOf(edge("a", "b")), mapOf(id("a") to id("c")), live)) }
    @Test fun bothEndpointsCarryUsesNewTarget() { assertEquals(listOf(id("c") to id("d")), remapPlanningComposition(listOf(edge("a", "b")), mapOf(id("a") to id("c"), id("b") to id("d")), live)) }
    @Test fun targetOnlyDoesNotInventSource() { assertTrue(remapPlanningComposition(listOf(edge("a", "b")), mapOf(id("b") to id("d")), live).isEmpty()) }
    @Test fun ambiguousOrMissingMappingFailsClosed() {
        assertFailsWith<IllegalArgumentException> { remapPlanningComposition(listOf(edge("a", "b")), mapOf(id("a") to id("c"), id("b") to id("c")), live) }
        assertFailsWith<IllegalArgumentException> { remapPlanningComposition(listOf(edge("a", "b")), mapOf(id("a") to id("missing")), live) }
    }
    @Test fun largeDagDoesNotUseRecursiveTraversal() {
        val graph = (0 until 10_000).map { edge(it.toString(), (it + 1).toString()) }
        assertTrue(validatePlanningCommitmentRelations(graph, (0..10_000).mapTo(hashSetOf()) { id(it.toString()) }).isEmpty())
    }
}
