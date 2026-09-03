package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogEntry
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef

object BacklogMigrationPlanner {
    fun plan(
        items: List<LegacyBacklogItemSource>,
        orders: List<LegacyBacklogOrderSource>,
        bindings: BacklogMigrationBindings,
    ): BacklogMigrationPlan {
        val issues = mutableListOf<BacklogMigrationIssue>()
        val invalidItemIndexes = diagnoseItemShape(items, bindings, issues)
        val outcomes =
            items.mapIndexed { index, source ->
                if (index in invalidItemIndexes) {
                    ItemOutcome(source, LegacyBacklogSourceDisposition.QUARANTINED)
                } else {
                    resolveItem(source, bindings, issues)
                }
            }.toMutableList()

        quarantineDuplicateLiveTargets(outcomes, issues)

        val migrated = outcomes.mapNotNull { it.resolved }
        val canonical = materializeCanonical(migrated)
        validateBacklogContract(canonical).forEach { violation ->
            issues +=
                issue(
                    code = BacklogMigrationIssueCode.CONTRACT_VIOLATION,
                    detail = "${violation.path}: ${violation.code}: ${violation.message}",
                )
        }

        val orderResult = accountLegacyBacklogOrders(items, orders)
        issues += orderResult.issues

        return BacklogMigrationPlan(
            itemSourceCount = items.size,
            orderSourceCount = orders.size,
            entries = canonical,
            itemAccounting = outcomes.map { BacklogSourceAccounting(it.source.id, it.disposition) },
            orderAccounting = orderResult.accounting,
            issues = issues.distinct(),
        )
    }
}

private fun diagnoseItemShape(
    items: List<LegacyBacklogItemSource>,
    bindings: BacklogMigrationBindings,
    issues: MutableList<BacklogMigrationIssue>,
): Set<Int> {
    val invalid = mutableSetOf<Int>()
    items.forEachIndexed { index, source ->
        fun reject(code: BacklogMigrationIssueCode, detail: String) {
            invalid += index
            issues += issue(source.id, code, detail)
        }
        if (source.id.isBlank()) reject(BacklogMigrationIssueCode.BLANK_ITEM_ID, "Backlog item id is blank")
        if (source.contextId.isBlank()) reject(BacklogMigrationIssueCode.BLANK_CONTEXT_ID, "Owner Context id is blank")
        if (source.entityId.isBlank()) reject(BacklogMigrationIssueCode.BLANK_ENTITY_ID, "Target entity id is blank")
        if (source.version < 0L) reject(BacklogMigrationIssueCode.INVALID_VERSION, "Backlog item version is negative")
        if (source.updatedAt != null && source.updatedAt < 0L) {
            reject(BacklogMigrationIssueCode.INVALID_ITEM_TIMESTAMP, "Backlog item updatedAt is negative")
        }
        if (!source.hasValidProvenanceShape()) {
            reject(
                BacklogMigrationIssueCode.MALFORMED_ASSOCIATION_PROVENANCE,
                "Association owner and tag must be either both absent or both non-blank",
            )
        }
        if (source.isDerivedHashtag() && source.itemType != "GOAL") {
            reject(
                BacklogMigrationIssueCode.MALFORMED_ASSOCIATION_PROVENANCE,
                "Only GOAL rows may carry hashtag-association provenance",
            )
        }
        if (source.itemType.toTargetKind() == null) {
            reject(BacklogMigrationIssueCode.UNSUPPORTED_ITEM_TYPE, "Unsupported Backlog item type ${source.itemType}")
        }
        val ownerWorkspaceId = bindings.workspaceIdByContextId[source.contextId]
        if (ownerWorkspaceId == null) {
            reject(BacklogMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE, "No proven Workspace owner")
        } else {
            val ownerState = bindings.ownerWorkspaceStateById[ownerWorkspaceId]
            if (ownerState == null) {
                reject(
                    BacklogMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE_STATE,
                    "Owner Workspace lifecycle state is unavailable",
                )
            } else if (ownerState.isDeleted) {
                reject(
                    BacklogMigrationIssueCode.DELETED_OWNER_WORKSPACE,
                    "Owner Workspace is deleted",
                )
            }
        }
    }

    items.withIndex().groupBy { it.value.id }.filterKeys { it.isNotBlank() }
        .filterValues { it.size > 1 }.values.flatten().forEach { indexed ->
            invalid += indexed.index
            issues += issue(indexed.value.id, BacklogMigrationIssueCode.DUPLICATE_ITEM_ID, "Backlog item id occurs more than once")
        }

    val contextIdsByWorkspace =
        items.map { it.contextId }.filter { it.isNotBlank() }.distinct()
            .groupBy { bindings.workspaceIdByContextId[it] }
            .filterKeys { it != null }.filterValues { it.size > 1 }
    contextIdsByWorkspace.values.flatten().toSet().let { ambiguousContexts ->
        items.forEachIndexed { index, source ->
            if (source.contextId in ambiguousContexts) {
                invalid += index
                issues +=
                    issue(
                        source.id,
                        BacklogMigrationIssueCode.MULTIPLE_CONTEXTS_FOR_WORKSPACE,
                        "Several legacy Context owners resolve to one Workspace",
                    )
            }
        }
    }
    return invalid
}

private fun resolveItem(
    source: LegacyBacklogItemSource,
    bindings: BacklogMigrationBindings,
    issues: MutableList<BacklogMigrationIssue>,
): ItemOutcome {
    if (source.isDerivedHashtag()) {
        return ItemOutcome(source, LegacyBacklogSourceDisposition.RETIRED_DERIVED_HASHTAG)
    }

    val workspaceId = bindings.workspaceIdByContextId.getValue(source.contextId)
    val capabilityId = bindings.capabilityInstanceIdByWorkspaceId[workspaceId]
    if (capabilityId == null) {
        issues += issue(source.id, BacklogMigrationIssueCode.UNRESOLVED_CAPABILITY_INSTANCE, "No BACKLOG capability instance")
        return ItemOutcome(source, LegacyBacklogSourceDisposition.QUARANTINED)
    }

    val target = source.resolveTarget(bindings)
    if (target == null) {
        issues += issue(source.id, BacklogMigrationIssueCode.UNRESOLVED_TARGET, "Canonical typed target cannot be resolved")
        return ItemOutcome(source, LegacyBacklogSourceDisposition.QUARANTINED)
    }

    if (
        target.kind == WorkspaceBacklogTargetKind.WORKSPACE &&
        bindings.parentWorkspaceIdByWorkspaceId[target.id] == workspaceId
    ) {
        return ItemOutcome(source, LegacyBacklogSourceDisposition.RETIRED_STRUCTURAL_HIERARCHY)
    }

    val targetState = bindings.targetStateByRef[target]
    if (targetState == null) {
        issues += issue(source.id, BacklogMigrationIssueCode.UNRESOLVED_TARGET, "Target state is unavailable for $target")
        return ItemOutcome(source, LegacyBacklogSourceDisposition.QUARANTINED)
    }
    if (!source.isDeleted && targetState.isDeleted) {
        issues +=
            issue(
                source.id,
                BacklogMigrationIssueCode.LIVE_PLACEMENT_TARGETS_DELETED_CONTENT,
                "Live Backlog placement targets deleted content $target",
            )
        return ItemOutcome(source, LegacyBacklogSourceDisposition.QUARANTINED)
    }
    if (source.id in bindings.existingCanonicalIds) {
        issues += issue(source.id, BacklogMigrationIssueCode.CANONICAL_ID_COLLISION, "Canonical Backlog id already exists")
        return ItemOutcome(source, LegacyBacklogSourceDisposition.QUARANTINED)
    }

    return ItemOutcome(
        source = source,
        disposition = LegacyBacklogSourceDisposition.MIGRATED_EXPLICIT,
        resolved = ResolvedBacklogItem(source, workspaceId, capabilityId, target),
    )
}

private fun quarantineDuplicateLiveTargets(
    outcomes: MutableList<ItemOutcome>,
    issues: MutableList<BacklogMigrationIssue>,
) {
    outcomes.withIndex().filter { it.value.resolved?.source?.isDeleted == false }
        .groupBy { indexed ->
            indexed.value.resolved!!.let { it.capabilityInstanceId to it.target }
        }.filterValues { it.size > 1 }.values.flatten().forEach { indexed ->
            val outcome = indexed.value
            issues +=
                issue(
                    outcome.source.id,
                    BacklogMigrationIssueCode.DUPLICATE_EXPLICIT_TARGET,
                    "Target has several live explicit placements in one Backlog",
                )
            outcomes[indexed.index] = outcome.copy(disposition = LegacyBacklogSourceDisposition.QUARANTINED, resolved = null)
        }
}

private fun materializeCanonical(resolved: List<ResolvedBacklogItem>): List<WorkspaceBacklogEntry> =
    resolved.groupBy { it.capabilityInstanceId }.values.flatMap { owned ->
        val sorted =
            owned.sortedWith(
                compareBy<ResolvedBacklogItem> { it.source.isDeleted }
                    .thenBy { it.source.order }
                    .thenBy { it.source.id },
            )
        sorted.mapIndexed { index, item -> item.toCanonical(index.toLong()) }
    }

private data class ItemOutcome(
    val source: LegacyBacklogItemSource,
    val disposition: LegacyBacklogSourceDisposition,
    val resolved: ResolvedBacklogItem? = null,
)

private data class ResolvedBacklogItem(
    val source: LegacyBacklogItemSource,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val target: WorkspaceBacklogTargetRef,
) {
    fun toCanonical(canonicalOrder: Long): WorkspaceBacklogEntry =
        WorkspaceBacklogEntry(
            id = source.id,
            createdAt = UNKNOWN_LEGACY_BACKLOG_TIMESTAMP,
            updatedAt = source.updatedAt ?: UNKNOWN_LEGACY_BACKLOG_TIMESTAMP,
            syncedAt = null,
            isDeleted = source.isDeleted,
            version = source.version,
            workspaceId = workspaceId,
            capabilityInstanceId = capabilityInstanceId,
            target = target,
            order = canonicalOrder,
        )
}

private fun LegacyBacklogItemSource.resolveTarget(bindings: BacklogMigrationBindings): WorkspaceBacklogTargetRef? {
    val kind = itemType.toTargetKind() ?: return null
    val targetId =
        when (kind) {
            WorkspaceBacklogTargetKind.ORIENTATION -> bindings.orientationIdByGoalId[entityId]
            WorkspaceBacklogTargetKind.WORKSPACE -> bindings.workspaceIdByContextId[entityId]
            else -> entityId
        } ?: return null
    return WorkspaceBacklogTargetRef(kind, targetId)
}

private fun String.toTargetKind(): WorkspaceBacklogTargetKind? =
    when (this) {
        "GOAL" -> WorkspaceBacklogTargetKind.ORIENTATION
        "SUBLIST", "PROJECT" -> WorkspaceBacklogTargetKind.WORKSPACE
        "LINK_ITEM" -> WorkspaceBacklogTargetKind.LINK_ITEM
        "NOTE" -> WorkspaceBacklogTargetKind.LEGACY_NOTE
        "NOTE_DOCUMENT" -> WorkspaceBacklogTargetKind.NOTE_DOCUMENT
        "CHECKLIST" -> WorkspaceBacklogTargetKind.CHECKLIST
        "MUSIC_NOTE" -> WorkspaceBacklogTargetKind.MUSIC_NOTE
        else -> null
    }

private fun LegacyBacklogItemSource.hasValidProvenanceShape(): Boolean =
    (associationOwnerContextId == null && associationTag == null) ||
        (!associationOwnerContextId.isNullOrBlank() && !associationTag.isNullOrBlank())

private fun LegacyBacklogItemSource.isDerivedHashtag(): Boolean =
    !associationOwnerContextId.isNullOrBlank() && !associationTag.isNullOrBlank()

internal fun issue(
    itemId: String? = null,
    code: BacklogMigrationIssueCode,
    detail: String,
    orderId: String? = null,
    severity: BacklogMigrationIssueSeverity = BacklogMigrationIssueSeverity.ERROR,
) = BacklogMigrationIssue(itemId, orderId, code, severity, detail)
