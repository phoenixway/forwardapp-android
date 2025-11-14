package com.romankozak.forwardappmobile.shared.features.projects.logs.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog
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
class ProjectExecutionLogsRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ProjectExecutionLogsRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = ProjectExecutionLogsRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun log(
        id: String,
        timestamp: Long,
        projectId: String = "project-1",
    ) = ProjectExecutionLog(
        id = id,
        projectId = projectId,
        timestamp = timestamp,
        type = "INFO",
        description = "desc-$id",
        details = null,
    )

    @Test
    fun `observeLogs emits entries ordered by timestamp desc`() = runTest {
        repository.upsertLog(log("log-1", timestamp = 1))
        repository.upsertLog(log("log-2", timestamp = 5))

        val logs = repository.observeLogs("project-1").first()

        assertEquals(listOf("log-2", "log-1"), logs.map { it.id })
    }

    @Test
    fun `updateLogDetails updates description and details`() = runTest {
        repository.upsertLog(log("log-1", timestamp = 1))

        repository.updateLogDetails("log-1", description = "updated", details = "new details")

        val logs = repository.observeLogs("project-1").first()
        assertEquals("updated", logs.first().description)
        assertEquals("new details", logs.first().details)
    }

    @Test
    fun `deleteLog removes entry`() = runTest {
        repository.upsertLog(log("log-1", timestamp = 1))

        repository.deleteLog("log-1")

        val logs = repository.observeLogs("project-1").first()
        assertEquals(emptyList(), logs)
    }

    @Test
    fun `upsertLog replaces existing entry`() = runTest {
        repository.upsertLog(log("log-1", timestamp = 1, projectId = "project-1"))
        val second = log("log-1", timestamp = 2, projectId = "project-1")

        repository.upsertLog(second)

        val stored = repository.observeLogs("project-1").first()
        assertEquals(1, stored.size)
        assertEquals(2, stored.first().timestamp)
    }
}
