package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonObject
import com.romankozak.forwardappmobile.core.data.models.sync.LocalSyncSelection
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurrenceRuleSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalRecurringSeriesWifiPushPlanTest {
    @Test
    fun `series-only dirty state still requires wifi push`() {
        assertTrue(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = listOf(series(id = "series-1", version = 5L)),
            ),
        )
    }

    @Test
    fun `truly empty state still short circuits`() {
        assertFalse(
            shouldPushCanonicalWifi(
                databaseIsEmpty = true,
                dirtyCanonicalSeries = emptyList(),
            ),
        )
    }

    @Test
    fun `series-only push includes only dirty canonical series and never legacy recurring tasks`() {
        val dirty = series(id = "series-dirty", version = 5L)
        val clean = series(id = "series-clean", version = 9L)
        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        recurringSeries = listOf(dirty, clean),
                    ),
                dirtyCanonicalSeries = listOf(dirty),
            )

        assertTrue(plan.snapshotDelta.recurringTasks.isEmpty())
        assertEquals(1, plan.snapshotDelta.recurringSeries.size)
        assertEquals("series-dirty", plan.snapshotDelta.recurringSeries.single().id)
        assertEquals(5L, plan.snapshotDelta.recurringSeries.single().version)
    }

    @Test
    fun `ack token preserves exact version captured for transport`() {
        val dirty = series(id = "series-ack", version = 5L)
        val plan =
            buildCanonicalWifiPushPlan(
                selection = LocalSyncSelection(),
                fullSnapshot =
                    SnapshotBundle(
                        version = 2,
                        recurringSeries = listOf(dirty),
                    ),
                dirtyCanonicalSeries = listOf(dirty),
            )

        assertEquals(1, plan.recurringSeriesAck.size)
        assertEquals("series-ack", plan.recurringSeriesAck.single().id)
        assertEquals(5L, plan.recurringSeriesAck.single().version)
    }

    private fun series(
        id: String,
        version: Long,
    ): CanonicalRecurringSeriesSnapshot =
        CanonicalRecurringSeriesSnapshot(
            id = id,
            kind = "TASK",
            rule =
                CanonicalRecurrenceRuleSnapshot(
                    frequency = "DAILY",
                    interval = 1,
                    daysOfWeek = null,
                ),
            startDayKey = "2026-08-21",
            endDayKey = null,
            template =
                JsonObject().apply {
                    addProperty("title", "Canonical transport series")
                },
            createdAt = 1_000L,
            updatedAt = 5_000L,
            syncedAt = null,
            isDeleted = false,
            version = version,
        )
}
