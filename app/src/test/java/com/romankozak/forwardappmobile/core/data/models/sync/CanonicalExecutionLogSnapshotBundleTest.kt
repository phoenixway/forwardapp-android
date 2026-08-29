package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalExecutionLogSnapshotBundleTest {
    private val gson = Gson()

    @Test
    fun `absent canonical execution log field means contract absent`() {
        val parsed = gson.fromJson("""{"snapshotVersion":2}""", SnapshotBundle::class.java)

        assertNull(parsed.canonicalExecutionLogs)
    }

    @Test
    fun `present empty canonical execution log field means authoritative empty`() {
        val parsed =
            gson.fromJson(
                """{"snapshotVersion":2,"canonicalExecutionLogs":[]}""",
                SnapshotBundle::class.java,
            )

        assertEquals(emptyList<CanonicalExecutionLogSnapshot>(), parsed.canonicalExecutionLogs)
    }
}
