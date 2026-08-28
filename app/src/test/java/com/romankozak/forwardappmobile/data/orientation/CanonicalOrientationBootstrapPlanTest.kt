package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalOrientationBootstrapPlanTest {
    @Test
    fun `bootstrap plan is idempotent after durable mapping exists`() {
        val projection = projection("goal-1")
        val first = planBootstrap(listOf(projection), emptyList(), emptySet(), Gson())

        assertEquals(1, first.rows.size)
        assertTrue(first.issues.isEmpty())

        val row = first.rows.single()
        val second =
            planBootstrap(
                projections = listOf(projection),
                existingMappings = listOf(row.mapping),
                existingSubjectIds = setOf(row.subject.id),
                gson = Gson(),
            )

        assertTrue(second.rows.isEmpty())
        assertTrue(second.issues.isEmpty())
    }

    @Test
    fun `bootstrap blocks a source mapped to a different subject`() {
        val projection = projection("goal-1")
        val conflicting =
            LegacySubjectMappingEntity(
                id = "mapping",
                sourceType = LegacyOrientationSourceType.GOAL.name,
                sourceId = "goal-1",
                subjectId = "different-subject",
                migrationVersion = 1,
                state = "MATERIALIZED",
                createdAt = 1,
                updatedAt = 1,
                syncedAt = null,
                isDeleted = false,
                version = 1,
            )

        val plan = planBootstrap(listOf(projection), listOf(conflicting), setOf("different-subject"), Gson())

        assertTrue(plan.rows.isEmpty())
        assertEquals("IDENTITY_COLLISION", plan.issues.single().code)
    }

    private fun projection(sourceId: String): EffectiveOrientation {
        val source = LegacySubjectRef(LegacyOrientationSourceType.GOAL, sourceId)
        val subjectId = LegacySubjectUuid.resolve(source)
        return EffectiveOrientation(
            subject =
                ManagedSubject(
                    id = subjectId,
                    createdAt = 1,
                    updatedAt = 2,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    subjectType = ManagedSubjectType.ORIENTATION,
                    title = "Goal",
                    description = null,
                ),
            orientation =
                OrientationNode(
                    subjectId = subjectId,
                    kind = OrientationKind.GOAL,
                    lifecycle = null,
                    lifecycleOrigin = ValueOrigin.UNSET,
                    assessment = emptyApplicableAssessment(),
                ),
            source = source,
            preservedSpecializedFields = emptyList(),
            diagnostics = emptyList(),
        )
    }
}
