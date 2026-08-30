package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemAttachmentRef
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemWorkspaceRef

data class LegacyKeyProblemsSource(
    val contextId: String,
    val payloadJson: String,
    val updatedAt: Long,
)

data class KeyProblemsMigrationBindings(
    val workspaceIdByContextId: Map<String, String>,
    val capabilityInstanceIdByWorkspaceId: Map<String, String>,
    val knownAttachmentIds: Set<String>,
    val existingProblemIds: Set<String> = emptySet(),
    val existingWorkspaceRefIds: Set<String> = emptySet(),
    val existingAttachmentRefIds: Set<String> = emptySet(),
)

enum class KeyProblemsMigrationIssueCode {
    INVALID_JSON,
    INVALID_PAYLOAD_SHAPE,
    INVALID_ISSUE,
    BLANK_ISSUE_ID,
    DUPLICATE_ISSUE_ID,
    UNKNOWN_STATUS,
    DATE_TIME_REQUIRES_DECISION,
    EMPTY_ISSUE,
    UNRESOLVED_OWNER_WORKSPACE,
    UNRESOLVED_CAPABILITY_INSTANCE,
    MULTIPLE_SOURCES_FOR_WORKSPACE,
    UNRESOLVED_RELATED_WORKSPACE,
    UNRESOLVED_ATTACHMENT,
    CANONICAL_ID_COLLISION,
    CONTRACT_VIOLATION,
}

data class KeyProblemsMigrationIssue(
    val sourceContextId: String,
    val issueId: String?,
    val code: KeyProblemsMigrationIssueCode,
    val detail: String,
)

data class ParsedLegacyProblem(
    val id: String,
    val title: String,
    val description: String,
    val status: WorkspaceProblemStatus,
    val relatedContextIds: List<String>,
    val relatedAttachmentIds: List<String>,
    val sourceOrder: Long,
    val sourceIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ParsedKeyProblemsSource(
    val source: LegacyKeyProblemsSource,
    val problems: List<ParsedLegacyProblem>,
    val issues: List<KeyProblemsMigrationIssue>,
)

data class KeyProblemsMigrationPlan(
    val sourceRowCount: Int,
    val parsedSourceRowCount: Int,
    val sourceProblemCount: Int,
    val problems: List<WorkspaceProblem>,
    val workspaceRefs: List<WorkspaceProblemWorkspaceRef>,
    val attachmentRefs: List<WorkspaceProblemAttachmentRef>,
    val issues: List<KeyProblemsMigrationIssue>,
) {
    val canApply: Boolean
        get() = issues.isEmpty()

    val isFullyAccounted: Boolean
        get() =
            canApply &&
                parsedSourceRowCount == sourceRowCount &&
                problems.size == sourceProblemCount
}
