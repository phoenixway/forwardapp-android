package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxCanonicalDeltaTest {
    @Test
    fun inboxTombstoneIsProjectedIntoCanonicalSnapshotDelta() {
        val tombstone =
            InboxRecord(
                id = "inbox-1",
                contextId = "context-1",
                text = "deleted inbox record",
                createdAt = 100L,
                order = -100L,
                updatedAt = 500L,
                syncedAt = null,
                hideInOwnerInbox = false,
                isDeleted = true,
                version = 3L,
            )

        val delta =
            buildCanonicalSnapshotDelta(
                source = DatabaseContent(inboxRecords = listOf(tombstone)),
                fullSnapshot = SnapshotBundle(version = 2),
            )

        assertEquals(1, delta.inbox.size)
        assertEquals(tombstone.id, delta.inbox.single().id)
        assertEquals(tombstone.version, delta.inbox.single().version)
        assertEquals(tombstone.updatedAt, delta.inbox.single().updatedAt)
        assertTrue(delta.inbox.single().isDeleted)
    }
}
