package com.romankozak.forwardappmobile.data.product

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalOrientationProductProjection
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalGoalSemanticReadRepositoryTest {
    @Test
    fun `CUT_OVER Goal common meaning comes from canonical Orientation`() {
        val result =
            projectCanonicalGoalSemantics(
                productRead =
                    CanonicalProductReadResult.Ready(
                        listOf(goalProjection("orientation", "Canonical title", "Canonical description")),
                    ),
                mappings = listOf(mapping("goal", "orientation")),
            )

        assertThat(result.single())
            .isEqualTo(
                CanonicalGoalSemanticProjection(
                    goalId = "goal",
                    orientationId = "orientation",
                    title = "Canonical title",
                    description = "Canonical description",
                    lifecycle = OrientationLifecycle.ACTIVE,
                ),
            )
    }

    @Test
    fun `missing canonical target fails without Goal fallback`() {
        assertThrows(IllegalArgumentException::class.java) {
            projectCanonicalGoalSemantics(
                productRead = CanonicalProductReadResult.Ready(emptyList()),
                mappings = listOf(mapping("goal", "missing")),
            )
        }
    }

    @Test
    fun `deleted mapping cannot resurrect legacy Goal semantics`() {
        val deleted = mapping("goal", "orientation").copy(isDeleted = true)

        val result =
            projectCanonicalGoalSemantics(
                productRead = CanonicalProductReadResult.Ready(emptyList()),
                mappings = listOf(deleted),
            )

        assertThat(result).isEmpty()
    }

    @Test
    fun `duplicate live Goal mappings fail closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            projectCanonicalGoalSemantics(
                productRead =
                    CanonicalProductReadResult.Ready(
                        listOf(
                            goalProjection("orientation-a", "A", null),
                            goalProjection("orientation-b", "B", null),
                        ),
                    ),
                mappings =
                    listOf(
                        mapping("goal", "orientation-a"),
                        mapping("goal", "orientation-b").copy(id = "mapping-b"),
                    ),
            )
        }
    }

    private fun goalProjection(
        id: String,
        title: String,
        description: String?,
    ): CanonicalOrientationProductProjection {
        val subject = mockk<ManagedSubject>()
        every { subject.id } returns id
        every { subject.title } returns title
        every { subject.description } returns description

        val orientation = mockk<OrientationNode>()
        every { orientation.kind } returns OrientationKind.GOAL
        every { orientation.lifecycle } returns OrientationLifecycle.ACTIVE

        return mockk<CanonicalOrientationProductProjection>().also { projection ->
            every { projection.subject } returns subject
            every { projection.orientation } returns orientation
        }
    }

    private fun mapping(
        goalId: String,
        orientationId: String,
    ) = LegacySubjectMappingEntity(
        id = "mapping-$goalId",
        sourceType = LegacyOrientationSourceType.GOAL.name,
        sourceId = goalId,
        subjectId = orientationId,
        migrationVersion = 1,
        state = LegacySubjectMappingState.CUT_OVER.name,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )
}
