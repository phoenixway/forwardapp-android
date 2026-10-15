package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeType
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateTacticalPlanningScopeExtensionDependency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalTacticalPlanningMappingTest {
    @Test
    fun `iteration identity and shared fields map without replacement identity`() {
        val source = iteration(status = TacticalIterationStatus.ACTIVE)
        val scope = source.toCanonicalTacticalPlanningScope()

        assertEquals(source.id, scope.id.value)
        assertEquals(PlanningScopeKind.TACTICAL_CYCLE, scope.kind)
        assertEquals(PlanningScopeLifecycle.ACTIVE, scope.lifecycle)
        assertEquals(source.title, scope.title)
        assertEquals(source.startedAt, scope.startsAt)
        assertEquals(source.plannedEndAt, scope.endsAt)
        assertEquals(source.createdAt, scope.createdAt)
        assertEquals(source.updatedAt, scope.updatedAt)
        assertEquals(source.syncedAt, scope.syncedAt)
        assertEquals(source.version, scope.version)
        assertEquals(source.isDeleted, scope.isDeleted)
    }

    @Test
    fun `all specialized lifecycle values map exactly`() {
        val expected =
            mapOf(
                TacticalIterationStatus.DRAFT to PlanningScopeLifecycle.DRAFT,
                TacticalIterationStatus.ACTIVE to PlanningScopeLifecycle.ACTIVE,
                TacticalIterationStatus.CLOSED to PlanningScopeLifecycle.CLOSED,
                TacticalIterationStatus.ARCHIVED to PlanningScopeLifecycle.ARCHIVED,
            )

        assertEquals(expected, TacticalIterationStatus.entries.associateWith { it.toCanonicalTacticalScopeLifecycle() })
    }

    @Test
    fun `tactical-only state maps to typed extension`() {
        val source = iteration(status = TacticalIterationStatus.CLOSED, closedAt = 400L)
        val scope = source.toCanonicalTacticalPlanningScope()
        val extension = source.toCanonicalTacticalPlanningScopeExtension()

        assertEquals(scope.id, extension.scopeId)
        assertEquals(TacticalPlanningScopeType.TIMEBOXED, extension.type)
        assertEquals(source.weekKey, extension.weekKey)
        assertEquals(source.closedAt, extension.closedAt)
        assertEquals(source.createdAt, extension.createdAt)
        assertEquals(source.updatedAt, extension.updatedAt)
        assertEquals(source.syncedAt, extension.syncedAt)
        assertEquals(source.version, extension.version)
        assertEquals(source.isDeleted, extension.isDeleted)
        assertTrue(validateTacticalPlanningScopeExtensionDependency(extension, scope).isEmpty())
    }

    @Test
    fun `open-ended iteration has no fabricated end or week key`() {
        val source =
            iteration(
                type = TacticalIterationType.OPEN_ENDED,
                weekKey = null,
                plannedEndAt = null,
            )
        val scope = source.toCanonicalTacticalPlanningScope()
        val extension = source.toCanonicalTacticalPlanningScopeExtension()

        assertEquals(null, scope.endsAt)
        assertEquals(TacticalPlanningScopeType.OPEN_ENDED, extension.type)
        assertEquals(null, extension.weekKey)
        assertTrue(validateTacticalPlanningScopeExtensionDependency(extension, scope).isEmpty())
    }

    @Test
    fun `blank title fails closed instead of losing parity`() {
        assertThrows(IllegalArgumentException::class.java) {
            iteration(title = "  ").toCanonicalTacticalPlanningScope()
        }
    }

    private fun iteration(
        title: String = "Tactical cycle",
        status: TacticalIterationStatus = TacticalIterationStatus.DRAFT,
        type: TacticalIterationType = TacticalIterationType.TIMEBOXED,
        weekKey: String? = "2026-W40",
        plannedEndAt: Long? = 200L,
        closedAt: Long? = null,
    ) = TacticalIteration(
        id = "cycle-1",
        title = title,
        startedAt = 100L,
        plannedEndAt = plannedEndAt,
        closedAt = closedAt,
        status = status,
        type = type,
        weekKey = weekKey,
        createdAt = 90L,
        updatedAt = 150L,
        syncedAt = 140L,
        isDeleted = false,
        version = 7L,
    )
}
