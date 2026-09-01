package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogEntry
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef

data class LegacyBacklogItemSource(
    val id: String,
    val contextId: String,
    val itemType: String,
    val entityId: String,
    val associationOwnerContextId: String?,
    val associationTag: String?,
    val order: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

data class LegacyBacklogOrderSource(
    val id: String,
    val listId: String,
    val itemId: String,
    val order: Long,
    val orderVersion: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
)

data class BacklogTargetState(
    val isDeleted: Boolean,
)

data class BacklogOwnerWorkspaceState(
    val isDeleted: Boolean,
)

data class BacklogMigrationBindings(
    val workspaceIdByContextId: Map<String, String>,
    val ownerWorkspaceStateById: Map<String, BacklogOwnerWorkspaceState>,
    val capabilityInstanceIdByWorkspaceId: Map<String, String>,
    val orientationIdByGoalId: Map<String, String>,
    val targetStateByRef: Map<WorkspaceBacklogTargetRef, BacklogTargetState>,
    val parentWorkspaceIdByWorkspaceId: Map<String, String?> = emptyMap(),
    val existingCanonicalIds: Set<String> = emptySet(),
)

enum class LegacyBacklogSourceDisposition {
    MIGRATED_EXPLICIT,
    RETIRED_DERIVED_HASHTAG,
    RETIRED_STRUCTURAL_HIERARCHY,
    QUARANTINED,
}

data class BacklogSourceAccounting(
    val sourceId: String,
    val disposition: LegacyBacklogSourceDisposition,
)

enum class LegacyBacklogOrderDisposition {
    ACCOUNTED_MIRROR,
    RETIRED_ORPHAN,
    QUARANTINED,
}

data class BacklogOrderAccounting(
    val sourceId: String,
    val disposition: LegacyBacklogOrderDisposition,
)

enum class BacklogMigrationIssueSeverity {
    WARNING,
    ERROR,
}

enum class BacklogMigrationIssueCode {
    BLANK_ITEM_ID,
    DUPLICATE_ITEM_ID,
    BLANK_CONTEXT_ID,
    BLANK_ENTITY_ID,
    INVALID_VERSION,
    INVALID_ITEM_TIMESTAMP,
    MALFORMED_ASSOCIATION_PROVENANCE,
    UNSUPPORTED_ITEM_TYPE,
    UNRESOLVED_OWNER_WORKSPACE,
    UNRESOLVED_OWNER_WORKSPACE_STATE,
    DELETED_OWNER_WORKSPACE,
    UNRESOLVED_CAPABILITY_INSTANCE,
    MULTIPLE_CONTEXTS_FOR_WORKSPACE,
    UNRESOLVED_TARGET,
    LIVE_PLACEMENT_TARGETS_DELETED_CONTENT,
    DUPLICATE_EXPLICIT_TARGET,
    CANONICAL_ID_COLLISION,
    BLANK_ORDER_ID,
    BLANK_ORDER_OWNER,
    BLANK_ORDER_TARGET,
    DUPLICATE_ORDER_ID,
    DUPLICATE_ORDER_KEY,
    INVALID_ORDER_VERSION,
    ORDER_OWNER_OR_TARGET_MISMATCH,
    AMBIGUOUS_ORDER_TARGET,
    ORPHAN_ORDER_RETIRED,
    ORDER_VALUE_DISAGREEMENT,
    CONTRACT_VIOLATION,
}

data class BacklogMigrationIssue(
    val itemId: String?,
    val orderId: String?,
    val code: BacklogMigrationIssueCode,
    val severity: BacklogMigrationIssueSeverity,
    val detail: String,
)

data class BacklogMigrationPlan(
    val itemSourceCount: Int,
    val orderSourceCount: Int,
    val entries: List<WorkspaceBacklogEntry>,
    val itemAccounting: List<BacklogSourceAccounting>,
    val orderAccounting: List<BacklogOrderAccounting>,
    val issues: List<BacklogMigrationIssue>,
) {
    val canApply: Boolean
        get() = issues.none { it.severity == BacklogMigrationIssueSeverity.ERROR }

    val isFullyAccounted: Boolean
        get() =
            canApply &&
                itemAccounting.size == itemSourceCount &&
                orderAccounting.size == orderSourceCount &&
                itemAccounting.none { it.disposition == LegacyBacklogSourceDisposition.QUARANTINED } &&
                orderAccounting.none { it.disposition == LegacyBacklogOrderDisposition.QUARANTINED }
}

const val UNKNOWN_LEGACY_BACKLOG_TIMESTAMP: Long = 0L
