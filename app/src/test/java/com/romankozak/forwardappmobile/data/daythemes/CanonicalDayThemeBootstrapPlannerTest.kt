package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalDayThemeBootstrapPlannerTest {
    @Test
    fun `existing canonical rows always win over legacy bootstrap candidates`() {
        val existingDefinition = definition("theme-existing")
        val existingDayTheme =
            dayTheme(
                id = "already-canonical-authority",
                themeId = "theme-existing",
                dayPlanId = "day-1",
            )
        val existingAssignment = assignmentDocument("day-1")

        val migrated =
            LegacyDayThemeCanonicalMigrationResult(
                themeDefinitions = listOf(existingDefinition, definition("theme-new")),
                dayThemes =
                    listOf(
                        dayTheme(
                            id = "day_theme:5:day-1:14:theme-existing",
                            themeId = "theme-existing",
                            dayPlanId = "day-1",
                        ),
                        dayTheme(
                            id = "day_theme:5:day-2:9:theme-new",
                            themeId = "theme-new",
                            dayPlanId = "day-2",
                        ),
                    ),
                dayThemeAssignmentDocuments =
                    listOf(
                        assignmentDocument("day-1"),
                        assignmentDocument("day-2"),
                    ),
                diagnostics = emptyList(),
            )

        val rows =
            planCanonicalDayThemeBootstrapRows(
                migrated = migrated,
                existingThemeDefinitions = listOf(existingDefinition),
                existingDayThemes = listOf(existingDayTheme),
                existingAssignmentDocuments = listOf(existingAssignment),
            )

        assertEquals(listOf("theme-new"), rows.themeDefinitions.map { it.id })
        assertEquals(listOf("day-2"), rows.dayThemes.map { it.dayPlanId })
        assertEquals(listOf("day-2"), rows.assignmentDocuments.map { it.dayPlanId })
    }

    @Test
    fun `empty canonical storage accepts all migrated rows`() {
        val migrated =
            LegacyDayThemeCanonicalMigrationResult(
                themeDefinitions = listOf(definition("theme-1")),
                dayThemes = listOf(dayTheme("canonical-1", "theme-1", "day-1")),
                dayThemeAssignmentDocuments = listOf(assignmentDocument("day-1")),
                diagnostics = emptyList(),
            )

        val rows =
            planCanonicalDayThemeBootstrapRows(
                migrated = migrated,
                existingThemeDefinitions = emptyList(),
                existingDayThemes = emptyList(),
                existingAssignmentDocuments = emptyList(),
            )

        assertEquals(migrated.themeDefinitions, rows.themeDefinitions)
        assertEquals(migrated.dayThemes, rows.dayThemes)
        assertEquals(migrated.dayThemeAssignmentDocuments, rows.assignmentDocuments)
    }

    private fun definition(id: String) =
        ThemeDefinitionSnapshot(
            id = id,
            title = id,
            colorArgb = 0xFF2563EB,
            iconKey = "target",
            description = "",
            carryForward = true,
            archived = false,
            createdAt = 1,
            updatedAt = 1,
            syncedAt = null,
            version = 1,
            isDeleted = false,
        )

    private fun dayTheme(
        id: String,
        themeId: String,
        dayPlanId: String,
    ) =
        DayThemeSnapshot(
            id = id,
            themeId = themeId,
            dayPlanId = dayPlanId,
            budgetPercent = 0,
            order = 0,
            isActive = true,
            createdAt = 1,
            updatedAt = 1,
            syncedAt = null,
            version = 1,
            isDeleted = false,
        )

    private fun assignmentDocument(dayPlanId: String) =
        DayThemeAssignmentDocumentSnapshot(
            dayPlanId = dayPlanId,
            assignments = emptyList(),
            createdAt = 1,
            updatedAt = 1,
            syncedAt = null,
            version = 1,
            isDeleted = false,
        )
}
