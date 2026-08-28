@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.orientation

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

@JsExport
enum class ManagedSubjectType {
    ORIENTATION,
    ASPECT,
}

@JsExport
enum class OrientationKind {
    MAIN_BEACON,
    MAIN_BEACON_GROUP,
    GOAL,
    DIRECTION,
    MILESTONE,
    ONGOING_STANDARD,
    OPPORTUNITY,
    DAY_THEME,
    ARC_QUEST,
}

@JsExport
enum class OrientationLifecycle {
    EXPLORING,
    READY,
    ACTIVE,
    PAUSED,
    REALIZED,
    DROPPED,
}

@JsExport
enum class OrientationAxis {
    IMPORTANCE,
    IMPACT,
    BREADTH,
    EXPECTED_SPAN,
    TARGET_WINDOW,
    ATTENTION_TIER,
    COMMITMENT,
    CONFIDENCE,
}

@JsExport
enum class ValueOrigin {
    EXPLICIT,
    INHERITED,
    DERIVED,
    UNSET,
    NOT_APPLICABLE,
}

@JsExport
enum class ImportanceValue { LOW, MEDIUM, HIGH, CRITICAL }

@JsExport
enum class ImpactValue { TINY, SMALL, MEDIUM, LARGE, TRANSFORMATIVE }

@JsExport
enum class BreadthValue { LOCAL, AREA, SYSTEM, MULTI_SYSTEM, LIFE_WIDE }

@JsExport
enum class ExpectedSpanValue { INSTANT, DAYS, WEEKS, MONTHS, YEAR_PLUS, ONGOING }

@JsExport
enum class TargetWindowValue { NOW, THIS_WEEK, THIS_MONTH, THIS_QUARTER, THIS_YEAR, LATER, SOMEDAY }

@JsExport
enum class AttentionTierValue { P3_LATER, P2_NEXT, P1_ACTIVE, P0_NOW }

@JsExport
enum class CommitmentValue { IDEA, CANDIDATE, INTENDED, COMMITTED, OBLIGATION }

@JsExport
enum class ConfidenceValue { SPECULATIVE, POSSIBLE, LIKELY, CONFIDENT, CERTAIN }

/** Raw code plus a known value. rawCode is retained when known is null. */
@JsExport
data class OrientationKindResolution(
    val rawCode: String,
    val known: OrientationKind?,
)

/** Raw code plus a known value. rawCode is retained when known is null. */
@JsExport
data class OrientationLifecycleResolution(
    val rawCode: String,
    val known: OrientationLifecycle?,
)

@JsExport
data class ValueProvenance(
    val sourceType: String,
    val sourceId: String,
    val fieldName: String,
    val rawValue: String?,
)

/**
 * Axis value uses a string code intentionally: unknown future values can cross
 * compatibility boundaries without being coerced to a known enum.
 */
@JsExport
data class AxisAssessment(
    val valueCode: String?,
    val origin: ValueOrigin,
    val provenance: ValueProvenance? = null,
)

@JsExport
data class OrientationAssessment(
    val importance: AxisAssessment,
    val impact: AxisAssessment,
    val breadth: AxisAssessment,
    val expectedSpan: AxisAssessment,
    val targetWindow: AxisAssessment,
    val attentionTier: AxisAssessment,
    val commitment: AxisAssessment,
    val confidence: AxisAssessment,
) {
    fun valueFor(axis: OrientationAxis): AxisAssessment =
        when (axis) {
            OrientationAxis.IMPORTANCE -> importance
            OrientationAxis.IMPACT -> impact
            OrientationAxis.BREADTH -> breadth
            OrientationAxis.EXPECTED_SPAN -> expectedSpan
            OrientationAxis.TARGET_WINDOW -> targetWindow
            OrientationAxis.ATTENTION_TIER -> attentionTier
            OrientationAxis.COMMITMENT -> commitment
            OrientationAxis.CONFIDENCE -> confidence
        }
}

@JsExport
data class ManagedSubject(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val subjectType: ManagedSubjectType,
    val title: String,
    val description: String?,
) : SyncEntityMeta

@JsExport
data class OrientationNode(
    val subjectId: String,
    val kind: OrientationKind,
    val lifecycle: OrientationLifecycle?,
    val lifecycleOrigin: ValueOrigin,
    val assessment: OrientationAssessment,
)

@JsExport
data class AspectNode(
    val subjectId: String,
    val parentAspectId: String?,
    val order: Long,
    val archived: Boolean,
)

@JsExport
enum class LegacyOrientationSourceType {
    MAIN_BEACON,
    MAIN_BEACON_GROUP,
    GOAL,
    CONTEXT,
    DIRECTION,
    THEME_DEFINITION,
    ARC_QUEST,
}

@JsExport
data class LegacySubjectRef(
    val sourceType: LegacyOrientationSourceType,
    val sourceId: String,
)

@JsExport
enum class LegacySubjectMappingState {
    PROJECTED,
    MATERIALIZED,
    CUT_OVER,
    QUARANTINED,
}

@JsExport
data class LegacySubjectMapping(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val source: LegacySubjectRef,
    val subjectId: String,
    val migrationVersion: Int,
    val state: LegacySubjectMappingState,
) : SyncEntityMeta

@JsExport
enum class AssessmentRevisionSource {
    USER,
    MIGRATION,
    IMPORT,
    AUTOMATION,
}

@JsExport
enum class HistoricalAssessmentMode {
    CURRENT_ASSESSMENT,
    AS_OF_ACTIVITY_TIME,
    AS_OF_PERIOD_END,
}

@JsExport
data class OrientationAssessmentRevision(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val orientationId: String,
    val effectiveFrom: Long,
    val recordedAt: Long,
    val source: AssessmentRevisionSource,
    val reason: String?,
    val assessment: OrientationAssessment,
) : SyncEntityMeta

@JsExport
data class CurrentOrientationAssessment(
    val orientationId: String,
    val revisionId: String,
    val assessment: OrientationAssessment,
)

/** Read-only compatibility result. It is never persistence authority. */
@JsExport
data class EffectiveOrientation(
    val subject: ManagedSubject,
    val orientation: OrientationNode,
    val source: LegacySubjectRef,
    val preservedSpecializedFields: List<String>,
    val diagnostics: List<String>,
)

fun unsetAxis(): AxisAssessment = AxisAssessment(valueCode = null, origin = ValueOrigin.UNSET)

fun notApplicableAxis(): AxisAssessment = AxisAssessment(valueCode = null, origin = ValueOrigin.NOT_APPLICABLE)

fun emptyApplicableAssessment(): OrientationAssessment =
    OrientationAssessment(
        importance = unsetAxis(),
        impact = unsetAxis(),
        breadth = unsetAxis(),
        expectedSpan = unsetAxis(),
        targetWindow = unsetAxis(),
        attentionTier = unsetAxis(),
        commitment = unsetAxis(),
        confidence = unsetAxis(),
    )

const val ORIENTATION_MODEL_VERSION: Int = 1
