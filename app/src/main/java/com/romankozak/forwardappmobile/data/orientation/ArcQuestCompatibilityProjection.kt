package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.shared.core.domain.orientation.mapLegacyArcLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment

sealed interface ArcQuestCompatibilityProjection {
    data class ManualOrientation(val value: EffectiveOrientation) : ArcQuestCompatibilityProjection

    data class ExistingSourcePlacement(
        val arcQuestId: String,
        val arcKey: String,
        val sourceType: ArcQuestSourceType?,
        val sourceId: String?,
        val linkedContextId: String?,
        val linkedMissionId: Long?,
        val localStatus: String,
        val order: Long,
        val diagnostics: List<String>,
    ) : ArcQuestCompatibilityProjection
}

fun ArcQuestEntity.toCompatibilityProjection(
    resolver: LegacySubjectIdResolver,
): ArcQuestCompatibilityProjection {
    val parsedType = runCatching { ArcQuestSourceType.valueOf(sourceType) }.getOrNull()
    if (parsedType != ArcQuestSourceType.MANUAL) {
        return ArcQuestCompatibilityProjection.ExistingSourcePlacement(
            arcQuestId = id,
            arcKey = arcKey,
            sourceType = parsedType,
            sourceId = sourceId,
            linkedContextId = linkedContextId,
            linkedMissionId = linkedMissionId,
            localStatus = status,
            order = order,
            diagnostics =
                if (parsedType == null) {
                    listOf("Unknown sourceType '$sourceType' retained as diagnostic; no Orientation created")
                } else {
                    emptyList()
                },
        )
    }

    val source = LegacySubjectRef(LegacyOrientationSourceType.ARC_QUEST, id)
    val subjectId = resolver.resolve(source)
    val lifecycle = mapLegacyArcLifecycle(status)
    return ArcQuestCompatibilityProjection.ManualOrientation(
        EffectiveOrientation(
            subject =
                ManagedSubject(
                    id = subjectId,
                    createdAt = createdAt,
                    updatedAt = updatedAt ?: createdAt,
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
                    kind = OrientationKind.ARC_QUEST,
                    lifecycle = lifecycle,
                    lifecycleOrigin = if (lifecycle == null) ValueOrigin.UNSET else ValueOrigin.DERIVED,
                    assessment = emptyApplicableAssessment(),
                ),
            source = source,
            preservedSpecializedFields = listOf("arcKey", "linkedContextId", "linkedMissionId", "status", "order"),
            diagnostics = emptyList(),
        ),
    )
}
