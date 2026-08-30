package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceInboxRecord

data class LegacyInboxRecordSource(
    val id: String,
    val contextId: String,
    val text: String,
    val createdAt: Long,
    val order: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val hideInOwnerInbox: Boolean,
    val version: Long,
)

data class InboxMigrationBindings(
    val workspaceIdByContextId: Map<String, String>,
    val capabilityInstanceIdByWorkspaceId: Map<String, String>,
    val existingCanonicalIds: Set<String> = emptySet(),
)

enum class InboxMigrationIssueCode {
    BLANK_ID,
    DUPLICATE_ID,
    UNRESOLVED_OWNER_WORKSPACE,
    UNRESOLVED_CAPABILITY_INSTANCE,
    MULTIPLE_CONTEXTS_FOR_WORKSPACE,
    LEGACY_HIDE_FLAG_REQUIRES_REVIEW,
    CANONICAL_ID_COLLISION,
    INVALID_VERSION,
    CONTRACT_VIOLATION,
}

data class InboxMigrationIssue(
    val recordId: String?,
    val contextId: String?,
    val code: InboxMigrationIssueCode,
    val detail: String,
)

data class InboxMigrationPlan(
    val sourceCount: Int,
    val records: List<WorkspaceInboxRecord>,
    val issues: List<InboxMigrationIssue>,
) {
    val canApply: Boolean
        get() = issues.isEmpty()

    val isFullyAccounted: Boolean
        get() = canApply && sourceCount == records.size
}

object InboxMigrationPlanner {
    fun plan(
        sources: List<LegacyInboxRecordSource>,
        bindings: InboxMigrationBindings,
    ): InboxMigrationPlan {
        val issues = mutableListOf<InboxMigrationIssue>()
        addSourceDiagnostics(sources, bindings, issues)

        val resolved =
            sources.mapNotNull { source ->
                val workspaceId = bindings.workspaceIdByContextId[source.contextId] ?: return@mapNotNull null
                val capabilityId = bindings.capabilityInstanceIdByWorkspaceId[workspaceId] ?: return@mapNotNull null
                ResolvedInboxSource(source, workspaceId, capabilityId)
            }

        val canonical =
            resolved.groupBy { it.workspaceId }.values.flatMap { owned ->
                owned.sortedWith(
                    compareByDescending<ResolvedInboxSource> { it.source.order }
                        .thenByDescending { it.source.createdAt }
                        .thenBy { it.source.id },
                ).mapIndexed { index, resolvedSource ->
                    resolvedSource.toCanonical(index.toLong())
                }
            }

        validateInboxContract(canonical).forEach { violation ->
            issues +=
                InboxMigrationIssue(
                    recordId = null,
                    contextId = null,
                    code = InboxMigrationIssueCode.CONTRACT_VIOLATION,
                    detail = "${violation.path}: ${violation.code}: ${violation.message}",
                )
        }
        return InboxMigrationPlan(sources.size, canonical, issues.distinct())
    }

    private fun addSourceDiagnostics(
        sources: List<LegacyInboxRecordSource>,
        bindings: InboxMigrationBindings,
        issues: MutableList<InboxMigrationIssue>,
    ) {
        sources.filter { it.id.isBlank() }.forEach { source ->
            issues += source.issue(InboxMigrationIssueCode.BLANK_ID, "Inbox id must not be blank")
        }
        sources.groupBy { it.id }.filterValues { it.size > 1 }.forEach { (id, duplicates) ->
            duplicates.forEach { source ->
                issues += source.issue(InboxMigrationIssueCode.DUPLICATE_ID, "Inbox id $id occurs more than once")
            }
        }
        sources.filter { it.id in bindings.existingCanonicalIds }.forEach { source ->
            issues += source.issue(InboxMigrationIssueCode.CANONICAL_ID_COLLISION, "Inbox id collides with canonical state")
        }
        sources.filter { it.version < 0L }.forEach { source ->
            issues += source.issue(InboxMigrationIssueCode.INVALID_VERSION, "Inbox version must not be negative")
        }
        sources.filter { !it.isDeleted && it.hideInOwnerInbox }.forEach { source ->
            issues +=
                source.issue(
                    InboxMigrationIssueCode.LEGACY_HIDE_FLAG_REQUIRES_REVIEW,
                    "Legacy per-row owner hiding cannot become canonical content authority",
                )
        }
        sources.forEach { source ->
            val workspaceId = bindings.workspaceIdByContextId[source.contextId]
            if (workspaceId == null) {
                issues += source.issue(InboxMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE, "No proven Workspace owner")
            } else if (bindings.capabilityInstanceIdByWorkspaceId[workspaceId] == null) {
                issues += source.issue(InboxMigrationIssueCode.UNRESOLVED_CAPABILITY_INSTANCE, "No INBOX capability instance")
            }
        }

        sources.map { it.contextId }.distinct()
            .groupBy { bindings.workspaceIdByContextId[it] }
            .filterKeys { it != null }
            .filterValues { it.size > 1 }
            .forEach { (workspaceId, contextIds) ->
                contextIds.forEach { contextId ->
                    issues +=
                        InboxMigrationIssue(
                            recordId = null,
                            contextId = contextId,
                            code = InboxMigrationIssueCode.MULTIPLE_CONTEXTS_FOR_WORKSPACE,
                            detail = "Several Context owners resolve to Workspace $workspaceId",
                        )
                }
            }
    }
}

private data class ResolvedInboxSource(
    val source: LegacyInboxRecordSource,
    val workspaceId: String,
    val capabilityInstanceId: String,
) {
    fun toCanonical(canonicalOrder: Long) =
        WorkspaceInboxRecord(
            id = source.id,
            createdAt = source.createdAt,
            updatedAt = source.updatedAt ?: source.createdAt,
            syncedAt = source.syncedAt,
            isDeleted = source.isDeleted,
            version = source.version,
            workspaceId = workspaceId,
            capabilityInstanceId = capabilityInstanceId,
            text = source.text,
            order = canonicalOrder,
        )
}

private fun LegacyInboxRecordSource.issue(
    code: InboxMigrationIssueCode,
    detail: String,
) = InboxMigrationIssue(id, contextId, code, detail)
