package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalWorkspaceProblemItem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextKeyProblemsRepositoryTest {
    @Test
    fun `loadData preserves canonical dateTime`() =
        runBlocking {
            val canonical = mockk<CanonicalKeyProblemsRepository>()
            coEvery { canonical.getItems("context") } returns
                listOf(
                    CanonicalWorkspaceProblemItem(
                        problem =
                            WorkspaceProblem(
                                id = "problem",
                                createdAt = 1L,
                                updatedAt = 2L,
                                syncedAt = null,
                                isDeleted = false,
                                version = 3L,
                                workspaceId = "context",
                                capabilityInstanceId = "capability",
                                title = "Dated problem",
                                description = "",
                                status = WorkspaceProblemStatus.OPEN,
                                order = 0L,
                                dateTime = 1_234L,
                            ),
                        relatedWorkspaceIds = emptyList(),
                        relatedAttachmentIds = emptyList(),
                    ),
                )

            val result = ContextKeyProblemsRepository(canonical).loadData("context")

            assertEquals(1_234L, result.issues.single().dateTime)
        }

    @Test
    fun `updateIssue forwards dateTime to canonical writer`() =
        runBlocking {
            val canonical = mockk<CanonicalKeyProblemsRepository>(relaxed = true)

            ContextKeyProblemsRepository(canonical).updateIssue(
                contextId = "context",
                issue =
                    ContextKeyProblemsRepository.IssueItem(
                        id = "problem",
                        title = "Dated problem",
                        dateTime = 5_678L,
                    ),
            )

            coVerify(exactly = 1) {
                canonical.updateProblem(
                    workspaceId = "context",
                    problemId = "problem",
                    title = "Dated problem",
                    description = "",
                    status = WorkspaceProblemStatus.OPEN,
                    relatedWorkspaceIds = emptyList(),
                    relatedAttachmentIds = emptyList(),
                    now = any(),
                    dateTime = 5_678L,
                )
            }
        }
}
