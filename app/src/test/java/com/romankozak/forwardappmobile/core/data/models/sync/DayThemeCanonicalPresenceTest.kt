package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayThemeCanonicalPresenceTest {
    private val gson = Gson()

    @Test
    fun `all eight canonical presence combinations follow the frozen contract`() {
        val cases =
            listOf(
                Triple(false, false, false) to DayThemeCanonicalPresence.LEGACY,
                Triple(false, false, true) to DayThemeCanonicalPresence.MALFORMED,
                Triple(false, true, false) to DayThemeCanonicalPresence.MALFORMED,
                Triple(false, true, true) to DayThemeCanonicalPresence.MALFORMED,
                Triple(true, false, false) to DayThemeCanonicalPresence.MALFORMED,
                Triple(true, false, true) to DayThemeCanonicalPresence.MALFORMED,
                Triple(true, true, false) to DayThemeCanonicalPresence.MALFORMED,
                Triple(true, true, true) to DayThemeCanonicalPresence.CANONICAL,
            )

        cases.forEach { (presence, expected) ->
            val (definitions, dayThemes, assignments) = presence
            val fields =
                buildList {
                    if (definitions) add("\"themeDefinitions\":[]")
                    if (dayThemes) add("\"dayThemes\":[]")
                    if (assignments) add("\"dayThemeAssignmentDocuments\":[]")
                }
            val json = "{${fields.joinToString(",")}}"
            val objectValue = JsonParser.parseString(json).asJsonObject

            assertEquals(expected, classifyDayThemeCanonicalPresence(objectValue))
        }
    }

    @Test
    fun `three empty canonical arrays are authoritative rather than legacy`() {
        val json =
            """{
                "themeDefinitions": [],
                "dayThemes": [],
                "dayThemeAssignmentDocuments": [],
                "dayThemeDocuments": [{"dayPlanId":"legacy-day","contentJson":"{}","createdAt":1,"isDeleted":false,"version":1}]
            }""".trimIndent()

        val raw = JsonParser.parseString(json).asJsonObject
        val decoded = gson.fromJson(json, SnapshotBundle::class.java)

        assertEquals(DayThemeCanonicalPresence.CANONICAL, classifyDayThemeCanonicalPresence(raw))
        assertNotNull(decoded.themeDefinitions)
        assertNotNull(decoded.dayThemes)
        assertNotNull(decoded.dayThemeAssignmentDocuments)
        assertTrue(decoded.themeDefinitions!!.isEmpty())
        assertTrue(decoded.dayThemes!!.isEmpty())
        assertTrue(decoded.dayThemeAssignmentDocuments!!.isEmpty())
        assertEquals(1, decoded.dayThemeDocuments.size)
    }

    @Test
    fun `all absent canonical fields remain distinguishable from present empty arrays`() {
        val json = "{\"dayThemeDocuments\":[]}"
        val raw = JsonParser.parseString(json).asJsonObject
        val decoded = gson.fromJson(json, SnapshotBundle::class.java)

        assertEquals(DayThemeCanonicalPresence.LEGACY, classifyDayThemeCanonicalPresence(raw))
        assertEquals(null, decoded.themeDefinitions)
        assertEquals(null, decoded.dayThemes)
        assertEquals(null, decoded.dayThemeAssignmentDocuments)
    }

    @Test
    fun `explicit null canonical fields are malformed not legacy`() {
        val json =
            """{
                "themeDefinitions": null,
                "dayThemes": null,
                "dayThemeAssignmentDocuments": null
            }""".trimIndent()

        val raw = JsonParser.parseString(json).asJsonObject

        assertEquals(DayThemeCanonicalPresence.MALFORMED, classifyDayThemeCanonicalPresence(raw))
    }

    @Test
    fun `canonical DTO fields survive Gson roundtrip`() {
        val bundle =
            SnapshotBundle(
                themeDefinitions =
                    listOf(
                        ThemeDefinitionSnapshot(
                            id = "theme-work",
                            title = "Work",
                            colorArgb = 0xFF2563EB,
                            iconKey = "work",
                            description = "Protected work time",
                            carryForward = true,
                            archived = false,
                            createdAt = 1,
                            updatedAt = 2,
                            syncedAt = null,
                            version = 3,
                            isDeleted = true,
                        ),
                    ),
                dayThemes =
                    listOf(
                        DayThemeSnapshot(
                            id = "day_theme:5:day-1:10:theme-work",
                            themeId = "theme-work",
                            dayPlanId = "day-1",
                            budgetPercent = 35,
                            order = 2,
                            isActive = true,
                            createdAt = 3,
                            updatedAt = 4,
                            syncedAt = null,
                            version = 1,
                            isDeleted = false,
                        ),
                    ),
                dayThemeAssignmentDocuments =
                    listOf(
                        DayThemeAssignmentDocumentSnapshot(
                            dayPlanId = "day-1",
                            assignments = emptyList(),
                            createdAt = 5,
                            updatedAt = 6,
                            syncedAt = null,
                            version = 1,
                            isDeleted = false,
                        ),
                    ),
            )

        val json = gson.toJson(bundle)
        val raw = JsonParser.parseString(json).asJsonObject
        val decoded = gson.fromJson(json, SnapshotBundle::class.java)

        assertEquals(DayThemeCanonicalPresence.CANONICAL, classifyDayThemeCanonicalPresence(raw))
        assertEquals("Work", decoded.themeDefinitions!!.single().title)
        assertTrue(decoded.themeDefinitions!!.single().isDeleted)
        assertEquals("theme-work", decoded.dayThemes!!.single().themeId)
        assertEquals("day-1", decoded.dayThemeAssignmentDocuments!!.single().dayPlanId)
        assertFalse(json.contains("\"themeIds\""))
        assertTrue(json.contains("\"dayThemeAssignmentDocuments\""))
    }
}
