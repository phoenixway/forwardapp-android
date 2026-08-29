package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceDirectionEntrySnapshotBundleTest {
    private val gson = Gson()

    @Test
    fun `absent canonical Direction field means contract absent`() {
        val parsed = gson.fromJson(
            """{"snapshotVersion":2}""",
            SnapshotBundle::class.java,
        )

        assertNull(parsed.workspaceDirectionEntries)
    }

    @Test
    fun `present empty canonical Direction field means authoritative empty`() {
        val parsed = gson.fromJson(
            """{"snapshotVersion":2,"workspaceDirectionEntries":[]}""",
            SnapshotBundle::class.java,
        )

        assertEquals(
            emptyList<WorkspaceDirectionEntrySnapshot>(),
            parsed.workspaceDirectionEntries,
        )
    }
}
