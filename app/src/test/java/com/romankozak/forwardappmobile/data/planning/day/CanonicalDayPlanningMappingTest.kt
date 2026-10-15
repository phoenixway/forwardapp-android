package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalDayPlanningMappingTest {
    @Test
    fun `DayPlan durable id and canonical metadata map exactly to DAY scope`() {
        val source =
            dayPlan(
                id = "existing-durable-day-id",
                status = DayStatus.IN_PROGRESS,
                name = "  Release day  ",
            )

        val scope = source.toCanonicalDayPlanningScope()

        assertEquals("existing-durable-day-id", scope.id.value)
        assertEquals(PlanningScopeKind.DAY, scope.kind)
        assertEquals(PlanningScopeLifecycle.ACTIVE, scope.lifecycle)
        assertEquals("Release day", scope.title)
        assertEquals(source.date, scope.startsAt)
        assertNull(scope.endsAt)
        assertEquals(source.createdAt, scope.createdAt)
        assertEquals(source.updatedAt, scope.updatedAt)
        assertEquals(source.syncedAt, scope.syncedAt)
        assertEquals(source.version, scope.version)
        assertFalse(scope.isDeleted)
    }

    @Test
    fun `blank legacy name becomes absent optional scope title`() {
        assertNull(dayPlan(name = "   ").toCanonicalDayPlanningScope().title)
    }

    @Test
    fun `Day outcomes remain outside common lifecycle while scope maps closed`() {
        assertEquals(
            PlanningScopeLifecycle.CLOSED,
            dayPlan(status = DayStatus.COMPLETED).toCanonicalDayPlanningScope().lifecycle,
        )
        assertEquals(
            PlanningScopeLifecycle.CLOSED,
            dayPlan(status = DayStatus.MISSED).toCanonicalDayPlanningScope().lifecycle,
        )
    }

    @Test
    fun `typed extension preserves calendar budget and only terminal Day outcome`() {
        val completed = dayPlan(status = DayStatus.COMPLETED).toCanonicalDayPlanningScopeExtension()
        val missed = dayPlan(status = DayStatus.MISSED).toCanonicalDayPlanningScopeExtension()
        val planned = dayPlan(status = DayStatus.PLANNED).toCanonicalDayPlanningScopeExtension()

        assertEquals("day-plan", completed.scopeId.value)
        assertEquals(1_780_000_000_000L, completed.calendarDayStartMillis)
        assertEquals(480L, completed.plannedBudgetMinutes)
        assertEquals(DayPlanningOutcome.COMPLETED, completed.outcome)
        assertEquals(DayPlanningOutcome.MISSED, missed.outcome)
        assertNull(planned.outcome)
    }

    @Test
    fun `all shared lifecycle mappings are explicit`() {
        assertEquals(
            PlanningScopeLifecycle.DRAFT,
            DayStatus.PLANNED.toCanonicalDayScopeLifecycle(),
        )
        assertEquals(
            PlanningScopeLifecycle.ACTIVE,
            DayStatus.IN_PROGRESS.toCanonicalDayScopeLifecycle(),
        )
        assertEquals(
            PlanningScopeLifecycle.ARCHIVED,
            DayStatus.ARCHIVED.toCanonicalDayScopeLifecycle(),
        )
    }

    private fun dayPlan(
        id: String = "day-plan",
        status: DayStatus = DayStatus.PLANNED,
        name: String? = null,
    ) =
        DayPlan(
            id = id,
            date = 1_780_000_000_000L,
            name = name,
            status = status,
            reflection = "execution evidence stays outside PlanningScope",
            predictedDurationMinutes = 480L,
            totalPlannedMinutes = 300L,
            totalCompletedMinutes = 120L,
            completionPercentage = 0.4f,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 30L,
            version = 7L,
            isDeleted = false,
        )
}
