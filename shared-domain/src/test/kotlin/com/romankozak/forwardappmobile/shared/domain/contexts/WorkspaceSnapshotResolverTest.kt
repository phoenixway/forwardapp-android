package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceSnapshotResolverTest {
    private val resolver = WorkspaceSnapshotResolver()

    @Test
    fun resolvesDesktopSnapshot() {
        val result = resolver.resolve(DESKTOP_SNAPSHOT_JSON)

        assertNotNull(result)
        assertEquals(WorkspaceSnapshotFormat.Desktop, result?.format)
        assertEquals(2, result?.snapshot?.contexts?.size)
        assertEquals(1, result?.snapshot?.backlogItems?.size)
    }

    @Test
    fun resolvesAndroidSnapshotBundleV2() {
        val result = resolver.resolve(WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON)

        assertNotNull(result)
        assertEquals(WorkspaceSnapshotFormat.AndroidSnapshotBundleV2, result?.format)
        assertEquals(2, result?.snapshot?.contexts?.size)
        assertEquals(2, result?.snapshot?.backlogItems?.size)
    }

    @Test
    fun resolvesAndroidLegacyDatabase() {
        val result = resolver.resolve(WorkspaceSnapshotFixtures.ANDROID_LEGACY_DATABASE_JSON)

        assertNotNull(result)
        assertEquals(WorkspaceSnapshotFormat.AndroidLegacyDatabase, result?.format)
        assertEquals(2, result?.snapshot?.contexts?.size)
        assertEquals(2, result?.snapshot?.backlogItems?.size)
    }

    @Test
    fun returnsNullForUnsupportedPayload() {
        assertNull(resolver.resolve("{\"broken\":true}"))
    }

    private companion object {
        const val DESKTOP_SNAPSHOT_JSON =
            """
            {
              "contexts": [
                {
                  "id": "root",
                  "name": "Root",
                  "description": "Workspace root",
                  "parentId": null,
                  "status": "Planning",
                  "defaultView": "Backlog",
                  "score": 0,
                  "isCompleted": false
                },
                {
                  "id": "project",
                  "name": "Desktop App",
                  "description": "Desktop work",
                  "parentId": "root",
                  "status": "InProgress",
                  "defaultView": "Dashboard",
                  "score": 10,
                  "isCompleted": false
                }
              ],
              "backlogItems": [
                {
                  "id": "item-1",
                  "contextId": "project",
                  "title": "Ship explorer",
                  "details": "Use shared store",
                  "kind": "Task",
                  "priority": "High",
                  "isDone": false
                }
              ]
            }
            """

    }
}
