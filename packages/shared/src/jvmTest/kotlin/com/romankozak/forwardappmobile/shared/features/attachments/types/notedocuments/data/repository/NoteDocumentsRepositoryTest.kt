package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocument
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocumentItem
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
class NoteDocumentsRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: NoteDocumentsRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = NoteDocumentsRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun sampleDocument(projectId: String, suffix: Int = 1) = NoteDocument(
        id = "doc_$suffix",
        projectId = projectId,
        name = "Doc $suffix",
        content = "Content $suffix",
        createdAt = suffix.toLong(),
        updatedAt = suffix.toLong(),
        lastCursorPosition = suffix.toLong(),
    )

    private fun sampleItem(documentId: String, suffix: Int = 1) = NoteDocumentItem(
        id = "item_$suffix",
        documentId = documentId,
        parentId = null,
        content = "Item $suffix",
        isCompleted = false,
        order = suffix.toLong(),
        createdAt = suffix.toLong(),
        updatedAt = suffix.toLong(),
    )

    @Test
    fun `observeDocuments returns documents ordered by updatedAt`() = runTest {
        val doc1 = sampleDocument("p1", 1)
        val doc2 = sampleDocument("p1", 2).copy(updatedAt = 5)
        repository.upsertDocument(doc1)
        repository.upsertDocument(doc2)

        val docs = repository.observeDocuments("p1").first()

        assertEquals(listOf(doc2, doc1), docs)
    }

    @Test
    fun `observeDocumentItems sorts by order`() = runTest {
        val doc = sampleDocument("p1", 3)
        repository.upsertDocument(doc)
        val item1 = sampleItem(doc.id, 1).copy(order = 5)
        val item2 = sampleItem(doc.id, 2).copy(order = 2)
        repository.upsertDocumentItem(item1)
        repository.upsertDocumentItem(item2)

        val items = repository.observeDocumentItems(doc.id).first()

        assertEquals(listOf(item2, item1), items)
    }

    @Test
    fun `getDocumentById returns null when missing`() = runTest {
        val result = repository.getDocumentById("missing")
        assertNull(result)
    }

    @Test
    fun `updateDocumentContent changes fields`() = runTest {
        val doc = sampleDocument("p1", 4)
        repository.upsertDocument(doc)

        repository.updateDocumentContent(
            id = doc.id,
            name = "Renamed",
            content = "Updated",
            updatedAt = 99,
            lastCursorPosition = 42,
        )

        val updated = repository.getDocumentById(doc.id)
        assertEquals("Renamed", updated?.name)
        assertEquals("Updated", updated?.content)
        assertEquals(99, updated?.updatedAt)
        assertEquals(42, updated?.lastCursorPosition)
    }

    @Test
    fun `deleteDocumentById removes items`() = runTest {
        val doc = sampleDocument("p2", 5)
        repository.upsertDocument(doc)
        repository.upsertDocumentItem(sampleItem(doc.id, 1))

        repository.deleteDocumentById(doc.id)

        assertTrue(repository.observeDocuments("p2").first().isEmpty())
        assertTrue(repository.observeDocumentItems(doc.id).first().isEmpty())
    }

    @Test
    fun `deleteDocuments with projectId keeps other projects`() = runTest {
        val doc1 = sampleDocument("p1", 1)
        val doc2 = sampleDocument("p2", 2)
        repository.upsertDocument(doc1)
        repository.upsertDocument(doc2)

        repository.deleteDocuments(projectId = "p1")

        assertTrue(repository.observeDocuments("p1").first().isEmpty())
        assertEquals(listOf(doc2), repository.observeDocuments("p2").first())
    }

    @Test
    fun `deleteDocumentItemsByIds removes selected items`() = runTest {
        val doc = sampleDocument("p3", 6)
        repository.upsertDocument(doc)
        val item1 = sampleItem(doc.id, 1)
        val item2 = sampleItem(doc.id, 2)
        repository.upsertDocumentItem(item1)
        repository.upsertDocumentItem(item2)

        repository.deleteDocumentItemsByIds(listOf(item1.id))

        assertEquals(listOf(item2), repository.observeDocumentItems(doc.id).first())
    }
}
