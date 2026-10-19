package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import javax.inject.Inject
import javax.inject.Singleton

internal enum class TacticalMissionOrientationResolutionCode {
    MANUAL_ADOPTION_REQUIRED,
    NON_ORIENTATION_BACKLOG_TARGET,
    SOURCE_MAPPING_REQUIRED,
    PREVIOUS_MISSION_UNRESOLVED,
    MISSING_SOURCE_REFERENCE,
    MISSING_SOURCE_RECORD,
    SOURCE_OWNER_MISMATCH,
    SOURCE_TYPE_MISMATCH,
    DUPLICATE_SOURCE_RECORD,
    MAPPING_CONFLICT,
    MAPPING_NOT_CUT_OVER,
    MISSING_ORIENTATION_TARGET,
    TOMBSTONED_ORIENTATION_TARGET,
    INVALID_ORIENTATION_TARGET,
    RESOLUTION_CYCLE,
}

internal enum class TacticalMissionOrientationResolutionPath {
    LEGACY_MANUAL_MAPPING,
    BACKLOG_ORIENTATION,
    ARC_SOURCE,
    PREVIOUS_MISSION,
}

internal sealed interface TacticalMissionOrientationResolution {
    data class Resolved(
        val orientationId: String,
        val path: TacticalMissionOrientationResolutionPath,
    ) : TacticalMissionOrientationResolution

    data class NeedsAdoption(
        val code: TacticalMissionOrientationResolutionCode,
        val sourceId: String,
        val deterministicSubjectId: String? = null,
        val detail: String,
    ) : TacticalMissionOrientationResolution

    data class SemanticPrerequisite(
        val code: TacticalMissionOrientationResolutionCode,
        val sourceId: String,
        val detail: String,
    ) : TacticalMissionOrientationResolution

    data class IntegrityFailure(
        val code: TacticalMissionOrientationResolutionCode,
        val sourceId: String,
        val detail: String,
    ) : TacticalMissionOrientationResolution
}

internal data class TacticalMissionOrientationResolutionSnapshot(
    val missions: List<TacticalMission>,
    val backlogEntries: List<WorkspaceBacklogEntryEntity>,
    val arcQuests: List<ArcQuestEntity>,
    val mappings: List<LegacySubjectMappingEntity>,
    val subjects: List<ManagedSubjectEntity>,
    val orientations: List<OrientationEntity>,
)

/**
 * Read-only semantic target resolver used before V3.5 activation.
 *
 * It never creates a PlanningCommitment or infers semantic identity from a
 * Workspace, Context, copied title, or legacy mission id. Eligible legacy
 * MANUAL missions expose a deterministic INTENT-adoption boundary; writes are
 * owned by [LegacyManualTacticalMissionIntentAdopter], never by this resolver.
 */
@Singleton
internal class TacticalMissionOrientationResolver
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun resolve(mission: TacticalMission): TacticalMissionOrientationResolution =
            database.withTransaction {
                resolveTacticalMissionOrientation(
                    mission = mission,
                    snapshot =
                        TacticalMissionOrientationResolutionSnapshot(
                            missions = database.tacticalMissionDao().getAllMissionsSync(),
                            backlogEntries = database.workspaceBacklogEntryDao().getAll(),
                            arcQuests = database.arcQuestDao().getAllSync(),
                            mappings = database.orientationDao().getAllLegacyMappings(),
                            subjects = database.orientationDao().getAllManagedSubjects(),
                            orientations = database.orientationDao().getAllOrientations(),
                        ),
                )
            }
    }

internal fun legacyManualTacticalMissionSubjectId(missionId: Long): String {
    require(missionId > 0L) { "Legacy TacticalMission id must be positive" }
    return LegacySubjectUuid.resolve(
        LegacySubjectRef(
            sourceType = LegacyOrientationSourceType.TACTICAL_MISSION,
            sourceId = missionId.toString(),
        ),
    )
}

internal fun resolveTacticalMissionOrientation(
    mission: TacticalMission,
    snapshot: TacticalMissionOrientationResolutionSnapshot,
): TacticalMissionOrientationResolution =
    TacticalMissionOrientationSnapshotResolver(snapshot).resolve(mission, emptySet())

private class TacticalMissionOrientationSnapshotResolver(
    private val snapshot: TacticalMissionOrientationResolutionSnapshot,
) {
    fun resolve(
        mission: TacticalMission,
        visitedMissionIds: Set<Long>,
    ): TacticalMissionOrientationResolution {
        if (mission.id <= 0L) {
            return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_REFERENCE,
                mission.id.toString(),
                "Persisted TacticalMission must have a positive id",
            )
        }
        if (mission.id in visitedMissionIds) {
            return failure(
                TacticalMissionOrientationResolutionCode.RESOLUTION_CYCLE,
                mission.id.toString(),
                "TacticalMission source lineage contains a cycle",
            )
        }
        val visited = visitedMissionIds + mission.id
        return when (mission.sourceType) {
            MissionSourceType.MANUAL -> resolveManual(mission)
            MissionSourceType.CONTEXT_BACKLOG_ITEM -> resolveBacklog(mission, requireSlot = false)
            MissionSourceType.SLOT_BACKLOG_ITEM -> resolveBacklog(mission, requireSlot = true)
            MissionSourceType.ARC_QUEST -> resolveArcQuest(mission, visited)
            MissionSourceType.PREVIOUS_WEEK -> resolvePrevious(mission, visited)
        }
    }

    private fun resolveManual(mission: TacticalMission): TacticalMissionOrientationResolution {
        val sourceId = mission.id.toString()
        val expectedSubjectId = legacyManualTacticalMissionSubjectId(mission.id)
        val mappings =
            snapshot.mappings.filter {
                it.sourceType == LegacyOrientationSourceType.TACTICAL_MISSION.name &&
                    it.sourceId == sourceId
            }
        if (mappings.isEmpty()) {
            return TacticalMissionOrientationResolution.NeedsAdoption(
                code = TacticalMissionOrientationResolutionCode.MANUAL_ADOPTION_REQUIRED,
                sourceId = sourceId,
                deterministicSubjectId = expectedSubjectId,
                detail = "Legacy MANUAL mission is eligible for deterministic canonical INTENT adoption",
            )
        }
        if (mappings.size != 1) {
            return failure(
                TacticalMissionOrientationResolutionCode.DUPLICATE_SOURCE_RECORD,
                sourceId,
                "Legacy MANUAL mission has ${mappings.size} source mappings",
            )
        }
        val mapping = mappings.single()
        if (mapping.subjectId != expectedSubjectId) {
            return failure(
                TacticalMissionOrientationResolutionCode.MAPPING_CONFLICT,
                sourceId,
                "Legacy MANUAL mapping targets ${mapping.subjectId}, expected $expectedSubjectId",
            )
        }
        return resolveMappedTarget(
            mapping = mapping,
            allowDeleted = mission.isDeleted,
            path = TacticalMissionOrientationResolutionPath.LEGACY_MANUAL_MAPPING,
            requiredKind = OrientationKind.INTENT,
        )
    }

    private fun resolveBacklog(
        mission: TacticalMission,
        requireSlot: Boolean,
    ): TacticalMissionOrientationResolution {
        val sourceId = mission.sourceBacklogItemId?.takeIf(String::isNotBlank)
            ?: return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_REFERENCE,
                mission.id.toString(),
                "Backlog-sourced TacticalMission has no source backlog item id",
            )
        val entries = snapshot.backlogEntries.filter { it.id == sourceId }
        if (entries.size != 1) {
            return failure(
                if (entries.isEmpty()) {
                    TacticalMissionOrientationResolutionCode.MISSING_SOURCE_RECORD
                } else {
                    TacticalMissionOrientationResolutionCode.DUPLICATE_SOURCE_RECORD
                },
                sourceId,
                "Backlog source resolves to ${entries.size} canonical entries",
            )
        }
        val entry = entries.single()
        val ownerId = mission.sourceContextId?.takeIf(String::isNotBlank)
            ?: return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_REFERENCE,
                sourceId,
                "Backlog-sourced TacticalMission has no source owner",
            )
        if (entry.workspaceId != ownerId) {
            return failure(
                TacticalMissionOrientationResolutionCode.SOURCE_OWNER_MISMATCH,
                sourceId,
                "Backlog source owner ${entry.workspaceId} differs from mission source owner $ownerId",
            )
        }
        if (requireSlot && mission.activitySlotContextId != ownerId) {
            return failure(
                TacticalMissionOrientationResolutionCode.SOURCE_OWNER_MISMATCH,
                sourceId,
                "Slot backlog mission does not identify its source owner as the activity slot",
            )
        }
        if (!requireSlot && mission.activitySlotContextId != null) {
            return failure(
                TacticalMissionOrientationResolutionCode.SOURCE_TYPE_MISMATCH,
                sourceId,
                "Context backlog mission unexpectedly carries an activity-slot owner",
            )
        }
        if (!mission.isDeleted && entry.isDeleted) {
            return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_RECORD,
                sourceId,
                "Live TacticalMission references a tombstoned backlog entry",
            )
        }
        if (entry.targetKind != WorkspaceBacklogTargetKind.ORIENTATION.name) {
            return TacticalMissionOrientationResolution.NeedsAdoption(
                code = TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET,
                sourceId = sourceId,
                detail = "Backlog target ${entry.targetKind}:${entry.targetId} is not an Orientation",
            )
        }
        return resolveOrientationTarget(
            orientationId = entry.targetId,
            allowDeleted = mission.isDeleted,
            path = TacticalMissionOrientationResolutionPath.BACKLOG_ORIENTATION,
            sourceId = sourceId,
        )
    }

    private fun resolvePrevious(
        mission: TacticalMission,
        visitedMissionIds: Set<Long>,
    ): TacticalMissionOrientationResolution {
        val previousId = mission.carriedFromMissionId
            ?: return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_REFERENCE,
                mission.id.toString(),
                "PREVIOUS_WEEK mission has no carried-from mission id",
            )
        val previous = snapshot.missions.filter { it.id == previousId }
        if (previous.size != 1) {
            return failure(
                if (previous.isEmpty()) {
                    TacticalMissionOrientationResolutionCode.MISSING_SOURCE_RECORD
                } else {
                    TacticalMissionOrientationResolutionCode.DUPLICATE_SOURCE_RECORD
                },
                previousId.toString(),
                "Previous mission reference resolves to ${previous.size} missions",
            )
        }
        return when (val inherited = resolve(previous.single(), visitedMissionIds)) {
            is TacticalMissionOrientationResolution.Resolved ->
                inherited.copy(path = TacticalMissionOrientationResolutionPath.PREVIOUS_MISSION)
            is TacticalMissionOrientationResolution.NeedsAdoption ->
                TacticalMissionOrientationResolution.NeedsAdoption(
                    code = TacticalMissionOrientationResolutionCode.PREVIOUS_MISSION_UNRESOLVED,
                    sourceId = previousId.toString(),
                    deterministicSubjectId = inherited.deterministicSubjectId,
                    detail = "Previous mission is not yet adopted: ${inherited.detail}",
                )
            else -> inherited
        }
    }

    private fun resolveArcQuest(
        mission: TacticalMission,
        visitedMissionIds: Set<Long>,
    ): TacticalMissionOrientationResolution {
        val arcQuestId = mission.sourceArcQuestId?.takeIf(String::isNotBlank)
            ?: return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_REFERENCE,
                mission.id.toString(),
                "ARC_QUEST mission has no ArcQuest id",
            )
        val quests = snapshot.arcQuests.filter { it.id == arcQuestId }
        if (quests.size != 1) {
            return failure(
                if (quests.isEmpty()) {
                    TacticalMissionOrientationResolutionCode.MISSING_SOURCE_RECORD
                } else {
                    TacticalMissionOrientationResolutionCode.DUPLICATE_SOURCE_RECORD
                },
                arcQuestId,
                "ArcQuest source resolves to ${quests.size} rows",
            )
        }
        val quest = quests.single()
        if (!mission.isDeleted && quest.isDeleted) {
            return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_RECORD,
                arcQuestId,
                "Live TacticalMission references a tombstoned ArcQuest",
            )
        }
        val sourceType = runCatching { ArcQuestSourceType.valueOf(quest.sourceType) }.getOrNull()
            ?: return failure(
                TacticalMissionOrientationResolutionCode.SOURCE_TYPE_MISMATCH,
                arcQuestId,
                "ArcQuest has unknown source type ${quest.sourceType}",
            )
        if (sourceType == ArcQuestSourceType.MANUAL) {
            return resolveLegacySource(
                sourceType = LegacyOrientationSourceType.ARC_QUEST,
                sourceId = arcQuestId,
                mission = mission,
                requiredKind = OrientationKind.QUEST,
            )
        }
        val sourceId = quest.sourceId?.takeIf(String::isNotBlank)
            ?: return failure(
                TacticalMissionOrientationResolutionCode.MISSING_SOURCE_REFERENCE,
                arcQuestId,
                "Source-backed ArcQuest has no source id",
            )
        return when (sourceType) {
            ArcQuestSourceType.MANUAL -> error("Handled above")
            ArcQuestSourceType.ORIENTATION -> resolveOrientationTarget(
                sourceId, quest.isDeleted, TacticalMissionOrientationResolutionPath.ARC_SOURCE, sourceId,
            )
            ArcQuestSourceType.MISSION -> {
                val sourceMissionId = sourceId.toLongOrNull()
                    ?: return failure(
                        TacticalMissionOrientationResolutionCode.SOURCE_TYPE_MISMATCH,
                        sourceId,
                        "ArcQuest mission source id is not numeric",
                    )
                val sourceMissions = snapshot.missions.filter { it.id == sourceMissionId }
                if (sourceMissions.size != 1) {
                    failure(
                        if (sourceMissions.isEmpty()) {
                            TacticalMissionOrientationResolutionCode.MISSING_SOURCE_RECORD
                        } else {
                            TacticalMissionOrientationResolutionCode.DUPLICATE_SOURCE_RECORD
                        },
                        sourceId,
                        "ArcQuest mission source resolves to ${sourceMissions.size} missions",
                    )
                } else {
                    resolve(sourceMissions.single(), visitedMissionIds)
                }
            }
            ArcQuestSourceType.CONTEXT -> resolveLegacySource(LegacyOrientationSourceType.CONTEXT, sourceId, mission)
            ArcQuestSourceType.BEACON -> resolveLegacySource(LegacyOrientationSourceType.MAIN_BEACON, sourceId, mission)
            ArcQuestSourceType.BEACON_GROUP ->
                resolveLegacySource(LegacyOrientationSourceType.MAIN_BEACON_GROUP, sourceId, mission)
        }
    }

    private fun resolveLegacySource(
        sourceType: LegacyOrientationSourceType,
        sourceId: String,
        mission: TacticalMission,
        requiredKind: OrientationKind? = null,
    ): TacticalMissionOrientationResolution {
        val mappings = snapshot.mappings.filter { it.sourceType == sourceType.name && it.sourceId == sourceId }
        if (mappings.isEmpty()) {
            return TacticalMissionOrientationResolution.NeedsAdoption(
                code = TacticalMissionOrientationResolutionCode.SOURCE_MAPPING_REQUIRED,
                sourceId = sourceId,
                detail = "$sourceType source has no canonical Orientation mapping",
            )
        }
        if (mappings.size != 1) {
            return failure(
                TacticalMissionOrientationResolutionCode.DUPLICATE_SOURCE_RECORD,
                sourceId,
                "$sourceType source has ${mappings.size} mappings",
            )
        }
        return resolveMappedTarget(
            mapping = mappings.single(),
            allowDeleted = mission.isDeleted,
            path = TacticalMissionOrientationResolutionPath.ARC_SOURCE,
            requiredKind = requiredKind,
        )
    }

    private fun resolveMappedTarget(
        mapping: LegacySubjectMappingEntity,
        allowDeleted: Boolean,
        path: TacticalMissionOrientationResolutionPath,
        requiredKind: OrientationKind? = null,
    ): TacticalMissionOrientationResolution {
        if (mapping.state != LegacySubjectMappingState.CUT_OVER.name || mapping.isDeleted) {
            return failure(
                TacticalMissionOrientationResolutionCode.MAPPING_NOT_CUT_OVER,
                mapping.sourceId,
                "Source mapping is not live CUT_OVER",
            )
        }
        return resolveOrientationTarget(mapping.subjectId, allowDeleted, path, mapping.sourceId, requiredKind)
    }

    private fun resolveOrientationTarget(
        orientationId: String,
        allowDeleted: Boolean,
        path: TacticalMissionOrientationResolutionPath,
        sourceId: String,
        requiredKind: OrientationKind? = null,
    ): TacticalMissionOrientationResolution {
        val subjects = snapshot.subjects.filter { it.id == orientationId }
        val orientations = snapshot.orientations.filter { it.subjectId == orientationId }
        if (subjects.size != 1 || orientations.size != 1) {
            return failure(
                TacticalMissionOrientationResolutionCode.MISSING_ORIENTATION_TARGET,
                sourceId,
                "Canonical target $orientationId is incomplete or duplicated",
            )
        }
        val subject = subjects.single()
        val orientation = orientations.single()
        if (subject.subjectType != ManagedSubjectType.ORIENTATION.name ||
            runCatching { OrientationKind.valueOf(orientation.kind) }.getOrNull() == null
        ) {
            return failure(
                TacticalMissionOrientationResolutionCode.INVALID_ORIENTATION_TARGET,
                sourceId,
                "Canonical target $orientationId is not a known Orientation",
            )
        }
        if (requiredKind != null && orientation.kind != requiredKind.name) {
            return failure(
                TacticalMissionOrientationResolutionCode.INVALID_ORIENTATION_TARGET,
                sourceId,
                "Canonical target $orientationId kind ${orientation.kind} is not ${requiredKind.name}",
            )
        }
        if (!allowDeleted && subject.isDeleted) {
            return failure(
                TacticalMissionOrientationResolutionCode.TOMBSTONED_ORIENTATION_TARGET,
                sourceId,
                "Live TacticalMission targets tombstoned Orientation $orientationId",
            )
        }
        return TacticalMissionOrientationResolution.Resolved(orientationId, path)
    }

    private fun failure(
        code: TacticalMissionOrientationResolutionCode,
        sourceId: String,
        detail: String,
    ): TacticalMissionOrientationResolution.IntegrityFailure =
        TacticalMissionOrientationResolution.IntegrityFailure(code, sourceId, detail)
}
