package com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model.InboxRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InboxRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: InboxRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = InboxRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun sampleRecord(projectId: String, idSuffix: Int = 1) = InboxRecord(
        id = "rec_$idSuffix",
        projectId = projectId,
        text = "Text$idSuffix",
        createdAt = idSuffix.toLong(),
        itemOrder = idSuffix.toLong(),
    )

    @Test
    fun `observeInbox empty initially`() = runTest {
        val records = repository.observeInbox("p1").first()
        assertTrue(records.isEmpty())
    }

    @Test
    fun `upsert stores and observe returns`() = runTest {
        val record = sampleRecord("p1")
        repository.upsert(record)
        val records = repository.observeInbox("p1").first()
        assertEquals(listOf(record), records)
    }

    @Test
    fun `delete removes record`() = runTest {
        val record = sampleRecord("p1")
        repository.upsert(record)
        repository.delete(record.id)
        val records = repository.observeInbox("p1").first()
        assertTrue(records.isEmpty())
    }

    @Test
    fun `deleteAll by project clears only that project`() = runTest {
        val r1 = sampleRecord("p1", 1)
        val r2 = sampleRecord("p2", 2)
        repository.upsert(r1)
        repository.upsert(r2)

        repository.deleteAll(projectId = "p1")

        val project1 = repository.observeInbox("p1").first()
        val project2 = repository.observeInbox("p2").first()
        assertTrue(project1.isEmpty())
        assertEquals(listOf(r2), project2)
    }

    @Test
    fun `deleteAll with null clears table`() = runTest {
        repository.upsert(sampleRecord("p1", 1))
        repository.upsert(sampleRecord("p2", 2))
        repository.deleteAll(projectId = null)

        val p1 = repository.observeInbox("p1").first()
        val p2 = repository.observeInbox("p2").first()
        assertTrue(p1.isEmpty())
        assertTrue(p2.isEmpty())
    }
}
