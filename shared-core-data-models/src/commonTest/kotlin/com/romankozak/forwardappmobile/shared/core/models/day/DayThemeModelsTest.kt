package com.romankozak.forwardappmobile.shared.core.models.day

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayThemeModelsTest {
    @Test
    fun `ThemeDefinition tombstone retains descriptive historical state`() {
        val definition =
            ThemeDefinition(
                id = "theme-work",
                createdAt = 10,
                updatedAt = 20,
                syncedAt = null,
                isDeleted = true,
                version = 3,
                title = "Work",
                colorArgb = 0xFF2563EB,
                iconKey = "work",
                description = "Protected work time",
                carryForward = true,
                archived = false,
            )

        assertTrue(definition.isDeleted)
        assertEquals("Work", definition.title)
        assertEquals(0xFF2563EB, definition.colorArgb)
        assertEquals("work", definition.iconKey)
        assertEquals("Protected work time", definition.description)
        assertTrue(definition.carryForward)
        assertFalse(definition.archived)
    }

    @Test
    fun `DayTheme contains daily state and deterministic definition reference`() {
        val dayPlanId = "day-1"
        val themeId = "theme-work"
        val dayTheme =
            DayTheme(
                id = canonicalDayThemeId(dayPlanId, themeId),
                createdAt = 30,
                updatedAt = 40,
                syncedAt = null,
                isDeleted = false,
                version = 1,
                themeId = themeId,
                dayPlanId = dayPlanId,
                budgetPercent = 35,
                order = 2,
                isActive = true,
            )

        assertEquals(canonicalDayThemeId(dayPlanId, themeId), dayTheme.id)
        assertEquals(themeId, dayTheme.themeId)
        assertEquals(dayPlanId, dayTheme.dayPlanId)
        assertEquals(35, dayTheme.budgetPercent)
        assertEquals(2L, dayTheme.order)
        assertTrue(dayTheme.isActive)
    }
}
