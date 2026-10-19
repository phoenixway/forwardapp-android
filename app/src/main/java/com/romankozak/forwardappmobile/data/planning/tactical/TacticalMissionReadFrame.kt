package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

/** Immutable read frame: no query or JSON validation is performed in a per-row Room transaction. */
internal data class TacticalMissionReadFrame(
    val snapshot: TacticalMissionOrientationResolutionSnapshot,
    val commitments: List<PlanningCommitment>,
    val cycleIds: Set<String>,
    val assessments: List<OrientationAssessmentEntity>,
    val revisions: List<OrientationAssessmentRevisionEntity>,
) {
    companion object {
        /** Caller owns the one coherent read transaction. Assessments are loaded once, only for legacy targets. */
        suspend fun load(db: AppDatabase): TacticalMissionReadFrame {
            val dao = db.orientationDao()
            val snapshot = TacticalMissionOrientationResolutionSnapshot(
                db.tacticalMissionDao().getAllMissionsSync(), db.workspaceBacklogEntryDao().getAll(),
                db.arcQuestDao().getAllSync(), dao.getAllLegacyMappings(), dao.getAllManagedSubjects(), dao.getAllOrientations(),
            )
            val commitments = db.planningDao().getAllCommitments().map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
            val ownedIds = commitments.filter { !it.isDeleted && it.role == PlanningRoleCode.MISSION &&
                it.provenance.sourceType == TACTICAL_MISSION_PROVENANCE_SOURCE }.mapTo(hashSetOf()) { it.provenance.sourceId }
            val legacyIds = snapshot.missions.filter { !it.isDeleted && it.id.toString() !in ownedIds }.mapNotNull {
                (resolveTacticalMissionOrientation(it, snapshot) as? TacticalMissionOrientationResolution.Resolved)?.orientationId
            }.distinct()
            val assessments = legacyIds.chunked(500).flatMap { dao.getAssessmentsFor(it) }
            return TacticalMissionReadFrame(snapshot, commitments,
                db.planningDao().getAllScopes().filter { it.kind == "TACTICAL_CYCLE" }.mapTo(hashSetOf()) { it.id } +
                    db.tacticalIterationDao().getAllSync().map { it.id },
                assessments, assessments.map { it.revisionId }.distinct().chunked(500).flatMap { dao.getAssessmentRevisionsByIds(it) })
        }
    }

    fun project(orientationRepository: CanonicalOrientationRepository): List<TacticalMission> {
        val subjects = snapshot.subjects.associateBy { it.id }
        val nodes = snapshot.orientations.groupBy { it.subjectId }
        val currents = assessments.groupBy { it.orientationId }
        val revisionById = revisions.associateBy { it.id }
        val validated = hashSetOf<String>()
        fun requireAggregate(id: String): ManagedSubjectEntity {
            if (validated.add(id)) {
                val current = currents[id].orEmpty()
                orientationRepository.requireCompleteActiveOrientationAggregate(id, subjects[id], nodes[id].orEmpty(),
                    current, current.mapNotNull { revisionById[it.revisionId] })
            }
            return requireNotNull(subjects[id])
        }
        val live = commitments.filter { !it.isDeleted && it.role == PlanningRoleCode.MISSION &&
            it.provenance.sourceType == TACTICAL_MISSION_PROVENANCE_SOURCE }.groupBy {
            requireNotNull(it.provenance.sourceId?.toLongOrNull()) { "Canonical Tactical MISSION commitment has invalid source id" }
        }
        require(live.keys.all { id -> snapshot.missions.any { it.id == id } }) {
            "Canonical Tactical MISSION commitment references missing compatibility row"
        }
        return snapshot.missions.mapNotNull { row ->
            val records = live[row.id].orEmpty()
            require(records.size <= 1) { "TacticalMission ${row.id} has multiple live canonical MISSION commitments" }
            if (records.isNotEmpty()) {
                val commitment = records.single()
                projectCanonicalTacticalMissionCompatibility(row, commitment, requireNotNull(subjects[commitment.orientationId]))
            } else if (row.isDeleted) null
            else {
                val alias = row.iterationId == null ||
                    (row.weekKey.isNotBlank() && row.iterationId == row.weekKey && row.iterationId !in cycleIds)
                val resolution = resolveTacticalMissionOrientation(row, snapshot)
                val subject = if (row.sourceType == MissionSourceType.MANUAL && alias) {
                    require(resolution is TacticalMissionOrientationResolution.Resolved &&
                        resolution.path == TacticalMissionOrientationResolutionPath.LEGACY_MANUAL_MAPPING) {
                        "Standalone MANUAL TacticalMission requires a verified canonical INTENT mapping"
                    }
                    requireAggregate(resolution.orientationId)
                } else {
                    require(row.sourceType == MissionSourceType.CONTEXT_BACKLOG_ITEM || row.sourceType == MissionSourceType.SLOT_BACKLOG_ITEM) {
                        "Eligible TacticalMission ${row.id} is missing canonical MISSION participation"
                    }
                    when (resolution) {
                        is TacticalMissionOrientationResolution.NeedsAdoption -> {
                            require(resolution.code == TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET) {
                                "TacticalMission ${row.id} has unsupported runtime deferral ${resolution.code}"
                            }; null
                        }
                        is TacticalMissionOrientationResolution.Resolved -> {
                            require(row.sourceType == MissionSourceType.CONTEXT_BACKLOG_ITEM && alias &&
                                resolution.path == TacticalMissionOrientationResolutionPath.BACKLOG_ORIENTATION) {
                                "Eligible TacticalMission ${row.id} resolved to Orientation but canonical MISSION participation is missing"
                            }
                            requireAggregate(resolution.orientationId)
                        }
                        else -> error("TacticalMission ${row.id} semantic prerequisite/integrity failure: $resolution")
                    }
                }
                subject?.let { row.copy(title = it.title, description = it.description) } ?: row
            }
        }.sortedWith(compareBy<TacticalMission> { it.orderInWeek }.thenByDescending { it.deadline }.thenBy { it.id })
    }
}
