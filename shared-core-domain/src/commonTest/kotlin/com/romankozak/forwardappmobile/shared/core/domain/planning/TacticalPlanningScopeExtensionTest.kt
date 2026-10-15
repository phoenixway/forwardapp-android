package com.romankozak.forwardappmobile.shared.core.domain.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TacticalPlanningScopeExtensionTest {
    @Test
    fun `timeboxed and open-ended extensions validate`() {
        assertTrue(validateTacticalPlanningScopeExtension(extension()).isEmpty())
        assertTrue(
            validateTacticalPlanningScopeExtension(
                extension(
                    type = TacticalPlanningScopeType.OPEN_ENDED,
                    weekKey = null,
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun `timeboxed extension requires bounded ISO week code`() {
        assertEquals(
            "INVALID_TIMEBOXED_WEEK_KEY",
            validateTacticalPlanningScopeExtension(extension(weekKey = "2026-W54")).single().code,
        )
        assertEquals(
            "INVALID_TIMEBOXED_WEEK_KEY",
            validateTacticalPlanningScopeExtension(extension(weekKey = null)).single().code,
        )
    }

    @Test
    fun `open-ended extension rejects week key`() {
        assertEquals(
            "OPEN_ENDED_WEEK_KEY",
            validateTacticalPlanningScopeExtension(
                extension(type = TacticalPlanningScopeType.OPEN_ENDED),
            ).single().code,
        )
    }

    @Test
    fun `metadata validation rejects negative and reversed values`() {
        val codes =
            validateTacticalPlanningScopeExtension(
                extension(
                    closedAt = -1L,
                    createdAt = 10L,
                    updatedAt = 9L,
                    version = -1L,
                ),
            ).map { it.code }.toSet()

        assertEquals(
            setOf("INVALID_CLOSED_AT", "NEGATIVE_VERSION", "UPDATED_BEFORE_CREATED"),
            codes,
        )
    }

    @Test
    fun `dependency requires matching live tactical scope`() {
        val extension = extension()
        assertEquals(
            "MISSING_SCOPE",
            validateTacticalPlanningScopeExtensionDependency(extension, null).single().code,
        )
        assertEquals(
            "NON_TACTICAL_SCOPE",
            validateTacticalPlanningScopeExtensionDependency(
                extension,
                scope(kind = PlanningScopeKind.DAY),
            ).single().code,
        )
        assertEquals(
            "TOMBSTONED_SCOPE",
            validateTacticalPlanningScopeExtensionDependency(
                extension,
                scope(isDeleted = true),
            ).single().code,
        )
    }

    @Test
    fun `open-ended extension rejects scope end bound`() {
        val violations =
            validateTacticalPlanningScopeExtensionDependency(
                extension(type = TacticalPlanningScopeType.OPEN_ENDED, weekKey = null),
                scope(endsAt = 20L),
            )
        assertEquals("OPEN_ENDED_HORIZON_END", violations.single().code)
    }

    private fun extension(
        type: TacticalPlanningScopeType = TacticalPlanningScopeType.TIMEBOXED,
        weekKey: String? = "2026-W40",
        closedAt: Long? = null,
        createdAt: Long = 1L,
        updatedAt: Long? = 2L,
        version: Long = 1L,
    ) = TacticalPlanningScopeExtension(
        scopeId = PlanningScopeId("cycle-1"),
        type = type,
        weekKey = weekKey,
        closedAt = closedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        version = version,
        isDeleted = false,
    )

    private fun scope(
        kind: PlanningScopeKind = PlanningScopeKind.TACTICAL_CYCLE,
        endsAt: Long? = null,
        isDeleted: Boolean = false,
    ) = PlanningScope(
        id = PlanningScopeId("cycle-1"),
        kind = kind,
        lifecycle = PlanningScopeLifecycle.ACTIVE,
        title = "Cycle",
        startsAt = 1L,
        endsAt = endsAt,
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = null,
        version = 1L,
        isDeleted = isDeleted,
    )
}
