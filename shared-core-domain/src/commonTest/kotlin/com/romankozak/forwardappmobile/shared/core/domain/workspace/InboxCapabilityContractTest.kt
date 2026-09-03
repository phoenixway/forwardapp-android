package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceInboxRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InboxCapabilityContractTest {
    @Test
    fun `config owns projection policy rather than per record hide state`() {
        val keep = InboxCapabilityConfigurationV1(InboxOwnerVisibility.KEEP_VISIBLE)
        val hide = InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED)

        assertTrue(inboxOwnerVisible(keep, hasForeignAssociation = true))
        assertFalse(inboxOwnerVisible(hide, hasForeignAssociation = true))
        assertTrue(inboxOwnerVisible(hide, hasForeignAssociation = false))

        val encoded = InboxCapabilityConfigurationCodec.encode(hide)
        assertEquals(hide, InboxCapabilityConfigurationCodec.decode(1, encoded))
        assertTrue(runCatching { InboxCapabilityConfigurationCodec.decode(2, encoded) }.isFailure)
    }

    @Test
    fun `js wire facade validates typed config and applies owner visibility`() {
        val keep = InboxCapabilityConfigurationCodec.encodeDefault()
        val hide = InboxCapabilityConfigurationCodec.encode(
            InboxCapabilityConfigurationV1(InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED),
        )

        assertTrue(validateInboxCapabilityConfigurationWire(1, keep).isEmpty())
        assertTrue(validateInboxCapabilityConfigurationWire(2, keep).isNotEmpty())
        assertTrue(validateInboxCapabilityConfigurationWire(1, "{}").isNotEmpty())
        assertTrue(inboxCapabilityOwnerVisibleWire(1, keep, hasForeignAssociation = true))
        assertFalse(inboxCapabilityOwnerVisibleWire(1, hide, hasForeignAssociation = true))
        assertTrue(inboxCapabilityOwnerVisibleWire(1, hide, hasForeignAssociation = false))
    }

    @Test
    fun `planner preserves identity sync lifecycle and current visible order`() {
        val plan =
            InboxMigrationPlanner.plan(
                sources =
                    listOf(
                        source(id = "newer", order = -200L, createdAt = 200L, version = 4L),
                        source(id = "older", order = -100L, createdAt = 100L, version = 2L),
                    ),
                bindings = bindings(),
            )

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertEquals(listOf("older", "newer"), plan.records.map { it.id })
        assertEquals(listOf(0L, 1L), plan.records.map { it.order })
        assertEquals(2L, plan.records.first().version)
        assertEquals(100L, plan.records.first().updatedAt)
        assertEquals("workspace", plan.records.first().workspaceId)
        assertEquals("inbox-capability", plan.records.first().capabilityInstanceId)
    }

    @Test
    fun `live legacy hide flag blocks while tombstone can discard obsolete presentation state`() {
        val live =
            InboxMigrationPlanner.plan(
                listOf(source(id = "live", hideInOwnerInbox = true)),
                bindings(),
            )
        assertFalse(live.canApply)
        assertTrue(live.issues.any { it.code == InboxMigrationIssueCode.LEGACY_HIDE_FLAG_REQUIRES_REVIEW })

        val tombstone =
            InboxMigrationPlanner.plan(
                listOf(source(id = "deleted", hideInOwnerInbox = true, isDeleted = true)),
                bindings(),
            )
        assertTrue(tombstone.canApply)
        assertTrue(tombstone.records.single().isDeleted)
    }

    @Test
    fun `duplicate unresolved and colliding owners fail closed`() {
        val sources =
            listOf(
                source(id = "duplicate", contextId = "one"),
                source(id = "duplicate", contextId = "missing"),
            )
        val plan =
            InboxMigrationPlanner.plan(
                sources,
                bindings().copy(existingCanonicalIds = setOf("duplicate")),
            )

        assertFalse(plan.canApply)
        assertTrue(plan.issues.any { it.code == InboxMigrationIssueCode.DUPLICATE_ID })
        assertTrue(plan.issues.any { it.code == InboxMigrationIssueCode.CANONICAL_ID_COLLISION })
        assertTrue(plan.issues.any { it.code == InboxMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE })
    }

    @Test
    fun `contract permits blank capture text but rejects duplicate live order`() {
        val violations =
            validateInboxContract(
                listOf(
                    canonical("one", order = 0L, text = ""),
                    canonical("two", order = 0L, text = "text"),
                ),
            )

        assertTrue(violations.any { it.code == "DUPLICATE_ORDER" })
        assertTrue(violations.none { it.code == "EMPTY_RECORD" })
    }

    private fun source(
        id: String,
        contextId: String = "context",
        order: Long = -1L,
        createdAt: Long = 1L,
        isDeleted: Boolean = false,
        hideInOwnerInbox: Boolean = false,
        version: Long = 1L,
    ) =
        LegacyInboxRecordSource(
            id = id,
            contextId = contextId,
            text = "text-$id",
            createdAt = createdAt,
            order = order,
            updatedAt = null,
            syncedAt = 5L,
            isDeleted = isDeleted,
            hideInOwnerInbox = hideInOwnerInbox,
            version = version,
        )

    private fun bindings() =
        InboxMigrationBindings(
            workspaceIdByContextId = mapOf("context" to "workspace", "one" to "workspace-one"),
            capabilityInstanceIdByWorkspaceId =
                mapOf(
                    "workspace" to "inbox-capability",
                    "workspace-one" to "inbox-capability-one",
                ),
        )

    private fun canonical(
        id: String,
        order: Long,
        text: String,
    ) =
        WorkspaceInboxRecord(
            id = id,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            workspaceId = "workspace",
            capabilityInstanceId = "capability",
            text = text,
            order = order,
        )
}
