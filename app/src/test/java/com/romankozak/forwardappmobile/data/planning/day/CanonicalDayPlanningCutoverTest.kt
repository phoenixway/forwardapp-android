package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalDayThemeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CanonicalDayPlanningCutoverTest {
    @Test
    fun `planner preserves Day identity and resolves Theme through CUT_OVER mapping`() {
        val plan = planCanonicalDayPlanningCutover(
            dayPlans = listOf(day()),
            dayThemes = listOf(dayTheme()),
            mappings = listOf(mapping()),
            subjects = listOf(subject()),
            orientations = listOf(orientation()),
        )

        assertEquals("day-1", plan.scopes.single().id.value)
        assertEquals("day-1", plan.extensions.single().scopeId.value)
        with(plan.commitments.single()) {
            assertEquals("theme-subject", orientationId)
            assertEquals(PlanningRoleCode.DAY_THEME, role)
            assertEquals(7L, order)
            assertEquals(PlanningCommitmentStatus.ACTIVE, status)
        }
    }

    @Test
    fun `commitment id is deterministic and source participation specific`() {
        assertEquals(
            canonicalDayThemeCommitmentId("participation-1"),
            canonicalDayThemeCommitmentId("participation-1"),
        )
        assertNotEquals(
            canonicalDayThemeCommitmentId("participation-1"),
            canonicalDayThemeCommitmentId("participation-2"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `live participation refuses MATERIALIZED Theme mapping`() {
        planCanonicalDayPlanningCutover(
            listOf(day()),
            listOf(dayTheme()),
            listOf(mapping(state = LegacySubjectMappingState.MATERIALIZED.name)),
            listOf(subject()),
            listOf(orientation()),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate live semantic participation fails closed`() {
        planCanonicalDayPlanningCutover(
            listOf(day()),
            listOf(dayTheme(id = "p1"), dayTheme(id = "p2")),
            listOf(mapping()),
            listOf(subject()),
            listOf(orientation()),
        )
    }

    private fun day() =
        DayPlan(
            id = "day-1",
            date = 1_700_000_000_000L,
            name = "Day",
            status = DayStatus.IN_PROGRESS,
            predictedDurationMinutes = 480L,
            createdAt = 10L,
            updatedAt = 11L,
            version = 4L,
        )

    private fun dayTheme(id: String = "participation-1") =
        CanonicalDayThemeEntity(
            id = id,
            themeId = "theme-definition",
            dayPlanId = "day-1",
            budgetPercent = 40,
            order = 7L,
            isActive = true,
            createdAt = 20L,
            updatedAt = 21L,
            syncedAt = null,
            version = 3L,
            isDeleted = false,
        )

    private fun mapping(state: String = LegacySubjectMappingState.CUT_OVER.name) =
        LegacySubjectMappingEntity(
            id = "mapping",
            sourceType = LegacyOrientationSourceType.THEME_DEFINITION.name,
            sourceId = "theme-definition",
            subjectId = "theme-subject",
            migrationVersion = 1,
            state = state,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun subject() =
        ManagedSubjectEntity(
            id = "theme-subject",
            subjectType = ManagedSubjectType.ORIENTATION.name,
            title = "Theme",
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun orientation() =
        OrientationEntity(
            subjectId = "theme-subject",
            kind = OrientationKind.THEME.name,
            lifecycle = null,
            lifecycleOrigin = "NONE",
        )
}
