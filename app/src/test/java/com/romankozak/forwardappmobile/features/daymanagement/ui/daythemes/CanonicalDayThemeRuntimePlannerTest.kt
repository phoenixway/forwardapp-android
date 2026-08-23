package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalDayThemeRuntimePlannerTest {
    @Test
    fun `new UI theme creates global definition daily materialization and remaps assignment`() {
        val plan =
            planCanonicalDayThemeRuntimeUpdate(
                dayPlanId = "day-1",
                now = 100,
                localThemeDefinitions = emptyList(),
                localDayThemes = emptyList(),
                localAssignmentDocument = null,
                transformedDocument =
                    DayThemeDocument(
                        themes =
                            listOf(
                                DayTheme(
                                    id = "temp-theme",
                                    dayPlanId = "day-1",
                                    title = "Focus",
                                    budgetPercent = 30,
                                ),
                            ),
                        assignments =
                            listOf(
                                DayThemeAssignment(
                                    dayPlanId = "day-1",
                                    entityId = "task-1",
                                    themeIds = setOf("temp-theme"),
                                ),
                            ),
                    ),
            )

        val expectedDayThemeId = canonicalDayThemeId("day-1", "temp-theme")
        assertEquals("temp-theme", plan.themeDefinitions.single().id)
        assertTrue(plan.themeDefinitions.single().carryForward)
        assertEquals(expectedDayThemeId, plan.dayThemes.single().id)
        assertEquals("temp-theme", plan.dayThemes.single().themeId)
        assertEquals(
            listOf(expectedDayThemeId),
            plan.assignmentDocument!!.assignments.single().dayThemeIds,
        )
    }

    @Test
    fun `edit increments global and daily authorities independently`() {
        val definition = definition(version = 4, title = "Old")
        val daily = dayTheme(version = 7, budgetPercent = 10)

        val plan =
            planCanonicalDayThemeRuntimeUpdate(
                dayPlanId = "day-1",
                now = 200,
                localThemeDefinitions = listOf(definition),
                localDayThemes = listOf(daily),
                localAssignmentDocument = null,
                transformedDocument =
                    DayThemeDocument(
                        themes =
                            listOf(
                                DayTheme(
                                    id = daily.id,
                                    dayPlanId = "day-1",
                                    title = "New title",
                                    colorArgb = definition.colorArgb,
                                    iconKey = definition.iconKey,
                                    comment = definition.description,
                                    budgetPercent = 25,
                                    order = daily.order,
                                    isActive = daily.isActive,
                                ),
                            ),
                    ),
            )

        assertEquals(5, plan.themeDefinitions.single().version)
        assertEquals("New title", plan.themeDefinitions.single().title)
        assertEquals(8, plan.dayThemes.single().version)
        assertEquals(25, plan.dayThemes.single().budgetPercent)
    }

    @Test
    fun `deleting UI theme tombstones daily materialization but preserves definition`() {
        val definition = definition()
        val daily = dayTheme()
        val assignments =
            DayThemeAssignmentDocumentSnapshot(
                dayPlanId = "day-1",
                assignments =
                    listOf(
                        DayThemeAssignmentSnapshot(
                            entityId = "task-1",
                            dayThemeIds = listOf(daily.id),
                        ),
                    ),
                createdAt = 1,
                updatedAt = 1,
                syncedAt = null,
                version = 2,
                isDeleted = false,
            )

        val plan =
            planCanonicalDayThemeRuntimeUpdate(
                dayPlanId = "day-1",
                now = 300,
                localThemeDefinitions = listOf(definition),
                localDayThemes = listOf(daily),
                localAssignmentDocument = assignments,
                transformedDocument = DayThemeDocument(),
            )

        assertTrue(plan.themeDefinitions.isEmpty())
        assertTrue(plan.dayThemes.single().isDeleted)
        assertEquals(daily.version + 1, plan.dayThemes.single().version)
        assertFalse(plan.assignmentDocument!!.isDeleted)
        assertTrue(plan.assignmentDocument.assignments.isEmpty())
        assertEquals(3, plan.assignmentDocument.version)
    }

    @Test
    fun `project then persist unchanged is a no-op`() {
        val definition = definition()
        val daily = dayTheme()
        val assignment =
            DayThemeAssignmentDocumentSnapshot(
                dayPlanId = "day-1",
                assignments =
                    listOf(
                        DayThemeAssignmentSnapshot(
                            entityId = "task-1",
                            dayThemeIds = listOf(daily.id),
                        ),
                    ),
                createdAt = 1,
                updatedAt = 2,
                syncedAt = null,
                version = 1,
                isDeleted = false,
            )

        val projected =
            projectCanonicalDayThemeDocument(
                dayPlanId = "day-1",
                themeDefinitions = listOf(definition),
                dayThemes = listOf(daily),
                assignmentDocument = assignment,
            )

        val plan =
            planCanonicalDayThemeRuntimeUpdate(
                dayPlanId = "day-1",
                now = 999,
                localThemeDefinitions = listOf(definition),
                localDayThemes = listOf(daily),
                localAssignmentDocument = assignment,
                transformedDocument = projected,
            )

        assertTrue(plan.themeDefinitions.isEmpty())
        assertTrue(plan.dayThemes.isEmpty())
        assertNull(plan.assignmentDocument)
    }

    private fun definition(
        version: Long = 1,
        title: String = "Focus",
    ) =
        ThemeDefinitionSnapshot(
            id = "definition-1",
            title = title,
            colorArgb = 0xFF2563EB,
            iconKey = "target",
            description = "comment",
            carryForward = true,
            archived = false,
            createdAt = 1,
            updatedAt = 2,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )

    private fun dayTheme(
        version: Long = 1,
        budgetPercent: Int = 20,
    ) =
        DayThemeSnapshot(
            id = canonicalDayThemeId("day-1", "definition-1"),
            themeId = "definition-1",
            dayPlanId = "day-1",
            budgetPercent = budgetPercent,
            order = 0,
            isActive = true,
            createdAt = 1,
            updatedAt = 2,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )
}
