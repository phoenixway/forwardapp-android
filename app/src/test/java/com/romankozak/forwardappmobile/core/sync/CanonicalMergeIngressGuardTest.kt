package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalMergeIngressGuardTest {
    @Test
    fun `merge rejects ordinary restore only backlog and inbox`() {
        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalMergeIngress(
                SnapshotBundle(backlogItems = listOf(backlog("ordinary"))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalMergeIngress(
                SnapshotBundle(inbox = listOf(inbox("ordinary"))),
            )
        }
    }

    @Test
    fun `canonical presence and exact System compatibility remain admitted`() {
        requireCanonicalMergeIngress(
            SnapshotBundle(
                backlogItems = listOf(backlog("ordinary")),
                inbox = listOf(inbox("ordinary")),
                workspaceBacklogEntries = emptyList(),
                workspaceInboxRecords = emptyList(),
            ),
        )
        requireCanonicalMergeIngress(
            SnapshotBundle(
                backlogItems = listOf(backlog(SystemContexts.INBOX.raw)),
                inbox = listOf(inbox(SystemContexts.INBOX.raw)),
            ),
        )
    }

    private fun backlog(ownerId: String) =
        BacklogItemSnapshot("backlog-$ownerId", ownerId, "NOTE", "note", 0L, 1L, 1L, false)

    private fun inbox(ownerId: String) =
        InboxRecordSnapshot("inbox-$ownerId", ownerId, "text", 1L, 0L, 1L, false, 1L, false)
}
