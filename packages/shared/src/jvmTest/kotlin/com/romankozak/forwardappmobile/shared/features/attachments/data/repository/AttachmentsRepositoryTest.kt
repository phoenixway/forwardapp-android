package com.romankozak.forwardappmobile.shared.features.attachments.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.romankozak.forwardappmobile.shared.data.database.createTestDatabase
import com.romankozak.forwardappmobile.shared.data.database.createTestDriver
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.domain.model.Attachment
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
class AttachmentsRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: ForwardAppDatabase
    private lateinit var repository: AttachmentsRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        database = createTestDatabase(driver)
        repository = AttachmentsRepositoryImpl(database, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun attachment(id: String, entityId: String = "entity-$id", ownerProjectId: String? = null) = Attachment(
        id = id,
        attachmentType = "NOTE",
        entityId = entityId,
        ownerProjectId = ownerProjectId,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `observeProjectAttachments sorts by attachmentOrder`() = runTest {
        val first = attachment("a1")
        val second = attachment("a2")
        repository.upsertAttachment(first)
        repository.upsertAttachment(second)
        repository.linkAttachmentToProject("project-1", first.id, attachmentOrder = 2)
        repository.linkAttachmentToProject("project-1", second.id, attachmentOrder = 1)

        val items = repository.observeProjectAttachments("project-1").first()

        assertEquals(
            listOf(second.id, first.id),
            items.map { it.attachment.id },
        )
    }

    @Test
    fun `findAttachmentByEntity returns matching attachment`() = runTest {
        val attach = attachment("a1", entityId = "doc-42")
        repository.upsertAttachment(attach)

        val found = repository.findAttachmentByEntity("NOTE", "doc-42")

        assertEquals(attach, found)
    }

    @Test
    fun `unlinkAttachmentFromProject deletes orphan when requested`() = runTest {
        val attach = attachment("a1")
        repository.upsertAttachment(attach)
        repository.linkAttachmentToProject("project-1", attach.id, attachmentOrder = 1)

        repository.unlinkAttachmentFromProject("project-1", attach.id, deleteOrphan = true)

        assertNull(repository.getAttachmentById(attach.id))
    }

    @Test
    fun `deleteAttachment removes all links`() = runTest {
        val attach = attachment("a1")
        repository.upsertAttachment(attach)
        repository.linkAttachmentToProject("project-1", attach.id, attachmentOrder = 1)
        repository.linkAttachmentToProject("project-2", attach.id, attachmentOrder = 1)

        repository.deleteAttachment(attach.id)

        val links = database.projectAttachmentCrossRefQueries.getAllProjectAttachmentLinks().executeAsList()
        assertTrue(links.isEmpty())
    }
}
