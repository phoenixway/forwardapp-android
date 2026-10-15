package com.romankozak.forwardappmobile.shared.core.domain.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StrategicPlanningScopeExtensionContractTest {
    @Test
    fun `valid extension requires matching Strategic scope`() {
        val scope = scope()
        val extension = extension()

        assertTrue(validateStrategicPlanningScopeExtensionDependency(extension, scope).isEmpty())
        assertEquals(
            "WRONG_SCOPE_KIND",
            validateStrategicPlanningScopeExtensionDependency(
                extension,
                scope.copy(kind = PlanningScopeKind.DAY),
            ).single().code,
        )
    }

    @Test
    fun `arc key is typed extension state and malformed metadata fails closed`() {
        val violations =
            validateStrategicPlanningScopeExtension(
                extension().copy(legacyArcKey = " ", updatedAt = 5L, version = -1L),
            )

        assertEquals(
            setOf("BLANK_LEGACY_ARC_KEY", "NEGATIVE_VERSION", "UPDATED_BEFORE_CREATED"),
            violations.map { it.code }.toSet(),
        )
    }

    private fun scope() =
        PlanningScope(
            id = PlanningScopeId("scope"),
            kind = PlanningScopeKind.STRATEGIC_ARC,
            lifecycle = PlanningScopeLifecycle.ACTIVE,
            title = null,
            startsAt = null,
            endsAt = null,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )

    private fun extension() =
        StrategicPlanningScopeExtension(
            scopeId = PlanningScopeId("scope"),
            legacyArcKey = "2026-10",
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )
}
