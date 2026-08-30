package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemWorkspaceRef
import kotlin.test.Test
import kotlin.test.assertTrue

class KeyProblemsContractTest {
    @Test
    fun `contract rejects duplicate order and empty unlinked problem`() {
        val violations =
            validateKeyProblemsContract(
                problems =
                    listOf(
                        problem("one", order = 0L),
                        problem("two", order = 0L, title = ""),
                    ),
                workspaceRefs = emptyList(),
                attachmentRefs = emptyList(),
            )

        assertTrue(violations.any { it.code == "DUPLICATE_ORDER" })
        assertTrue(violations.any { it.code == "EMPTY_PROBLEM" })
    }

    @Test
    fun `relation can keep a tombstoned target identity but requires a live problem`() {
        val problem = problem("problem", order = 0L)
        val validRef = workspaceRef("ref", problem.id, "tombstoned-target")

        assertTrue(
            validateKeyProblemsContract(
                problems = listOf(problem),
                workspaceRefs = listOf(validRef),
                attachmentRefs = emptyList(),
            ).isEmpty(),
        )

        val violations =
            validateKeyProblemsContract(
                problems = listOf(problem.copy(isDeleted = true)),
                workspaceRefs = listOf(validRef),
                attachmentRefs = emptyList(),
            )
        assertTrue(violations.any { it.code == "MISSING_PROBLEM" })
    }

    private fun problem(
        id: String,
        order: Long,
        title: String = "Problem",
    ) =
        WorkspaceProblem(
            id = id,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            workspaceId = "workspace",
            capabilityInstanceId = "capability",
            title = title,
            description = "",
            status = WorkspaceProblemStatus.OPEN,
            order = order,
        )

    private fun workspaceRef(
        id: String,
        problemId: String,
        targetWorkspaceId: String,
    ) =
        WorkspaceProblemWorkspaceRef(
            id = id,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            problemId = problemId,
            targetWorkspaceId = targetWorkspaceId,
        )
}
