package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository.DayPlanRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DayTaskRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var planRepository: DayPlanRepositoryImpl
    private lateinit var taskRepository: DayTaskRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        planRepository = DayPlanRepositoryImpl(database, Dispatchers.Unconfined)
        taskRepository = DayTaskRepositoryImpl(database, Dispatchers.Unconfined)
        runTest {
            planRepository.upsertDayPlan(
                DayPlan(
                    id = "plan-1",
                    date = 1L,
                    name = "Day Plan",
                    status = DayStatus.PLANNED,
                    reflection = null,
                    energyLevel = null,
                    mood = null,
                    weatherConditions = null,
                    totalPlannedMinutes = 0,
                    totalCompletedMinutes = 0,
                    completionPercentage = 0f,
                    createdAt = 0L,
                    updatedAt = null,
                )
            )
        }
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun task(id: String, order: Long = 0L) = DayTask(
        id = id,
        dayPlanId = "plan-1",
        title = "Task $id",
        description = null,
        goalId = null,
        projectId = null,
        activityRecordId = null,
        recurringTaskId = null,
        taskType = null,
        entityId = null,
        order = order,
        priority = TaskPriority.MEDIUM,
        status = TaskStatus.NOT_STARTED,
        completed = false,
        scheduledTime = null,
        estimatedDurationMinutes = null,
        actualDurationMinutes = null,
        dueTime = null,
        valueImportance = 0f,
        valueImpact = 0f,
        effort = 0f,
        cost = 0f,
        risk = 0f,
        location = null,
        tags = null,
        notes = null,
        createdAt = 0L,
        updatedAt = null,
        completedAt = null,
        nextOccurrenceTime = null,
        points = 0,
    )

    @Test
    fun `observeTasksForPlan orders by order asc`() = runTest {
        taskRepository.upsertTask(task("t2", order = 2))
        taskRepository.upsertTask(task("t1", order = 1))

        val tasks = taskRepository.observeTasksForPlan("plan-1").first()

        assertEquals(listOf("t1", "t2"), tasks.map { it.id })
    }

    @Test
    fun `deleteTask removes entry`() = runTest {
        taskRepository.upsertTask(task("temp"))

        taskRepository.deleteTask("temp")

        val tasks = taskRepository.observeTasksForPlan("plan-1").first()
        assertEquals(emptyList(), tasks)
    }
}
