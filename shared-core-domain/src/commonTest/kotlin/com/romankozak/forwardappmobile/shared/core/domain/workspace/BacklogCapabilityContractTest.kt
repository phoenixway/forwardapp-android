package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogEntry
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BacklogCapabilityContractTest {
    @Test
    fun `configuration v1 is an exact empty object`() {
        assertEquals("{}", BacklogCapabilityConfigurationCodec.encodeDefault())
        assertEquals(
            BacklogCapabilityConfigurationV1,
            BacklogCapabilityConfigurationCodec.decode(1, "{}"),
        )
        assertTrue(runCatching { BacklogCapabilityConfigurationCodec.decode(2, "{}") }.isFailure)
        assertTrue(runCatching { BacklogCapabilityConfigurationCodec.decode(1, "{\"x\":1}") }.isFailure)
    }

    @Test
    fun `planner maps the typed target union and preserves runtime item order`() {
        val items =
            listOf(
                item("goal-placement", "GOAL", "goal", order = -300L, version = 3L),
                item("workspace-placement", "PROJECT", "child-context", order = -200L),
                item("document-placement", "NOTE_DOCUMENT", "document", order = 5L),
            )
        val bindings =
            bindings(
                targetStates =
                    setOf(
                        ref(WorkspaceBacklogTargetKind.ORIENTATION, "orientation"),
                        ref(WorkspaceBacklogTargetKind.WORKSPACE, "child-workspace"),
                        ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, "document"),
                    ),
            )

        val plan = BacklogMigrationPlanner.plan(items, emptyList(), bindings)

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertEquals(
            listOf(
                ref(WorkspaceBacklogTargetKind.ORIENTATION, "orientation"),
                ref(WorkspaceBacklogTargetKind.WORKSPACE, "child-workspace"),
                ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, "document"),
            ),
            plan.entries.map { it.target },
        )
        assertEquals(listOf(0L, 1L, 2L), plan.entries.map { it.order })
        assertEquals(UNKNOWN_LEGACY_BACKLOG_TIMESTAMP, plan.entries.first().createdAt)
        assertEquals(50L, plan.entries.first().updatedAt)
        assertNull(plan.entries.first().syncedAt)
        assertEquals(3L, plan.entries.first().version)
    }

    @Test
    fun `derived hashtag and structural hierarchy rows are accounted without becoming authority`() {
        val derived =
            item(
                id = "derived",
                type = "GOAL",
                entityId = "missing-goal-is-safe-for-rebuildable-projection",
                associationOwnerContextId = "context",
                associationTag = "home",
            )
        val structural = item("structural", "SUBLIST", "child-context")
        val bindings =
            bindings(
                targetStates = setOf(ref(WorkspaceBacklogTargetKind.WORKSPACE, "child-workspace")),
                parentWorkspaceIdByWorkspaceId = mapOf("child-workspace" to "workspace"),
            )

        val plan = BacklogMigrationPlanner.plan(listOf(derived, structural), emptyList(), bindings)

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertTrue(plan.entries.isEmpty())
        assertEquals(
            listOf(
                LegacyBacklogSourceDisposition.RETIRED_DERIVED_HASHTAG,
                LegacyBacklogSourceDisposition.RETIRED_STRUCTURAL_HIERARCHY,
            ),
            plan.itemAccounting.map { it.disposition },
        )
    }

    @Test
    fun `deleted owner Workspace blocks migration even when identity is proven`() {
        val source = item("placement", "NOTE_DOCUMENT", "document")
        val target = ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, "document")
        val bindings =
            BacklogMigrationBindings(
                workspaceIdByContextId = mapOf("context" to "workspace"),
                ownerWorkspaceStateById =
                    mapOf("workspace" to BacklogOwnerWorkspaceState(isDeleted = true)),
                capabilityInstanceIdByWorkspaceId = mapOf("workspace" to "backlog-capability"),
                orientationIdByGoalId = emptyMap(),
                targetStateByRef = mapOf(target to BacklogTargetState(isDeleted = false)),
            )

        val plan = BacklogMigrationPlanner.plan(listOf(source), emptyList(), bindings)

        assertFalse(plan.canApply)
        assertFalse(plan.isFullyAccounted)
        assertEquals(
            LegacyBacklogSourceDisposition.QUARANTINED,
            plan.itemAccounting.single().disposition,
        )
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.DELETED_OWNER_WORKSPACE })
    }

    @Test
    fun `deleted Workspace target remains resolvable for tombstoned placement`() {
        val source =
            item(
                id = "placement",
                type = "PROJECT",
                entityId = "child-context",
                isDeleted = true,
            )
        val target = ref(WorkspaceBacklogTargetKind.WORKSPACE, "child-workspace")
        val bindings =
            BacklogMigrationBindings(
                workspaceIdByContextId =
                    mapOf(
                        "context" to "workspace",
                        "child-context" to "child-workspace",
                    ),
                ownerWorkspaceStateById =
                    mapOf(
                        "workspace" to BacklogOwnerWorkspaceState(isDeleted = false),
                        "child-workspace" to BacklogOwnerWorkspaceState(isDeleted = true),
                    ),
                capabilityInstanceIdByWorkspaceId = mapOf("workspace" to "backlog-capability"),
                orientationIdByGoalId = emptyMap(),
                targetStateByRef = mapOf(target to BacklogTargetState(isDeleted = true)),
            )

        val plan = BacklogMigrationPlanner.plan(listOf(source), emptyList(), bindings)

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertEquals(1, plan.entries.size)
        assertTrue(plan.entries.single().isDeleted)
        assertEquals(target, plan.entries.single().target)
    }

    @Test
    fun `BacklogOrder disagreement and orphan are warnings and item order remains authority`() {
        val items =
            listOf(
                item("first", "NOTE_DOCUMENT", "one", order = -20L),
                item("second", "NOTE_DOCUMENT", "two", order = -10L),
            )
        val orders =
            listOf(
                order("first", "one", order = 99L),
                order("old-attachment-order", "orphan", order = 0L),
            )
        val plan =
            BacklogMigrationPlanner.plan(
                items,
                orders,
                bindings(
                    targetStates =
                        setOf(
                            ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, "one"),
                            ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, "two"),
                        ),
                ),
            )

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertEquals(listOf("first", "second"), plan.entries.map { it.id })
        assertEquals(
            listOf(
                LegacyBacklogOrderDisposition.ACCOUNTED_MIRROR,
                LegacyBacklogOrderDisposition.RETIRED_ORPHAN,
            ),
            plan.orderAccounting.map { it.disposition },
        )
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.ORDER_VALUE_DISAGREEMENT })
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.ORPHAN_ORDER_RETIRED })
        assertTrue(plan.issues.all { it.severity == BacklogMigrationIssueSeverity.WARNING })
    }

    @Test
    fun `planner preserves legacy NOTE as a distinct historical target`() {
        val target = ref(WorkspaceBacklogTargetKind.LEGACY_NOTE, "note")
        val plan =
            BacklogMigrationPlanner.plan(
                items = listOf(item("legacy-note", "NOTE", "note")),
                orders = emptyList(),
                bindings = bindings(targetStates = setOf(target)),
            )

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertEquals(target, plan.entries.single().target)
    }

    @Test
    fun `planner fails closed on malformed provenance unsupported type and unresolved target`() {
        val plan =
            BacklogMigrationPlanner.plan(
                items =
                    listOf(
                        item("malformed", "GOAL", "goal", associationOwnerContextId = "context"),
                        item(
                            "non-goal-derived",
                            "CHECKLIST",
                            "checklist",
                            associationOwnerContextId = "context",
                            associationTag = "home",
                        ),
                        item("unsupported", "SCRIPT", "script"),
                        item("unresolved", "CHECKLIST", "missing"),
                    ),
                orders = emptyList(),
                bindings = bindings(),
            )

        assertFalse(plan.canApply)
        assertFalse(plan.isFullyAccounted)
        assertTrue(plan.entries.isEmpty())
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.MALFORMED_ASSOCIATION_PROVENANCE })
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.UNSUPPORTED_ITEM_TYPE })
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.UNRESOLVED_TARGET })
    }

    @Test
    fun `planner quarantines malformed and duplicate legacy order identity`() {
        val item = item("placement", "NOTE_DOCUMENT", "document")
        val orders =
            listOf(
                order("", "document", 0L),
                order("duplicate-a", "same-key", 1L),
                order("duplicate-b", "same-key", 2L),
            )
        val plan =
            BacklogMigrationPlanner.plan(
                listOf(item),
                orders,
                bindings(targetStates = setOf(ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, "document"))),
            )

        assertFalse(plan.canApply)
        assertFalse(plan.isFullyAccounted)
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.BLANK_ORDER_ID })
        assertTrue(plan.issues.any { it.code == BacklogMigrationIssueCode.DUPLICATE_ORDER_KEY })
        assertTrue(
            plan.orderAccounting.all {
                it.disposition == LegacyBacklogOrderDisposition.QUARANTINED
            },
        )
    }

    @Test
    fun `live placement rejects deleted target while tombstone preserves history`() {
        val target = ref(WorkspaceBacklogTargetKind.CHECKLIST, "checklist")
        val bindings =
            bindings(
                targetStates = emptySet(),
                explicitTargetStates = mapOf(target to BacklogTargetState(isDeleted = true)),
            )

        val live = BacklogMigrationPlanner.plan(listOf(item("live", "CHECKLIST", "checklist")), emptyList(), bindings)
        assertFalse(live.canApply)
        assertTrue(live.issues.any { it.code == BacklogMigrationIssueCode.LIVE_PLACEMENT_TARGETS_DELETED_CONTENT })

        val tombstone =
            BacklogMigrationPlanner.plan(
                listOf(item("deleted", "CHECKLIST", "checklist", isDeleted = true)),
                emptyList(),
                bindings,
            )
        assertTrue(tombstone.canApply)
        assertTrue(tombstone.isFullyAccounted)
        assertTrue(tombstone.entries.single().isDeleted)
    }

    @Test
    fun `same target may appear in different Backlogs but not twice in one Backlog`() {
        val target = ref(WorkspaceBacklogTargetKind.ORIENTATION, "orientation")
        val twoWorkspaces =
            bindings(targetStates = setOf(target)).copy(
                workspaceIdByContextId =
                    mapOf(
                        "context" to "workspace",
                        "context-2" to "workspace-2",
                        "goal" to "unused",
                    ),
                ownerWorkspaceStateById =
                    mapOf(
                        "workspace" to BacklogOwnerWorkspaceState(isDeleted = false),
                        "workspace-2" to BacklogOwnerWorkspaceState(isDeleted = false),
                    ),
                capabilityInstanceIdByWorkspaceId =
                    mapOf(
                        "workspace" to "backlog-capability",
                        "workspace-2" to "backlog-capability-2",
                    ),
            )
        val allowed =
            BacklogMigrationPlanner.plan(
                listOf(
                    item("one", "GOAL", "goal"),
                    item("two", "GOAL", "goal", contextId = "context-2"),
                ),
                emptyList(),
                twoWorkspaces,
            )
        assertTrue(allowed.canApply)
        assertEquals(2, allowed.entries.size)

        val duplicate =
            BacklogMigrationPlanner.plan(
                listOf(item("one", "GOAL", "goal"), item("two", "GOAL", "goal")),
                emptyList(),
                bindings(targetStates = setOf(target)),
            )
        assertFalse(duplicate.canApply)
        assertTrue(duplicate.issues.any { it.code == BacklogMigrationIssueCode.DUPLICATE_EXPLICIT_TARGET })
    }

    @Test
    fun `contract rejects conflicting ownership order and target placement`() {
        val violations =
            validateBacklogContract(
                listOf(
                    canonical("one", "workspace", "target", 0L),
                    canonical("two", "other-workspace", "target", 0L),
                ),
            )

        assertTrue(violations.any { it.code == "CAPABILITY_OWNER_MISMATCH" })
        assertTrue(violations.any { it.code == "DUPLICATE_ORDER" })
        assertTrue(violations.any { it.code == "DUPLICATE_LIVE_TARGET" })
    }

    private fun item(
        id: String,
        type: String,
        entityId: String,
        contextId: String = "context",
        order: Long = -1L,
        associationOwnerContextId: String? = null,
        associationTag: String? = null,
        isDeleted: Boolean = false,
        version: Long = 1L,
    ) = LegacyBacklogItemSource(
        id = id,
        contextId = contextId,
        itemType = type,
        entityId = entityId,
        associationOwnerContextId = associationOwnerContextId,
        associationTag = associationTag,
        order = order,
        updatedAt = 50L,
        syncedAt = 40L,
        isDeleted = isDeleted,
        version = version,
    )

    private fun order(
        id: String,
        itemId: String,
        order: Long,
    ) = LegacyBacklogOrderSource(id, "context", itemId, order, 1L, 50L, 40L, false)

    private fun bindings(
        targetStates: Set<WorkspaceBacklogTargetRef> = emptySet(),
        explicitTargetStates: Map<WorkspaceBacklogTargetRef, BacklogTargetState> = emptyMap(),
        parentWorkspaceIdByWorkspaceId: Map<String, String?> = emptyMap(),
    ) = BacklogMigrationBindings(
        workspaceIdByContextId =
            mapOf(
                "context" to "workspace",
                "child-context" to "child-workspace",
            ),
        ownerWorkspaceStateById =
            mapOf(
                "workspace" to BacklogOwnerWorkspaceState(isDeleted = false),
                "child-workspace" to BacklogOwnerWorkspaceState(isDeleted = false),
            ),
        capabilityInstanceIdByWorkspaceId = mapOf("workspace" to "backlog-capability"),
        orientationIdByGoalId = mapOf("goal" to "orientation"),
        targetStateByRef = targetStates.associateWith { BacklogTargetState(isDeleted = false) } + explicitTargetStates,
        parentWorkspaceIdByWorkspaceId = parentWorkspaceIdByWorkspaceId,
    )

    private fun ref(
        kind: WorkspaceBacklogTargetKind,
        id: String,
    ) = WorkspaceBacklogTargetRef(kind, id)

    private fun canonical(
        id: String,
        workspaceId: String,
        targetId: String,
        order: Long,
    ) = WorkspaceBacklogEntry(
        id = id,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        workspaceId = workspaceId,
        capabilityInstanceId = "capability",
        target = ref(WorkspaceBacklogTargetKind.NOTE_DOCUMENT, targetId),
        order = order,
    )
}
