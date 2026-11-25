package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonParser
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SnapshotBackupWireGuardTest {
    @Test
    fun `canonical fields and nested hierarchy envelopes are accepted`() {
        val snapshot = JsonParser.parseString(
            """{"managedSubjects":[{"id":"s1","title":"Name"}],"bigPicturePresentation":{"containers":[{"id":"c1"}],"memberships":[],"formatVersion":1},"groupScopeCoverage":{"formatVersion":1,"records":[]}}""",
        ).asJsonObject

        SnapshotBackupWireGuard.requireStableFields(snapshot)
    }

    @Test
    fun `minified collection and hierarchy envelope are rejected before restore`() {
        listOf(
            """{"managedSubjects":[{"a":"s1","b":"ORIENTATION"}]}""",
            """{"bigPicturePresentation":{"a":[{"a":"c1"}],"b":[],"c":1}}""",
        ).forEach { json ->
            try {
                SnapshotBackupWireGuard.requireStableFields(JsonParser.parseString(json).asJsonObject)
                fail("Expected minified backup to be rejected")
            } catch (failure: IllegalArgumentException) {
                assertTrue(failure.message.orEmpty().contains("нестабільними JSON-полями"))
            }
        }
    }
}
