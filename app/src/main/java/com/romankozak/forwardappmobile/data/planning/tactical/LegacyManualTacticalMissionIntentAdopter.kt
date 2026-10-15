package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.toCanonicalRows
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import javax.inject.Inject
import javax.inject.Singleton

internal const val TACTICAL_MISSION_INTENT_ADOPTION_VERSION = 1
private const val TACTICAL_MISSION_INTENT_ADOPTION_REASON =
    "Legacy MANUAL TacticalMission INTENT adoption v1"

internal sealed interface LegacyManualIntentAdoptionResult {
    val orientationId: String

    data class Created(
        override val orientationId: String,
    ) : LegacyManualIntentAdoptionResult

    data class AlreadyAdopted(
        override val orientationId: String,
    ) : LegacyManualIntentAdoptionResult
}

/**
 * Finite semantic-migration primitive for legacy MANUAL Tactical missions.
 *
 * This class deliberately does not participate in ordinary mission reads or
 * writes. V3.5 activation may invoke it inside its finite migration boundary;
 * until then specialized Tactical storage remains runtime authority.
 */
@Singleton
internal class LegacyManualTacticalMissionIntentAdopter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val canonicalOrientationRepository: CanonicalOrientationRepository,
    ) {
        private val gson = Gson()

        suspend fun adopt(missionId: Long): LegacyManualIntentAdoptionResult =
            database.withTransaction {
                val mission =
                    requireNotNull(database.tacticalMissionDao().getMissionById(missionId)) {
                        "TacticalMission $missionId does not exist"
                    }
                require(mission.sourceType == MissionSourceType.MANUAL) {
                    "TacticalMission $missionId is not MANUAL"
                }
                require(!mission.isDeleted) {
                    "Deleted TacticalMission $missionId cannot create a live INTENT"
                }
                require(mission.title.isNotBlank()) {
                    "TacticalMission $missionId title must not be blank"
                }

                val rows =
                    mission
                        .toLegacyManualIntentProjection()
                        .toCanonicalRows(gson, TACTICAL_MISSION_INTENT_ADOPTION_VERSION)
                        .let { canonical ->
                            canonical.copy(
                                revision = canonical.revision.copy(reason = TACTICAL_MISSION_INTENT_ADOPTION_REASON),
                                mapping = canonical.mapping.copy(state = LegacySubjectMappingState.CUT_OVER.name),
                            )
                        }
                val dao = database.orientationDao()
                val mappings = dao.getAllLegacyMappings()
                val sourceMappings =
                    mappings.filter {
                        it.sourceType == LegacyOrientationSourceType.TACTICAL_MISSION.name &&
                            it.sourceId == mission.id.toString()
                    }
                require(sourceMappings.size <= 1) {
                    "TacticalMission ${mission.id} has competing canonical source mappings"
                }
                val mapping = sourceMappings.singleOrNull()
                val subjectMappings = mappings.filter { it.subjectId == rows.subject.id }
                val subject = dao.getManagedSubject(rows.subject.id)
                val orientation = dao.getAllOrientations().filter { it.subjectId == rows.subject.id }
                val assessments = dao.getAllAssessments().filter { it.orientationId == rows.subject.id }
                val revisions = dao.getAllAssessmentRevisions().filter { it.orientationId == rows.subject.id }
                val hasTargetState =
                    subject != null || orientation.isNotEmpty() || assessments.isNotEmpty() ||
                        revisions.isNotEmpty() || subjectMappings.isNotEmpty()

                if (mapping == null) {
                    require(!hasTargetState) {
                        "Deterministic TacticalMission INTENT target ${rows.subject.id} is already occupied"
                    }
                    dao.upsertManagedSubjects(listOf(rows.subject))
                    dao.upsertOrientations(listOf(rows.orientation))
                    dao.upsertAssessmentRevisions(listOf(rows.revision))
                    dao.upsertAssessments(listOf(rows.assessment))
                    dao.upsertLegacyMappings(listOf(rows.mapping))
                    return@withTransaction LegacyManualIntentAdoptionResult.Created(rows.subject.id)
                }

                require(mapping.subjectId == rows.subject.id) {
                    "TacticalMission ${mission.id} mapping targets ${mapping.subjectId}, expected ${rows.subject.id}"
                }
                require(
                    mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                        !mapping.isDeleted &&
                        mapping.migrationVersion == TACTICAL_MISSION_INTENT_ADOPTION_VERSION,
                ) {
                    "TacticalMission ${mission.id} mapping is not a live supported CUT_OVER mapping"
                }
                require(subjectMappings.size == 1 && subjectMappings.single().id == mapping.id) {
                    "TacticalMission ${mission.id} canonical subject has competing provenance"
                }
                require(orientation.singleOrNull()?.kind == OrientationKind.INTENT.name) {
                    "TacticalMission ${mission.id} canonical target is not INTENT"
                }
                canonicalOrientationRepository.requireCompleteActiveOrientationAggregate(rows.subject.id)
                LegacyManualIntentAdoptionResult.AlreadyAdopted(rows.subject.id)
            }
    }

internal fun TacticalMission.toLegacyManualIntentProjection(): EffectiveOrientation {
    require(id > 0L) { "Legacy TacticalMission id must be positive" }
    require(sourceType == MissionSourceType.MANUAL) { "Only MANUAL TacticalMission can adopt INTENT" }
    require(!isDeleted) { "Deleted TacticalMission cannot create a live INTENT" }
    val semanticTitle = title.trim()
    require(semanticTitle.isNotEmpty()) { "Legacy TacticalMission title must not be blank" }
    val semanticDescription = description?.trim()?.takeIf(String::isNotEmpty)
    val subjectId = legacyManualTacticalMissionSubjectId(id)
    val canonicalUpdatedAt = maxOf(createdAt, updatedAt ?: createdAt)
    val canonicalVersion = version.coerceAtLeast(1L)
    val assessment = initialOrientationAssessment(OrientationKind.INTENT)
    return EffectiveOrientation(
        subject =
            ManagedSubject(
                id = subjectId,
                createdAt = createdAt,
                updatedAt = canonicalUpdatedAt,
                syncedAt = null,
                isDeleted = false,
                version = canonicalVersion,
                subjectType = ManagedSubjectType.ORIENTATION,
                title = semanticTitle,
                description = semanticDescription,
            ),
        orientation =
            OrientationNode(
                subjectId = subjectId,
                kind = OrientationKind.INTENT,
                lifecycle = null,
                lifecycleOrigin = ValueOrigin.UNSET,
                assessment = assessment,
            ),
        source =
            LegacySubjectRef(
                sourceType = LegacyOrientationSourceType.TACTICAL_MISSION,
                sourceId = id.toString(),
            ),
        preservedSpecializedFields =
            listOf(
                "deadline",
                "status",
                "priority",
                "order",
                "iterationId",
                "missionStreamId",
                "activitySlotContextId",
                "projectWorkspaceId",
                "attachments",
            ),
        diagnostics = emptyList(),
    )
}
