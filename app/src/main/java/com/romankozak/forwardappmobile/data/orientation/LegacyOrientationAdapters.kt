package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.shared.core.domain.orientation.mapLegacyContextLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.orientation.mapLegacyGoalLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.orientation.projectLegacyImpact
import com.romankozak.forwardappmobile.shared.core.domain.orientation.projectLegacyImportance
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.notApplicableAxis

fun interface LegacySubjectIdResolver {
    fun resolve(source: LegacySubjectRef): String
}

fun Goal.toEffectiveOrientation(resolver: LegacySubjectIdResolver): EffectiveOrientation {
    val source = LegacySubjectRef(LegacyOrientationSourceType.GOAL, id)
    val importance = projectLegacyImportance(valueImportance, scoringStatus, source)
    val impact = projectLegacyImpact(valueImpact, scoringStatus, source)
    return effectiveOrientation(
        resolver = resolver,
        source = source,
        kind = OrientationKind.GOAL,
        title = text,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt ?: createdAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
        lifecycle = mapLegacyGoalLifecycle(goalStatus),
        assessment = assessmentWith(importance.assessment, impact.assessment),
        preservedFields =
            listOf(
                "completed",
                "goalStatus",
                "tags",
                "relatedLinks",
                "effort",
                "cost",
                "risk",
                "weights",
                "rawScore",
                "displayScore",
                "relativeSize",
                "parentValueImportance",
                "impactOnParentGoal",
                "timeCost",
                "financialCost",
            ),
        diagnostics = importance.diagnostics + impact.diagnostics,
    )
}

/** Context projection requires an explicit reviewed Orientation kind. */
fun Context.toEffectiveOrientation(
    kind: OrientationKind,
    resolver: LegacySubjectIdResolver,
): EffectiveOrientation {
    val source = LegacySubjectRef(LegacyOrientationSourceType.CONTEXT, id)
    val importance = projectLegacyImportance(valueImportance, scoringStatus, source)
    val impact = projectLegacyImpact(valueImpact, scoringStatus, source)
    return effectiveOrientation(
        resolver = resolver,
        source = source,
        kind = kind,
        title = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt ?: createdAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
        lifecycle = mapLegacyContextLifecycle(contextStatus),
        assessment = assessmentWith(importance.assessment, impact.assessment),
        preservedFields =
            listOf(
                "parentId",
                "tags",
                "relatedLinks",
                "roleCode",
                "contextStatus",
                "contextStatusText",
                "contextLogLevel",
                "effort",
                "cost",
                "risk",
                "weights",
                "rawScore",
                "displayScore",
                "defaultViewModeName",
            ),
        diagnostics = importance.diagnostics + impact.diagnostics + "Context classification was supplied explicitly",
    )
}

fun MainBeacon.toEffectiveOrientation(resolver: LegacySubjectIdResolver): EffectiveOrientation {
    val source = LegacySubjectRef(LegacyOrientationSourceType.MAIN_BEACON, id)
    return effectiveOrientation(
        resolver = resolver,
        source = source,
        kind = OrientationKind.MAIN_BEACON,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        version = 0L,
        isDeleted = false,
        lifecycle = null,
        assessment = emptyApplicableAssessment(),
        preservedFields =
            listOf(
                "whyItMatters",
                "successShape",
                "failureShape",
                "antiGoal",
                "decisionImpact",
                "readinessStatus",
                "blockerText",
                "nextActionText",
                "parentBeaconId",
                "order",
                "isExpanded",
            ),
        diagnostics = listOf("Main Beacon readiness does not infer Orientation lifecycle", "Legacy source has no version/tombstone fields"),
    )
}

fun MainBeaconGroup.toEffectiveOrientation(resolver: LegacySubjectIdResolver): EffectiveOrientation {
    val source = LegacySubjectRef(LegacyOrientationSourceType.MAIN_BEACON_GROUP, id)
    return effectiveOrientation(
        resolver = resolver,
        source = source,
        kind = OrientationKind.MAIN_BEACON_GROUP,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        version = 0L,
        isDeleted = false,
        lifecycle = null,
        assessment = emptyApplicableAssessment(),
        preservedFields = listOf("order", "orderedMemberships"),
        diagnostics = listOf("Group assessment is applicable but currently UNSET", "Legacy source has no version/tombstone fields"),
    )
}

fun DirectionItemEntity.toEffectiveOrientation(resolver: LegacySubjectIdResolver): EffectiveOrientation {
    val source = LegacySubjectRef(LegacyOrientationSourceType.DIRECTION, id)
    val timestamp = updatedAt ?: 0L
    return effectiveOrientation(
        resolver = resolver,
        source = source,
        kind = OrientationKind.DIRECTION,
        title = text,
        description = null,
        createdAt = timestamp,
        updatedAt = timestamp,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
        lifecycle = null,
        assessment = emptyApplicableAssessment(),
        preservedFields = listOf("contextId", "linkedContextId", "itemOrder"),
        diagnostics = if (updatedAt == null) listOf("Direction has no source timestamp; projection uses 0") else emptyList(),
    )
}

fun ThemeDefinitionEntity.toEffectiveOrientation(resolver: LegacySubjectIdResolver): EffectiveOrientation {
    val source = LegacySubjectRef(LegacyOrientationSourceType.THEME_DEFINITION, id)
    val base = emptyApplicableAssessment()
    val assessment =
        base.copy(
            expectedSpan = notApplicableAxis(),
            targetWindow = notApplicableAxis(),
            attentionTier = notApplicableAxis(),
        )
    return effectiveOrientation(
        resolver = resolver,
        source = source,
        kind = OrientationKind.DAY_THEME,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
        lifecycle = null,
        assessment = assessment,
        preservedFields = listOf("colorArgb", "iconKey", "carryForward", "archived"),
        diagnostics = listOf("Archived state remains ThemeDefinition-specific"),
    )
}

private fun assessmentWith(
    importance: AxisAssessment,
    impact: AxisAssessment,
): OrientationAssessment =
    emptyApplicableAssessment().copy(
        importance = importance,
        impact = impact,
    )

private fun effectiveOrientation(
    resolver: LegacySubjectIdResolver,
    source: LegacySubjectRef,
    kind: OrientationKind,
    title: String,
    description: String?,
    createdAt: Long,
    updatedAt: Long,
    syncedAt: Long?,
    version: Long,
    isDeleted: Boolean,
    lifecycle: OrientationLifecycle?,
    assessment: OrientationAssessment,
    preservedFields: List<String>,
    diagnostics: List<String>,
): EffectiveOrientation {
    val subjectId = resolver.resolve(source)
    return EffectiveOrientation(
        subject =
            ManagedSubject(
                id = subjectId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = syncedAt,
                isDeleted = isDeleted,
                version = version,
                subjectType = ManagedSubjectType.ORIENTATION,
                title = title,
                description = description,
            ),
        orientation =
            OrientationNode(
                subjectId = subjectId,
                kind = kind,
                lifecycle = lifecycle,
                lifecycleOrigin = if (lifecycle == null) ValueOrigin.UNSET else ValueOrigin.DERIVED,
                assessment = assessment,
            ),
        source = source,
        preservedSpecializedFields = preservedFields,
        diagnostics = diagnostics,
    )
}
