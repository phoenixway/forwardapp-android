package com.romankozak.forwardappmobile.data.planning.strategic

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolution
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolutionCode
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolutionSnapshot
import com.romankozak.forwardappmobile.data.planning.tactical.resolveTacticalMissionOrientation
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import java.util.UUID

internal enum class StrategicParticipantDeferredCode {
    SOURCE_MAPPING_REQUIRED,
}

internal enum class StrategicParticipantIntegrityCode {
    BLANK_ARC_KEY,
    UNKNOWN_SOURCE_TYPE,
    UNKNOWN_STATUS,
    MISSING_SOURCE_REFERENCE,
    MISSING_SOURCE_RECORD,
    DUPLICATE_SOURCE_RECORD,
    MAPPING_NOT_CUT_OVER,
    MAPPING_CONFLICT,
    MISSING_ORIENTATION_TARGET,
    INVALID_ORIENTATION_TARGET,
    TOMBSTONED_ORIENTATION_TARGET,
    DUPLICATE_LIVE_PARTICIPATION,
}

internal sealed interface StrategicParticipantMapping {
    val arcQuestId: String

    data class Eligible(
        override val arcQuestId: String,
        val commitment: PlanningCommitment,
    ) : StrategicParticipantMapping

    data class Deferred(
        override val arcQuestId: String,
        val code: StrategicParticipantDeferredCode,
        val sourceId: String,
    ) : StrategicParticipantMapping

    data class IntegrityFailure(
        override val arcQuestId: String,
        val code: StrategicParticipantIntegrityCode,
        val detail: String,
    ) : StrategicParticipantMapping
}

internal data class StrategicPlanningMapping(
    val scopes: List<PlanningScope>,
    val extensions: List<StrategicPlanningScopeExtension>,
    val participants: List<StrategicParticipantMapping>,
) {
    val commitments: List<PlanningCommitment>
        get() = participants.filterIsInstance<StrategicParticipantMapping.Eligible>().map { it.commitment }

    val deferred: List<StrategicParticipantMapping.Deferred>
        get() = participants.filterIsInstance<StrategicParticipantMapping.Deferred>()

    val integrityFailures: List<StrategicParticipantMapping.IntegrityFailure>
        get() = participants.filterIsInstance<StrategicParticipantMapping.IntegrityFailure>()
}

internal data class StrategicPlanningResolutionSnapshot(
    val mappings: List<LegacySubjectMappingEntity>,
    val subjects: List<ManagedSubjectEntity>,
    val orientations: List<OrientationEntity>,
    val tactical: TacticalMissionOrientationResolutionSnapshot,
)

/** Pure V3.6a mapping. It performs no persistence, adoption, or authority activation. */
internal fun mapCanonicalStrategicPlanning(
    arcQuests: List<ArcQuestEntity>,
    snapshot: StrategicPlanningResolutionSnapshot,
): StrategicPlanningMapping {
    require(arcQuests.map(ArcQuestEntity::id).distinct().size == arcQuests.size) {
        "Strategic mapping contains duplicate ArcQuest ids"
    }
    val grouped = arcQuests.groupBy(ArcQuestEntity::arcKey)
    require(grouped.keys.none(String::isBlank)) { "Strategic mapping contains a blank arcKey" }

    val scopes = grouped.entries.sortedBy { it.key }.map { (arcKey, quests) -> strategicScope(arcKey, quests) }
    val extensions =
        grouped.entries.sortedBy { it.key }.map { (arcKey, quests) -> strategicExtension(arcKey, quests) }
    val initial =
        arcQuests.sortedBy(ArcQuestEntity::id).map { quest ->
            mapStrategicParticipant(quest, snapshot)
        }
    val duplicateIds =
        initial.filterIsInstance<StrategicParticipantMapping.Eligible>()
            .filterNot { it.commitment.isDeleted }
            .groupBy { Triple(it.commitment.scopeId, it.commitment.orientationId, it.commitment.role) }
            .filterValues { it.size > 1 }
            .values.flatten().map { it.arcQuestId }.toSet()
    val participants =
        initial.map { result ->
            if (result.arcQuestId !in duplicateIds) {
                result
            } else {
                StrategicParticipantMapping.IntegrityFailure(
                    arcQuestId = result.arcQuestId,
                    code = StrategicParticipantIntegrityCode.DUPLICATE_LIVE_PARTICIPATION,
                    detail = "Multiple live ArcQuest rows resolve to the same scope/orientation/FOCUS tuple",
                )
            }
        }
    check(participants.size == arcQuests.size) { "Strategic participant accounting is incomplete" }
    return StrategicPlanningMapping(scopes, extensions, participants)
}

internal fun canonicalStrategicArcScopeId(legacyArcKey: String): PlanningScopeId {
    require(legacyArcKey.isNotBlank())
    return PlanningScopeId(
        LegacySubjectUuid.uuidV5(
            STRATEGIC_ARC_SCOPE_NAMESPACE,
            "STRATEGIC_ARC_SCOPE:$legacyArcKey",
        ).toString(),
    )
}

internal fun canonicalStrategicFocusCommitmentId(
    scopeId: PlanningScopeId,
    arcQuestId: String,
): PlanningCommitmentId {
    require(arcQuestId.isNotBlank())
    return PlanningCommitmentId(
        LegacySubjectUuid.uuidV5(
            STRATEGIC_FOCUS_COMMITMENT_NAMESPACE,
            "STRATEGIC_FOCUS_COMMITMENT:${scopeId.value}:$arcQuestId:${PlanningRoleCode.FOCUS.value}",
        ).toString(),
    )
}

private fun strategicScope(arcKey: String, quests: List<ArcQuestEntity>): PlanningScope {
    val metadata = strategicMetadata(quests)
    return PlanningScope(
        id = canonicalStrategicArcScopeId(arcKey),
        kind = PlanningScopeKind.STRATEGIC_ARC,
        lifecycle = PlanningScopeLifecycle.ACTIVE,
        title = null,
        startsAt = null,
        endsAt = null,
        createdAt = metadata.createdAt,
        updatedAt = metadata.updatedAt,
        syncedAt = null,
        version = metadata.version,
        isDeleted = false,
    )
}

private fun strategicExtension(
    arcKey: String,
    quests: List<ArcQuestEntity>,
): StrategicPlanningScopeExtension {
    val metadata = strategicMetadata(quests)
    return StrategicPlanningScopeExtension(
        scopeId = canonicalStrategicArcScopeId(arcKey),
        legacyArcKey = arcKey,
        createdAt = metadata.createdAt,
        updatedAt = metadata.updatedAt,
        syncedAt = null,
        version = metadata.version,
        isDeleted = false,
    )
}

private data class StrategicAggregateMetadata(
    val createdAt: Long,
    val updatedAt: Long?,
    val version: Long,
)

private fun strategicMetadata(quests: List<ArcQuestEntity>): StrategicAggregateMetadata {
    require(quests.isNotEmpty())
    return StrategicAggregateMetadata(
        createdAt = quests.minOf(ArcQuestEntity::createdAt),
        updatedAt = quests.mapNotNull(ArcQuestEntity::updatedAt).maxOrNull(),
        version = quests.maxOf(ArcQuestEntity::version).coerceAtLeast(0L),
    )
}

internal fun mapStrategicParticipant(
    quest: ArcQuestEntity,
    snapshot: StrategicPlanningResolutionSnapshot,
): StrategicParticipantMapping {
    val sourceType = runCatching { ArcQuestSourceType.valueOf(quest.sourceType) }.getOrNull()
        ?: return quest.failure(
            StrategicParticipantIntegrityCode.UNKNOWN_SOURCE_TYPE,
            "Unknown ArcQuest sourceType ${quest.sourceType}",
        )
    val status = runCatching { ArcQuestStatus.valueOf(quest.status) }.getOrNull()
        ?: return quest.failure(
            StrategicParticipantIntegrityCode.UNKNOWN_STATUS,
            "Unknown ArcQuest status ${quest.status}",
        )
    val target = resolveTarget(quest, sourceType, snapshot)
    if (target !is StrategicTargetResolution.Resolved) return target.toParticipant(quest.id)
    return StrategicParticipantMapping.Eligible(
        arcQuestId = quest.id,
        commitment =
            PlanningCommitment(
                id = canonicalStrategicFocusCommitmentId(canonicalStrategicArcScopeId(quest.arcKey), quest.id),
                scopeId = canonicalStrategicArcScopeId(quest.arcKey),
                orientationId = target.orientationId,
                role = PlanningRoleCode.FOCUS,
                order = quest.order,
                priorityLevel = null,
                status =
                    if (quest.isDeleted) {
                        PlanningCommitmentStatus.DROPPED
                    } else {
                        when (status) {
                            ArcQuestStatus.ACTIVE -> PlanningCommitmentStatus.ACTIVE
                            ArcQuestStatus.PAUSED -> PlanningCommitmentStatus.PAUSED
                            ArcQuestStatus.DONE -> PlanningCommitmentStatus.COMPLETED
                        }
                    },
                provenance =
                    PlanningProvenance(
                        kind = PlanningProvenanceKind.MIGRATED,
                        sourceType = ARC_QUEST_PROVENANCE_SOURCE,
                        sourceId = quest.id,
                    ),
                createdAt = quest.createdAt,
                updatedAt = quest.updatedAt,
                syncedAt = quest.syncedAt,
                version = quest.version,
                isDeleted = quest.isDeleted,
            ),
    )
}

private sealed interface StrategicTargetResolution {
    data class Resolved(val orientationId: String) : StrategicTargetResolution
    data class Deferred(val sourceId: String) : StrategicTargetResolution
    data class Failure(val code: StrategicParticipantIntegrityCode, val detail: String) : StrategicTargetResolution
}

private fun resolveTarget(
    quest: ArcQuestEntity,
    sourceType: ArcQuestSourceType,
    snapshot: StrategicPlanningResolutionSnapshot,
): StrategicTargetResolution =
    when (sourceType) {
        ArcQuestSourceType.ORIENTATION -> resolveDirectOrientation(quest, snapshot)
        ArcQuestSourceType.MANUAL ->
            resolveMappedTarget(
                quest = quest,
                sourceType = LegacyOrientationSourceType.ARC_QUEST,
                sourceId = quest.id,
                requiredKind = OrientationKind.QUEST,
                snapshot = snapshot,
                missingIsDeferred = false,
            )
        ArcQuestSourceType.CONTEXT -> resolveSourceBacked(quest, LegacyOrientationSourceType.CONTEXT, snapshot)
        ArcQuestSourceType.BEACON -> resolveSourceBacked(quest, LegacyOrientationSourceType.MAIN_BEACON, snapshot)
        ArcQuestSourceType.BEACON_GROUP ->
            resolveSourceBacked(quest, LegacyOrientationSourceType.MAIN_BEACON_GROUP, snapshot)
        ArcQuestSourceType.MISSION -> resolveMissionTarget(quest, snapshot)
    }

private fun resolveDirectOrientation(
    quest: ArcQuestEntity,
    snapshot: StrategicPlanningResolutionSnapshot,
): StrategicTargetResolution {
    val id = quest.sourceId?.takeIf { it.isNotBlank() }
        ?: return StrategicTargetResolution.Failure(StrategicParticipantIntegrityCode.MISSING_SOURCE_REFERENCE, "Missing Orientation source")
    val subject = snapshot.subjects.singleOrNull { it.id == id }
    val orientation = snapshot.orientations.singleOrNull { it.subjectId == id }
    if (subject?.subjectType != ManagedSubjectType.ORIENTATION.name || orientation == null) {
        return StrategicTargetResolution.Failure(StrategicParticipantIntegrityCode.INVALID_ORIENTATION_TARGET, "Invalid typed Orientation source")
    }
    if (!quest.isDeleted && subject.isDeleted) {
        return StrategicTargetResolution.Failure(StrategicParticipantIntegrityCode.TOMBSTONED_ORIENTATION_TARGET, "Deleted Orientation source")
    }
    return StrategicTargetResolution.Resolved(id)
}

private fun resolveSourceBacked(
    quest: ArcQuestEntity,
    sourceType: LegacyOrientationSourceType,
    snapshot: StrategicPlanningResolutionSnapshot,
): StrategicTargetResolution {
    val sourceId = quest.sourceId?.takeIf(String::isNotBlank)
        ?: return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.MISSING_SOURCE_REFERENCE,
            "$sourceType ArcQuest has no source id",
        )
    return resolveMappedTarget(quest, sourceType, sourceId, null, snapshot, missingIsDeferred = true)
}

private fun resolveMissionTarget(
    quest: ArcQuestEntity,
    snapshot: StrategicPlanningResolutionSnapshot,
): StrategicTargetResolution {
    val sourceId = quest.sourceId?.takeIf(String::isNotBlank)
        ?: return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.MISSING_SOURCE_REFERENCE,
            "MISSION ArcQuest has no source id",
        )
    val missionId = sourceId.toLongOrNull()
        ?: return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.MISSING_SOURCE_REFERENCE,
            "MISSION ArcQuest source id is not numeric",
        )
    val missions = snapshot.tactical.missions.filter { it.id == missionId }
    if (missions.size != 1) {
        return StrategicTargetResolution.Failure(
            if (missions.isEmpty()) {
                StrategicParticipantIntegrityCode.MISSING_SOURCE_RECORD
            } else {
                StrategicParticipantIntegrityCode.DUPLICATE_SOURCE_RECORD
            },
            "MISSION ArcQuest source resolves to ${missions.size} TacticalMission rows",
        )
    }
    return when (val resolution = resolveTacticalMissionOrientation(missions.single(), snapshot.tactical)) {
        is TacticalMissionOrientationResolution.Resolved -> StrategicTargetResolution.Resolved(resolution.orientationId)
        is TacticalMissionOrientationResolution.NeedsAdoption ->
            if (resolution.code in DEFERRED_MISSION_SOURCE_CODES) {
                StrategicTargetResolution.Deferred(sourceId)
            } else {
                StrategicTargetResolution.Failure(
                    StrategicParticipantIntegrityCode.MAPPING_CONFLICT,
                    "MISSION ArcQuest requires missing closed prerequisite: ${resolution.code}",
                )
            }
        is TacticalMissionOrientationResolution.SemanticPrerequisite
        -> StrategicTargetResolution.Deferred(sourceId)
        is TacticalMissionOrientationResolution.IntegrityFailure ->
            StrategicTargetResolution.Failure(
                StrategicParticipantIntegrityCode.MAPPING_CONFLICT,
                "MISSION ArcQuest semantic source is invalid: ${resolution.code}",
            )
    }
}

private fun resolveMappedTarget(
    quest: ArcQuestEntity,
    sourceType: LegacyOrientationSourceType,
    sourceId: String,
    requiredKind: OrientationKind?,
    snapshot: StrategicPlanningResolutionSnapshot,
    missingIsDeferred: Boolean,
): StrategicTargetResolution {
    val mappings = snapshot.mappings.filter { it.sourceType == sourceType.name && it.sourceId == sourceId }
    if (mappings.isEmpty()) {
        return if (missingIsDeferred) {
            StrategicTargetResolution.Deferred(sourceId)
        } else {
            StrategicTargetResolution.Failure(
                StrategicParticipantIntegrityCode.MISSING_SOURCE_RECORD,
                "$sourceType source has no canonical mapping",
            )
        }
    }
    if (mappings.size != 1) {
        return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.DUPLICATE_SOURCE_RECORD,
            "$sourceType source has ${mappings.size} canonical mappings",
        )
    }
    val mapping = mappings.single()
    if (sourceType == LegacyOrientationSourceType.ARC_QUEST) {
        val expected =
            LegacySubjectUuid.resolve(
                com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef(
                    LegacyOrientationSourceType.ARC_QUEST,
                    sourceId,
                ),
            )
        if (mapping.subjectId != expected) {
            return StrategicTargetResolution.Failure(
                StrategicParticipantIntegrityCode.MAPPING_CONFLICT,
                "Manual ArcQuest mapping targets ${mapping.subjectId}, expected $expected",
            )
        }
    }
    // Historical deletion tombstones both the ArcQuest and its CUT_OVER
    // mapping. That identity remains valid for a deleted FOCUS commitment,
    // but must never be used to resurrect a live participant.
    if (
        mapping.state != LegacySubjectMappingState.CUT_OVER.name ||
            (mapping.isDeleted && !quest.isDeleted)
    ) {
        return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.MAPPING_NOT_CUT_OVER,
            "$sourceType source mapping is not live CUT_OVER",
        )
    }
    val subjects = snapshot.subjects.filter { it.id == mapping.subjectId }
    val orientations = snapshot.orientations.filter { it.subjectId == mapping.subjectId }
    if (subjects.size != 1 || orientations.size != 1) {
        return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.MISSING_ORIENTATION_TARGET,
            "Canonical target ${mapping.subjectId} is incomplete or duplicated",
        )
    }
    val subject = subjects.single()
    val orientation = orientations.single()
    if (subject.subjectType != ManagedSubjectType.ORIENTATION.name ||
        runCatching { OrientationKind.valueOf(orientation.kind) }.getOrNull() == null ||
        (requiredKind != null && orientation.kind != requiredKind.name)
    ) {
        return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.INVALID_ORIENTATION_TARGET,
            "Canonical target ${mapping.subjectId} has invalid semantic kind",
        )
    }
    if (!quest.isDeleted && subject.isDeleted) {
        return StrategicTargetResolution.Failure(
            StrategicParticipantIntegrityCode.TOMBSTONED_ORIENTATION_TARGET,
            "Live ArcQuest targets tombstoned Orientation ${mapping.subjectId}",
        )
    }
    return StrategicTargetResolution.Resolved(mapping.subjectId)
}

private fun StrategicTargetResolution.toParticipant(arcQuestId: String): StrategicParticipantMapping =
    when (this) {
        is StrategicTargetResolution.Resolved -> error("Resolved target must materialize a commitment")
        is StrategicTargetResolution.Deferred ->
            StrategicParticipantMapping.Deferred(
                arcQuestId,
                StrategicParticipantDeferredCode.SOURCE_MAPPING_REQUIRED,
                sourceId,
            )
        is StrategicTargetResolution.Failure -> StrategicParticipantMapping.IntegrityFailure(arcQuestId, code, detail)
    }

private fun ArcQuestEntity.failure(
    code: StrategicParticipantIntegrityCode,
    detail: String,
): StrategicParticipantMapping.IntegrityFailure = StrategicParticipantMapping.IntegrityFailure(id, code, detail)

internal const val ARC_QUEST_PROVENANCE_SOURCE = "ARC_QUEST"
private val STRATEGIC_ARC_SCOPE_NAMESPACE = UUID.fromString("0a09e49d-ae0b-5df3-bdc6-c175c8127bd4")
private val STRATEGIC_FOCUS_COMMITMENT_NAMESPACE = UUID.fromString("d6adff29-e530-52ed-9471-8176e7fd30e3")
private val DEFERRED_MISSION_SOURCE_CODES =
    setOf(
        TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET,
        TacticalMissionOrientationResolutionCode.SOURCE_MAPPING_REQUIRED,
        TacticalMissionOrientationResolutionCode.PREVIOUS_MISSION_UNRESOLVED,
    )
