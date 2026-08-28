package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AttentionTierValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.BreadthValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.CommitmentValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ConfidenceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ExpectedSpanValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAxis
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKindResolution
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycleResolution
import com.romankozak.forwardappmobile.shared.core.models.orientation.TargetWindowValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin

enum class AxisApplicability {
    APPLICABLE,
    DERIVED_ONGOING,
    NOT_APPLICABLE,
    DAILY_ASSIGNMENT_ONLY,
}

data class OrientationContractViolation(
    val path: String,
    val code: String,
    val message: String,
)

fun resolveOrientationKind(rawCode: String): OrientationKindResolution {
    val normalized = rawCode.trim().uppercase()
    return OrientationKindResolution(
        rawCode = rawCode,
        known = OrientationKind.entries.firstOrNull { it.name == normalized },
    )
}

fun resolveOrientationLifecycle(rawCode: String): OrientationLifecycleResolution {
    val normalized = rawCode.trim().uppercase()
    return OrientationLifecycleResolution(
        rawCode = rawCode,
        known = OrientationLifecycle.entries.firstOrNull { it.name == normalized },
    )
}

fun axisValueCodes(axis: OrientationAxis): List<String> =
    when (axis) {
        OrientationAxis.IMPORTANCE -> ImportanceValue.entries.map { it.name }
        OrientationAxis.IMPACT -> ImpactValue.entries.map { it.name }
        OrientationAxis.BREADTH -> BreadthValue.entries.map { it.name }
        OrientationAxis.EXPECTED_SPAN -> ExpectedSpanValue.entries.map { it.name }
        OrientationAxis.TARGET_WINDOW -> TargetWindowValue.entries.map { it.name }
        OrientationAxis.ATTENTION_TIER -> AttentionTierValue.entries.map { it.name }
        OrientationAxis.COMMITMENT -> CommitmentValue.entries.map { it.name }
        OrientationAxis.CONFIDENCE -> ConfidenceValue.entries.map { it.name }
    }

fun axisApplicability(
    kind: OrientationKind,
    axis: OrientationAxis,
): AxisApplicability =
    when (kind) {
        OrientationKind.ONGOING_STANDARD ->
            when (axis) {
                OrientationAxis.EXPECTED_SPAN -> AxisApplicability.DERIVED_ONGOING
                OrientationAxis.TARGET_WINDOW -> AxisApplicability.NOT_APPLICABLE
                else -> AxisApplicability.APPLICABLE
            }

        OrientationKind.DAY_THEME ->
            when (axis) {
                OrientationAxis.EXPECTED_SPAN,
                OrientationAxis.TARGET_WINDOW,
                -> AxisApplicability.NOT_APPLICABLE

                OrientationAxis.ATTENTION_TIER -> AxisApplicability.DAILY_ASSIGNMENT_ONLY
                else -> AxisApplicability.APPLICABLE
            }

        else -> AxisApplicability.APPLICABLE
    }

fun validateOrientationAssessment(
    kind: OrientationKind,
    assessment: OrientationAssessment,
): List<OrientationContractViolation> =
    OrientationAxis.entries.flatMap { axis ->
        validateAxisAssessment(
            axis = axis,
            applicability = axisApplicability(kind, axis),
            assessment = assessment.valueFor(axis),
        )
    }

private fun validateAxisAssessment(
    axis: OrientationAxis,
    applicability: AxisApplicability,
    assessment: AxisAssessment,
): List<OrientationContractViolation> {
    val path = "assessment.${axis.name.lowercase()}"
    val violations = mutableListOf<OrientationContractViolation>()
    val requiresNull = assessment.origin == ValueOrigin.UNSET || assessment.origin == ValueOrigin.NOT_APPLICABLE

    if (requiresNull && assessment.valueCode != null) {
        violations += violation(path, "VALUE_WITH_EMPTY_ORIGIN", "UNSET and NOT_APPLICABLE require a null value")
    }
    if (!requiresNull && assessment.valueCode == null) {
        violations += violation(path, "MISSING_VALUE", "${assessment.origin} requires a value")
    }
    assessment.valueCode?.let { value ->
        if (value !in axisValueCodes(axis)) {
            violations += violation(path, "UNKNOWN_VALUE", "Unknown ${axis.name} value: $value")
        }
    }

    when (applicability) {
        AxisApplicability.APPLICABLE ->
            if (assessment.origin == ValueOrigin.NOT_APPLICABLE) {
                violations += violation(path, "AXIS_APPLIES", "${axis.name} applies to this Orientation kind")
            }

        AxisApplicability.NOT_APPLICABLE,
        AxisApplicability.DAILY_ASSIGNMENT_ONLY,
        -> if (assessment.origin != ValueOrigin.NOT_APPLICABLE) {
            violations += violation(path, "AXIS_NOT_APPLICABLE", "${axis.name} is not defined on this Orientation")
        }

        AxisApplicability.DERIVED_ONGOING ->
            if (assessment.valueCode != ExpectedSpanValue.ONGOING.name || assessment.origin != ValueOrigin.DERIVED) {
                violations += violation(path, "EXPECTED_ONGOING", "Ongoing standard span must be DERIVED ONGOING")
            }
    }

    return violations
}

private fun violation(
    path: String,
    code: String,
    message: String,
): OrientationContractViolation = OrientationContractViolation(path, code, message)
