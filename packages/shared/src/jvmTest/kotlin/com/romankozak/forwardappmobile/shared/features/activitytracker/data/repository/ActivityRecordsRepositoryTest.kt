package com.romankozak.forwardappmobile.shared.features.activitytracker.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.model.ActivityRecord
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
class ActivityRecordsRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ActivityRecordsRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = ActivityRecordsRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun record(
        id: String,
        name: String = "Activity $id",
        createdAt: Long = 0L,
        startTime: Long? = null,
        endTime: Long? = null,
        projectId: String? = null,
    ) = ActivityRecord(
        id = id,
        name = name,
        description = null,
        createdAt = createdAt,
        startTime = startTime,
        endTime = endTime,
        totalTimeSpentMinutes = null,
        tags = null,
        relatedLinks = null,
        isCompleted = endTime != null,
        activityType = "FOCUS",
        parentProjectId = projectId,
    )

    @Test
    fun `observeActivityRecords sorts by createdAt desc`() = runTest {
        repository.upsertActivityRecord(record("a1", createdAt = 100))
        repository.upsertActivityRecord(record("a2", createdAt = 200))

        val items = repository.observeActivityRecords().first()

        assertEquals(listOf("a2", "a1"), items.map { it.id })
    }

    @Test
    fun `searchActivityRecords uses FTS`() = runTest {
        repository.upsertActivityRecord(record(id = "focus", name = "Deep Focus"))
        repository.upsertActivityRecord(record(id = "other", name = "Morning Walk"))

        val results = repository.searchActivityRecords("Focus").first()

        assertEquals(listOf("focus"), results.map { it.id })
    }

    @Test
    fun `findLastOngoingActivityForProject returns record without end time`() = runTest {
        repository.upsertActivityRecord(record("done", projectId = "p1", startTime = 10, endTime = 20))
        repository.upsertActivityRecord(record("ongoing", projectId = "p1", startTime = 30))

        val result = repository.findLastOngoingActivityForProject("p1")

        assertEquals("ongoing", result?.id)
    }

    @Test
    fun `deleteActivityRecord removes entry`() = runTest {
        repository.upsertActivityRecord(record("temp"))

        repository.deleteActivityRecord("temp")

        val items = repository.observeActivityRecords().first()
        assertEquals(emptyList<String>(), items.map { it.id })
    }
}
