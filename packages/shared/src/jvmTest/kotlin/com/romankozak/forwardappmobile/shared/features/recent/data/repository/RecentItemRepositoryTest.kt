package com.romankozak.forwardappmobile.shared.features.recent.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.features.recent.domain.model.RecentItem
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
class RecentItemRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: RecentItemRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = RecentItemRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `observeRecentItems returns empty list initially`() = runTest {
        val items = repository.observeRecentItems().first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `upsert stores item and observe returns it`() = runTest {
        val item = RecentItem(
            id = "rec_1",
            type = "PROJECT",
            lastAccessed = 100L,
            displayName = "Test",
            target = "project/rec_1",
            isPinned = false,
        )

        repository.upsert(item)

        val items = repository.observeRecentItems().first()
        assertEquals(listOf(item), items)
    }

    @Test
    fun `deleteAll removes items`() = runTest {
        val item = RecentItem(
            id = "rec_1",
            type = "PROJECT",
            lastAccessed = 100L,
            displayName = "Test",
            target = "project/rec_1",
            isPinned = false,
        )
        repository.upsert(item)

        repository.deleteAll()

        val items = repository.observeRecentItems().first()
        assertTrue(items.isEmpty())
    }
}
