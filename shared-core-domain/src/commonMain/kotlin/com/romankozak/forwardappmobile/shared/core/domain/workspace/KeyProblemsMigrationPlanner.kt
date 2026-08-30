package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemAttachmentRef
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemWorkspaceRef

object KeyProblemsMigrationPlanner {
    fun plan(
        sources: List<LegacyKeyProblemsSource>,
        bindings: KeyProblemsMigrationBindings,
    ): KeyProblemsMigrationPlan {
        val parsed = sources.map(LegacyKeyProblemsPayloadParser::parse)
        val issues = parsed.flatMapTo(mutableListOf()) { it.issues }

        addOwnerDiagnostics(sources, bindings, issues)
        addDuplicateSourceDiagnostics(sources, bindings, issues)
        addGlobalProblemIdDiagnostics(parsed, bindings, issues)

        val problems = mutableListOf<WorkspaceProblem>()
        val workspaceRefs = mutableListOf<WorkspaceProblemWorkspaceRef>()
        val attachmentRefs = mutableListOf<WorkspaceProblemAttachmentRef>()

        parsed.forEach { parsedSource ->
            val workspaceId = bindings.workspaceIdByContextId[parsedSource.source.contextId] ?: return@forEach
            val capabilityId = bindings.capabilityInstanceIdByWorkspaceId[workspaceId] ?: return@forEach
            parsedSource.problems
                .sortedWith(compareBy<ParsedLegacyProblem> { it.sourceOrder }.thenBy { it.sourceIndex })
                .forEachIndexed { order, legacy ->
                    problems += legacy.toCanonical(workspaceId, capabilityId, order.toLong())
                    workspaceRefs += legacy.workspaceRefs(parsedSource.source, bindings, issues)
                    attachmentRefs += legacy.attachmentRefs(parsedSource.source, bindings, issues)
                }
        }

        addCanonicalCollisionDiagnostics(workspaceRefs, attachmentRefs, bindings, issues)
        validateKeyProblemsContract(problems, workspaceRefs, attachmentRefs).forEach { violation ->
            issues +=
                KeyProblemsMigrationIssue(
                    sourceContextId = "<canonical>",
                    issueId = null,
                    code = KeyProblemsMigrationIssueCode.CONTRACT_VIOLATION,
                    detail = "${violation.path}: ${violation.code}: ${violation.message}",
                )
        }

        return KeyProblemsMigrationPlan(
            sourceRowCount = sources.size,
            parsedSourceRowCount = parsed.count { it.issues.isEmpty() },
            sourceProblemCount = parsed.sumOf { it.problems.size },
            problems = problems,
            workspaceRefs = workspaceRefs,
            attachmentRefs = attachmentRefs,
            issues = issues.distinct(),
        )
    }

    private fun addOwnerDiagnostics(
        sources: List<LegacyKeyProblemsSource>,
        bindings: KeyProblemsMigrationBindings,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ) {
        sources.forEach { source ->
            val workspaceId = bindings.workspaceIdByContextId[source.contextId]
            if (workspaceId == null) {
                issues +=
                    source.issue(
                        KeyProblemsMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE,
                        "No provenance-backed Workspace mapping for owning Context",
                    )
            } else if (bindings.capabilityInstanceIdByWorkspaceId[workspaceId] == null) {
                issues +=
                    source.issue(
                        KeyProblemsMigrationIssueCode.UNRESOLVED_CAPABILITY_INSTANCE,
                        "Workspace has no KEY_PROBLEMS capability instance",
                    )
            }
        }
    }

    private fun addDuplicateSourceDiagnostics(
        sources: List<LegacyKeyProblemsSource>,
        bindings: KeyProblemsMigrationBindings,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ) {
        sources.groupBy { bindings.workspaceIdByContextId[it.contextId] }
            .filterKeys { it != null }
            .filterValues { it.size > 1 }
            .forEach { (workspaceId, duplicateSources) ->
                duplicateSources.forEach { source ->
                    issues +=
                        source.issue(
                            KeyProblemsMigrationIssueCode.MULTIPLE_SOURCES_FOR_WORKSPACE,
                            "Multiple legacy source rows resolve to Workspace $workspaceId",
                        )
                }
            }
    }

    private fun addGlobalProblemIdDiagnostics(
        parsed: List<ParsedKeyProblemsSource>,
        bindings: KeyProblemsMigrationBindings,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ) {
        val occurrences =
            parsed.flatMap { source -> source.problems.map { it.id to source.source.contextId } }
                .groupBy({ it.first }, { it.second })
        occurrences.filterValues { it.toSet().size > 1 }.forEach { (id, contextIds) ->
            contextIds.forEach { contextId ->
                issues +=
                    KeyProblemsMigrationIssue(
                        contextId,
                        id,
                        KeyProblemsMigrationIssueCode.DUPLICATE_ISSUE_ID,
                        "Issue id occurs in more than one legacy position",
                    )
            }
        }
        parsed.flatMap { it.problems }.filter { it.id in bindings.existingProblemIds }.forEach { problem ->
            issues +=
                KeyProblemsMigrationIssue(
                    "<canonical>",
                    problem.id,
                    KeyProblemsMigrationIssueCode.CANONICAL_ID_COLLISION,
                    "Legacy problem id collides with existing canonical state",
                )
        }
    }

    private fun addCanonicalCollisionDiagnostics(
        workspaceRefs: List<WorkspaceProblemWorkspaceRef>,
        attachmentRefs: List<WorkspaceProblemAttachmentRef>,
        bindings: KeyProblemsMigrationBindings,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ) {
        workspaceRefs.filter { it.id in bindings.existingWorkspaceRefIds }.forEach { ref ->
            issues += collision(ref.problemId, ref.id, "Workspace-ref")
        }
        attachmentRefs.filter { it.id in bindings.existingAttachmentRefIds }.forEach { ref ->
            issues += collision(ref.problemId, ref.id, "Attachment-ref")
        }
    }
}

private fun ParsedLegacyProblem.toCanonical(
    workspaceId: String,
    capabilityInstanceId: String,
    canonicalOrder: Long,
) =
    WorkspaceProblem(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        title = title,
        description = description,
        status = status,
        order = canonicalOrder,
    )

private fun ParsedLegacyProblem.workspaceRefs(
    source: LegacyKeyProblemsSource,
    bindings: KeyProblemsMigrationBindings,
    issues: MutableList<KeyProblemsMigrationIssue>,
): List<WorkspaceProblemWorkspaceRef> =
    relatedContextIds.mapNotNull { contextId ->
        val targetWorkspaceId = bindings.workspaceIdByContextId[contextId]
        if (targetWorkspaceId == null) {
            issues +=
                source.issue(
                    KeyProblemsMigrationIssueCode.UNRESOLVED_RELATED_WORKSPACE,
                    "Related Context $contextId has no provenance-backed Workspace mapping",
                    id,
                )
            null
        } else {
            WorkspaceProblemWorkspaceRef(
                id = workspaceRefId(id, targetWorkspaceId),
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                problemId = id,
                targetWorkspaceId = targetWorkspaceId,
            )
        }
    }

private fun ParsedLegacyProblem.attachmentRefs(
    source: LegacyKeyProblemsSource,
    bindings: KeyProblemsMigrationBindings,
    issues: MutableList<KeyProblemsMigrationIssue>,
): List<WorkspaceProblemAttachmentRef> =
    relatedAttachmentIds.mapNotNull { attachmentId ->
        if (attachmentId !in bindings.knownAttachmentIds) {
            issues +=
                source.issue(
                    KeyProblemsMigrationIssueCode.UNRESOLVED_ATTACHMENT,
                    "Related Attachment $attachmentId does not exist",
                    id,
                )
            null
        } else {
            WorkspaceProblemAttachmentRef(
                id = attachmentRefId(id, attachmentId),
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                problemId = id,
                attachmentId = attachmentId,
            )
        }
    }

private fun workspaceRefId(
    problemId: String,
    workspaceId: String,
) = "KEY_PROBLEM_WORKSPACE_REF:${problemId.length}:$problemId:${workspaceId.length}:$workspaceId"

private fun attachmentRefId(
    problemId: String,
    attachmentId: String,
) = "KEY_PROBLEM_ATTACHMENT_REF:${problemId.length}:$problemId:${attachmentId.length}:$attachmentId"

private fun LegacyKeyProblemsSource.issue(
    code: KeyProblemsMigrationIssueCode,
    detail: String,
    issueId: String? = null,
) = KeyProblemsMigrationIssue(contextId, issueId, code, detail)

private fun collision(
    problemId: String,
    id: String,
    kind: String,
) =
    KeyProblemsMigrationIssue(
        sourceContextId = "<canonical>",
        issueId = problemId,
        code = KeyProblemsMigrationIssueCode.CANONICAL_ID_COLLISION,
        detail = "$kind id $id collides with existing canonical state",
    )
