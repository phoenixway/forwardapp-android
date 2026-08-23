package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CanonicalDayThemeMergePlannerTest {
    @Test
    fun `DayPlan remap recomputes DayTheme identity and assignment references`() {
        val oldId = canonicalDayThemeId("incoming-day", "theme-1")
        val newId = canonicalDayThemeId("local-day", "theme-1")

        val plan =
            planCanonicalDayThemeMerge(
                incomingThemeDefinitions = listOf(definition()),
                incomingDayThemes = listOf(dayTheme(id = oldId, dayPlanId = "incoming-day")),
                incomingAssignmentDocuments =
                    listOf(
                        assignmentDocument(
                            dayPlanId = "incoming-day",
                            dayThemeId = oldId,
                        ),
                    ),
                incomingPlanIdRemap = mapOf("incoming-day" to "local-day"),
                validPlanIds = setOf("local-day"),
                localThemeDefinitions = emptyList(),
                localDayThemes = emptyList(),
                localAssignmentDocuments = emptyList(),
            )

        assertEquals("local-day", plan.dayThemes.single().dayPlanId)
        assertEquals(newId, plan.dayThemes.single().id)
        assertEquals("local-day", plan.assignmentDocuments.single().dayPlanId)
        assertEquals(
            listOf(newId),
            plan.assignmentDocuments.single().assignments.single().dayThemeIds,
        )
    }

    @Test
    fun `higher version beats newer local timestamp`() {
        val incoming = definition(version = 3, updatedAt = 10)
        val local = definition(version = 2, updatedAt = 999)

        val plan = mergeDefinitions(incoming, local)

        assertEquals(listOf(incoming), plan.themeDefinitions)
    }

    @Test
    fun `newer updatedAt wins when versions tie`() {
        val incoming = definition(version = 3, updatedAt = 20)
        val local = definition(version = 3, updatedAt = 10)

        val plan = mergeDefinitions(incoming, local)

        assertEquals(listOf(incoming), plan.themeDefinitions)
    }

    @Test
    fun `tombstone wins exact version and timestamp tie`() {
        val incoming = definition(version = 3, updatedAt = 20, isDeleted = true)
        val local = definition(version = 3, updatedAt = 20, isDeleted = false)

        val plan = mergeDefinitions(incoming, local)

        assertEquals(listOf(incoming), plan.themeDefinitions)
    }

    @Test
    fun `local wins exact semantic tie`() {
        val incoming = definition(version = 3, updatedAt = 20)
        val local = definition(version = 3, updatedAt = 20)

        val plan = mergeDefinitions(incoming, local)

        assertTrue(plan.themeDefinitions.isEmpty())
    }

    @Test
    fun `colliding identities after DayPlan remap fail closed`() {
        val first = dayTheme(
            id = canonicalDayThemeId("day-a", "theme-1"),
            dayPlanId = "day-a",
        )
        val second = dayTheme(
            id = canonicalDayThemeId("day-b", "theme-1"),
            dayPlanId = "day-b",
        )

        try {
            planCanonicalDayThemeMerge(
                incomingThemeDefinitions = listOf(definition()),
                incomingDayThemes = listOf(first, second),
                incomingAssignmentDocuments = emptyList(),
                incomingPlanIdRemap = mapOf("day-a" to "local-day", "day-b" to "local-day"),
                validPlanIds = setOf("local-day"),
                localThemeDefinitions = emptyList(),
                localDayThemes = emptyList(),
                localAssignmentDocuments = emptyList(),
            )
            fail("Expected remap collision to fail closed")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun `legacy 000 merge is translated directly into canonical authority`() {
        val localDefinition = definition(version = 4, updatedAt = 100)
        val legacyDocument =
            DayThemeDocumentSnapshot(
                dayPlanId = "legacy-day",
                contentJson =
                    """{
                        "themes": [
                            {
                                "id": "theme-1",
                                "title": "Legacy title",
                                "budgetPercent": 35,
                                "order": 2,
                                "isActive": true
                            }
                        ],
                        "assignments": [
                            {
                                "entityId": "task-1",
                                "themeIds": ["theme-1"]
                            }
                        ]
                    }""".trimIndent(),
                createdAt = 10,
                updatedAt = 20,
                version = 5,
            )

        val plan =
            planLegacyDayThemeMerge(
                incomingLegacyDocuments = listOf(legacyDocument),
                incomingPlanIdRemap = mapOf("legacy-day" to "local-day"),
                validPlanIds = setOf("local-day"),
                localThemeDefinitions = listOf(localDefinition),
                localDayThemes = emptyList(),
                localAssignmentDocuments = emptyList(),
            )

        // Existing canonical global definition keeps authority over legacy descriptive fields.
        assertTrue(plan.themeDefinitions.isEmpty())

        val expectedDayThemeId = canonicalDayThemeId("local-day", "theme-1")
        assertEquals("local-day", plan.dayThemes.single().dayPlanId)
        assertEquals(expectedDayThemeId, plan.dayThemes.single().id)
        assertEquals(35, plan.dayThemes.single().budgetPercent)

        assertEquals("local-day", plan.assignmentDocuments.single().dayPlanId)
        assertEquals(
            listOf(expectedDayThemeId),
            plan.assignmentDocuments.single().assignments.single().dayThemeIds,
        )
    }

    private fun mergeDefinitions(
        incoming: ThemeDefinitionSnapshot,
        local: ThemeDefinitionSnapshot,
    ) =
        planCanonicalDayThemeMerge(
            incomingThemeDefinitions = listOf(incoming),
            incomingDayThemes = emptyList(),
            incomingAssignmentDocuments = emptyList(),
            incomingPlanIdRemap = emptyMap(),
            validPlanIds = emptySet(),
            localThemeDefinitions = listOf(local),
            localDayThemes = emptyList(),
            localAssignmentDocuments = emptyList(),
        )

    private fun definition(
        version: Long = 1,
        updatedAt: Long = 1,
        isDeleted: Boolean = false,
    ) =
        ThemeDefinitionSnapshot(
            id = "theme-1",
            title = "Focus",
            colorArgb = 1,
            iconKey = "target",
            description = "",
            carryForward = true,
            archived = false,
            createdAt = 1,
            updatedAt = updatedAt,
            syncedAt = null,
            version = version,
            isDeleted = isDeleted,
        )

    private fun dayTheme(
        id: String,
        dayPlanId: String,
    ) =
        DayThemeSnapshot(
            id = id,
            themeId = "theme-1",
            dayPlanId = dayPlanId,
            budgetPercent = 40,
            order = 0,
            isActive = true,
            createdAt = 1,
            updatedAt = 1,
            syncedAt = null,
            version = 1,
            isDeleted = false,
        )

    private fun assignmentDocument(
        dayPlanId: String,
        dayThemeId: String,
    ) =
        DayThemeAssignmentDocumentSnapshot(
            dayPlanId = dayPlanId,
            assignments =
                listOf(
                    DayThemeAssignmentSnapshot(
                        entityId = "task-1",
                        dayThemeIds = listOf(dayThemeId),
                    ),
                ),
            createdAt = 1,
            updatedAt = 1,
            syncedAt = null,
            version = 1,
            isDeleted = false,
        )
}
