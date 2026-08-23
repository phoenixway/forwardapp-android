package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalDayThemePersistenceMappingTest {
    @Test
    fun `canonical snapshots survive Room representation roundtrip`() {
        val definition =
            ThemeDefinitionSnapshot(
                id = "theme-a",
                title = "Focus",
                colorArgb = 123456,
                iconKey = "brain",
                description = "Deep work",
                carryForward = true,
                archived = false,
                createdAt = 1,
                updatedAt = 2,
                syncedAt = 3,
                version = 4,
                isDeleted = true,
            )
        val dayTheme =
            DayThemeSnapshot(
                id = "day_theme:5:day-1:7:theme-a",
                themeId = "theme-a",
                dayPlanId = "day-1",
                budgetPercent = 55,
                order = 8,
                isActive = false,
                createdAt = 10,
                updatedAt = 11,
                syncedAt = null,
                version = 12,
                isDeleted = false,
            )
        val assignmentDocument =
            DayThemeAssignmentDocumentSnapshot(
                dayPlanId = "day-1",
                assignments =
                    listOf(
                        DayThemeAssignmentSnapshot(
                            entityId = "task-1",
                            dayThemeIds = listOf(dayTheme.id),
                        ),
                    ),
                createdAt = 20,
                updatedAt = 21,
                syncedAt = 22,
                version = 23,
                isDeleted = false,
            )

        assertEquals(definition, definition.toCanonicalEntity().toCanonicalSnapshot())
        assertEquals(dayTheme, dayTheme.toCanonicalEntity().toCanonicalSnapshot())
        assertEquals(assignmentDocument, assignmentDocument.toCanonicalEntity().toCanonicalSnapshot())
    }
}
