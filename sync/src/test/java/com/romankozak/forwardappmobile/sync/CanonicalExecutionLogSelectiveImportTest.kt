package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalExecutionLogSelectiveImportTest {
    private val gson = Gson()

    @Test
    fun `preview projection exposes only logs with proven live Context backed owners`() {
        val source =
            bundle(
                """
                {
                  "workspaces": [
                    {
                      "id": "context-1",
                      "provenance": "CONTEXT_BACKED",
                      "sourceContextId": "context-1",
                      "isDeleted": false
                    },
                    {
                      "id": "canonical-only",
                      "provenance": "CANONICAL_ONLY",
                      "sourceContextId": null,
                      "isDeleted": false
                    },
                    {
                      "id": "malformed",
                      "provenance": "CONTEXT_BACKED",
                      "sourceContextId": "other-context",
                      "isDeleted": false
                    },
                    {
                      "id": "deleted-context",
                      "provenance": "CONTEXT_BACKED",
                      "sourceContextId": "deleted-context",
                      "isDeleted": true
                    }
                  ],
                  "canonicalExecutionLogs": [
                    {
                      "id": "keep",
                      "workspaceId": "context-1",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "keep",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    },
                    {
                      "id": "canonical-only-log",
                      "workspaceId": "canonical-only",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "canonical",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    },
                    {
                      "id": "malformed-log",
                      "workspaceId": "malformed",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "malformed",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    },
                    {
                      "id": "deleted-owner-log",
                      "workspaceId": "deleted-context",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "deleted owner",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    }
                  ]
                }
                """.trimIndent(),
            )

        val projected = source.projectCanonicalExecutionLogsForSelectiveImportPreview()

        assertEquals(listOf("keep"), projected.map { it.id })
        assertEquals("context-1", projected.single().contextId)
        assertEquals(2L, projected.single().version)
        assertEquals(110L, projected.single().updatedAt)
    }

    @Test
    fun `filter emits selected canonical logs only for selected Context backed owners and never legacy logs`() {
        val source =
            bundle(
                """
                {
                  "contexts": [
                    { "id": "context-1" },
                    { "id": "context-2" }
                  ],
                  "workspaces": [
                    {
                      "id": "context-1",
                      "provenance": "CONTEXT_BACKED",
                      "sourceContextId": "context-1",
                      "isDeleted": false
                    },
                    {
                      "id": "context-2",
                      "provenance": "CONTEXT_BACKED",
                      "sourceContextId": "context-2",
                      "isDeleted": false
                    }
                  ],
                  "logs": [
                    {
                      "id": "legacy-log",
                      "contextId": "context-1",
                      "timestamp": 1,
                      "type": "COMMENT",
                      "description": "legacy",
                      "details": null,
                      "updatedAt": 1,
                      "version": 1,
                      "isDeleted": false
                    }
                  ],
                  "canonicalExecutionLogs": [
                    {
                      "id": "keep",
                      "workspaceId": "context-1",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "keep",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    },
                    {
                      "id": "wrong-owner",
                      "workspaceId": "context-2",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "wrong owner",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    },
                    {
                      "id": "not-selected",
                      "workspaceId": "context-1",
                      "timestamp": 100,
                      "type": "COMMENT",
                      "description": "not selected",
                      "details": null,
                      "updatedAt": 110,
                      "version": 2,
                      "isDeleted": false
                    }
                  ]
                }
                """.trimIndent(),
            )

        val filtered =
            SnapshotBundleSelectiveImportFilter().filter(
                source = source,
                selection =
                    WorkspaceSelectiveImportSelection(
                        selectedContextIds = setOf("context-1"),
                        selectedContextLogIds =
                            setOf(
                                "keep",
                                "wrong-owner",
                                "legacy-log",
                            ),
                    ),
            )

        assertTrue(filtered.logs.isEmpty())
        assertNotNull(filtered.canonicalExecutionLogs)
        assertEquals(
            listOf("keep"),
            filtered.canonicalExecutionLogs.orEmpty().map { it.id },
        )
    }

    @Test
    fun `filter preserves absent canonical EXECUTION_LOG contract as absent and still rejects legacy authority`() {
        val source =
            bundle(
                """
                {
                  "contexts": [
                    { "id": "context-1" }
                  ],
                  "logs": [
                    {
                      "id": "legacy-log",
                      "contextId": "context-1",
                      "timestamp": 1,
                      "type": "COMMENT",
                      "description": "legacy",
                      "details": null,
                      "updatedAt": 1,
                      "version": 1,
                      "isDeleted": false
                    }
                  ]
                }
                """.trimIndent(),
            )

        val filtered =
            SnapshotBundleSelectiveImportFilter().filter(
                source = source,
                selection =
                    WorkspaceSelectiveImportSelection(
                        selectedContextIds = setOf("context-1"),
                        selectedContextLogIds = setOf("legacy-log"),
                    ),
            )

        assertTrue(filtered.logs.isEmpty())
        assertNull(filtered.canonicalExecutionLogs)
    }

    private fun bundle(json: String): SnapshotBundle =
        gson.fromJson(json, SnapshotBundle::class.java)
}
