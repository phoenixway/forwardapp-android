package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.sync.datasource.CanonicalDayThemeSyncPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalDayThemeWifiPushPlanTest {
    @Test
    fun `Day Theme only dirty state still requires wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalDayThemes =
                    CanonicalDayThemeSyncPayload(
                        themeDefinitions = listOf(definition(version = 5)),
                    ),
            ),
        )
    }

    @Test
    fun `empty canonical Day Theme state does not force wifi push`() {
        assertFalse(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalDayThemes = CanonicalDayThemeSyncPayload(),
            ),
        )
    }

    @Test
    fun `definition only push emits canonical 111 and never legacy DayThemeDocuments`() {
        val dirty = definition(version = 5)
        val plan =
            buildCanonicalWifiPushPlan(
                source = DatabaseContent(),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        themeDefinitions = listOf(dirty),
                        dayThemes = emptyList(),
                        dayThemeAssignmentDocuments = emptyList(),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalDayThemes =
                    CanonicalDayThemeSyncPayload(
                        themeDefinitions = listOf(dirty),
                    ),
            )

        assertTrue(plan.snapshotDelta.dayThemeDocuments.isEmpty())
        assertNotNull(plan.snapshotDelta.themeDefinitions)
        assertNotNull(plan.snapshotDelta.dayThemes)
        assertNotNull(plan.snapshotDelta.dayThemeAssignmentDocuments)
        assertEquals(listOf(dirty), plan.snapshotDelta.themeDefinitions)
        assertTrue(plan.snapshotDelta.dayThemes!!.isEmpty())
        assertTrue(plan.snapshotDelta.dayThemeAssignmentDocuments!!.isEmpty())
    }

    @Test
    fun `Day Theme ack preserves exact dirty version`() {
        val dirty = definition(version = 7)
        val plan =
            buildCanonicalWifiPushPlan(
                source = DatabaseContent(),
                fullSnapshot =
                    SnapshotBundle(
                        themeDefinitions = listOf(dirty),
                        dayThemes = emptyList(),
                        dayThemeAssignmentDocuments = emptyList(),
                    ),
                dirtyCanonicalSeries = emptyList(),
                dirtyCanonicalDayThemes =
                    CanonicalDayThemeSyncPayload(
                        themeDefinitions = listOf(dirty),
                    ),
            )

        assertEquals(1, plan.dayThemesAck.themeDefinitions.size)
        assertEquals("theme-1", plan.dayThemesAck.themeDefinitions.single().id)
        assertEquals(7L, plan.dayThemesAck.themeDefinitions.single().version)
        assertTrue(plan.dayThemesAck.dayThemes.isEmpty())
        assertTrue(plan.dayThemesAck.assignmentDocuments.isEmpty())
    }

    private fun definition(version: Long) =
        ThemeDefinitionSnapshot(
            id = "theme-1",
            title = "Focus",
            colorArgb = 1L,
            iconKey = "target",
            description = "",
            carryForward = true,
            archived = false,
            createdAt = 1,
            updatedAt = 2,
            syncedAt = null,
            version = version,
            isDeleted = false,
        )
}
