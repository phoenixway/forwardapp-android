package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceBacklogEntrySnapshotBundleTest {
    private val gson = Gson()

    @Test
    fun `absent canonical Backlog field means contract absent`() {
        val parsed = gson.fromJson("""{"snapshotVersion":2}""", SnapshotBundle::class.java)

        assertNull(parsed.workspaceBacklogEntries)
    }

    @Test
    fun `present empty canonical Backlog field means authoritative empty`() {
        val parsed =
            gson.fromJson(
                """{"snapshotVersion":2,"workspaceBacklogEntries":[]}""",
                SnapshotBundle::class.java,
            )

        assertEquals(emptyList<WorkspaceBacklogEntrySnapshot>(), parsed.workspaceBacklogEntries)
    }
}
