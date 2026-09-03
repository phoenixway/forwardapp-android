package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceConnection
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Legacy Context-scoped placement source.
 *
 * attachmentOrder is display/order state only. It must never be interpreted as
 * a creation timestamp because legacy reorder paths overwrite it with ordinary
 * positional indices.
 */
data class LegacyConnectionPlacementSource(
    val contextId: String,
    val attachmentId: String,
    val attachmentOrder: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

/**
 * Target Attachment metadata needed only to reproduce legacy visible ordering
 * and to reject live placements that target deleted content.
 */
data class LegacyConnectionAttachmentState(
    val createdAt: Long,
    val isDeleted: Boolean,
)

data class ConnectionsMigrationBindings(
    val workspaceIdByContextId: Map<String, String>,
    val capabilityInstanceIdByWorkspaceId: Map<String, String>,
    val attachmentStateById: Map<String, LegacyConnectionAttachmentState>,
    val existingCanonicalIds: Set<String> = emptySet(),
)

enum class ConnectionsMigrationIssueCode {
    BLANK_CONTEXT_ID,
    BLANK_ATTACHMENT_ID,
    DUPLICATE_SOURCE_PLACEMENT,
    UNRESOLVED_OWNER_WORKSPACE,
    UNRESOLVED_CAPABILITY_INSTANCE,
    MULTIPLE_CONTEXTS_FOR_WORKSPACE,
    UNRESOLVED_ATTACHMENT,
    LIVE_PLACEMENT_TARGETS_DELETED_ATTACHMENT,
    CANONICAL_ID_COLLISION,
    INVALID_VERSION,
    INVALID_ATTACHMENT_TIMESTAMP,
    CONTRACT_VIOLATION,
}

data class ConnectionsMigrationIssue(
    val contextId: String?,
    val attachmentId: String?,
    val code: ConnectionsMigrationIssueCode,
    val detail: String,
)

data class ConnectionsMigrationPlan(
    val sourceCount: Int,
    val resolvedSourceCount: Int,
    val connections: List<WorkspaceConnection>,
    val issues: List<ConnectionsMigrationIssue>,
) {
    val canApply: Boolean
        get() = issues.isEmpty()

    val isFullyAccounted: Boolean
        get() =
            canApply &&
                resolvedSourceCount == sourceCount &&
                connections.size == sourceCount
}

/**
 * Legacy ContextAttachmentCrossRef has no trustworthy creation timestamp.
 *
 * Zero is an explicit sentinel meaning "historical placement creation time
 * unknown". Migration must never use wall-clock time to manufacture history.
 */
const val UNKNOWN_LEGACY_CONNECTION_TIMESTAMP: Long = 0L

object ConnectionsMigrationPlanner {
    fun plan(
        sources: List<LegacyConnectionPlacementSource>,
        bindings: ConnectionsMigrationBindings,
    ): ConnectionsMigrationPlan {
        val issues = mutableListOf<ConnectionsMigrationIssue>()

        addSourceDiagnostics(
            sources = sources,
            bindings = bindings,
            issues = issues,
        )

        val resolved =
            sources.mapNotNull { source ->
                val workspaceId =
                    bindings.workspaceIdByContextId[source.contextId]
                        ?: return@mapNotNull null

                val capabilityInstanceId =
                    bindings.capabilityInstanceIdByWorkspaceId[workspaceId]
                        ?: return@mapNotNull null

                val attachmentState =
                    bindings.attachmentStateById[source.attachmentId]
                        ?: return@mapNotNull null

                if (!source.isDeleted && attachmentState.isDeleted) {
                    return@mapNotNull null
                }

                ResolvedConnectionSource(
                    source = source,
                    workspaceId = workspaceId,
                    capabilityInstanceId = capabilityInstanceId,
                    attachmentState = attachmentState,
                )
            }

        resolved.forEach { item ->
            val id =
                canonicalWorkspaceConnectionId(
                    capabilityInstanceId = item.capabilityInstanceId,
                    attachmentId = item.source.attachmentId,
                )
            if (id in bindings.existingCanonicalIds) {
                issues +=
                    item.source.issue(
                        code = ConnectionsMigrationIssueCode.CANONICAL_ID_COLLISION,
                        detail = "Canonical connection id $id already exists",
                    )
            }
        }

        val canonical =
            resolved
                .groupBy { it.capabilityInstanceId }
                .values
                .flatMap { owned ->
                    val sorted =
                        owned.sortedWith(
                            compareBy<ResolvedConnectionSource> {
                                it.source.attachmentOrder
                            }.thenByDescending {
                                it.attachmentState.createdAt
                            }.thenBy {
                                it.source.attachmentId
                            },
                        )

                    val live = sorted.filterNot { it.source.isDeleted }
                    val tombstones = sorted.filter { it.source.isDeleted }

                    (live + tombstones).mapIndexed { index, item ->
                        item.toCanonical(index.toLong())
                    }
                }

        validateConnectionsContract(canonical)
            .forEach { violation ->
                issues +=
                    ConnectionsMigrationIssue(
                        contextId = null,
                        attachmentId = null,
                        code = ConnectionsMigrationIssueCode.CONTRACT_VIOLATION,
                        detail = "${violation.path}: ${violation.code}: ${violation.message}",
                    )
            }

        return ConnectionsMigrationPlan(
            sourceCount = sources.size,
            resolvedSourceCount = resolved.size,
            connections = canonical,
            issues = issues.distinct(),
        )
    }

    private fun addSourceDiagnostics(
        sources: List<LegacyConnectionPlacementSource>,
        bindings: ConnectionsMigrationBindings,
        issues: MutableList<ConnectionsMigrationIssue>,
    ) {
        sources.filter { it.contextId.isBlank() }
            .forEach { source ->
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.BLANK_CONTEXT_ID,
                        "Legacy connection Context id must not be blank",
                    )
            }

        sources.filter { it.attachmentId.isBlank() }
            .forEach { source ->
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.BLANK_ATTACHMENT_ID,
                        "Legacy connection Attachment id must not be blank",
                    )
            }

        sources.groupBy { it.contextId to it.attachmentId }
            .filterValues { it.size > 1 }
            .values
            .flatten()
            .forEach { source ->
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.DUPLICATE_SOURCE_PLACEMENT,
                        "Legacy Context/Attachment placement occurs more than once",
                    )
            }

        sources.filter { it.version < 0L }
            .forEach { source ->
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.INVALID_VERSION,
                        "Legacy connection version must not be negative",
                    )
            }

        sources.forEach { source ->
            val workspaceId = bindings.workspaceIdByContextId[source.contextId]
            if (workspaceId == null) {
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE,
                        "No provenance-backed Workspace owner exists",
                    )
            } else if (bindings.capabilityInstanceIdByWorkspaceId[workspaceId] == null) {
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.UNRESOLVED_CAPABILITY_INSTANCE,
                        "No CONNECTIONS capability instance exists for Workspace $workspaceId",
                    )
            }

            val attachmentState = bindings.attachmentStateById[source.attachmentId]
            if (attachmentState == null) {
                issues +=
                    source.issue(
                        ConnectionsMigrationIssueCode.UNRESOLVED_ATTACHMENT,
                        "Attachment ${source.attachmentId} does not exist",
                    )
            } else {
                if (attachmentState.createdAt < 0L) {
                    issues +=
                        source.issue(
                            ConnectionsMigrationIssueCode.INVALID_ATTACHMENT_TIMESTAMP,
                            "Attachment ${source.attachmentId} has a negative createdAt timestamp",
                        )
                }

                if (!source.isDeleted && attachmentState.isDeleted) {
                    issues +=
                        source.issue(
                            ConnectionsMigrationIssueCode.LIVE_PLACEMENT_TARGETS_DELETED_ATTACHMENT,
                            "Live connection targets deleted Attachment ${source.attachmentId}",
                        )
                }
            }
        }

        sources.map { it.contextId }
            .distinct()
            .groupBy { bindings.workspaceIdByContextId[it] }
            .filterKeys { it != null }
            .filterValues { it.size > 1 }
            .forEach { (workspaceId, contextIds) ->
                contextIds.forEach { contextId ->
                    issues +=
                        ConnectionsMigrationIssue(
                            contextId = contextId,
                            attachmentId = null,
                            code = ConnectionsMigrationIssueCode.MULTIPLE_CONTEXTS_FOR_WORKSPACE,
                            detail = "Several legacy Context owners resolve to Workspace $workspaceId",
                        )
                }
            }
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun canonicalWorkspaceConnectionId(
    capabilityInstanceId: String,
    attachmentId: String,
): String =
    "WORKSPACE_CONNECTION:${capabilityInstanceId.length}:$capabilityInstanceId:${attachmentId.length}:$attachmentId"

private data class ResolvedConnectionSource(
    val source: LegacyConnectionPlacementSource,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val attachmentState: LegacyConnectionAttachmentState,
) {
    fun toCanonical(order: Long): WorkspaceConnection =
        WorkspaceConnection(
            id =
                canonicalWorkspaceConnectionId(
                    capabilityInstanceId = capabilityInstanceId,
                    attachmentId = source.attachmentId,
                ),
            createdAt = UNKNOWN_LEGACY_CONNECTION_TIMESTAMP,
            updatedAt = source.updatedAt ?: UNKNOWN_LEGACY_CONNECTION_TIMESTAMP,
            syncedAt = null,
            isDeleted = source.isDeleted,
            version = source.version,
            workspaceId = workspaceId,
            capabilityInstanceId = capabilityInstanceId,
            attachmentId = source.attachmentId,
            order = order,
        )
}

private fun LegacyConnectionPlacementSource.issue(
    code: ConnectionsMigrationIssueCode,
    detail: String,
): ConnectionsMigrationIssue =
    ConnectionsMigrationIssue(
        contextId = contextId,
        attachmentId = attachmentId,
        code = code,
        detail = detail,
    )
