package com.romankozak.forwardappmobile.shared.core.models

import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalModelContractTest {
    @Test
    fun taskSeriesHasCanonicalKindAndProvenanceShape() {
        val series =
            RecurringTaskSeries(
                id = "series:task:daily",
                createdAt = 100L,
                updatedAt = 100L,
                syncedAt = null,
                isDeleted = false,
                version = 3L,
                rule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 1,
                        daysOfWeek = null,
                    ),
                startDayKey = "2026-08-17",
                endDayKey = null,
                template =
                    RecurringTaskTemplate(
                        title = "Daily task",
                        description = null,
                        goalId = null,
                        linkedProjectIds = emptyList(),
                        linkedAttachmentIds = emptyList(),
                        priority = TaskPriority.MEDIUM,
                        estimatedDurationMinutes = 30L,
                        points = 5,
                    ),
            )

        val origin =
            RecurrenceOrigin(
                seriesId = series.id,
                occurrenceDayKey = "2026-08-17",
                sourceSeriesVersion = series.version,
            )

        assertEquals(RecurringSeriesKind.TASK, series.kind)
        assertEquals("series:task:daily", origin.seriesId)
        assertEquals("2026-08-17", origin.occurrenceDayKey)
        assertEquals(3L, origin.sourceSeriesVersion)
    }
}
