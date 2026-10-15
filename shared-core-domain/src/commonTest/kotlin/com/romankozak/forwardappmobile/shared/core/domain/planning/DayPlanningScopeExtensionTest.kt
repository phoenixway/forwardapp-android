package com.romankozak.forwardappmobile.shared.core.domain.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayPlanningScopeExtensionTest {
    @Test
    fun `valid typed Day extension preserves calendar outcome and budget`() {
        val extension = extension()

        assertTrue(validateDayPlanningScopeExtension(extension).isEmpty())
        assertEquals(DayPlanningOutcome.COMPLETED, extension.outcome)
        assertEquals(480L, extension.plannedBudgetMinutes)
    }

    @Test
    fun `invalid calendar budget version and timestamps fail closed`() {
        val violations =
            validateDayPlanningScopeExtension(
                extension().copy(
                    calendarDayStartMillis = -1L,
                    plannedBudgetMinutes = 0L,
                    createdAt = 10L,
                    updatedAt = 9L,
                    syncedAt = -1L,
                    version = -1L,
                ),
            )

        assertEquals(
            setOf(
                "INVALID_CALENDAR_DAY_KEY",
                "INVALID_PLANNED_BUDGET",
                "NEGATIVE_VERSION",
                "INVALID_TIMESTAMP",
                "UPDATED_BEFORE_CREATED",
            ),
            violations.mapTo(mutableSetOf(), PlanningContractViolation::code),
        )
    }

    @Test
    fun `live extension requires a live DAY scope`() {
        val extension = extension()

        assertCode(extension, null, "MISSING_SCOPE")
        assertCode(extension, scope(PlanningScopeKind.TACTICAL_CYCLE), "NON_DAY_SCOPE")
        assertCode(extension, scope(PlanningScopeKind.DAY).copy(isDeleted = true), "TOMBSTONED_SCOPE")
        assertTrue(
            validateDayPlanningScopeExtensionDependency(extension, scope(PlanningScopeKind.DAY)).isEmpty(),
        )
    }

    private fun assertCode(
        extension: DayPlanningScopeExtension,
        scope: PlanningScope?,
        code: String,
    ) {
        assertTrue(
            validateDayPlanningScopeExtensionDependency(extension, scope).any { it.code == code },
        )
    }

    private fun extension() =
        DayPlanningScopeExtension(
            scopeId = PlanningScopeId("day-1"),
            calendarDayStartMillis = 1_700_000_000_000L,
            outcome = DayPlanningOutcome.COMPLETED,
            plannedBudgetMinutes = 480L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )

    private fun scope(kind: PlanningScopeKind) =
        PlanningScope(
            id = PlanningScopeId("day-1"),
            kind = kind,
            lifecycle = PlanningScopeLifecycle.CLOSED,
            title = null,
            startsAt = null,
            endsAt = null,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )
}
