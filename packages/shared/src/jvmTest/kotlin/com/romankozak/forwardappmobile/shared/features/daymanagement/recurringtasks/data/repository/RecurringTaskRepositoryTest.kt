package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceRule
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurringTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringTaskRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: RecurringTaskRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = RecurringTaskRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun sampleTask(goalId: String?, suffix: Int = 1) = RecurringTask(
        id = "rec_$suffix",
        title = "Recurring $suffix",
        description = "Description $suffix",
        goalId = goalId,
        durationMinutes = suffix * 10,
        priority = TaskPriority.MEDIUM,
        points = suffix * 5,
        recurrenceRule = RecurrenceRule(
            frequency = RecurrenceFrequency.DAILY,
            interval = suffix,
            daysOfWeek = listOf("MONDAY", "WEDNESDAY"),
        ),
        startDate = 100L * suffix,
        endDate = null,
    )

    @Test
    fun `observeRecurringTasks filters by goal`() = runTest {
        val task1 = sampleTask("goal1", 1)
        val task2 = sampleTask("goal2", 2)
        repository.upsertRecurringTask(task1)
        repository.upsertRecurringTask(task2)

        val goal1Tasks = repository.observeRecurringTasks("goal1").first()
        val allTasks = repository.observeRecurringTasks().first()

        assertEquals(listOf(task1), goal1Tasks)
        assertEquals(listOf(task2, task1), allTasks)
    }

    @Test
    fun `getRecurringTaskById returns null when missing`() = runTest {
        assertNull(repository.getRecurringTaskById("missing"))
    }

    @Test
    fun `deleteRecurringTasksByGoal removes only matching rows`() = runTest {
        val task1 = sampleTask("goal1", 1)
        val task2 = sampleTask("goal2", 2)
        repository.upsertRecurringTask(task1)
        repository.upsertRecurringTask(task2)

        repository.deleteRecurringTasksByGoal("goal1")

        assertTrue(repository.observeRecurringTasks("goal1").first().isEmpty())
        assertEquals(listOf(task2), repository.observeRecurringTasks("goal2").first())
    }

    @Test
    fun `deleteAll clears table`() = runTest {
        repository.upsertRecurringTask(sampleTask(null, 3))

        repository.deleteAll()

        assertTrue(repository.observeRecurringTasks().first().isEmpty())
    }
}
