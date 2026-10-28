package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlin.test.*
import kotlinx.serialization.json.Json

class PlanningCompositionTransportContractTest {
    private fun scope(id: String, kind: String) = SharedPlanningScopeSnapshot(id, kind, "ACTIVE", id, null, null, 1, 1, null, 1, false)
    private fun commitment(id: String, scope: String) = SharedPlanningCommitmentSnapshot(id, scope, "orientation-$id", "FOCUS", 0,
        null, "ACTIVE", "MANUAL", null, null, 1, 1, null, 1, false)
    private val edge = SharedPlanningCommitmentRelationSnapshot("edge", "day", "mission", "COMPONENT_OF", 0, 1, 1, null, false, 1)
    private fun snapshot() = DesktopWorkspaceSnapshot(emptyList(), emptyList(), planningFormatVersion = 5,
        planningScopes = listOf(scope("d", "DAY"), scope("t", "TACTICAL_CYCLE")),
        planningCommitments = listOf(commitment("day", "d"), commitment("mission", "t")), planningCommitmentRelations = listOf(edge),
        dayPlanningScopeExtensions = emptyList(), tacticalPlanningScopeExtensions = emptyList(), strategicPlanningScopeExtensions = emptyList())
    @Test fun v5RoundTrip() {
        val original = snapshot()
        original.requireValidCanonicalPlanningTransport()
        val decoded = Json.decodeFromString(DesktopWorkspaceSnapshot.serializer(), Json.encodeToString(DesktopWorkspaceSnapshot.serializer(), original))
        assertEquals(original, decoded); decoded.requireValidCanonicalPlanningTransport()
    }
    @Test fun v4CannotClaimRelationsAndV5CannotOmitThem() {
        assertFailsWith<IllegalArgumentException> { snapshot().copy(planningFormatVersion = 4).requireValidCanonicalPlanningTransport() }
        assertFailsWith<IllegalArgumentException> { snapshot().copy(planningCommitmentRelations = null).requireValidCanonicalPlanningTransport() }
    }
    @Test fun missingOrDeletedCommitmentFailsClosed() {
        assertFailsWith<IllegalArgumentException> { snapshot().copy(planningCommitments = emptyList()).requireValidCanonicalPlanningTransport() }
        assertFailsWith<IllegalArgumentException> { snapshot().copy(planningCommitments = snapshot().planningCommitments!!.map { it.copy(isDeleted = true) }).requireValidCanonicalPlanningTransport() }
    }
    @Test fun directAndTransitiveCyclesFailClosed() {
        assertFailsWith<IllegalArgumentException> { snapshot().copy(planningCommitmentRelations = listOf(edge,
            edge.copy(id = "reverse", sourceCommitmentId = "mission", targetCommitmentId = "day"))).requireValidCanonicalPlanningTransport() }
    }
    @Test fun unknownRelationVocabularyFailsClosed() {
        assertFailsWith<IllegalArgumentException> { snapshot().copy(planningCommitmentRelations = listOf(edge.copy(relationType = "UNKNOWN"))).requireValidCanonicalPlanningTransport() }
    }
}
