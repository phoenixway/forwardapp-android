package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import org.junit.Assert.fail
import org.junit.Test

class CanonicalDayThemePayloadValidatorTest {
    @Test
    fun `empty canonical authority is valid`() {
        requireValidCanonicalDayThemePayload(
            SnapshotBundle(
                themeDefinitions = emptyList(),
                dayThemes = emptyList(),
                dayThemeAssignmentDocuments = emptyList(),
            ),
        )
    }

    @Test
    fun `legacy absence remains valid`() {
        requireValidCanonicalDayThemePayload(SnapshotBundle())
    }

    @Test
    fun `partial typed canonical payload is rejected`() {
        expectInvalid(
            SnapshotBundle(
                themeDefinitions = emptyList(),
                dayThemes = emptyList(),
                dayThemeAssignmentDocuments = null,
            ),
        )
    }

    @Test
    fun `blank ThemeDefinition title is rejected`() {
        expectInvalid(
            SnapshotBundle(
                themeDefinitions = listOf(definition(title = "   ")),
                dayThemes = emptyList(),
                dayThemeAssignmentDocuments = emptyList(),
            ),
        )
    }

    @Test
    fun `non canonical DayTheme id is rejected before persistence`() {
        expectInvalid(
            SnapshotBundle(
                themeDefinitions = listOf(definition()),
                dayThemes =
                    listOf(
                        DayThemeSnapshot(
                            id = "wrong-id",
                            themeId = "theme-1",
                            dayPlanId = "day-1",
                            budgetPercent = 50,
                            order = 0,
                            isActive = true,
                            createdAt = 1,
                            updatedAt = 1,
                            syncedAt = null,
                            version = 1,
                            isDeleted = false,
                        ),
                    ),
                dayThemeAssignmentDocuments = emptyList(),
            ),
        )
    }

    private fun definition(title: String = "Focus") =
        ThemeDefinitionSnapshot(
            id = "theme-1",
            title = title,
            colorArgb = 1,
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

    private fun expectInvalid(bundle: SnapshotBundle) {
        try {
            requireValidCanonicalDayThemePayload(bundle)
            fail("Expected canonical Day Theme validation failure")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
