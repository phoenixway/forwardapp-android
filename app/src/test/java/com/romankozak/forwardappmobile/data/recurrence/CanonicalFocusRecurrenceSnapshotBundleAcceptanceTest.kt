package com.romankozak.forwardappmobile.data.recurrence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class CanonicalFocusRecurrenceSnapshotBundleAcceptanceTest {
    private lateinit var context: Context
    private lateinit var sourceDb: AppDatabase
    private lateinit var restoredDb: AppDatabase
    private lateinit var previousTimeZone: TimeZone

    private val gson = GsonBuilder().create()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        previousTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context.deleteDatabase(SOURCE_DB_NAME)
        context.deleteDatabase(RESTORED_DB_NAME)
        sourceDb = openDatabase(SOURCE_DB_NAME)
    }

    @After
    fun tearDown() {
        if (::sourceDb.isInitialized) {
            sourceDb.close()
        }
        if (::restoredDb.isInitialized) {
            restoredDb.close()
        }
        context.deleteDatabase(SOURCE_DB_NAME)
        context.deleteDatabase(RESTORED_DB_NAME)
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun focusSplitStateSurvivesSnapshotBundleJsonAndCleanRoomRestore() = runBlocking {
        verifyWireRoundTrip(DayFocusType.FOCUS)
    }

    @Test
    fun responsibilitySplitStateSurvivesSnapshotBundleJsonAndCleanRoomRestore() = runBlocking {
        verifyWireRoundTrip(DayFocusType.RESPONSIBILITY)
    }

    private suspend fun verifyWireRoundTrip(type: DayFocusType) {
        val dayKeys =
            listOf(
                "2026-08-20",
                "2026-08-22",
                "2026-08-23",
                "2026-08-24",
                "2026-08-26",
            )
        val plans =
            dayKeys.associateWith { dayKey ->
                DayPlan(
                    id = planId(type, dayKey),
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
        sourceDb.dayPlanDao().insertPlans(plans.values.toList())

        val kind = type.toSeriesKind()
        val oldSeriesId = "wire-${type.name.lowercase()}-old"
        val newSeriesId = "wire-${type.name.lowercase()}-new"

        val oldSeries =
            recurringSeries(
                type = type,
                id = oldSeriesId,
                startDayKey = "2026-08-10",
                endDayKey = "2026-08-19",
                interval = 1,
                version = 2,
                title = "Old template",
            )
        val newSeries =
            recurringSeries(
                type = type,
                id = newSeriesId,
                startDayKey = "2026-08-20",
                endDayKey = null,
                interval = 2,
                version = 1,
                title = "New template",
            )

        sourceDb.canonicalRecurringSeriesDao().insertAll(
            listOf(
                oldSeries.toAndroidEntity(),
                newSeries.toAndroidEntity(),
            ),
        )

        val oldTombstone20 =
            canonicalItem(
                type = type,
                kind = kind,
                seriesId = oldSeriesId,
                dayKey = "2026-08-20",
                title = "Old template",
                sourceSeriesVersion = 1,
                isDeleted = true,
            )
        val newLive20 =
            canonicalItem(
                type = type,
                kind = kind,
                seriesId = newSeriesId,
                dayKey = "2026-08-20",
                title = "New template",
                sourceSeriesVersion = 1,
                isDeleted = false,
            )
        val custom22 =
            canonicalItem(
                type = type,
                kind = kind,
                seriesId = newSeriesId,
                dayKey = "2026-08-22",
                title = "Custom 22",
                notes = "customized occurrence",
                sourceSeriesVersion = 1,
                isDeleted = false,
            )
        val newTombstone24 =
            canonicalItem(
                type = type,
                kind = kind,
                seriesId = newSeriesId,
                dayKey = "2026-08-24",
                title = "New template",
                sourceSeriesVersion = 1,
                isDeleted = true,
            )
        val detached23 =
            DayFocusItem(
                id = "detached-${type.name.lowercase()}-2026-08-23",
                dayPlanId = planId(type, "2026-08-23"),
                title = "Detached 23",
                notes = "detached customization",
                relatedLinks = emptyList(),
                type = type,
                isEveryday = false,
                recurringKey = null,
                recurrenceSeriesId = null,
                recurrenceOccurrenceDayKey = null,
                recurrenceSourceSeriesVersion = null,
                budgetPercent = 33,
                order = 1,
                createdAt = 3_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                isDeleted = false,
                version = 1,
            )

        sourceDb.dayFocusItemDao().insertAll(
            listOf(
                oldTombstone20,
                newLive20,
                custom22,
                detached23,
                newTombstone24,
            ),
        )

        val exported =
            SnapshotBundle(
                version = 2,
                exportedAt = 50_000L,
                dayPlans = sourceDb.dayPlanDao().getAllPlansSync().map { it.toSnapshot() },
                dayFocusItems =
                    sourceDb.dayFocusItemDao().getAllSync().map { item ->
                        CanonicalRecurrenceSnapshotMapper.dayFocusItemSnapshot(
                            entity = item,
                            baseSnapshot = item.toSnapshot(),
                        )
                    },
                recurringSeries =
                    sourceDb.canonicalRecurringSeriesDao().getAllSync().map { it.toSnapshot() },
            )

        assertEquals(2, exported.version)
        assertEquals(2, exported.recurringSeries.size)
        assertEquals(5, exported.dayFocusItems.size)
        assertEquals(4, exported.dayFocusItems.count { it.recurrence != null })
        assertTrue(exported.dayFocusItems.all { !it.isEveryday })
        assertTrue(exported.dayFocusItems.all { it.recurringKey == null })
        assertNull(exported.dayFocusItems.single { it.id == detached23.id }.recurrence)

        val exportedNew20 = exported.dayFocusItems.single { it.id == newLive20.id }
        assertEquals(newSeriesId, exportedNew20.recurrence?.seriesId)
        assertEquals("2026-08-20", exportedNew20.recurrence?.occurrenceDayKey)
        assertEquals(1L, exportedNew20.recurrence?.sourceSeriesVersion)

        val json = gson.toJson(exported)
        assertTrue(json.contains("\"snapshotVersion\":2"))
        assertTrue(json.contains("\"recurringSeries\""))
        assertTrue(json.contains("\"recurrence\""))
        assertFalse(json.contains("\"recurrenceSeriesId\""))

        val decoded = gson.fromJson(json, SnapshotBundle::class.java)

        assertEquals(2, decoded.version)
        assertEquals(2, decoded.recurringSeries.size)
        assertEquals(5, decoded.dayFocusItems.size)
        assertEquals(4, decoded.dayFocusItems.count { it.recurrence != null })
        assertTrue(decoded.dayFocusItems.all { !it.isEveryday })
        assertTrue(decoded.dayFocusItems.all { it.recurringKey == null })

        val decodedOldSeries = decoded.recurringSeries.single { it.id == oldSeriesId }
        val decodedNewSeries = decoded.recurringSeries.single { it.id == newSeriesId }
        assertEquals(type.toSeriesKind().name, decodedOldSeries.kind)
        assertEquals(type.toSeriesKind().name, decodedNewSeries.kind)
        assertEquals("2026-08-19", decodedOldSeries.endDayKey)
        assertEquals("2026-08-20", decodedNewSeries.startDayKey)
        assertNull(decodedNewSeries.endDayKey)
        assertEquals("DAILY", decodedNewSeries.rule.frequency)
        assertEquals(2, decodedNewSeries.rule.interval)
        assertEquals("New template", decodedNewSeries.template.asJsonObject.get("title").asString)

        restoredDb = openDatabase(RESTORED_DB_NAME)

        restoredDb.dayPlanDao().insertPlans(decoded.dayPlans.map { it.toEntity() })
        restoredDb.dayFocusItemDao().insertAll(
            decoded.dayFocusItems.map { snapshot ->
                CanonicalRecurrenceSnapshotMapper.dayFocusItemEntity(
                    snapshot = snapshot,
                    baseEntity = snapshot.toEntity(),
                )
            },
        )
        restoredDb.canonicalRecurringSeriesDao().insertAll(
            decoded.recurringSeries.map { it.toEntity() },
        )

        val restoredSeries = restoredDb.canonicalRecurringSeriesDao().getAllSync()
        assertEquals(2, restoredSeries.size)

        val restoredOldSeries = restoredSeries.single { it.id == oldSeriesId }.toCanonicalSeries()
        val restoredNewSeries = restoredSeries.single { it.id == newSeriesId }.toCanonicalSeries()

        assertEquals("2026-08-19", restoredOldSeries.endDayKey)
        assertEquals(2L, restoredOldSeries.version)
        assertEquals(kind, restoredNewSeries.kind)
        assertEquals("2026-08-20", restoredNewSeries.startDayKey)
        assertNull(restoredNewSeries.endDayKey)
        assertEquals(RecurrenceFrequency.DAILY, restoredNewSeries.rule.frequency)
        assertEquals(2, restoredNewSeries.rule.interval)
        assertEquals("New template", restoredNewSeries.focusTemplateForTest().title)

        val restoredItems = restoredDb.dayFocusItemDao().getAllSync()
        assertEquals(5, restoredItems.size)
        assertTrue(restoredItems.all { !it.isEveryday })
        assertTrue(restoredItems.all { it.recurringKey == null })

        val restoredOldTombstone20 =
            requireNotNull(
                restoredDb.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(oldTombstone20.id),
            )
        assertTrue(restoredOldTombstone20.isDeleted)
        assertEquals(oldSeriesId, restoredOldTombstone20.recurrenceSeriesId)
        assertEquals("2026-08-20", restoredOldTombstone20.recurrenceOccurrenceDayKey)

        val restoredNew20 =
            requireNotNull(
                restoredDb.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(newLive20.id),
            )
        assertFalse(restoredNew20.isDeleted)
        assertEquals(newSeriesId, restoredNew20.recurrenceSeriesId)
        assertEquals("2026-08-20", restoredNew20.recurrenceOccurrenceDayKey)
        assertEquals(1L, restoredNew20.recurrenceSourceSeriesVersion)
        assertFalse(restoredNew20.isEveryday)
        assertNull(restoredNew20.recurringKey)

        val restoredCustom22 =
            requireNotNull(
                restoredDb.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(custom22.id),
            )
        assertEquals("Custom 22", restoredCustom22.title)
        assertEquals("customized occurrence", restoredCustom22.notes)
        assertEquals(newSeriesId, restoredCustom22.recurrenceSeriesId)
        assertFalse(restoredCustom22.isDeleted)

        val restoredTombstone24 =
            requireNotNull(
                restoredDb.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(newTombstone24.id),
            )
        assertTrue(restoredTombstone24.isDeleted)
        assertEquals(newSeriesId, restoredTombstone24.recurrenceSeriesId)
        assertEquals("2026-08-24", restoredTombstone24.recurrenceOccurrenceDayKey)

        val restoredDetached23 = restoredItems.single { it.id == detached23.id }
        assertFalse(restoredDetached23.isDeleted)
        assertEquals("Detached 23", restoredDetached23.title)
        assertNull(restoredDetached23.recurrenceSeriesId)
        assertNull(restoredDetached23.recurrenceOccurrenceDayKey)
        assertNull(restoredDetached23.recurrenceSourceSeriesVersion)

        val materializer =
            CanonicalRecurrenceMaterializationAdapter(
                appDatabase = restoredDb,
                ioDispatcher = Dispatchers.Unconfined,
            )

        listOf(
            "2026-08-20",
            "2026-08-22",
            "2026-08-23",
            "2026-08-24",
        ).forEach { dayKey ->
            val result =
                materializer.materializeForDate(
                    date = epochMillis(dayKey),
                    now = 60_000L,
                )
            assertTrue(
                "Unexpected rematerialization on $dayKey for $type: ${result.focusItemsToCreate}",
                result.focusItemsToCreate.isEmpty(),
            )
        }

        val materialized26 =
            materializer.materializeForDate(
                date = epochMillis("2026-08-26"),
                now = 70_000L,
            )
        assertEquals(1, materialized26.focusItemsToCreate.size)

        val occurrence26Id =
            recurrenceOccurrenceId(
                kind = kind,
                seriesId = newSeriesId,
                dayKey = "2026-08-26",
            )
        val persisted26 =
            requireNotNull(
                restoredDb.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(occurrence26Id),
            )
        assertFalse(persisted26.isDeleted)
        assertEquals(type, persisted26.type)
        assertEquals(newSeriesId, persisted26.recurrenceSeriesId)
        assertEquals("2026-08-26", persisted26.recurrenceOccurrenceDayKey)
        assertEquals(1L, persisted26.recurrenceSourceSeriesVersion)
        assertEquals("New template", persisted26.title)
        assertFalse(persisted26.isEveryday)
        assertNull(persisted26.recurringKey)

        val second26 =
            materializer.materializeForDate(
                date = epochMillis("2026-08-26"),
                now = 80_000L,
            )
        assertTrue(second26.focusItemsToCreate.isEmpty())

        assertNotNull(restoredDb.canonicalRecurringSeriesDao().getById(newSeriesId))
    }

    private fun recurringSeries(
        type: DayFocusType,
        id: String,
        startDayKey: String,
        endDayKey: String?,
        interval: Int,
        version: Long,
        title: String,
    ): RecurringSeries {
        val template =
            RecurringFocusTemplate(
                title = title,
                notes = if (title == "New template") "after split" else null,
                relatedLinks = emptyList(),
                budgetPercent = if (title == "New template") 55 else 25,
            )
        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.DAILY,
                interval = interval,
                daysOfWeek = null,
            )

        return when (type) {
            DayFocusType.FOCUS ->
                RecurringFocusSeries(
                    id = id,
                    createdAt = 1_000L,
                    updatedAt = 2_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = version,
                    rule = rule,
                    startDayKey = startDayKey,
                    endDayKey = endDayKey,
                    template = template,
                )

            DayFocusType.RESPONSIBILITY ->
                RecurringResponsibilitySeries(
                    id = id,
                    createdAt = 1_000L,
                    updatedAt = 2_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = version,
                    rule = rule,
                    startDayKey = startDayKey,
                    endDayKey = endDayKey,
                    template = template,
                )

            else -> error("Unsupported wire acceptance type: $type")
        }
    }

    private fun canonicalItem(
        type: DayFocusType,
        kind: RecurringSeriesKind,
        seriesId: String,
        dayKey: String,
        title: String,
        notes: String? = null,
        sourceSeriesVersion: Long,
        isDeleted: Boolean,
    ): DayFocusItem =
        DayFocusItem(
            id =
                recurrenceOccurrenceId(
                    kind = kind,
                    seriesId = seriesId,
                    dayKey = dayKey,
                ),
            dayPlanId = planId(type, dayKey),
            title = title,
            notes = notes,
            relatedLinks = emptyList(),
            type = type,
            isEveryday = false,
            recurringKey = null,
            recurrenceSeriesId = seriesId,
            recurrenceOccurrenceDayKey = dayKey,
            recurrenceSourceSeriesVersion = sourceSeriesVersion,
            budgetPercent = 55,
            order = 1,
            createdAt = 2_000L,
            updatedAt = 2_000L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = if (isDeleted) 2 else 1,
        )

    private fun RecurringSeries.focusTemplateForTest(): RecurringFocusTemplate =
        when (this) {
            is RecurringFocusSeries -> template
            is RecurringResponsibilitySeries -> template
            else -> error("Expected focus-compatible canonical series, got $kind")
        }

    private fun DayFocusType.toSeriesKind(): RecurringSeriesKind =
        when (this) {
            DayFocusType.FOCUS -> RecurringSeriesKind.FOCUS
            DayFocusType.RESPONSIBILITY -> RecurringSeriesKind.RESPONSIBILITY
            else -> error("Unsupported wire acceptance type: $this")
        }

    private fun planId(
        type: DayFocusType,
        dayKey: String,
    ): String = "wire-plan-${type.name}-$dayKey"

    private fun epochMillis(dayKey: String): Long {
        val parts = dayKey.split('-').map(String::toInt)
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }.timeInMillis
    }

    private fun openDatabase(name: String): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            name,
        ).allowMainThreadQueries().build()

    companion object {
        private const val SOURCE_DB_NAME = "canonical_focus_recurrence_wire_source.db"
        private const val RESTORED_DB_NAME = "canonical_focus_recurrence_wire_restored.db"
    }
}
