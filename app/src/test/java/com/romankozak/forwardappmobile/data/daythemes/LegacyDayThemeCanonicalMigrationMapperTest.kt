package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyDayThemeCanonicalMigrationMapperTest {
    @Test
    fun `same legacy definition on two days becomes one definition and two deterministic DayThemes`() {
        val result =
            LegacyDayThemeCanonicalMigrationMapper.migrate(
                listOf(
                    document(
                        dayPlanId = "day-1",
                        contentJson =
                            payload(
                                themeJson(
                                    id = "theme-a",
                                    title = "Focus",
                                    comment = "Old description",
                                    budgetPercent = 70,
                                    order = 0,
                                    updatedAt = 20,
                                ),
                            ),
                    ),
                    document(
                        dayPlanId = "day-2",
                        contentJson =
                            payload(
                                themeJson(
                                    id = "theme-a",
                                    title = "Focus",
                                    comment = "Current description",
                                    budgetPercent = 25,
                                    order = 3,
                                    updatedAt = 40,
                                ),
                            ),
                    ),
                ),
            )

        assertEquals(1, result.themeDefinitions.size)
        assertEquals("theme-a", result.themeDefinitions.single().id)
        assertEquals("Current description", result.themeDefinitions.single().description)
        assertTrue(result.themeDefinitions.single().carryForward)

        assertEquals(2, result.dayThemes.size)
        assertEquals(
            setOf(
                canonicalDayThemeId("day-1", "theme-a"),
                canonicalDayThemeId("day-2", "theme-a"),
            ),
            result.dayThemes.map { it.id }.toSet(),
        )
        assertEquals(setOf("theme-a"), result.dayThemes.map { it.themeId }.toSet())
        assertEquals(70, result.dayThemes.single { it.dayPlanId == "day-1" }.budgetPercent)
        assertEquals(25, result.dayThemes.single { it.dayPlanId == "day-2" }.budgetPercent)
        assertTrue(result.diagnostics.isEmpty())
    }

    @Test
    fun `legacy assignments remap through the concrete DayTheme identity and orphan refs are omitted`() {
        val result =
            LegacyDayThemeCanonicalMigrationMapper.migrate(
                listOf(
                    document(
                        dayPlanId = "day-1",
                        version = 7,
                        contentJson =
                            """{
                                "themes": [${themeJson(id = "theme-a", title = "Focus")}],
                                "assignments": [
                                    {
                                        "dayPlanId": "day-1",
                                        "entityId": "task-1",
                                        "themeIds": ["theme-a", "missing-theme"]
                                    }
                                ]
                            }""".trimIndent(),
                    ),
                ),
            )

        val assignmentDocument = result.dayThemeAssignmentDocuments.single()
        val assignment = assignmentDocument.assignments.single()

        assertEquals("task-1", assignment.entityId)
        assertEquals(
            listOf(canonicalDayThemeId("day-1", "theme-a")),
            assignment.dayThemeIds,
        )
        assertEquals(7L, assignmentDocument.version)
        assertEquals(null, assignmentDocument.syncedAt)

        assertEquals(1, result.diagnostics.size)
        assertEquals(
            LegacyDayThemeMigrationDiagnosticCode.ORPHAN_ASSIGNMENT_THEME,
            result.diagnostics.single().code,
        )
        assertEquals("missing-theme", result.diagnostics.single().legacyThemeId)
    }

    @Test
    fun `existing canonical definition is never overwritten by embedded legacy fields`() {
        val existing =
            ThemeDefinitionSnapshot(
                id = "theme-a",
                title = "Canonical title",
                colorArgb = 123,
                iconKey = "brain",
                description = "Canonical description",
                carryForward = false,
                archived = true,
                createdAt = 1,
                updatedAt = 999,
                syncedAt = 999,
                version = 8,
                isDeleted = true,
            )

        val result =
            LegacyDayThemeCanonicalMigrationMapper.migrate(
                legacyDocuments =
                    listOf(
                        document(
                            dayPlanId = "day-1",
                            contentJson =
                                payload(
                                    themeJson(
                                        id = "theme-a",
                                        title = "Legacy title",
                                        comment = "Legacy description",
                                        updatedAt = 5000,
                                    ),
                                ),
                        ),
                    ),
                existingThemeDefinitions = listOf(existing),
            )

        assertEquals(listOf(existing), result.themeDefinitions)
        assertEquals("theme-a", result.dayThemes.single().themeId)
        assertFalse(result.dayThemes.single().isDeleted)
    }

    @Test
    fun `migration is idempotent for identical legacy input`() {
        val input =
            listOf(
                document(
                    dayPlanId = "day-2",
                    version = 4,
                    contentJson =
                        """{
                            "themes": [
                                ${themeJson(id = "b", title = "B", order = 2)},
                                ${themeJson(id = "a", title = "A", order = 1)}
                            ],
                            "assignments": [
                                {"dayPlanId":"day-2","entityId":"task-z","themeIds":["b","a","a"]}
                            ]
                        }""".trimIndent(),
                ),
            )

        val first = LegacyDayThemeCanonicalMigrationMapper.migrate(input)
        val second = LegacyDayThemeCanonicalMigrationMapper.migrate(input)

        assertEquals(first, second)
        assertEquals(listOf("a", "b"), first.themeDefinitions.map { it.id })
        assertEquals(
            listOf(
                canonicalDayThemeId("day-2", "a"),
                canonicalDayThemeId("day-2", "b"),
            ).sorted(),
            first.dayThemeAssignmentDocuments.single().assignments.single().dayThemeIds,
        )
    }

    private fun document(
        dayPlanId: String,
        contentJson: String,
        version: Long = 1,
    ): DayThemeDocumentSnapshot =
        DayThemeDocumentSnapshot(
            dayPlanId = dayPlanId,
            contentJson = contentJson,
            createdAt = 10,
            updatedAt = 100,
            isDeleted = false,
            version = version,
        )

    private fun payload(theme: String): String =
        """{"themes":[$theme],"assignments":[]}"""

    private fun themeJson(
        id: String,
        title: String,
        comment: String = "",
        budgetPercent: Int = 0,
        order: Long = 0,
        updatedAt: Long = 20,
    ): String =
        """{
            "id":"$id",
            "dayPlanId":"legacy-day",
            "title":"$title",
            "colorArgb":4280644591,
            "iconKey":"target",
            "comment":"$comment",
            "budgetPercent":$budgetPercent,
            "order":$order,
            "isActive":true,
            "createdAt":10,
            "updatedAt":$updatedAt
        }""".trimIndent()
}
