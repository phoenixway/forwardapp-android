package com.romankozak.forwardappmobile.data.recurrence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class CanonicalFocusRecurrenceRoomAcceptanceTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var previousTimeZone: TimeZone

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        previousTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context.deleteDatabase(DB_NAME)
        db = openDatabase()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
        context.deleteDatabase(DB_NAME)
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun focusSplitSurvivesRestartAndRematerializationWithoutResurrection() = runBlocking {
        verifySplitPersistence(DayFocusType.FOCUS)
    }

    @Test
    fun responsibilitySplitSurvivesRestartAndRematerializationWithoutResurrection() = runBlocking {
        verifySplitPersistence(DayFocusType.RESPONSIBILITY)
    }

    @Test
    fun staleCanonicalSeriesSyncAckDoesNotMarkNewerLocalVersionSynced() = runBlocking {
        val seriesId = "sync-ack-stale-version"
        val localVersion = 6L
        val sentVersion = 5L
        val ackTimestamp = 7_000L
        val entity =
            oldSeries(
                type = DayFocusType.FOCUS,
                seriesId = seriesId,
            ).toAndroidEntity().copy(
                version = localVersion,
                updatedAt = 6_000L,
                syncedAt = null,
            )

        db.canonicalRecurringSeriesDao().insert(entity)

        val updated =
            db.canonicalRecurringSeriesDao().markSyncedIfVersionMatches(
                seriesId = seriesId,
                expectedVersion = sentVersion,
                syncedAt = ackTimestamp,
            )

        assertEquals(0, updated)

        val persisted = requireNotNull(db.canonicalRecurringSeriesDao().getById(seriesId))
        assertEquals(localVersion, persisted.version)
        assertNull(persisted.syncedAt)
    }

    @Test
    fun matchingCanonicalSeriesSyncAckMarksExactlyThatVersionSynced() = runBlocking {
        val seriesId = "sync-ack-matching-version"
        val sentVersion = 5L
        val ackTimestamp = 7_000L
        val entity =
            oldSeries(
                type = DayFocusType.FOCUS,
                seriesId = seriesId,
            ).toAndroidEntity().copy(
                version = sentVersion,
                updatedAt = 5_000L,
                syncedAt = null,
            )

        db.canonicalRecurringSeriesDao().insert(entity)

        val updated =
            db.canonicalRecurringSeriesDao().markSyncedIfVersionMatches(
                seriesId = seriesId,
                expectedVersion = sentVersion,
                syncedAt = ackTimestamp,
            )

        assertEquals(1, updated)

        val persisted = requireNotNull(db.canonicalRecurringSeriesDao().getById(seriesId))
        assertEquals(sentVersion, persisted.version)
        assertEquals(ackTimestamp, persisted.syncedAt)
    }

    private suspend fun verifySplitPersistence(type: DayFocusType) {
        val dayKeys =
            listOf(
                "2026-08-20",
                "2026-08-21",
                "2026-08-22",
                "2026-08-23",
                "2026-08-24",
                "2026-08-25",
            )
        val plans =
            dayKeys.associateWith { dayKey ->
                DayPlan(
                    id = "plan-${type.name}-$dayKey",
                    date = epochMillis(dayKey),
                    name = null,
                    status = DayStatus.PLANNED,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                )
            }
        db.dayPlanDao().insertAll(plans.values.toList())

        val oldSeriesId = "acceptance-${type.name.lowercase()}-old"
        val oldSeries = oldSeries(type = type, seriesId = oldSeriesId)
        db.canonicalRecurringSeriesDao().insert(oldSeries.toAndroidEntity())

        var materializer =
            CanonicalRecurrenceMaterializationAdapter(
                appDatabase = db,
                ioDispatcher = Dispatchers.Unconfined,
            )
        val authoring =
            CanonicalFocusRecurrenceAuthoringAdapter(
                appDatabase = db,
                materializationAdapter = materializer,
            )

        dayKeys.forEachIndexed { index, dayKey ->
            val result =
                materializer.materializeForDate(
                    date = requireNotNull(plans[dayKey]).date,
                    now = 10_000L + index,
                )
            assertEquals(1, result.focusItemsToCreate.size)
        }

        val kind = type.toSeriesKind()
        val occurrence20 = requireOccurrence(kind, oldSeriesId, "2026-08-20")
        val occurrence22 = requireOccurrence(kind, oldSeriesId, "2026-08-22")
        val occurrence23 = requireOccurrence(kind, oldSeriesId, "2026-08-23")
        val occurrence24 = requireOccurrence(kind, oldSeriesId, "2026-08-24")

        db.dayFocusItemDao().update(
            occurrence22.copy(
                title = "Custom 22",
                notes = "customized before split",
                updatedAt = 20_022L,
                syncedAt = null,
                version = occurrence22.version + 1,
            ),
        )
        db.dayFocusItemDao().update(
            occurrence23.copy(
                title = "Custom 23",
                notes = "detached after split",
                updatedAt = 20_023L,
                syncedAt = null,
                version = occurrence23.version + 1,
            ),
        )
        db.dayFocusItemDao().softDelete(
            itemId = occurrence24.id,
            updatedAt = 20_024L,
        )

        val returned =
            authoring.splitSeriesFromOccurrence(
                item = occurrence20,
                title = "New template",
                notes = "after split",
                relatedLinks = emptyList(),
                budgetPercent = 55,
                rule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 2,
                        daysOfWeek = null,
                    ),
            )

        assertEquals("2026-08-20", returned.recurrenceOccurrenceDayKey)
        assertEquals("New template", returned.title)
        assertNotEquals(oldSeriesId, returned.recurrenceSeriesId)

        val newSeriesId = requireNotNull(returned.recurrenceSeriesId)

        val seriesBeforeRestart = db.canonicalRecurringSeriesDao().getAllSync()
        assertEquals(2, seriesBeforeRestart.size)
        val endedOldSeries = seriesBeforeRestart.single { it.id == oldSeriesId }.toCanonicalSeries()
        assertEquals("2026-08-19", endedOldSeries.endDayKey)
        assertEquals(2L, endedOldSeries.version)

        val newSeriesBeforeRestart =
            seriesBeforeRestart.single { it.id == newSeriesId }.toCanonicalSeries()
        assertEquals("2026-08-20", newSeriesBeforeRestart.startDayKey)
        assertNull(newSeriesBeforeRestart.endDayKey)
        assertEquals(RecurrenceFrequency.DAILY, newSeriesBeforeRestart.rule.frequency)
        assertEquals(2, newSeriesBeforeRestart.rule.interval)

        db.close()
        db = openDatabase()

        materializer =
            CanonicalRecurrenceMaterializationAdapter(
                appDatabase = db,
                ioDispatcher = Dispatchers.Unconfined,
            )

        val firstPass =
            dayKeys.map { dayKey ->
                materializer.materializeForDate(
                    date = requireNotNull(plans[dayKey]).date,
                    now = 30_000L,
                )
            }
        val secondPass =
            dayKeys.map { dayKey ->
                materializer.materializeForDate(
                    date = requireNotNull(plans[dayKey]).date,
                    now = 40_000L,
                )
            }

        assertTrue(firstPass.all { it.focusItemsToCreate.isEmpty() })
        assertTrue(secondPass.all { it.focusItemsToCreate.isEmpty() })

        val allItems = db.dayFocusItemDao().getAllSync()

        val oldAliases =
            allItems.filter { item ->
                item.recurrenceSeriesId == oldSeriesId &&
                    requireNotNull(item.recurrenceOccurrenceDayKey) >= "2026-08-20"
            }
        assertEquals(6, oldAliases.size)
        assertTrue(oldAliases.all { it.isDeleted })

        val newSeriesItems =
            allItems.filter { item -> item.recurrenceSeriesId == newSeriesId }
        assertEquals(3, newSeriesItems.size)

        val new20 =
            newSeriesItems.single { it.recurrenceOccurrenceDayKey == "2026-08-20" }
        assertFalse(new20.isDeleted)
        assertEquals("New template", new20.title)

        val new22 =
            newSeriesItems.single { it.recurrenceOccurrenceDayKey == "2026-08-22" }
        assertFalse(new22.isDeleted)
        assertEquals("Custom 22", new22.title)
        assertEquals("customized before split", new22.notes)

        val new24 =
            newSeriesItems.single { it.recurrenceOccurrenceDayKey == "2026-08-24" }
        assertTrue(new24.isDeleted)

        val detached23 =
            allItems.single { item ->
                item.dayPlanId == requireNotNull(plans["2026-08-23"]).id &&
                    !item.isDeleted &&
                    item.recurrenceSeriesId == null &&
                    item.title == "Custom 23"
            }
        assertNull(detached23.recurrenceOccurrenceDayKey)
        assertNull(detached23.recurrenceSourceSeriesVersion)
        assertEquals("detached after split", detached23.notes)

        val liveByDay =
            dayKeys.associateWith { dayKey ->
                val planId = requireNotNull(plans[dayKey]).id
                allItems.filter { item -> item.dayPlanId == planId && !item.isDeleted }
            }

        assertEquals(1, requireNotNull(liveByDay["2026-08-20"]).size)
        assertEquals(0, requireNotNull(liveByDay["2026-08-21"]).size)
        assertEquals(1, requireNotNull(liveByDay["2026-08-22"]).size)
        assertEquals(1, requireNotNull(liveByDay["2026-08-23"]).size)
        assertEquals(0, requireNotNull(liveByDay["2026-08-24"]).size)
        assertEquals(0, requireNotNull(liveByDay["2026-08-25"]).size)

        val logicalKeys =
            allItems.mapNotNull { item ->
                val seriesId = item.recurrenceSeriesId ?: return@mapNotNull null
                val dayKey = item.recurrenceOccurrenceDayKey ?: return@mapNotNull null
                "$seriesId@$dayKey"
            }
        assertEquals(logicalKeys.size, logicalKeys.toSet().size)

        val persistedSeries = db.canonicalRecurringSeriesDao().getAllSync()
        assertEquals(2, persistedSeries.size)
        assertEquals("2026-08-19", persistedSeries.single { it.id == oldSeriesId }.endDayKey)
        assertEquals("2026-08-20", persistedSeries.single { it.id == newSeriesId }.startDayKey)
    }

    private suspend fun requireOccurrence(
        kind: RecurringSeriesKind,
        seriesId: String,
        dayKey: String,
    ): DayFocusItem {
        val id =
            recurrenceOccurrenceId(
                kind = kind,
                seriesId = seriesId,
                dayKey = dayKey,
            )
        return requireNotNull(db.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(id))
    }

    private fun oldSeries(
        type: DayFocusType,
        seriesId: String,
    ): RecurringSeries {
        val template =
            RecurringFocusTemplate(
                title = "Old template",
                notes = null,
                relatedLinks = emptyList(),
                budgetPercent = 25,
            )
        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.DAILY,
                interval = 1,
                daysOfWeek = null,
            )

        return when (type) {
            DayFocusType.FOCUS ->
                RecurringFocusSeries(
                    id = seriesId,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    rule = rule,
                    startDayKey = "2026-08-20",
                    endDayKey = null,
                    template = template,
                )

            DayFocusType.RESPONSIBILITY ->
                RecurringResponsibilitySeries(
                    id = seriesId,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    rule = rule,
                    startDayKey = "2026-08-20",
                    endDayKey = null,
                    template = template,
                )

            else -> error("Unsupported acceptance type: $type")
        }
    }

    private fun DayFocusType.toSeriesKind(): RecurringSeriesKind =
        when (this) {
            DayFocusType.FOCUS -> RecurringSeriesKind.FOCUS
            DayFocusType.RESPONSIBILITY -> RecurringSeriesKind.RESPONSIBILITY
            else -> error("Unsupported acceptance type: $this")
        }

    private fun epochMillis(dayKey: String): Long {
        val parts = dayKey.split('-').map(String::toInt)
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }.timeInMillis
    }

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DB_NAME,
        ).allowMainThreadQueries().build()

    companion object {
        private const val DB_NAME = "canonical_focus_recurrence_acceptance.db"
    }
}
