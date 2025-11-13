package com.romankozak.forwardappmobile.shared.features.aichat.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
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
class ConversationFolderRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: ConversationFolderRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = ConversationFolderRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `observeFolders emits empty list initially`() = runTest {
        val folders = repository.observeFolders().first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun `insertFolder stores data and observe reflects it`() = runTest {
        val id = repository.insertFolder("ChatGPT")
        val folders = repository.observeFolders().first()
        assertEquals(1, folders.size)
        assertEquals(id, folders.first().id)
        assertEquals("ChatGPT", folders.first().name)
    }

    @Test
    fun `renameFolder updates existing record`() = runTest {
        val id = repository.insertFolder("Old")
        repository.renameFolder(id, "New")

        val folders = repository.observeFolders().first()
        assertEquals("New", folders.first().name)
    }

    @Test
    fun `deleteFolder removes record`() = runTest {
        val id = repository.insertFolder("Temp")
        repository.deleteFolder(id)

        val folders = repository.observeFolders().first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun `deleteAll clears table`() = runTest {
        repository.insertFolder("One")
        repository.insertFolder("Two")
        repository.deleteAll()

        val folders = repository.observeFolders().first()
        assertTrue(folders.isEmpty())
    }
}
