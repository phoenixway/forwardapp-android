package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.model.DailyMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DailyMetricsRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: DailyMetricsRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = DailyMetricsRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun metric(id: String, date: Long) = DailyMetric(
        id = id,
        dayPlanId = "plan-$date",
        date = date,
        tasksPlanned = 4,
        tasksCompleted = 2,
        completionRate = 0.5,
        totalPlannedTime = 120,
        totalActiveTime = 90,
        completedPoints = 10,
        totalBreakTime = 15,
        morningEnergyLevel = null,
        eveningEnergyLevel = null,
        overallMood = null,
        stressLevel = null,
        customMetrics = null,
        createdAt = 0L,
        updatedAt = null,
    )

    @Test
    fun `observeMetrics returns items ordered by date desc`() = runTest {
        repository.upsertMetric(metric("m1", date = 1))
        repository.upsertMetric(metric("m2", date = 2))

        val metrics = repository.observeMetrics().first()

        assertEquals(listOf("m2", "m1"), metrics.map { it.id })
    }

    @Test
    fun `observeMetricsForDayPlan filters by plan`() = runTest {
        repository.upsertMetric(metric("m1", date = 1))
        repository.upsertMetric(metric("m2", date = 2))

        val metrics = repository.observeMetricsForDayPlan("plan-2").first()

        assertEquals(listOf("m2"), metrics.map { it.id })
    }
}
