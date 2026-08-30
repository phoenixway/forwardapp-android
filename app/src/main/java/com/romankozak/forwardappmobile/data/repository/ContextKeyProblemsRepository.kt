package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Context-screen compatibility facade over canonical Workspace KEY_PROBLEMS.
 *
 * Context-backed Workspace ids equal their source Context ids after the
 * Workspace cutover, so no secondary ownership mapping is introduced here.
 *
 * dateTime remains only as a temporary UI DTO field for source compatibility.
 * Canonical KEY_PROBLEMS v1 has no dateTime meaning: reads always return null
 * and writes with a value fail instead of silently discarding data.
 */
@Singleton
class ContextKeyProblemsRepository
    @Inject
    constructor(
        private val canonicalRepository: CanonicalKeyProblemsRepository,
    ) {
        enum class IssueStatus {
            OPEN,
            IN_PROGRESS,
            BLOCKED,
            RESOLVED,
            CLOSED,
        }

        data class IssueItem(
            val id: String,
            val title: String,
            val description: String = "",
            val dateTime: Long? = null,
            val status: IssueStatus = IssueStatus.OPEN,
            val relatedContextIds: List<String> = emptyList(),
            val relatedAttachmentIds: List<String> = emptyList(),
            val order: Long = 0L,
            val createdAt: Long = System.currentTimeMillis(),
            val updatedAt: Long = System.currentTimeMillis(),
        )

        data class KeyProblemsData(
            val issues: List<IssueItem> = emptyList(),
        )

        fun observe(contextId: String): Flow<KeyProblemsData> =
            canonicalRepository.observeItems(contextId).map { items ->
                KeyProblemsData(items.map { it.toCompatibilityIssue() })
            }

        suspend fun addIssue(
            contextId: String,
            title: String,
        ): IssueItem {
            val id =
                canonicalRepository.createProblem(
                    workspaceId = contextId,
                    title = title,
                )
            return requireNotNull(
                canonicalRepository.getItems(contextId)
                    .firstOrNull { it.problem.id == id },
            ) {
                "Canonical KEY_PROBLEMS create did not materialize Problem $id"
            }.toCompatibilityIssue()
        }

        suspend fun updateIssue(
            contextId: String,
            issue: IssueItem,
        ) {
            require(issue.dateTime == null) {
                "KEY_PROBLEMS canonical v1 does not support dateTime"
            }
            canonicalRepository.updateProblem(
                workspaceId = contextId,
                problemId = issue.id,
                title = issue.title,
                description = issue.description,
                status = issue.status.toCanonical(),
                relatedWorkspaceIds = issue.relatedContextIds,
                relatedAttachmentIds = issue.relatedAttachmentIds,
            )
        }

        suspend fun deleteIssue(
            contextId: String,
            issueId: String,
        ) {
            canonicalRepository.deleteProblem(
                workspaceId = contextId,
                problemId = issueId,
            )
        }

        suspend fun reorderIssues(
            contextId: String,
            issueIds: List<String>,
        ) {
            canonicalRepository.reorderProblems(
                workspaceId = contextId,
                orderedProblemIds = issueIds,
            )
        }

        suspend fun updateDescription(
            contextId: String,
            description: String,
        ) {
            val current = loadData(contextId)
            val primary = current.issues.firstOrNull()
            if (primary == null) {
                val created = addIssue(contextId, "Issue")
                updateIssue(contextId, created.copy(description = description))
            } else {
                updateIssue(contextId, primary.copy(description = description))
            }
        }

        suspend fun addFocusContext(
            contextId: String,
            focusContextId: String,
        ) {
            val current = loadData(contextId)
            val primary = current.issues.firstOrNull() ?: addIssue(contextId, "Issue")
            if (focusContextId in primary.relatedContextIds) return
            updateIssue(
                contextId,
                primary.copy(
                    relatedContextIds = primary.relatedContextIds + focusContextId,
                ),
            )
        }

        suspend fun removeFocusContext(
            contextId: String,
            focusContextId: String,
        ) {
            val primary = loadData(contextId).issues.firstOrNull() ?: return
            updateIssue(
                contextId,
                primary.copy(
                    relatedContextIds =
                        primary.relatedContextIds.filterNot { it == focusContextId },
                ),
            )
        }

        suspend fun loadData(contextId: String): KeyProblemsData =
            KeyProblemsData(
                canonicalRepository.getItems(contextId)
                    .map { it.toCompatibilityIssue() },
            )

        private fun com.romankozak.forwardappmobile.data.workspace.capability.CanonicalWorkspaceProblemItem.toCompatibilityIssue() =
            IssueItem(
                id = problem.id,
                title = problem.title,
                description = problem.description,
                dateTime = null,
                status = problem.status.toCompatibility(),
                relatedContextIds = relatedWorkspaceIds,
                relatedAttachmentIds = relatedAttachmentIds,
                order = problem.order,
                createdAt = problem.createdAt,
                updatedAt = problem.updatedAt,
            )

        private fun IssueStatus.toCanonical(): WorkspaceProblemStatus =
            WorkspaceProblemStatus.valueOf(name)

        private fun WorkspaceProblemStatus.toCompatibility(): IssueStatus =
            IssueStatus.valueOf(name)
    }
