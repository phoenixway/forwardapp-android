package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
import com.romankozak.forwardappmobile.data.recurrence.toAndroidEntity
import com.romankozak.forwardappmobile.data.recurrence.toCanonicalSeries
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class CanonicalFocusRecurrenceMergeRoomAcceptanceTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var previousTimeZone: TimeZone

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        previousTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        db =
            Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java,
            ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun `focus and responsibility split state survives repeated canonical SnapshotBundle merge`() = runBlocking {
        val dayKeys =
            listOf(
                "2026-08-20",
                "2026-08-22",
                "2026-08-23",
                "2026-08-24",
            )

        val localPlans =
            dayKeys.associateWith { dayKey ->
                dayPlan(
                    id = "android-plan-$dayKey",
                    dayKey = dayKey,
                )
            }
        db.dayPlanDao().insertPlans(localPlans.values.toList())

        val incomingPlans =
            dayKeys.associateWith { dayKey ->
                dayPlan(
                    id = "desktop-plan-$dayKey",
                    dayKey = dayKey,
                )
            }

        val series =
            listOf(DayFocusType.FOCUS, DayFocusType.RESPONSIBILITY)
                .flatMap { type ->
                    listOf(
                        recurringSeries(type, "old"),
                        recurringSeries(type, "new"),
                    )
                }

        val items =
            listOf(DayFocusType.FOCUS, DayFocusType.RESPONSIBILITY)
                .flatMap { type ->
                    val kind = type.toSeriesKind()
                    val oldSeriesId = seriesId(type, "old")
                    val newSeriesId = seriesId(type, "new")

                    listOf(
                        canonicalItem(
                            type = type,
                            kind = kind,
                            seriesId = oldSeriesId,
                            dayKey = "2026-08-20",
                            title = "Old ${type.name}",
                            isDeleted = true,
                        ),
                        canonicalItem(
                            type = type,
                            kind = kind,
                            seriesId = newSeriesId,
                            dayKey = "2026-08-20",
                            title = "New ${type.name}",
                            notes = "after split",
                        ),
                        canonicalItem(
                            type = type,
                            kind = kind,
                            seriesId = newSeriesId,
                            dayKey = "2026-08-22",
                            title = "Customized ${type.name}",
                            notes = "custom occurrence",
                        ),
                        detachedItem(
                            type = type,
                            dayKey = "2026-08-23",
                        ),
                        canonicalItem(
                            type = type,
                            kind = kind,
                            seriesId = newSeriesId,
                            dayKey = "2026-08-24",
                            title = "Deleted ${type.name}",
                            isDeleted = true,
                        ),
                    )
                }

        val bundle =
            SnapshotBundle(
                version = 2,
                exportedAt = 50_000L,
                dayPlans = incomingPlans.values.map { it.toSnapshot() },
                dayFocusItems =
                    items.map { item ->
                        CanonicalRecurrenceSnapshotMapper.dayFocusItemSnapshot(
                            entity = item,
                            baseSnapshot = item.toSnapshot(),
                        )
                    },
                recurringSeries =
                    series
                        .map { it.toAndroidEntity() }
                        .map { it.toSnapshot() },
            )

        val subject = createSubject(db)

        subject.applySnapshotBundle(bundle)

        val firstItems = db.dayFocusItemDao().getAllSync().sortedBy { it.id }
        val firstSeries = db.canonicalRecurringSeriesDao().getAllSync().sortedBy { it.id }

        assertEquals(10, firstItems.size)
        assertEquals(4, firstSeries.size)

        assertEquals(
            localPlans.values.map { it.id }.sorted(),
            db.dayPlanDao().getAllPlansSync().map { it.id }.sorted(),
        )

        firstItems.forEach { item ->
            assertTrue(item.dayPlanId.startsWith("android-plan-"))
            assertFalse(item.isEveryday)
            assertNull(item.recurringKey)
        }

        for (type in listOf(DayFocusType.FOCUS, DayFocusType.RESPONSIBILITY)) {
            val oldSeriesId = seriesId(type, "old")
            val newSeriesId = seriesId(type, "new")

            val oldSeries =
                firstSeries
                    .single { it.id == oldSeriesId }
                    .toCanonicalSeries()
            val newSeries =
                firstSeries
                    .single { it.id == newSeriesId }
                    .toCanonicalSeries()

            assertEquals(type.toSeriesKind(), oldSeries.kind)
            assertEquals("2026-08-19", oldSeries.endDayKey)
            assertEquals(2L, oldSeries.version)

            assertEquals(type.toSeriesKind(), newSeries.kind)
            assertEquals("2026-08-20", newSeries.startDayKey)
            assertNull(newSeries.endDayKey)
            assertEquals(RecurrenceFrequency.DAILY, newSeries.rule.frequency)
            assertEquals(2, newSeries.rule.interval)

            val oldTombstoneId =
                recurrenceOccurrenceId(
                    kind = type.toSeriesKind(),
                    seriesId = oldSeriesId,
                    dayKey = "2026-08-20",
                )
            val oldTombstone = firstItems.single { it.id == oldTombstoneId }
            assertTrue(oldTombstone.isDeleted)
            assertEquals(oldSeriesId, oldTombstone.recurrenceSeriesId)
            assertEquals("2026-08-20", oldTombstone.recurrenceOccurrenceDayKey)

            val custom22Id =
                recurrenceOccurrenceId(
                    kind = type.toSeriesKind(),
                    seriesId = newSeriesId,
                    dayKey = "2026-08-22",
                )
            val custom22 = firstItems.single { it.id == custom22Id }
            assertFalse(custom22.isDeleted)
            assertEquals("Customized ${type.name}", custom22.title)
            assertEquals("custom occurrence", custom22.notes)
            assertEquals(newSeriesId, custom22.recurrenceSeriesId)
            assertEquals("2026-08-22", custom22.recurrenceOccurrenceDayKey)
            assertEquals(1L, custom22.recurrenceSourceSeriesVersion)

            val tombstone24Id =
                recurrenceOccurrenceId(
                    kind = type.toSeriesKind(),
                    seriesId = newSeriesId,
                    dayKey = "2026-08-24",
                )
            val tombstone24 = firstItems.single { it.id == tombstone24Id }
            assertTrue(tombstone24.isDeleted)
            assertEquals(newSeriesId, tombstone24.recurrenceSeriesId)

            val detached =
                firstItems.single {
                    it.id == "detached-${type.name.lowercase()}-2026-08-23"
                }
            assertFalse(detached.isDeleted)
            assertEquals("Detached ${type.name}", detached.title)
            assertNull(detached.recurrenceSeriesId)
            assertNull(detached.recurrenceOccurrenceDayKey)
            assertNull(detached.recurrenceSourceSeriesVersion)
        }

        val firstLogicalKeys =
            firstItems.mapNotNull { item ->
                val seriesId = item.recurrenceSeriesId ?: return@mapNotNull null
                val dayKey = item.recurrenceOccurrenceDayKey ?: return@mapNotNull null
                "$seriesId@$dayKey"
            }
        assertEquals(firstLogicalKeys.size, firstLogicalKeys.toSet().size)

        subject.applySnapshotBundle(bundle)

        val secondItems = db.dayFocusItemDao().getAllSync().sortedBy { it.id }
        val secondSeries = db.canonicalRecurringSeriesDao().getAllSync().sortedBy { it.id }

        assertEquals(firstItems, secondItems)
        assertEquals(firstSeries, secondSeries)

        val secondLogicalKeys =
            secondItems.mapNotNull { item ->
                val seriesId = item.recurrenceSeriesId ?: return@mapNotNull null
                val dayKey = item.recurrenceOccurrenceDayKey ?: return@mapNotNull null
                "$seriesId@$dayKey"
            }
        assertEquals(secondLogicalKeys.size, secondLogicalKeys.toSet().size)
    }

    @Test
    fun `canonical merge contains different physical aliases without creating duplicate logical focus occurrences`() = runBlocking {
        val dayKey = "2026-08-20"
        val localPlan = dayPlan("android-plan-$dayKey", dayKey)
        val incomingPlan = dayPlan("desktop-plan-$dayKey", dayKey)
        db.dayPlanDao().insertPlans(listOf(localPlan))

        val localAliases =
            listOf(DayFocusType.FOCUS, DayFocusType.RESPONSIBILITY).mapIndexed { index, type ->
                DayFocusItem(
                    id = "local-alias-${type.name.lowercase()}",
                    dayPlanId = localPlan.id,
                    title = "Local ${type.name}",
                    notes = null,
                    relatedLinks = emptyList(),
                    type = type,
                    isEveryday = false,
                    recurringKey = null,
                    recurrenceSeriesId = seriesId(type, "new"),
                    recurrenceOccurrenceDayKey = dayKey,
                    recurrenceSourceSeriesVersion = 1,
                    budgetPercent = 55,
                    order = index.toLong(),
                    createdAt = 1_000L,
                    updatedAt = 2_000L,
                    syncedAt = null,
                    isDeleted = type == DayFocusType.FOCUS,
                    version = 3,
                )
            }
        db.dayFocusItemDao().insertAll(localAliases)

        val incomingSeries =
            listOf(DayFocusType.FOCUS, DayFocusType.RESPONSIBILITY).map {
                recurringSeries(it, "new")
            }
        val incomingItems =
            listOf(DayFocusType.FOCUS, DayFocusType.RESPONSIBILITY).map { type ->
                canonicalItem(
                    type = type,
                    kind = type.toSeriesKind(),
                    seriesId = seriesId(type, "new"),
                    dayKey = dayKey,
                    title = "Incoming ${type.name}",
                    isDeleted = type == DayFocusType.RESPONSIBILITY,
                )
            }

        val bundle =
            SnapshotBundle(
                version = 2,
                dayPlans = listOf(incomingPlan.toSnapshot()),
                dayFocusItems =
                    incomingItems.map { item ->
                        CanonicalRecurrenceSnapshotMapper.dayFocusItemSnapshot(
                            entity = item,
                            baseSnapshot = item.toSnapshot(),
                        )
                    },
                recurringSeries =
                    incomingSeries
                        .map { it.toAndroidEntity() }
                        .map { it.toSnapshot() },
            )

        createSubject(db).applySnapshotBundle(bundle)

        val persisted = db.dayFocusItemDao().getAllSync().sortedBy { it.id }
        assertEquals(2, persisted.size)
        assertEquals(localAliases.map { it.id }.sorted(), persisted.map { it.id }.sorted())

        val logicalKeys =
            persisted.map { item ->
                "${requireNotNull(item.recurrenceSeriesId)}@${requireNotNull(item.recurrenceOccurrenceDayKey)}"
            }
        assertEquals(logicalKeys.size, logicalKeys.toSet().size)

        assertTrue(persisted.single { it.type == DayFocusType.FOCUS }.isDeleted)
        assertFalse(persisted.single { it.type == DayFocusType.RESPONSIBILITY }.isDeleted)
        assertEquals(2, db.canonicalRecurringSeriesDao().getAllSync().size)
    }

    @Test
    fun `same physical canonical conflicts use version updatedAt and tombstone precedence`() = runBlocking {
        val dayKeys =
            listOf(
                "2026-08-20",
                "2026-08-21",
                "2026-08-22",
                "2026-08-23",
            )
        db.dayPlanDao().insertPlans(
            dayKeys.map { dayKey ->
                dayPlan(
                    id = "desktop-plan-$dayKey",
                    dayKey = dayKey,
                )
            },
        )

        val focusSeriesId = seriesId(DayFocusType.FOCUS, "new")
        val responsibilitySeriesId = seriesId(DayFocusType.RESPONSIBILITY, "new")

        val localFocusSeries =
            recurringSeries(DayFocusType.FOCUS, "new")
                .toAndroidEntity()
                .copy(
                    updatedAt = 5_000L,
                    isDeleted = true,
                    version = 5,
                )
        val localResponsibilitySeries =
            recurringSeries(DayFocusType.RESPONSIBILITY, "new")
                .toAndroidEntity()
                .copy(
                    updatedAt = 5_000L,
                    isDeleted = false,
                    version = 5,
                )
        db.canonicalRecurringSeriesDao().insertAll(
            listOf(localFocusSeries, localResponsibilitySeries),
        )

        val focusVersionWinner =
            canonicalItem(
                type = DayFocusType.FOCUS,
                kind = RecurringSeriesKind.FOCUS,
                seriesId = focusSeriesId,
                dayKey = "2026-08-20",
                title = "Local focus tombstone wins by version",
                isDeleted = true,
            ).copy(
                updatedAt = 5_000L,
                version = 5,
            )
        val focusUpdatedAtLoser =
            canonicalItem(
                type = DayFocusType.FOCUS,
                kind = RecurringSeriesKind.FOCUS,
                seriesId = focusSeriesId,
                dayKey = "2026-08-21",
                title = "Local focus live loses by updatedAt",
                isDeleted = false,
            ).copy(
                updatedAt = 5_000L,
                version = 5,
            )
        val responsibilityTieLive =
            canonicalItem(
                type = DayFocusType.RESPONSIBILITY,
                kind = RecurringSeriesKind.RESPONSIBILITY,
                seriesId = responsibilitySeriesId,
                dayKey = "2026-08-22",
                title = "Local responsibility live loses tie",
                isDeleted = false,
            ).copy(
                updatedAt = 5_000L,
                version = 5,
            )
        val responsibilityTieTombstone =
            canonicalItem(
                type = DayFocusType.RESPONSIBILITY,
                kind = RecurringSeriesKind.RESPONSIBILITY,
                seriesId = responsibilitySeriesId,
                dayKey = "2026-08-23",
                title = "Local responsibility tombstone wins tie",
                isDeleted = true,
            ).copy(
                updatedAt = 5_000L,
                version = 5,
            )

        db.dayFocusItemDao().insertAll(
            listOf(
                focusVersionWinner,
                focusUpdatedAtLoser,
                responsibilityTieLive,
                responsibilityTieTombstone,
            ),
        )

        val incomingFocusVersionLoser =
            focusVersionWinner.copy(
                title = "Incoming stale live must lose",
                updatedAt = 9_000L,
                isDeleted = false,
                version = 4,
            )
        val incomingFocusUpdatedAtWinner =
            focusUpdatedAtLoser.copy(
                title = "Incoming newer tombstone wins",
                updatedAt = 6_000L,
                isDeleted = true,
                version = 5,
            )
        val incomingResponsibilityTieTombstone =
            responsibilityTieLive.copy(
                title = "Incoming tombstone wins exact tie",
                isDeleted = true,
            )
        val incomingResponsibilityTieLive =
            responsibilityTieTombstone.copy(
                title = "Incoming live loses exact tie",
                isDeleted = false,
            )

        val incomingFocusSeries =
            localFocusSeries.copy(
                updatedAt = 9_000L,
                isDeleted = false,
                version = 4,
            )
        val incomingResponsibilitySeries =
            localResponsibilitySeries.copy(
                isDeleted = true,
            )

        val incomingItems =
            listOf(
                incomingFocusVersionLoser,
                incomingFocusUpdatedAtWinner,
                incomingResponsibilityTieTombstone,
                incomingResponsibilityTieLive,
            )

        val bundle =
            SnapshotBundle(
                version = 2,
                dayPlans =
                    dayKeys.map { dayKey ->
                        dayPlan("desktop-plan-$dayKey", dayKey).toSnapshot()
                    },
                dayFocusItems =
                    incomingItems.map { item ->
                        CanonicalRecurrenceSnapshotMapper.dayFocusItemSnapshot(
                            entity = item,
                            baseSnapshot = item.toSnapshot(),
                        )
                    },
                recurringSeries =
                    listOf(incomingFocusSeries, incomingResponsibilitySeries)
                        .map { it.toSnapshot() },
            )

        createSubject(db).applySnapshotBundle(bundle)

        val persisted = db.dayFocusItemDao().getAllSync().associateBy { it.id }

        val versionWinner = requireNotNull(persisted[focusVersionWinner.id])
        assertTrue(versionWinner.isDeleted)
        assertEquals("Local focus tombstone wins by version", versionWinner.title)
        assertEquals(5L, versionWinner.version)
        assertEquals(5_000L, versionWinner.updatedAt)

        val updatedAtWinner = requireNotNull(persisted[focusUpdatedAtLoser.id])
        assertTrue(updatedAtWinner.isDeleted)
        assertEquals("Incoming newer tombstone wins", updatedAtWinner.title)
        assertEquals(5L, updatedAtWinner.version)
        assertEquals(6_000L, updatedAtWinner.updatedAt)

        val incomingTieTombstoneWinner = requireNotNull(persisted[responsibilityTieLive.id])
        assertTrue(incomingTieTombstoneWinner.isDeleted)
        assertEquals("Incoming tombstone wins exact tie", incomingTieTombstoneWinner.title)

        val localTieTombstoneWinner = requireNotNull(persisted[responsibilityTieTombstone.id])
        assertTrue(localTieTombstoneWinner.isDeleted)
        assertEquals("Local responsibility tombstone wins tie", localTieTombstoneWinner.title)

        val persistedFocusSeries =
            requireNotNull(db.canonicalRecurringSeriesDao().getById(focusSeriesId))
        assertTrue(persistedFocusSeries.isDeleted)
        assertEquals(5L, persistedFocusSeries.version)
        assertEquals(5_000L, persistedFocusSeries.updatedAt)

        val persistedResponsibilitySeries =
            requireNotNull(db.canonicalRecurringSeriesDao().getById(responsibilitySeriesId))
        assertTrue(persistedResponsibilitySeries.isDeleted)
        assertEquals(5L, persistedResponsibilitySeries.version)
        assertEquals(5_000L, persistedResponsibilitySeries.updatedAt)
    }

    private fun dayPlan(
        id: String,
        dayKey: String,
    ): DayPlan =
        DayPlan(
            id = id,
            date = epochMillis(dayKey),
            name = null,
            status = DayStatus.PLANNED,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            syncedAt = null,
            isDeleted = false,
            version = 1,
        )

    private fun recurringSeries(
        type: DayFocusType,
        phase: String,
    ): RecurringSeries {
        val isOld = phase == "old"
        val template =
            RecurringFocusTemplate(
                title = if (isOld) "Old ${type.name}" else "New ${type.name}",
                notes = if (isOld) null else "after split",
                relatedLinks = emptyList(),
                budgetPercent = if (isOld) 25 else 55,
            )
        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.DAILY,
                interval = if (isOld) 1 else 2,
                daysOfWeek = null,
            )

        return when (type) {
            DayFocusType.FOCUS ->
                RecurringFocusSeries(
                    id = seriesId(type, phase),
                    createdAt = 1_000L,
                    updatedAt = if (isOld) 3_000L else 4_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = if (isOld) 2 else 1,
                    rule = rule,
                    startDayKey = if (isOld) "2026-08-10" else "2026-08-20",
                    endDayKey = if (isOld) "2026-08-19" else null,
                    template = template,
                )

            DayFocusType.RESPONSIBILITY ->
                RecurringResponsibilitySeries(
                    id = seriesId(type, phase),
                    createdAt = 1_000L,
                    updatedAt = if (isOld) 3_000L else 4_000L,
                    syncedAt = null,
                    isDeleted = false,
                    version = if (isOld) 2 else 1,
                    rule = rule,
                    startDayKey = if (isOld) "2026-08-10" else "2026-08-20",
                    endDayKey = if (isOld) "2026-08-19" else null,
                    template = template,
                )

            else -> error("Unsupported canonical focus type: $type")
        }
    }

    private fun canonicalItem(
        type: DayFocusType,
        kind: RecurringSeriesKind,
        seriesId: String,
        dayKey: String,
        title: String,
        notes: String? = null,
        isDeleted: Boolean = false,
    ): DayFocusItem =
        DayFocusItem(
            id = recurrenceOccurrenceId(kind, seriesId, dayKey),
            dayPlanId = "desktop-plan-$dayKey",
            title = title,
            notes = notes,
            relatedLinks = emptyList(),
            type = type,
            isEveryday = false,
            recurringKey = null,
            recurrenceSeriesId = seriesId,
            recurrenceOccurrenceDayKey = dayKey,
            recurrenceSourceSeriesVersion = 1,
            budgetPercent = 55,
            order = 0,
            createdAt = 2_000L,
            updatedAt = 2_000L,
            syncedAt = null,
            isDeleted = isDeleted,
            version = if (isDeleted) 2 else 1,
        )

    private fun detachedItem(
        type: DayFocusType,
        dayKey: String,
    ): DayFocusItem =
        DayFocusItem(
            id = "detached-${type.name.lowercase()}-$dayKey",
            dayPlanId = "desktop-plan-$dayKey",
            title = "Detached ${type.name}",
            notes = "detached customization",
            relatedLinks = emptyList(),
            type = type,
            isEveryday = false,
            recurringKey = null,
            recurrenceSeriesId = null,
            recurrenceOccurrenceDayKey = null,
            recurrenceSourceSeriesVersion = null,
            budgetPercent = 33,
            order = 0,
            createdAt = 2_000L,
            updatedAt = 2_000L,
            syncedAt = null,
            isDeleted = false,
            version = 1,
        )

    private fun seriesId(
        type: DayFocusType,
        phase: String,
    ): String = "merge-${type.name.lowercase()}-$phase"

    private fun DayFocusType.toSeriesKind(): RecurringSeriesKind =
        when (this) {
            DayFocusType.FOCUS -> RecurringSeriesKind.FOCUS
            DayFocusType.RESPONSIBILITY -> RecurringSeriesKind.RESPONSIBILITY
            else -> error("Unsupported canonical focus type: $this")
        }

    private fun epochMillis(dayKey: String): Long {
        val parts = dayKey.split('-').map(String::toInt)
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }.timeInMillis
    }

    private fun createSubject(db: AppDatabase): MergeLocalDataSourceImpl {
        val constructor = MergeLocalDataSourceImpl::class.java.declaredConstructors.single()
        constructor.isAccessible = true

        val arguments =
            constructor.parameterTypes.map { parameterType ->
                when (parameterType) {
                    AppDatabase::class.java -> db
                    DayPlanDao::class.java -> db.dayPlanDao()
                    DayTaskDao::class.java -> db.dayTaskDao()
                    DayFocusItemDao::class.java -> db.dayFocusItemDao()
                    CanonicalRecurringSeriesDao::class.java -> db.canonicalRecurringSeriesDao()
                    else -> relaxedMock(parameterType)
                }
            }.toTypedArray()

        return constructor.newInstance(*arguments) as MergeLocalDataSourceImpl
    }

    @Suppress("UNCHECKED_CAST")
    private fun relaxedMock(type: Class<*>): Any =
        mockkClass(type.kotlin as KClass<Any>, relaxed = true)
}
