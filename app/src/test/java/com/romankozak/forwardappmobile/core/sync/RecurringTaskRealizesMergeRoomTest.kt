package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.planning.PlanningSourceRoomFixture
import com.romankozak.forwardappmobile.data.recurrence.toAndroidEntity
import com.romankozak.forwardappmobile.data.recurrence.toCanonicalSeries
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskRealizesDefaults
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecurringTaskRealizesMergeRoomTest {
    private val empty = RecurringTaskRealizesDefaults()
    private val populated = RecurringTaskRealizesDefaults(
        themeDefinitionIds = setOf("theme"),
        prioritySeriesIds = setOf("priority"),
    )
    private val legacyTemplate = RecurringTaskTemplate(
        title = "Task",
        description = null,
        goalId = null,
        linkedProjectIds = emptyList(),
        linkedAttachmentIds = emptyList(),
        priority = TaskPriority.NONE,
        estimatedDurationMinutes = null,
        points = 0,
    )
    private val series = RecurringTaskSeries(
        id = "task-series",
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
        rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null),
        startDayKey = "2026-11-20",
        endDayKey = null,
        template = legacyTemplate.copy(
            realizesDefaults = populated,
            realizesChanges = mapOf("2026-11-27" to empty),
        ),
    )

    @Test
    fun `normal merge preserves intent after a legacy edit and applies a newer explicit clear`() = runBlocking {
        PlanningSourceRoomFixture().use { fixture ->
            val dao = fixture.db.canonicalRecurringSeriesDao()
            dao.insert(series.toAndroidEntity())
            val merge = createRoomMergeSubject(fixture.db)
            val legacyUpdate = series.copy(
                updatedAt = 2,
                version = 2,
                template = legacyTemplate.copy(title = "Changed on older peer"),
            )
            merge.applySnapshotBundle(legacyUpdate.bundle())
            val preserved = requireNotNull(dao.getById(series.id)).toCanonicalSeries() as RecurringTaskSeries
            assertEquals(legacyUpdate.version, preserved.version)
            assertEquals(legacyUpdate.template.title, preserved.template.title)
            assertEquals(series.template.realizesDefaults, preserved.template.realizesDefaults)
            assertEquals(series.template.realizesChanges, preserved.template.realizesChanges)

            val clear = preserved.copy(
                version = 3,
                updatedAt = 3,
                template = preserved.template.copy(realizesDefaults = empty, realizesChanges = emptyMap()),
            )
            merge.applySnapshotBundle(clear.bundle())
            merge.applySnapshotBundle(preserved.bundle())
            assertEquals(clear, requireNotNull(dao.getById(series.id)).toCanonicalSeries())
        }
    }

    @Test
    fun `same legacy revision can receive preserved intent without a second content edit`() = runBlocking {
        PlanningSourceRoomFixture().use { fixture ->
            val dao = fixture.db.canonicalRecurringSeriesDao()
            val legacyCopy = series.copy(template = legacyTemplate.copy(title = "Local title"))
            dao.insert(legacyCopy.toAndroidEntity())
            val merge = createRoomMergeSubject(fixture.db)

            merge.applySnapshotBundle(series.bundle())

            val merged = requireNotNull(dao.getById(series.id)).toCanonicalSeries() as RecurringTaskSeries
            assertEquals(legacyCopy.template.title, merged.template.title)
            assertEquals(series.template.realizesDefaults, merged.template.realizesDefaults)
            assertEquals(series.template.realizesChanges, merged.template.realizesChanges)
            assertEquals(legacyCopy.version, merged.version)
        }
    }

    private fun RecurringTaskSeries.bundle(): SnapshotBundle =
        SnapshotBundle(version = 2, recurringSeries = listOf(toAndroidEntity().toSnapshot()))
}
