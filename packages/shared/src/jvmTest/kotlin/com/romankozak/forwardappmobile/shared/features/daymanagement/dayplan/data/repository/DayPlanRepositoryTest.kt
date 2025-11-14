package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DayPlanRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: DayPlanRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = DayPlanRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun plan(
        id: String,
        date: Long,
        status: DayStatus = DayStatus.PLANNED,
    ) = DayPlan(
        id = id,
        date = date,
        name = "Plan $id",
        status = status,
        reflection = null,
        energyLevel = null,
        mood = null,
        weatherConditions = null,
        totalPlannedMinutes = 120,
        totalCompletedMinutes = 60,
        completionPercentage = 50f,
        createdAt = 0L,
        updatedAt = null,
    )

    @Test
    fun `observeDayPlansInRange emits ordered plans`() = runTest {
        repository.upsertDayPlan(plan("p1", date = 1))
        repository.upsertDayPlan(plan("p2", date = 2))

        val plans = repository.observeDayPlansInRange(0, 5).first()

        assertEquals(listOf("p1", "p2"), plans.map { it.id })
    }

    @Test
    fun `getDayPlanByDate returns null when missing`() = runTest {
        val result = repository.getDayPlanByDate(42)

        assertNull(result)
    }

    @Test
    fun `deleteDayPlan removes entry`() = runTest {
        repository.upsertDayPlan(plan("p1", date = 1))

        repository.deleteDayPlan("p1")

        val plans = repository.observeDayPlansInRange(0, 5).first()
        assertEquals(emptyList(), plans)
    }
}
