package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueProvenance

data class LegacyAxisProjection(
    val assessment: AxisAssessment,
    val diagnostics: List<String>,
)

fun projectLegacyImportance(
    value: Float,
    scoringStatus: String,
    source: LegacySubjectRef,
): LegacyAxisProjection =
    projectLegacyAxis(
        fieldName = "valueImportance",
        value = value,
        scoringStatus = scoringStatus,
        source = source,
        mapping =
            mapOf(
                1 to ImportanceValue.LOW.name,
                2 to ImportanceValue.LOW.name,
                3 to ImportanceValue.LOW.name,
                4 to ImportanceValue.MEDIUM.name,
                5 to ImportanceValue.MEDIUM.name,
                6 to ImportanceValue.MEDIUM.name,
                7 to ImportanceValue.HIGH.name,
                8 to ImportanceValue.HIGH.name,
                9 to ImportanceValue.HIGH.name,
                10 to ImportanceValue.CRITICAL.name,
                11 to ImportanceValue.CRITICAL.name,
                12 to ImportanceValue.CRITICAL.name,
            ),
    )

fun projectLegacyImpact(
    value: Float,
    scoringStatus: String,
    source: LegacySubjectRef,
): LegacyAxisProjection =
    projectLegacyAxis(
        fieldName = "valueImpact",
        value = value,
        scoringStatus = scoringStatus,
        source = source,
        mapping =
            mapOf(
                1 to ImpactValue.TINY.name,
                2 to ImpactValue.SMALL.name,
                3 to ImpactValue.MEDIUM.name,
                5 to ImpactValue.LARGE.name,
                8 to ImpactValue.TRANSFORMATIVE.name,
                13 to ImpactValue.TRANSFORMATIVE.name,
            ),
    )

fun mapLegacyGoalLifecycle(rawStatus: String?): OrientationLifecycle? =
    when (rawStatus?.trim()?.uppercase()) {
        "ACTIVE" -> OrientationLifecycle.READY
        "IN_WORK" -> OrientationLifecycle.ACTIVE
        "PAUSED" -> OrientationLifecycle.PAUSED
        "UNSURE" -> OrientationLifecycle.EXPLORING
        "DONE" -> OrientationLifecycle.REALIZED
        "CANCELED" -> OrientationLifecycle.DROPPED
        else -> null
    }

fun mapLegacyContextLifecycle(rawStatus: String?): OrientationLifecycle? =
    when (rawStatus?.trim()?.uppercase()) {
        "NO_PLAN", "PLANNING" -> OrientationLifecycle.READY
        "IN_PROGRESS" -> OrientationLifecycle.ACTIVE
        "COMPLETED" -> OrientationLifecycle.REALIZED
        "ON_HOLD", "PAUSED" -> OrientationLifecycle.PAUSED
        else -> null
    }

fun mapLegacyArcLifecycle(rawStatus: String?): OrientationLifecycle? =
    when (rawStatus?.trim()?.uppercase()) {
        "ACTIVE" -> OrientationLifecycle.ACTIVE
        "PAUSED" -> OrientationLifecycle.PAUSED
        "DONE" -> OrientationLifecycle.REALIZED
        else -> null
    }

private fun projectLegacyAxis(
    fieldName: String,
    value: Float,
    scoringStatus: String,
    source: LegacySubjectRef,
    mapping: Map<Int, String>,
): LegacyAxisProjection {
    val normalizedStatus = scoringStatus.trim().uppercase()
    if (normalizedStatus != "ASSESSED") {
        val diagnostic =
            if (normalizedStatus == "IMPOSSIBLE_TO_ASSESS") {
                "$fieldName is legacy-unassessable; canonical value remains UNSET"
            } else {
                "$fieldName was not assessed"
            }
        return LegacyAxisProjection(
            assessment = AxisAssessment(valueCode = null, origin = ValueOrigin.UNSET),
            diagnostics = listOf(diagnostic),
        )
    }

    val integral = value.toInt()
    val mapped = if (value == integral.toFloat()) mapping[integral] else null
    if (mapped == null) {
        return LegacyAxisProjection(
            assessment = AxisAssessment(valueCode = null, origin = ValueOrigin.UNSET),
            diagnostics = listOf("Unknown legacy $fieldName value $value; preserved without coercion"),
        )
    }

    return LegacyAxisProjection(
        assessment =
            AxisAssessment(
                valueCode = mapped,
                origin = ValueOrigin.DERIVED,
                provenance =
                    ValueProvenance(
                        sourceType = source.sourceType.name,
                        sourceId = source.sourceId,
                        fieldName = fieldName,
                        rawValue = value.toString(),
                    ),
            ),
        diagnostics = emptyList(),
    )
}
