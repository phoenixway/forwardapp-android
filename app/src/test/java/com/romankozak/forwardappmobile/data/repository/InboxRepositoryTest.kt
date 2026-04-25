package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxRepositoryTest {
    private val inboxRecordDao = mockk<InboxRecordDao>(relaxed = true)
    private val inboxRecordLinkDao = mockk<InboxRecordLinkDao>(relaxed = true)
    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val tagAssociationHandler = mockk<TagAssociationHandler>(relaxed = true)
    private val contextStructureRepository = mockk<ContextStructureRepository>(relaxed = true)

    private val repository =
        InboxRepository(
            inboxRecordDao = inboxRecordDao,
            inboxRecordLinkDao = inboxRecordLinkDao,
            goalRepository = goalRepository,
            tagAssociationHandler = tagAssociationHandler,
            contextStructureRepository = contextStructureRepository,
        )

    @Test
    fun addInboxRecordHidesOwnerRecordWhenRouterOptionEnabledAndAutocopyHappened() = runTest {
        val insertedRecord = slot<InboxRecord>()
        val updatedRecord = slot<InboxRecord>()
        coEvery { inboxRecordDao.insert(capture(insertedRecord)) } returns Unit
        coEvery { inboxRecordDao.update(capture(updatedRecord)) } returns Unit
        coEvery {
            contextStructureRepository.getStructureByContext("source")
        } returns ContextConfiguration.default("source").copy(removeInboxEntryAfterTagAutocopy = true)
        coEvery {
            tagAssociationHandler.syncInboxRecordAssociations(any())
        } returns mapOf("target" to "tag1")

        repository.addInboxRecord(text = "hello #tag1", contextId = "source")

        assertFalse(insertedRecord.captured.hideInOwnerInbox)
        assertTrue(updatedRecord.captured.hideInOwnerInbox)
        coVerify(exactly = 1) { inboxRecordDao.update(any()) }
    }

    @Test
    fun addInboxRecordKeepsOwnerRecordVisibleWhenRouterOptionDisabled() = runTest {
        val insertedRecord = slot<InboxRecord>()
        coEvery { inboxRecordDao.insert(capture(insertedRecord)) } returns Unit
        coEvery {
            contextStructureRepository.getStructureByContext("source")
        } returns ContextConfiguration.default("source").copy(removeInboxEntryAfterTagAutocopy = false)
        coEvery {
            tagAssociationHandler.syncInboxRecordAssociations(any())
        } returns mapOf("target" to "tag1")

        repository.addInboxRecord(text = "hello #tag1", contextId = "source")

        assertFalse(insertedRecord.captured.hideInOwnerInbox)
        coVerify(exactly = 0) { inboxRecordDao.update(any()) }
    }
}
