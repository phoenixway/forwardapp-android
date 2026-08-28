package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisCompareFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.FilterComparison
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAxis
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.notApplicableAxis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrientationContractTest {
    @Test
    fun unknownCodesRemainVisibleInsteadOfDefaulting() {
        val kind = resolveOrientationKind("future_kind")
        val lifecycle = resolveOrientationLifecycle("future_state")

        assertNull(kind.known)
        assertEquals("future_kind", kind.rawCode)
        assertNull(lifecycle.known)
        assertEquals("future_state", lifecycle.rawCode)
    }

    @Test
    fun dayThemeRejectsDefinitionLevelAttention() {
        val assessment =
            emptyApplicableAssessment().copy(
                expectedSpan = notApplicableAxis(),
                targetWindow = notApplicableAxis(),
                attentionTier = AxisAssessment("P1_ACTIVE", ValueOrigin.EXPLICIT),
            )

        val errors = validateOrientationAssessment(OrientationKind.DAY_THEME, assessment)

        assertTrue(errors.any { it.code == "AXIS_NOT_APPLICABLE" })
    }

    @Test
    fun legacyNumericMappingIsExplicitAndDoesNotClampUnknowns() {
        val source = LegacySubjectRef(LegacyOrientationSourceType.GOAL, "g1")
        val known = projectLegacyImportance(10f, "ASSESSED", source)
        val unknown = projectLegacyImportance(99f, "ASSESSED", source)

        assertEquals(ImportanceValue.CRITICAL.name, known.assessment.valueCode)
        assertEquals(ValueOrigin.DERIVED, known.assessment.origin)
        assertNull(unknown.assessment.valueCode)
        assertTrue(unknown.diagnostics.isNotEmpty())
    }

    @Test
    fun orderedAxisFilterUsesAcceptedOrdering() {
        val item = effectiveWithImportance(ImportanceValue.HIGH)
        val filter =
            AxisCompareFilter(
                axis = OrientationAxis.IMPORTANCE,
                comparison = FilterComparison.GREATER_OR_EQUAL,
                valueCode = ImportanceValue.MEDIUM.name,
            )

        assertTrue(matchesOrientationFilter(item, filter))
        assertFalse(
            matchesOrientationFilter(
                item,
                filter.copy(valueCode = ImportanceValue.CRITICAL.name),
            ),
        )
    }

    private fun effectiveWithImportance(value: ImportanceValue): EffectiveOrientation {
        val subject =
            ManagedSubject(
                id = "subject-1",
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                subjectType = ManagedSubjectType.ORIENTATION,
                title = "Test",
                description = null,
            )
        return EffectiveOrientation(
            subject = subject,
            orientation =
                OrientationNode(
                    subjectId = subject.id,
                    kind = OrientationKind.GOAL,
                    lifecycle = OrientationLifecycle.READY,
                    lifecycleOrigin = ValueOrigin.DERIVED,
                    assessment =
                        emptyApplicableAssessment().copy(
                            importance = AxisAssessment(value.name, ValueOrigin.EXPLICIT),
                        ),
                ),
            source = LegacySubjectRef(LegacyOrientationSourceType.GOAL, "g1"),
            preservedSpecializedFields = emptyList(),
            diagnostics = emptyList(),
        )
    }
}
