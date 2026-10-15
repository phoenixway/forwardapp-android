package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StrategicPlanningTransportContractTest {
    @Test
    fun `v4 preserves exact Strategic extension and authoritative empty`() {
        val populated = snapshot(strategicExtensions = listOf(strategicExtension()))

        populated.requireValidCanonicalPlanningTransport()
        val decoded = Json.decodeFromString<DesktopWorkspaceSnapshot>(Json.encodeToString(populated))

        assertEquals(" Exact/Arc Key ", decoded.strategicPlanningScopeExtensions?.single()?.legacyArcKey)
        assertTrue(decoded.hasCanonicalStrategicPlanningExtensionTransport())

        val empty = snapshot(strategicExtensions = emptyList())
        empty.requireValidCanonicalPlanningTransport()
        assertEquals(emptyList(), empty.strategicPlanningScopeExtensions)
    }

    @Test
    fun `v3 remains valid while Strategic extension is unsupported`() {
        val v3 =
            snapshot(strategicExtensions = null).copy(
                planningFormatVersion = TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION,
            )

        v3.requireValidCanonicalPlanningTransport()

        assertFalse(v3.hasCanonicalStrategicPlanningExtensionTransport())
    }

    @Test
    fun `Strategic extension rejects missing or wrong-kind scope dependency`() {
        assertFailsWith<IllegalArgumentException> {
            snapshot(scopes = emptyList(), strategicExtensions = listOf(strategicExtension()))
                .requireValidCanonicalPlanningTransport()
        }
        assertFailsWith<IllegalArgumentException> {
            snapshot(
                scopes = listOf(scope(kind = "DAY")),
                strategicExtensions = listOf(strategicExtension()),
            ).requireValidCanonicalPlanningTransport()
        }
    }

    private fun snapshot(
        scopes: List<SharedPlanningScopeSnapshot> = listOf(scope()),
        strategicExtensions: List<SharedStrategicPlanningScopeExtensionSnapshot>?,
    ) =
        DesktopWorkspaceSnapshot(
            contexts = emptyList(),
            backlogItems = emptyList(),
            planningFormatVersion = CURRENT_SHARED_PLANNING_FORMAT_VERSION,
            planningScopes = scopes,
            planningCommitments = emptyList(),
            dayPlanningScopeExtensions = emptyList(),
            tacticalPlanningScopeExtensions = emptyList(),
            strategicPlanningScopeExtensions = strategicExtensions,
        )

    private fun scope(kind: String = "STRATEGIC_ARC") =
        SharedPlanningScopeSnapshot(
            id = "scope-1",
            kind = kind,
            lifecycle = "ACTIVE",
            title = null,
            startsAt = null,
            endsAt = null,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            version = 3L,
            isDeleted = false,
        )

    private fun strategicExtension() =
        SharedStrategicPlanningScopeExtensionSnapshot(
            scopeId = "scope-1",
            legacyArcKey = " Exact/Arc Key ",
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            version = 3L,
            isDeleted = false,
        )
}
