package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionsCapabilityContractTest {
    @Test
    fun `configuration v1 is exact empty object`() {
        assertEquals("{}", ConnectionsCapabilityConfigurationCodec.encodeDefault())
        assertEquals(
            ConnectionsCapabilityConfigurationV1,
            ConnectionsCapabilityConfigurationCodec.decode(1, "{}"),
        )
        assertTrue(
            runCatching {
                ConnectionsCapabilityConfigurationCodec.decode(2, "{}")
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ConnectionsCapabilityConfigurationCodec.decode(1, "{\"x\":1}")
            }.isFailure,
        )
    }

    @Test
    fun `js wire facade keeps configuration validation shared-owned`() {
        assertTrue(validateConnectionsCapabilityConfigurationWire(1, "{}").isEmpty())
        assertTrue(validateConnectionsCapabilityConfigurationWire(2, "{}").isNotEmpty())
        assertTrue(validateConnectionsCapabilityConfigurationWire(1, "{\"x\":1}").isNotEmpty())
    }

    @Test
    fun `planner preserves visible order and never invents legacy creation time`() {
        val plan =
            ConnectionsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            attachmentId = "older-target",
                            attachmentOrder = -100L,
                            updatedAt = null,
                            version = 2L,
                        ),
                        source(
                            attachmentId = "newer-target",
                            attachmentOrder = -100L,
                            updatedAt = 77L,
                            version = 4L,
                        ),
                        source(
                            attachmentId = "indexed-target",
                            attachmentOrder = 3L,
                            updatedAt = 88L,
                            version = 6L,
                        ),
                    ),
                bindings =
                    bindings(
                        attachmentStates =
                            mapOf(
                                "older-target" to attachment(createdAt = 100L),
                                "newer-target" to attachment(createdAt = 200L),
                                "indexed-target" to attachment(createdAt = 300L),
                            ),
                    ),
            )

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)

        assertEquals(
            listOf("newer-target", "older-target", "indexed-target"),
            plan.connections.map { it.attachmentId },
        )
        assertEquals(listOf(0L, 1L, 2L), plan.connections.map { it.order })

        val newer = plan.connections.first()
        assertEquals(UNKNOWN_LEGACY_CONNECTION_TIMESTAMP, newer.createdAt)
        assertEquals(77L, newer.updatedAt)
        assertEquals(null, newer.syncedAt)
        assertEquals(4L, newer.version)

        val older = plan.connections[1]
        assertEquals(UNKNOWN_LEGACY_CONNECTION_TIMESTAMP, older.createdAt)
        assertEquals(UNKNOWN_LEGACY_CONNECTION_TIMESTAMP, older.updatedAt)
    }

    @Test
    fun `tombstone may preserve history to deleted attachment but live placement may not`() {
        val tombstone =
            ConnectionsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            attachmentId = "deleted-target",
                            isDeleted = true,
                        ),
                    ),
                bindings =
                    bindings(
                        attachmentStates =
                            mapOf(
                                "deleted-target" to
                                    attachment(
                                        createdAt = 10L,
                                        isDeleted = true,
                                    ),
                            ),
                    ),
            )

        assertTrue(tombstone.canApply)
        assertTrue(tombstone.isFullyAccounted)
        assertTrue(tombstone.connections.single().isDeleted)

        val live =
            ConnectionsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            attachmentId = "deleted-target",
                            isDeleted = false,
                        ),
                    ),
                bindings =
                    bindings(
                        attachmentStates =
                            mapOf(
                                "deleted-target" to
                                    attachment(
                                        createdAt = 10L,
                                        isDeleted = true,
                                    ),
                            ),
                    ),
            )

        assertFalse(live.canApply)
        assertTrue(
            live.issues.any {
                it.code ==
                    ConnectionsMigrationIssueCode.LIVE_PLACEMENT_TARGETS_DELETED_ATTACHMENT
            },
        )
    }

    @Test
    fun `planner fails closed on unresolved duplicate and colliding legacy state`() {
        val duplicate =
            source(
                contextId = "context",
                attachmentId = "attachment",
            )

        val expectedId =
            canonicalWorkspaceConnectionId(
                capabilityInstanceId = "connections-capability",
                attachmentId = "attachment",
            )

        val plan =
            ConnectionsMigrationPlanner.plan(
                sources =
                    listOf(
                        duplicate,
                        duplicate,
                        source(
                            contextId = "missing-context",
                            attachmentId = "missing-attachment",
                        ),
                    ),
                bindings =
                    bindings(
                        attachmentStates =
                            mapOf(
                                "attachment" to attachment(createdAt = 1L),
                            ),
                    ).copy(
                        existingCanonicalIds = setOf(expectedId),
                    ),
            )

        assertFalse(plan.canApply)
        assertTrue(
            plan.issues.any {
                it.code == ConnectionsMigrationIssueCode.DUPLICATE_SOURCE_PLACEMENT
            },
        )
        assertTrue(
            plan.issues.any {
                it.code == ConnectionsMigrationIssueCode.CANONICAL_ID_COLLISION
            },
        )
        assertTrue(
            plan.issues.any {
                it.code == ConnectionsMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE
            },
        )
        assertTrue(
            plan.issues.any {
                it.code == ConnectionsMigrationIssueCode.UNRESOLVED_ATTACHMENT
            },
            "Actual issue codes: ${plan.issues.map { it.code }}",
        )
    }

    @Test
    fun `contract rejects duplicate order and duplicate attachment placement`() {
        val violations =
            validateConnectionsContract(
                listOf(
                    canonical(
                        id = "one",
                        attachmentId = "attachment",
                        order = 0L,
                    ),
                    canonical(
                        id = "two",
                        attachmentId = "attachment",
                        order = 0L,
                    ),
                ),
            )

        assertTrue(violations.any { it.code == "DUPLICATE_ORDER" })
        assertTrue(
            violations.any {
                it.code == "DUPLICATE_ATTACHMENT_PLACEMENT"
            },
        )
    }

    @Test
    fun `canonical identity belongs to capability placement not attachment content`() {
        val first =
            canonicalWorkspaceConnectionId(
                capabilityInstanceId = "capability-a",
                attachmentId = "attachment",
            )
        val same =
            canonicalWorkspaceConnectionId(
                capabilityInstanceId = "capability-a",
                attachmentId = "attachment",
            )
        val otherCapability =
            canonicalWorkspaceConnectionId(
                capabilityInstanceId = "capability-b",
                attachmentId = "attachment",
            )

        assertEquals(first, same)
        assertTrue(first != otherCapability)
    }

    private fun source(
        contextId: String = "context",
        attachmentId: String,
        attachmentOrder: Long = -1L,
        updatedAt: Long? = 5L,
        isDeleted: Boolean = false,
        version: Long = 1L,
    ): LegacyConnectionPlacementSource =
        LegacyConnectionPlacementSource(
            contextId = contextId,
            attachmentId = attachmentId,
            attachmentOrder = attachmentOrder,
            updatedAt = updatedAt,
            syncedAt = 4L,
            isDeleted = isDeleted,
            version = version,
        )

    private fun attachment(
        createdAt: Long,
        isDeleted: Boolean = false,
    ): LegacyConnectionAttachmentState =
        LegacyConnectionAttachmentState(
            createdAt = createdAt,
            isDeleted = isDeleted,
        )

    private fun bindings(
        attachmentStates: Map<String, LegacyConnectionAttachmentState>,
    ): ConnectionsMigrationBindings =
        ConnectionsMigrationBindings(
            workspaceIdByContextId =
                mapOf(
                    "context" to "workspace",
                ),
            capabilityInstanceIdByWorkspaceId =
                mapOf(
                    "workspace" to "connections-capability",
                ),
            attachmentStateById = attachmentStates,
        )

    private fun canonical(
        id: String,
        attachmentId: String,
        order: Long,
    ): WorkspaceConnection =
        WorkspaceConnection(
            id = id,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            workspaceId = "workspace",
            capabilityInstanceId = "capability",
            attachmentId = attachmentId,
            order = order,
        )
}
