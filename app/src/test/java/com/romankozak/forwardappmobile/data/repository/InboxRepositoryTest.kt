package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.data.logic.InboxAssociationCache
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxRepositoryTest {
    private val inboxRecordDao = mockk<InboxRecordDao>(relaxed = true)
    private val inboxRecordLinkDao = mockk<InboxRecordLinkDao>(relaxed = true)
    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val inboxAssociationCache = mockk<InboxAssociationCache>(relaxed = true)

    private val repository =
        InboxRepository(
            inboxRecordDao = inboxRecordDao,
            inboxRecordLinkDao = inboxRecordLinkDao,
            goalRepository = goalRepository,
            inboxAssociationCache = inboxAssociationCache,
        )

    @Test
    fun deleteInboxRecordCreatesSyncableTombstoneAndRemovesDerivedCache() = runTest {
        val existing =
            InboxRecord(
                id = "inbox-1",
                contextId = "source",
                text = "hello #work",
                createdAt = 100L,
                order = -100L,
                updatedAt = 200L,
                syncedAt = 210L,
                hideInOwnerInbox = false,
                isDeleted = false,
                version = 4L,
            )
        val updatedRecord = slot<InboxRecord>()

        coEvery { inboxRecordDao.getRecordById(existing.id) } returns existing
        coEvery { inboxRecordDao.update(capture(updatedRecord)) } returns Unit

        repository.deleteInboxRecordById(existing.id)

        assertTrue(updatedRecord.captured.isDeleted)
        assertTrue((updatedRecord.captured.updatedAt ?: 0L) > (existing.updatedAt ?: 0L))
        assertNull(updatedRecord.captured.syncedAt)
        assertEquals(existing.version + 1, updatedRecord.captured.version)
        coVerify(exactly = 1) { inboxAssociationCache.remove(existing.id) }
        coVerify(exactly = 0) { inboxRecordDao.deleteById(any()) }
    }

    @Test
    fun addInboxRecordRefreshesDerivedAssociationCacheWithoutPersistingVisibility() = runTest {
        val insertedRecord = slot<InboxRecord>()
        coEvery { inboxRecordDao.insert(capture(insertedRecord)) } returns Unit

        repository.addInboxRecord(text = "hello #tag1", contextId = "source")

        assertFalse(insertedRecord.captured.hideInOwnerInbox)
        coVerify(exactly = 1) { inboxAssociationCache.refresh(insertedRecord.captured) }
        coVerify(exactly = 0) { inboxRecordDao.update(any()) }
    }

}
