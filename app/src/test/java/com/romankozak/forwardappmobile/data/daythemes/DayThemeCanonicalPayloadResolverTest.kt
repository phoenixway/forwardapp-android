package com.romankozak.forwardappmobile.data.daythemes

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DayThemeCanonicalPayloadResolverTest {
    private val gson = Gson()

    @Test
    fun `000 migrates legacy DayTheme documents`() {
        val legacy =
            DayThemeDocumentSnapshot(
                dayPlanId = "day-1",
                contentJson =
                    """{
                        "themes": [{
                            "id":"theme-a",
                            "dayPlanId":"day-1",
                            "title":"Focus",
                            "colorArgb":4280644591,
                            "iconKey":"target",
                            "comment":"Deep work",
                            "budgetPercent":60,
                            "order":0,
                            "isActive":true,
                            "createdAt":10,
                            "updatedAt":20
                        }],
                        "assignments": [{
                            "dayPlanId":"day-1",
                            "entityId":"task-1",
                            "themeIds":["theme-a"]
                        }]
                    }""".trimIndent(),
                createdAt = 10,
                updatedAt = 20,
                isDeleted = false,
                version = 2,
            )
        val source = SnapshotBundle(dayThemeDocuments = listOf(legacy))
        val raw = JsonParser.parseString(gson.toJson(source)).asJsonObject
        val decoded = gson.fromJson(raw, SnapshotBundle::class.java)

        val resolved = DayThemeCanonicalPayloadResolver.resolve(raw, decoded)

        assertEquals(DayThemeCanonicalPayloadSource.LEGACY_MIGRATION, resolved.source)
        assertEquals("theme-a", resolved.themeDefinitions.single().id)
        assertEquals("Deep work", resolved.themeDefinitions.single().description)
        assertEquals(
            canonicalDayThemeId("day-1", "theme-a"),
            resolved.dayThemes.single().id,
        )
        assertEquals(
            listOf(canonicalDayThemeId("day-1", "theme-a")),
            resolved.dayThemeAssignmentDocuments.single().assignments.single().dayThemeIds,
        )
    }

    @Test
    fun `111 canonical empty arrays suppress stale legacy state completely`() {
        val staleLegacy =
            DayThemeDocumentSnapshot(
                dayPlanId = "legacy-day",
                contentJson = "{definitely-not-valid-json",
                createdAt = 1,
                updatedAt = 2,
                isDeleted = false,
                version = 9,
            )
        val source =
            SnapshotBundle(
                dayThemeDocuments = listOf(staleLegacy),
                themeDefinitions = emptyList(),
                dayThemes = emptyList(),
                dayThemeAssignmentDocuments = emptyList(),
            )
        val raw = JsonParser.parseString(gson.toJson(source)).asJsonObject
        val decoded = gson.fromJson(raw, SnapshotBundle::class.java)

        val resolved = DayThemeCanonicalPayloadResolver.resolve(raw, decoded)

        assertEquals(DayThemeCanonicalPayloadSource.CANONICAL, resolved.source)
        assertTrue(resolved.themeDefinitions.isEmpty())
        assertTrue(resolved.dayThemes.isEmpty())
        assertTrue(resolved.dayThemeAssignmentDocuments.isEmpty())
        assertTrue(resolved.diagnostics.isEmpty())
    }

    @Test
    fun `111 returns canonical trio exactly without legacy merge`() {
        val definition =
            ThemeDefinitionSnapshot(
                id = "theme-a",
                title = "Canonical",
                colorArgb = 123,
                iconKey = "brain",
                description = "Canonical description",
                carryForward = false,
                archived = true,
                createdAt = 10,
                updatedAt = 20,
                syncedAt = 30,
                version = 4,
                isDeleted = true,
            )
        val dayTheme =
            DayThemeSnapshot(
                id = canonicalDayThemeId("day-1", "theme-a"),
                themeId = "theme-a",
                dayPlanId = "day-1",
                budgetPercent = 42,
                order = 7,
                isActive = false,
                createdAt = 11,
                updatedAt = 21,
                syncedAt = 31,
                version = 5,
                isDeleted = false,
            )
        val assignmentDocument =
            DayThemeAssignmentDocumentSnapshot(
                dayPlanId = "day-1",
                assignments = emptyList(),
                createdAt = 12,
                updatedAt = 22,
                syncedAt = 32,
                version = 6,
                isDeleted = false,
            )
        val source =
            SnapshotBundle(
                dayThemeDocuments =
                    listOf(
                        DayThemeDocumentSnapshot(
                            dayPlanId = "day-1",
                            contentJson = "{}",
                            createdAt = 999,
                            updatedAt = 999,
                            isDeleted = false,
                            version = 999,
                        ),
                    ),
                themeDefinitions = listOf(definition),
                dayThemes = listOf(dayTheme),
                dayThemeAssignmentDocuments = listOf(assignmentDocument),
            )
        val raw = JsonParser.parseString(gson.toJson(source)).asJsonObject
        val decoded = gson.fromJson(raw, SnapshotBundle::class.java)

        val resolved = DayThemeCanonicalPayloadResolver.resolve(raw, decoded)

        assertEquals(DayThemeCanonicalPayloadSource.CANONICAL, resolved.source)
        assertEquals(listOf(definition), resolved.themeDefinitions)
        assertEquals(listOf(dayTheme), resolved.dayThemes)
        assertEquals(listOf(assignmentDocument), resolved.dayThemeAssignmentDocuments)
        assertTrue(resolved.diagnostics.isEmpty())
    }

    @Test
    fun `partial canonical presence is rejected instead of falling back to legacy`() {
        expectMalformed(
            """{
                "themeDefinitions": [],
                "dayThemeDocuments": []
            }""".trimIndent(),
        )
    }

    @Test
    fun `explicit null canonical trio is rejected`() {
        expectMalformed(
            """{
                "themeDefinitions": null,
                "dayThemes": null,
                "dayThemeAssignmentDocuments": null,
                "dayThemeDocuments": []
            }""".trimIndent(),
        )
    }

    @Test
    fun `wrong canonical field shape is rejected`() {
        expectMalformed(
            """{
                "themeDefinitions": {},
                "dayThemes": [],
                "dayThemeAssignmentDocuments": [],
                "dayThemeDocuments": []
            }""".trimIndent(),
        )
    }

    private fun expectMalformed(json: String) {
        val raw = JsonParser.parseString(json).asJsonObject

        try {
            DayThemeCanonicalPayloadResolver.resolve(
                rawSnapshotBundle = raw,
                decodedBundle = SnapshotBundle(),
            )
            fail("Expected malformed canonical Day Themes payload")
        } catch (_: MalformedDayThemeCanonicalPayloadException) {
            // Expected.
        }
    }
}
