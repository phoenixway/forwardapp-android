package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.reflect.KClass

class MergeLocalDataSourceImplRecurringOccurrenceTest {
    @Test
    fun `plan remap does not silently insert live recurring alias beside local tombstone`() = runTest {
        val day = 1_786_910_400_000L
        val localPlanId = "plan-local"
        val incomingPlanId = "plan-incoming"
        val recurringTaskId = "series-1"

        val localPlan =
            DayPlan(
                id = localPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )
        val incomingPlan =
            DayPlan(
                id = incomingPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )

        val localTombstone =
            DayTask(
                id = "local-tombstone",
                dayPlanId = localPlanId,
                title = "Recurring task",
                priority = TaskPriority.MEDIUM,
                recurrenceSeriesId = recurringTaskId,
                recurrenceOccurrenceDayKey = "logical-day",
                recurrenceSourceSeriesVersion = 1,
                isDeleted = true,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                version = 3,
            )
        val incomingLiveAlias =
            DayTask(
                id = "incoming-live-alias",
                dayPlanId = incomingPlanId,
                title = "Recurring task",
                priority = TaskPriority.MEDIUM,
                recurrenceSeriesId = recurringTaskId,
                recurrenceOccurrenceDayKey = "logical-day",
                recurrenceSourceSeriesVersion = 1,
                isDeleted = false,
                createdAt = 4_000L,
                updatedAt = 4_000L,
                syncedAt = null,
                version = 1,
            )

        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(any()) } returns localPlan
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(localPlan)
        coEvery { dayTaskDao.getAllTasksSync() } returns listOf(localTombstone)

        val insertedTasks = slot<List<DayTask>>()
        coEvery { dayTaskDao.insertTasks(capture(insertedTasks)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                )

            val gson = Gson()
            val incomingBundle =
                gson.fromJson(
                    """
                    {
                      "version": 2,
                      "dayPlans": [${gson.toJson(incomingPlan)}],
                      "dayTasks": [${gson.toJson(CanonicalRecurrenceSnapshotMapper.dayTaskSnapshot(incomingLiveAlias, incomingLiveAlias.toSnapshot()))}]
                    }
                    """.trimIndent(),
                    SnapshotBundle::class.java,
                )

            subject.applySnapshotBundle(incomingBundle)
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedTasks.isCaptured).isTrue()
        assertThat(insertedTasks.captured.map { it.id })
            .doesNotContain(incomingLiveAlias.id)
    }

    @Test
    fun `plan remap does not silently insert tombstone recurring alias beside local live occurrence`() = runTest {
        val day = 1_786_910_400_000L
        val localPlanId = "plan-local"
        val incomingPlanId = "plan-incoming"
        val recurringTaskId = "series-1"

        val localPlan =
            DayPlan(
                id = localPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )
        val incomingPlan =
            DayPlan(
                id = incomingPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )

        val localLive =
            DayTask(
                id = "local-live",
                dayPlanId = localPlanId,
                title = "Recurring task",
                priority = TaskPriority.MEDIUM,
                recurrenceSeriesId = recurringTaskId,
                recurrenceOccurrenceDayKey = "logical-day",
                recurrenceSourceSeriesVersion = 1,
                isDeleted = false,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                version = 3,
            )
        val incomingTombstoneAlias =
            DayTask(
                id = "incoming-tombstone-alias",
                dayPlanId = incomingPlanId,
                title = "Recurring task",
                priority = TaskPriority.MEDIUM,
                recurrenceSeriesId = recurringTaskId,
                recurrenceOccurrenceDayKey = "logical-day",
                recurrenceSourceSeriesVersion = 1,
                isDeleted = true,
                createdAt = 4_000L,
                updatedAt = 4_000L,
                syncedAt = null,
                version = 1,
            )

        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(any()) } returns localPlan
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(localPlan)
        coEvery { dayTaskDao.getAllTasksSync() } returns listOf(localLive)

        val insertedTasks = slot<List<DayTask>>()
        coEvery { dayTaskDao.insertTasks(capture(insertedTasks)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                )

            val gson = Gson()
            val incomingBundle =
                gson.fromJson(
                    """
                    {
                      "version": 2,
                      "dayPlans": [${gson.toJson(incomingPlan)}],
                      "dayTasks": [${gson.toJson(CanonicalRecurrenceSnapshotMapper.dayTaskSnapshot(incomingTombstoneAlias, incomingTombstoneAlias.toSnapshot()))}]
                    }
                    """.trimIndent(),
                    SnapshotBundle::class.java,
                )

            subject.applySnapshotBundle(incomingBundle)
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedTasks.isCaptured).isTrue()
        assertThat(insertedTasks.captured.map { it.id })
            .doesNotContain(incomingTombstoneAlias.id)
    }

    @Test
    fun `plan remap still inserts recurring occurrence from a different series`() = runTest {
        val day = 1_786_910_400_000L
        val localPlanId = "plan-local"
        val incomingPlanId = "plan-incoming"

        val localPlan =
            DayPlan(
                id = localPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )
        val incomingPlan =
            DayPlan(
                id = incomingPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )

        val localTombstone =
            DayTask(
                id = "local-series-a",
                dayPlanId = localPlanId,
                title = "Series A",
                priority = TaskPriority.MEDIUM,
                recurrenceSeriesId = "series-a",
                recurrenceOccurrenceDayKey = "logical-day",
                recurrenceSourceSeriesVersion = 1,
                isDeleted = true,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                version = 3,
            )
        val incomingDifferentSeries =
            DayTask(
                id = "incoming-series-b",
                dayPlanId = incomingPlanId,
                title = "Series B",
                priority = TaskPriority.MEDIUM,
                recurrenceSeriesId = "series-b",
                recurrenceOccurrenceDayKey = "logical-day",
                recurrenceSourceSeriesVersion = 1,
                isDeleted = false,
                createdAt = 4_000L,
                updatedAt = 4_000L,
                syncedAt = null,
                version = 1,
            )

        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(any()) } returns localPlan
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(localPlan)
        coEvery { dayTaskDao.getAllTasksSync() } returns listOf(localTombstone)

        val insertedTasks = slot<List<DayTask>>()
        coEvery { dayTaskDao.insertTasks(capture(insertedTasks)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                )

            val gson = Gson()
            val incomingBundle =
                gson.fromJson(
                    """
                    {
                      "version": 2,
                      "dayPlans": [${gson.toJson(incomingPlan)}],
                      "dayTasks": [${gson.toJson(CanonicalRecurrenceSnapshotMapper.dayTaskSnapshot(incomingDifferentSeries, incomingDifferentSeries.toSnapshot()))}]
                    }
                    """.trimIndent(),
                    SnapshotBundle::class.java,
                )

            subject.applySnapshotBundle(incomingBundle)
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedTasks.isCaptured).isTrue()
        assertThat(insertedTasks.captured.map { it.id })
            .contains(incomingDifferentSeries.id)
        assertThat(insertedTasks.captured.single { it.id == incomingDifferentSeries.id }.dayPlanId)
            .isEqualTo(localPlanId)
    }

    @Test
    fun `plan remap does not silently insert live everyday focus alias beside local tombstone`() = runTest {
        val day = 1_786_910_400_000L
        val localPlanId = "plan-local"
        val incomingPlanId = "plan-incoming"
        val recurringKey = "focus-series-1"

        val localPlan =
            DayPlan(
                id = localPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )
        val incomingPlan =
            DayPlan(
                id = incomingPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )

        val localTombstone =
            DayFocusItem(
                id = "local-focus-tombstone",
                dayPlanId = localPlanId,
                title = "Everyday focus",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 25,
                order = 0,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                isDeleted = true,
                version = 3,
            )
        val incomingLiveAlias =
            DayFocusItem(
                id = "incoming-focus-live-alias",
                dayPlanId = incomingPlanId,
                title = "Everyday focus",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 25,
                order = 0,
                createdAt = 4_000L,
                updatedAt = 4_000L,
                syncedAt = null,
                isDeleted = false,
                version = 1,
            )

        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(any()) } returns localPlan
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(localPlan)
        coEvery { dayTaskDao.getAllTasksSync() } returns emptyList()
        coEvery { dayFocusItemDao.getAllSync() } returns listOf(localTombstone)

        val insertedFocusItems = slot<List<DayFocusItem>>()
        coEvery { dayFocusItemDao.insertAll(capture(insertedFocusItems)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                    dayFocusItemDao = dayFocusItemDao,
                )

            val gson = Gson()
            val incomingBundle =
                gson.fromJson(
                    """
                    {
                      "version": 2,
                      "dayPlans": [${gson.toJson(incomingPlan)}],
                      "dayFocusItems": [${gson.toJson(incomingLiveAlias)}]
                    }
                    """.trimIndent(),
                    SnapshotBundle::class.java,
                )

            subject.applySnapshotBundle(incomingBundle)
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedFocusItems.isCaptured).isTrue()
        assertThat(insertedFocusItems.captured.map { it.id })
            .doesNotContain(incomingLiveAlias.id)
    }

    @Test
    fun `plan remap does not silently insert tombstone everyday focus alias beside local live occurrence`() = runTest {
        val day = 1_786_910_400_000L
        val localPlanId = "plan-local"
        val incomingPlanId = "plan-incoming"
        val recurringKey = "focus-series-1"

        val localPlan =
            DayPlan(
                id = localPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )
        val incomingPlan = localPlan.copy(id = incomingPlanId)

        val localLive =
            DayFocusItem(
                id = "local-focus-live",
                dayPlanId = localPlanId,
                title = "Everyday focus",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                isEveryday = true,
                recurringKey = recurringKey,
                budgetPercent = 25,
                order = 0,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                isDeleted = false,
                version = 3,
            )
        val incomingTombstoneAlias =
            localLive.copy(
                id = "incoming-focus-tombstone-alias",
                dayPlanId = incomingPlanId,
                createdAt = 4_000L,
                updatedAt = 4_000L,
                isDeleted = true,
                version = 1,
            )

        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(any()) } returns localPlan
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(localPlan)
        coEvery { dayTaskDao.getAllTasksSync() } returns emptyList()
        coEvery { dayFocusItemDao.getAllSync() } returns listOf(localLive)

        val insertedFocusItems = slot<List<DayFocusItem>>()
        coEvery { dayFocusItemDao.insertAll(capture(insertedFocusItems)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                    dayFocusItemDao = dayFocusItemDao,
                )

            val gson = Gson()
            val incomingBundle =
                gson.fromJson(
                    """
                    {
                      "version": 2,
                      "dayPlans": [${gson.toJson(incomingPlan)}],
                      "dayFocusItems": [${gson.toJson(incomingTombstoneAlias)}]
                    }
                    """.trimIndent(),
                    SnapshotBundle::class.java,
                )

            subject.applySnapshotBundle(incomingBundle)
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedFocusItems.isCaptured).isTrue()
        assertThat(insertedFocusItems.captured.map { it.id })
            .doesNotContain(incomingTombstoneAlias.id)
    }

    @Test
    fun `plan remap still inserts everyday focus from a different recurring key`() = runTest {
        val day = 1_786_910_400_000L
        val localPlanId = "plan-local"
        val incomingPlanId = "plan-incoming"

        val localPlan =
            DayPlan(
                id = localPlanId,
                date = day,
                name = null,
                status = DayStatus.PLANNED,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                syncedAt = null,
                version = 1,
            )
        val incomingPlan = localPlan.copy(id = incomingPlanId)

        val localTombstone =
            DayFocusItem(
                id = "local-focus-a",
                dayPlanId = localPlanId,
                title = "Series A",
                notes = null,
                relatedLinks = emptyList(),
                type = DayFocusType.FOCUS,
                isEveryday = true,
                recurringKey = "focus-series-a",
                budgetPercent = 25,
                order = 0,
                createdAt = 2_000L,
                updatedAt = 3_000L,
                syncedAt = null,
                isDeleted = true,
                version = 3,
            )
        val incomingDifferentSeries =
            localTombstone.copy(
                id = "incoming-focus-b",
                dayPlanId = incomingPlanId,
                title = "Series B",
                recurringKey = "focus-series-b",
                createdAt = 4_000L,
                updatedAt = 4_000L,
                isDeleted = false,
                version = 1,
            )

        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(any()) } returns localPlan
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(localPlan)
        coEvery { dayTaskDao.getAllTasksSync() } returns emptyList()
        coEvery { dayFocusItemDao.getAllSync() } returns listOf(localTombstone)

        val insertedFocusItems = slot<List<DayFocusItem>>()
        coEvery { dayFocusItemDao.insertAll(capture(insertedFocusItems)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                    dayFocusItemDao = dayFocusItemDao,
                )

            val gson = Gson()
            val incomingBundle =
                gson.fromJson(
                    """
                    {
                      "version": 2,
                      "dayPlans": [${gson.toJson(incomingPlan)}],
                      "dayFocusItems": [${gson.toJson(incomingDifferentSeries)}]
                    }
                    """.trimIndent(),
                    SnapshotBundle::class.java,
                )

            subject.applySnapshotBundle(incomingBundle)
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedFocusItems.isCaptured).isTrue()
        assertThat(insertedFocusItems.captured.map { it.id })
            .contains(incomingDifferentSeries.id)
        assertThat(insertedFocusItems.captured.single { it.id == incomingDifferentSeries.id }.dayPlanId)
            .isEqualTo(localPlanId)
    }

    @Test
    fun `log merge does not resurrect older live row over newer local tombstone`() = runTest {
        val db = mockk<AppDatabase>(relaxed = true)
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
        val contextManagementDao = mockk<ContextManagementDao>(relaxed = true)

        val localTombstone =
            ContextLog(
                id = "log-1",
                contextId = "context-1",
                timestamp = 1_000L,
                type = "COMMENT",
                description = "deleted locally",
                details = null,
                updatedAt = 5_000L,
                syncedAt = null,
                isDeleted = true,
                version = 5L,
            )

        coEvery { contextManagementDao.getAllLogs() } returns listOf(localTombstone)

        val insertedLogs = slot<List<ContextLog>>()
        coEvery { contextManagementDao.insertLogs(capture(insertedLogs)) } returns Unit

        val transactionBlock = slot<suspend () -> Unit>()
        mockkStatic("androidx.room.RoomDatabaseKt")
        try {
            coEvery { db.withTransaction<Unit>(capture(transactionBlock)) } coAnswers {
                transactionBlock.captured.invoke()
            }

            val subject =
                createSubject(
                    db = db,
                    dayPlanDao = dayPlanDao,
                    dayTaskDao = dayTaskDao,
                    contextManagementDao = contextManagementDao,
                )

            val incomingOlderLive =
                ContextLogSnapshot(
                    id = localTombstone.id,
                    contextId = requireNotNull(localTombstone.contextId),
                    timestamp = localTombstone.timestamp,
                    type = localTombstone.type,
                    description = "older remote live row",
                    details = null,
                    updatedAt = 6_000L,
                    version = 4L,
                    isDeleted = false,
                )

            subject.applySnapshotBundle(
                SnapshotBundle(
                    version = 2,
                    exportedAt = 7_000L,
                    logs = listOf(incomingOlderLive),
                ),
            )
        } finally {
            unmockkStatic("androidx.room.RoomDatabaseKt")
        }

        assertThat(insertedLogs.isCaptured).isTrue()
        assertThat(insertedLogs.captured).isEmpty()
    }

    private fun createSubject(
        db: AppDatabase,
        dayPlanDao: DayPlanDao,
        dayTaskDao: DayTaskDao,
        dayFocusItemDao: DayFocusItemDao? = null,
        contextManagementDao: ContextManagementDao? = null,
    ): MergeLocalDataSourceImpl {
        val constructor = MergeLocalDataSourceImpl::class.java.declaredConstructors.single()
        constructor.isAccessible = true
        val arguments =
            constructor.parameterTypes.map { parameterType ->
                when (parameterType) {
                    AppDatabase::class.java -> db
                    DayPlanDao::class.java -> dayPlanDao
                    DayTaskDao::class.java -> dayTaskDao
                    DayFocusItemDao::class.java -> dayFocusItemDao ?: relaxedMock(parameterType)
                    ContextManagementDao::class.java -> contextManagementDao ?: relaxedMock(parameterType)
                    else -> relaxedMock(parameterType)
                }
            }.toTypedArray()
        return constructor.newInstance(*arguments) as MergeLocalDataSourceImpl
    }

    @Suppress("UNCHECKED_CAST")
    private fun relaxedMock(type: Class<*>): Any =
        mockkClass(type.kotlin as KClass<Any>, relaxed = true)
}
