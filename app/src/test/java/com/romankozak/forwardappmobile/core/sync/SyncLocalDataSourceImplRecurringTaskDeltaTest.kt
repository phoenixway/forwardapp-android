package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek

class SyncLocalDataSourceImplRecurringTaskDeltaTest {
    @Test
    fun `ended recurring master remains in delta when end date predates sync watermark`() = runTest {
        val recurringTaskDao = mockk<RecurringTaskDao>(relaxed = true)
        val endedSeries = mockk<RecurringTask>()
        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 2,
                daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            )

        every { endedSeries.endDate } returns 1_000L
        every { endedSeries.recurrenceRule } returns rule
        coEvery { recurringTaskDao.getAll() } returns listOf(endedSeries)

        val dataSource = createDataSource(recurringTaskDao)

        val delta = dataSource.getChangesSince(timestamp = 2_000L)
        val outgoing = delta.recurringTasks.single { it === endedSeries }

        assert(outgoing.endDate == 1_000L)
        assert(outgoing.recurrenceRule.frequency == RecurrenceFrequency.WEEKLY)
        assert(outgoing.recurrenceRule.interval == 2)
        assert(outgoing.recurrenceRule.daysOfWeek == listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
    }

    @Test
    fun `active recurring master remains in delta with calendar rule unchanged`() = runTest {
        val recurringTaskDao = mockk<RecurringTaskDao>(relaxed = true)
        val activeSeries = mockk<RecurringTask>()
        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.MONTHLY,
                interval = 3,
                daysOfWeek = null,
            )

        every { activeSeries.endDate } returns null
        every { activeSeries.recurrenceRule } returns rule
        coEvery { recurringTaskDao.getAll() } returns listOf(activeSeries)

        val dataSource = createDataSource(recurringTaskDao)

        val delta = dataSource.getChangesSince(timestamp = 2_000L)
        val outgoing = delta.recurringTasks.single { it === activeSeries }

        assert(outgoing.endDate == null)
        assert(outgoing.recurrenceRule.frequency == RecurrenceFrequency.MONTHLY)
        assert(outgoing.recurrenceRule.interval == 3)
        assert(outgoing.recurrenceRule.daysOfWeek == null)
    }

    private fun createDataSource(recurringTaskDao: RecurringTaskDao): SyncLocalDataSourceImpl {
        val constructor =
            SyncLocalDataSourceImpl::class.java.declaredConstructors
                .maxByOrNull { it.parameterCount }
                ?: error("SyncLocalDataSourceImpl constructor not found")

        constructor.isAccessible = true
        val arguments =
            constructor.parameterTypes
                .map { parameterType ->
                    when (parameterType) {
                        RecurringTaskDao::class.java -> recurringTaskDao
                        ActivityRecordDao::class.java ->
                            mockk<ActivityRecordDao>(relaxed = true) {
                                every { getAllRecordsStream() } returns flowOf(emptyList())
                            }
                        else -> mockkClass(parameterType.kotlin, relaxed = true)
                    }
                }.toTypedArray()

        return constructor.newInstance(*arguments) as SyncLocalDataSourceImpl
    }
}
