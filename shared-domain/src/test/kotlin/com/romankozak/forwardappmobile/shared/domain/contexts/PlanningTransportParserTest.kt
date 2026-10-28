package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PlanningTransportParserTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun parse(version: Int, relations: String = "[]") = parseCanonicalPlanningTransport(json.parseToJsonElement("""{
        "planningFormatVersion":$version,"planningScopes":[],"planningCommitments":[],
        "dayPlanningScopeExtensions":[],"tacticalPlanningScopeExtensions":[],"strategicPlanningScopeExtensions":[],
        "planningCommitmentRelations":$relations }""").jsonObject, json)
    @Test fun `v5 admits explicit empty composition`() { assertEquals(emptyList<SharedPlanningCommitmentRelationSnapshot>(), parse(5).relations) }
    @Test fun `v4 cannot discard a v5 relation field`() { assertTrue(runCatching { parse(4) }.isFailure) }
    @Test fun `v5 missing collection is invalid`() { assertTrue(runCatching { parse(5, "null") }.isFailure) }
    @Test fun `relation fields survive Android to shared parsing`() {
        val row = SharedPlanningCommitmentRelationSnapshot("edge", "day", "mission", "COMPONENT_OF", 2, 1, 3, null, true, 4)
        val encoded = json.encodeToString(SharedPlanningCommitmentRelationSnapshot.serializer(), row)
        assertEquals(listOf(row), parse(5, "[$encoded]").relations)
    }
}
