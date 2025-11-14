package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote
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
class LegacyNotesRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: LegacyNotesRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = LegacyNotesRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun sampleNote(projectId: String, suffix: Int = 1) = LegacyNote(
        id = "note_$suffix",
        projectId = projectId,
        title = "Title$suffix",
        content = "Content$suffix",
        createdAt = suffix.toLong(),
        updatedAt = null,
    )

    @Test
    fun `observeNotes returns ordered notes for project`() = runTest {
        val n1 = sampleNote("p1", 1)
        val n2 = sampleNote("p1", 2)
        repository.insert(n1)
        repository.insert(n2.copy(createdAt = 5))

        val notes = repository.observeNotes(projectId = "p1").first()

        assertEquals(listOf(n2.copy(createdAt = 5), n1), notes)
    }

    @Test
    fun `observeNotes without project returns all`() = runTest {
        repository.insert(sampleNote("p1", 1))
        repository.insert(sampleNote("p2", 2))

        val notes = repository.observeNotes().first()

        assertEquals(2, notes.size)
    }

    @Test
    fun `getNoteById returns note`() = runTest {
        val note = sampleNote("p1", 3)
        repository.insert(note)

        val loaded = repository.getNoteById(note.id)

        assertEquals(note, loaded)
    }

    @Test
    fun `getNoteById returns null when missing`() = runTest {
        val loaded = repository.getNoteById("missing")
        assertNull(loaded)
    }

    @Test
    fun `updateContent changes title and content`() = runTest {
        val note = sampleNote("p1", 4)
        repository.insert(note)

        repository.updateContent(
            id = note.id,
            title = "New Title",
            content = "New Content",
            updatedAt = 10L,
        )

        val updated = repository.getNoteById(note.id)
        assertEquals("New Title", updated?.title)
        assertEquals("New Content", updated?.content)
        assertEquals(10L, updated?.updatedAt)
    }

    @Test
    fun `deleteById removes note`() = runTest {
        val note = sampleNote("p1", 5)
        repository.insert(note)

        repository.deleteById(note.id)

        val notes = repository.observeNotes("p1").first()
        assertTrue(notes.isEmpty())
    }

    @Test
    fun `deleteAll removes only selected project when provided`() = runTest {
        val note1 = sampleNote("p1", 1)
        val note2 = sampleNote("p2", 2)
        repository.insert(note1)
        repository.insert(note2)

        repository.deleteAll(projectId = "p1")

        assertTrue(repository.observeNotes("p1").first().isEmpty())
        assertEquals(listOf(note2), repository.observeNotes("p2").first())
    }

    @Test
    fun `deleteAll without project clears table`() = runTest {
        repository.insert(sampleNote("p1", 1))
        repository.insert(sampleNote("p2", 2))

        repository.deleteAll()

        assertTrue(repository.observeNotes().first().isEmpty())
    }
}
