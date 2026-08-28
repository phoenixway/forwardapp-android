package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AttributionMode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ContributionRole
import com.romankozak.forwardappmobile.shared.core.models.orientation.ContributionSet
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationContribution
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelation
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationAndWorkspaceContractTest {
    @Test
    fun detectsPartOfCycle() {
        val relations =
            listOf(
                relation("r1", "a", "b"),
                relation("r2", "b", "a"),
            )

        assertTrue(validateOrientationRelations(setOf("a", "b"), relations).any { it.code == "CYCLE" })
    }

    @Test
    fun rejectsTwoEmbodiedWorkspacesForOneSubject() {
        val bindings =
            listOf(
                binding("b1", "w1", "s1"),
                binding("b2", "w2", "s1"),
            )

        assertTrue(validateWorkspaceBindings(bindings).any { it.code == "MULTIPLE_EMBODIED_WORKSPACES" })
    }

    @Test
    fun normalizesAllocatedWeights() {
        val result =
            normalizedAllocation(
                ContributionSet(
                    mode = AttributionMode.ALLOCATED,
                    contributions =
                        listOf(
                            OrientationContribution("a", ContributionRole.ADVANCES, true, 1.0),
                            OrientationContribution("b", ContributionRole.SUPPORTS, false, 3.0),
                        ),
                ),
            )

        assertEquals(0.25, result[0].allocationWeight)
        assertEquals(0.75, result[1].allocationWeight)
    }

    private fun relation(id: String, from: String, to: String) =
        OrientationRelation(id, 1L, 1L, null, false, 1L, from, to, OrientationRelationType.PART_OF, 0L)

    private fun binding(id: String, workspaceId: String, subjectId: String) =
        WorkspaceBinding(id, 1L, 1L, null, false, 1L, workspaceId, subjectId, WorkspaceBindingType.EMBODIES, true, 0L)
}
