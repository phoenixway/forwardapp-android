package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V3.5g runtime authority adapter for Tactical MISSION participation.
 *
 * Canonical PlanningCommitment + Orientation own semantic/common planning
 * facts after Tactical authority admission. tactical_missions remains the
 * carrier for Tactical-only fields and compatibility transport projection.
 *
 * The only specialized-only live rows admitted after activation are unresolved
 * CONTEXT_BACKLOG_ITEM / SLOT_BACKLOG_ITEM rows whose backlog target is not an
 * Orientation. Any eligible row missing its canonical commitment fails closed.
 */
@Singleton
class CanonicalTacticalMissionAdapter
    @Inject
    internal constructor(
        private val database: AppDatabase,
        private val authorityActivator: CanonicalTacticalPlanningAuthorityActivator,
        private val orientationResolver: TacticalMissionOrientationResolver,
        private val orientationRepository: CanonicalOrientationRepository,
        private val planningRepository: CanonicalPlanningRepository,
        private val tacticalPlanningAdapter: CanonicalTacticalPlanningAdapter,
    ) {
        fun getAllMissions(): Flow<List<TacticalMission>> =
            flow {
                authorityActivator.requireActivated()
                emitAll(
                    // An atomic writer does not make independently collected table Flows atomic.
                    // Read carriers, participation and identities from the same committed frame.
                    database.invalidationTracker.createFlow(
                        "tactical_missions", "planning_commitments", "managed_subjects",
                        "orientations", "legacy_subject_mappings", "arc_quests", "planning_scopes",
                        "orientation_assessments", "orientation_assessment_revisions", "tactical_iterations", "workspace_backlog_entries",
                    ).map {
                        val frame = com.romankozak.forwardappmobile.StartupTrace.measure("Planning.Mission.readFrame") {
                            database.withTransaction { TacticalMissionReadFrame.load(database) }
                        }
                        com.romankozak.forwardappmobile.StartupTrace.measureSync("Planning.Mission.project") { frame.project(orientationRepository) }
                    }.distinctUntilChanged().flowOn(Dispatchers.Default),
                )
            }

        fun getMissionsForProject(projectId: String): Flow<List<TacticalMission>> =
            getAllMissions().map { missions ->
                missions.filter { it.projectWorkspaceId == projectId }
            }

        fun observeBacklogMissionsForWeek(weekKey: String): Flow<List<TacticalMission>> =
            getAllMissions().map { missions ->
                missions.filter {
                    it.weekKey == weekKey &&
                        !it.sourceBacklogItemId.isNullOrBlank()
                }
            }

        fun observeBacklogMissionIdsForWeek(weekKey: String): Flow<List<String>> =
            observeBacklogMissionsForWeek(weekKey).map { missions ->
                missions.mapNotNull(TacticalMission::sourceBacklogItemId)
            }

        suspend fun getMissionById(missionId: Long): TacticalMission? {
            authorityActivator.requireActivated()
            val row = database.tacticalMissionDao().getMissionById(missionId) ?: return null
            val commitments = missionCommitments(missionId)
            val selected =
                commitments.singleOrNull { !it.isDeleted }
                    ?: commitments.maxByOrNull { it.updatedAt ?: it.createdAt }

            if (selected == null) {
                val standaloneSubject = requireDeferredSpecializedOnly(row)
                return row.takeUnless(TacticalMission::isDeleted)?.let { liveRow ->
                    standaloneSubject?.let { subject ->
                        liveRow.copy(
                            title = subject.title,
                            description = subject.description,
                        )
                    } ?: liveRow
                }
            }
            return projectCanonical(row, selected)
        }

        suspend fun getMissionForBacklogItemInWeek(
            backlogItemId: String,
            weekKey: String,
        ): TacticalMission? {
            authorityActivator.requireActivated()
            val row =
                database.tacticalMissionDao()
                    .getMissionForBacklogItemInWeek(backlogItemId, weekKey)
                    ?: return null
            return requireNotNull(getMissionById(row.id)) {
                "Live backlog TacticalMission ${row.id} disappeared during canonical projection"
            }
        }

        suspend fun insertMission(
            mission: TacticalMission,
            source: com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget? = null,
        ): Long {
            authorityActivator.requireActivated()
            val admittedMission = admitMissionScope(mission)
            return database.withTransaction {
                val insertedId = database.tacticalMissionDao().insertMission(admittedMission)
                val persisted =
                    requireNotNull(database.tacticalMissionDao().getMissionById(insertedId)) {
                        "Inserted TacticalMission $insertedId is missing"
                    }

                val existingCommitments = missionCommitments(insertedId)
                require(existingCommitments.none { !it.isDeleted }) {
                    "TacticalMission $insertedId already has live canonical MISSION participation"
                }

                val orientationId =
                    if (source?.kind == com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind.ORIENTATION) {
                        require(persisted.sourceType == MissionSourceType.MANUAL) { "Canonical source requires explicit native authoring" }
                        orientationRepository.requireCompleteActiveOrientationAggregate(source.id)
                        source.id
                    } else when (persisted.sourceType) {
                        MissionSourceType.MANUAL ->
                            orientationRepository.createOrientation(
                                kind = OrientationKind.INTENT,
                                title = persisted.title,
                                description = persisted.description,
                                now = persisted.updatedAt ?: persisted.createdAt,
                            )

                        MissionSourceType.PREVIOUS_WEEK ->
                            requirePreviousCanonicalOrientation(persisted)

                        else -> resolveExistingOrientationOrDefer(persisted)
                    }

                if (orientationId != null) {
                    planningRepository.saveCommitment(
                        persisted.toCanonicalMissionCommitment(
                            orientationId = orientationId,
                            provenanceKind = persisted.runtimeProvenanceKind(),
                        ),
                    )
                    persisted.carriedFromMissionId?.let { previousId ->
                        val origins = missionCommitments(previousId).filterNot { it.isDeleted }
                        require(origins.size <= 1) { "Ambiguous carried Mission participation" }
                        origins.singleOrNull()?.let { origin ->
                            val target = persisted.toCanonicalMissionCommitment(orientationId, persisted.runtimeProvenanceKind())
                            com.romankozak.forwardappmobile.data.planning.CanonicalPlanningCommitmentRelationRepository(database)
                                .carryRelations(mapOf(origin.id to target.id), persisted.updatedAt ?: persisted.createdAt)
                        }
                    }
                }
                source?.let {
                    com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository(database).attach(
                        com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                            com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.TACTICAL_MISSION,
                            insertedId.toString(),
                        ), it, com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceRole.SOURCE,
                        persisted.updatedAt ?: persisted.createdAt,
                    )
                }
                insertedId
            }
        }

        suspend fun insertMissionWithAutoOrder(
            mission: TacticalMission,
            source: com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget? = null,
        ): Long {
            authorityActivator.requireActivated()
            val admittedMission = admitMissionScope(mission)
            val scopeId = requireNotNull(admittedMission.iterationId)

            return database.withTransaction {
                com.romankozak.forwardappmobile.StartupTrace.mark("Planning.Mission.transaction.entered")
                val topOrder = (database.tacticalMissionDao().minimumCanonicalOrder(scopeId) ?: 0L) - 1L
                insertMission(admittedMission.copy(orderInWeek = topOrder), source)
            }
        }

        suspend fun updateMission(mission: TacticalMission) {
            authorityActivator.requireActivated()
            require(mission.id > 0L) { "Persisted TacticalMission id must be positive" }

            database.withTransaction {
                val existingRow =
                    requireNotNull(database.tacticalMissionDao().getMissionById(mission.id)) {
                        "TacticalMission ${mission.id} does not exist"
                    }
                val commitments = missionCommitments(mission.id)
                val live = commitments.filterNot(PlanningCommitment::isDeleted)

                if (live.isEmpty()) {
                    val standaloneSubject = requireDeferredSpecializedOnly(existingRow)
                    if (standaloneSubject != null) {
                        require(
                            mission.sourceType == existingRow.sourceType &&
                                mission.iterationId == existingRow.iterationId &&
                                mission.weekKey == existingRow.weekKey &&
                                mission.sourceContextId == existingRow.sourceContextId &&
                                mission.sourceBacklogItemId == existingRow.sourceBacklogItemId &&
                                mission.activitySlotContextId == existingRow.activitySlotContextId,
                        ) {
                            "Standalone TacticalMission cannot silently change its source or cycle"
                        }
                        require(mission.title.isNotBlank()) {
                            "Standalone TacticalMission title must not be blank"
                        }

                        orientationRepository.updateOrientationSemantics(
                            subjectId = standaloneSubject.id,
                            title = mission.title.trim(),
                            description = mission.description?.trim()?.takeIf(String::isNotEmpty),
                            now = mission.updatedAt ?: System.currentTimeMillis(),
                        )
                    }
                    database.tacticalMissionDao().updateMission(mission)
                    return@withTransaction
                }

                require(live.size == 1) {
                    "TacticalMission ${mission.id} has ${live.size} live canonical MISSION commitments"
                }
                val current = live.single()
                val targetScopeId =
                    requireTacticalScopeId(
                        mission.iterationId ?: current.scopeId.value,
                    )

                val normalizedTitle = mission.title.trim()
                require(normalizedTitle.isNotEmpty()) {
                    "Eligible TacticalMission ${mission.id} title must not be blank"
                }
                val normalizedDescription =
                    mission.description?.trim()?.takeIf { it.isNotEmpty() }

                val admittedMission =
                    mission.copy(
                        title = normalizedTitle,
                        description = normalizedDescription,
                        iterationId = targetScopeId,
                        // mission_order remains specialized compatibility state.
                        // Canonical weekly ordering is PlanningCommitment.order.
                        order = existingRow.order,
                    )

                orientationRepository.updateOrientationSemantics(
                    subjectId = current.orientationId,
                    title = admittedMission.title,
                    description = admittedMission.description,
                    now = admittedMission.updatedAt ?: System.currentTimeMillis(),
                )

                database.tacticalMissionDao().updateMission(admittedMission)

                val desiredId =
                    canonicalTacticalMissionCommitmentId(
                        targetScopeId,
                        admittedMission.id,
                    )

                if (desiredId == current.id) {
                    planningRepository.saveCommitment(
                        admittedMission.toCanonicalMissionCommitment(
                            orientationId = current.orientationId,
                            provenanceKind = current.provenance.kind,
                        ).copy(
                            createdAt = current.createdAt,
                            provenance = current.provenance,
                        ),
                    )
                } else {
                    planningRepository.tombstoneCommitment(current.id)
                    planningRepository.saveCommitment(
                        admittedMission.toCanonicalMissionCommitment(
                            orientationId = current.orientationId,
                            provenanceKind = current.provenance.kind,
                        ),
                    )
                }
            }
        }

        suspend fun deleteMissionById(missionId: Long) {
            authorityActivator.requireActivated()
            database.withTransaction {
                val row =
                    requireNotNull(database.tacticalMissionDao().getMissionById(missionId)) {
                        "TacticalMission $missionId does not exist"
                    }
                val live = missionCommitments(missionId).filterNot(PlanningCommitment::isDeleted)
                val now = System.currentTimeMillis()
                com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository(database).retireOwner(
                    com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                        com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.TACTICAL_MISSION, missionId.toString()), now)

                if (live.isEmpty()) {
                    requireDeferredSpecializedOnly(row)
                } else {
                    require(live.size == 1) {
                        "TacticalMission $missionId has ${live.size} live canonical MISSION commitments"
                    }
                    planningRepository.tombstoneCommitment(live.single().id, now)
                }

                database.tacticalMissionDao().updateMission(
                    row.copy(
                        isDeleted = true,
                        updatedAt = now,
                        syncedAt = null,
                        version = row.version + 1L,
                    ),
                )
            }
        }

        suspend fun reorderMissions(missions: List<TacticalMission>) {
            authorityActivator.requireActivated()
            missions.forEachIndexed { index, mission ->
                val current =
                    requireNotNull(getMissionById(mission.id)) {
                        "TacticalMission ${mission.id} does not exist"
                    }
                val target = index.toLong()
                if (current.orderInWeek == target) return@forEachIndexed
                updateMission(
                    current.copy(
                        orderInWeek = target,
                        updatedAt = System.currentTimeMillis(),
                        syncedAt = null,
                        version = current.version + 1L,
                    ),
                )
            }
        }


        private suspend fun projectCanonical(
            row: TacticalMission,
            commitment: PlanningCommitment,
            subjectsById: Map<String, ManagedSubjectEntity>? = null,
        ): TacticalMission {
            val subject =
                subjectsById?.get(commitment.orientationId)
                    ?: database.orientationDao().getManagedSubject(commitment.orientationId)
            val canonicalSubject =
                requireNotNull(subject) {
                    "Canonical Tactical MISSION ${commitment.id.value} is missing Orientation subject"
                }

            return projectCanonicalTacticalMissionCompatibility(
                row = row,
                commitment = commitment,
                canonicalSubject = canonicalSubject,
            )
        }

        private suspend fun admitMissionScope(mission: TacticalMission): TacticalMission {
            val scopeId =
                mission.iterationId?.let { requireTacticalScopeId(it) }
                    ?: requireNotNull(tacticalPlanningAdapter.getCurrentIteration()?.id) {
                        "Tactical mission authoring requires a current canonical Tactical cycle"
                    }

            return mission.copy(iterationId = scopeId)
        }

        private suspend fun requireTacticalScopeId(scopeId: String): String {
            requireNotNull(tacticalPlanningAdapter.getById(scopeId)) {
                "Tactical mission target scope $scopeId is not a canonical Tactical cycle"
            }
            return scopeId
        }

        private suspend fun missionCommitments(missionId: Long): List<PlanningCommitment> =
            database.planningDao()
                .getAllCommitments()
                .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                .filter {
                    it.role == PlanningRoleCode.MISSION &&
                        it.provenance.sourceType == TACTICAL_MISSION_PROVENANCE_SOURCE &&
                        it.provenance.sourceId == missionId.toString()
                }

        private suspend fun requireDeferredSpecializedOnly(
            mission: TacticalMission,
        ): ManagedSubjectEntity? {
            val legacyIterationId = mission.iterationId
            val historicalWeekAlias =
                legacyIterationId != null &&
                    mission.weekKey.isNotBlank() &&
                    legacyIterationId == mission.weekKey &&
                    database.tacticalIterationDao().getById(legacyIterationId) == null &&
                    tacticalPlanningAdapter.getById(legacyIterationId) == null

            if (
                mission.sourceType == MissionSourceType.MANUAL &&
                (legacyIterationId == null || historicalWeekAlias)
            ) {
                val resolution = orientationResolver.resolve(mission)
                require(
                    resolution is TacticalMissionOrientationResolution.Resolved &&
                        resolution.path ==
                            TacticalMissionOrientationResolutionPath.LEGACY_MANUAL_MAPPING,
                ) {
                    "Standalone MANUAL TacticalMission requires a verified canonical INTENT mapping"
                }
                orientationRepository.requireCompleteActiveOrientationAggregate(
                    resolution.orientationId,
                )
                return requireNotNull(
                    database.orientationDao().getManagedSubject(resolution.orientationId),
                ) {
                    "Standalone MANUAL TacticalMission is missing its canonical INTENT subject"
                }
            }

            require(
                mission.sourceType == MissionSourceType.CONTEXT_BACKLOG_ITEM ||
                    mission.sourceType == MissionSourceType.SLOT_BACKLOG_ITEM,
            ) {
                "Eligible TacticalMission ${mission.id} is missing canonical MISSION participation"
            }

            when (val resolution = orientationResolver.resolve(mission)) {
                is TacticalMissionOrientationResolution.NeedsAdoption ->
                    require(
                        resolution.code ==
                            TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET,
                    ) {
                        "TacticalMission ${mission.id} has unsupported runtime deferral ${resolution.code}"
                    }

                is TacticalMissionOrientationResolution.Resolved -> {
                    // A historical week alias is not a real Tactical Cycle.
                    // Orientation remains canonical; no MISSION commitment
                    // can be created until actual Tactical participation.
                    if (
                        mission.sourceType == MissionSourceType.CONTEXT_BACKLOG_ITEM &&
                        (legacyIterationId == null || historicalWeekAlias) &&
                        resolution.path ==
                            TacticalMissionOrientationResolutionPath.BACKLOG_ORIENTATION
                    ) {
                        orientationRepository.requireCompleteActiveOrientationAggregate(
                            resolution.orientationId,
                        )
                        return requireNotNull(
                            database.orientationDao()
                                .getManagedSubject(resolution.orientationId),
                        ) {
                            "Historical backlog alias lacks its canonical Orientation subject"
                        }
                    }

                    error(
                        "Eligible TacticalMission ${mission.id} resolved to Orientation " +
                            "${resolution.orientationId} but canonical MISSION participation is missing",
                    )
                }

                is TacticalMissionOrientationResolution.SemanticPrerequisite ->
                    error(
                        "TacticalMission ${mission.id} semantic prerequisite failed: ${resolution.code}",
                    )

                is TacticalMissionOrientationResolution.IntegrityFailure ->
                    error(
                        "TacticalMission ${mission.id} integrity failure: " +
                            "${resolution.code}: ${resolution.detail}",
                    )
            }
            return null
        }

        private suspend fun resolveExistingOrientationOrDefer(
            mission: TacticalMission,
        ): String? =
            when (val resolution = orientationResolver.resolve(mission)) {
                is TacticalMissionOrientationResolution.Resolved ->
                    resolution.orientationId

                is TacticalMissionOrientationResolution.NeedsAdoption -> {
                    if (
                        mission.sourceType in
                            setOf(
                                MissionSourceType.CONTEXT_BACKLOG_ITEM,
                                MissionSourceType.SLOT_BACKLOG_ITEM,
                            ) &&
                        resolution.code ==
                            TacticalMissionOrientationResolutionCode.NON_ORIENTATION_BACKLOG_TARGET
                    ) {
                        null
                    } else {
                        error(
                            "TacticalMission ${mission.id} cannot be authored without canonical semantic target: " +
                                resolution.code,
                        )
                    }
                }

                is TacticalMissionOrientationResolution.SemanticPrerequisite ->
                    error(
                        "TacticalMission ${mission.id} semantic prerequisite failed: ${resolution.code}",
                    )

                is TacticalMissionOrientationResolution.IntegrityFailure ->
                    error(
                        "TacticalMission ${mission.id} integrity failure: " +
                            "${resolution.code}: ${resolution.detail}",
                    )
            }

        private suspend fun requirePreviousCanonicalOrientation(
            mission: TacticalMission,
        ): String {
            val previousId =
                requireNotNull(mission.carriedFromMissionId) {
                    "PREVIOUS_WEEK TacticalMission ${mission.id} has no previous mission"
                }
            val previous = requireNotNull(database.tacticalMissionDao().getMissionById(previousId)) {
                "Previous TacticalMission $previousId does not exist"
            }
            require(!previous.isDeleted) { "Previous TacticalMission $previousId is deleted" }
            val previousRecords = missionCommitments(previousId)
            val previousLive = previousRecords.filterNot(PlanningCommitment::isDeleted)
            require(previousLive.size <= 1) {
                "Previous TacticalMission $previousId has multiple live canonical MISSION commitments"
            }
            previousLive.singleOrNull()?.let { commitment ->
                projectCanonical(previous, commitment)
                orientationRepository.requireCompleteActiveOrientationAggregate(commitment.orientationId)
                return commitment.orientationId
            }

            // Historical week aliases/unscoped missions may already have a verified
            // canonical subject but no actual cycle participation. Reuse precisely
            // that admitted identity, without rewriting history or adopting by title.
            require(previousRecords.isEmpty()) {
                "Previous TacticalMission $previousId has only retired canonical MISSION participation"
            }
            return requireNotNull(requireDeferredSpecializedOnly(previous)) {
                "Previous TacticalMission $previousId has no supported canonical Orientation for carry-forward"
            }.id
        }
    }

internal fun projectCanonicalTacticalMissionCompatibility(
    row: TacticalMission,
    commitment: PlanningCommitment,
    canonicalSubject: ManagedSubjectEntity,
): TacticalMission {
    require(commitment.role == PlanningRoleCode.MISSION)
    require(commitment.provenance.sourceType == TACTICAL_MISSION_PROVENANCE_SOURCE)
    require(commitment.provenance.sourceId == row.id.toString()) {
        "Canonical MISSION commitment ${commitment.id.value} does not belong to TacticalMission ${row.id}"
    }
    require(
        commitment.id ==
            canonicalTacticalMissionCommitmentId(commitment.scopeId.value, row.id),
    ) {
        "Canonical MISSION commitment ${commitment.id.value} has non-deterministic identity"
    }
    require(
        canonicalSubject.id == commitment.orientationId &&
            canonicalSubject.subjectType == ManagedSubjectType.ORIENTATION.name &&
            !canonicalSubject.isDeleted,
    ) {
        "Canonical Tactical MISSION ${commitment.id.value} target is not an active Orientation"
    }

    return row.copy(
        title = canonicalSubject.title,
        description = canonicalSubject.description,
        iterationId = commitment.scopeId.value,
        orderInWeek = commitment.order,
        status = commitment.status.toMissionStatus(),
        priority =
            requireNotNull(commitment.priorityLevel) {
                "Canonical Tactical MISSION ${commitment.id.value} has no priority"
            }.toMissionPriority(),
        createdAt = commitment.createdAt,
        updatedAt = commitment.updatedAt,
        syncedAt = commitment.syncedAt,
        isDeleted = commitment.isDeleted,
        version = commitment.version,
    )
}

private fun TacticalMission.runtimeProvenanceKind(): PlanningProvenanceKind =
    when (sourceType) {
        MissionSourceType.MANUAL -> PlanningProvenanceKind.MANUAL
        MissionSourceType.PREVIOUS_WEEK -> PlanningProvenanceKind.CARRIED_FORWARD
        MissionSourceType.CONTEXT_BACKLOG_ITEM,
        MissionSourceType.SLOT_BACKLOG_ITEM,
        MissionSourceType.ARC_QUEST,
        -> PlanningProvenanceKind.DERIVED
    }

private fun PlanningCommitmentStatus.toMissionStatus(): MissionStatus =
    when (this) {
        PlanningCommitmentStatus.PLANNED -> MissionStatus.INACTIVE
        PlanningCommitmentStatus.ACTIVE -> MissionStatus.ACTIVE
        PlanningCommitmentStatus.PAUSED -> MissionStatus.PAUSED
        PlanningCommitmentStatus.COMPLETED -> MissionStatus.COMPLETED
        PlanningCommitmentStatus.DROPPED -> MissionStatus.INACTIVE
    }

private fun PlanningPriorityLevel.toMissionPriority(): MissionPriority =
    when (this) {
        PlanningPriorityLevel.LOW -> MissionPriority.LOW
        PlanningPriorityLevel.MEDIUM -> MissionPriority.MEDIUM
        PlanningPriorityLevel.HIGH -> MissionPriority.HIGH
        PlanningPriorityLevel.CRITICAL -> MissionPriority.CRITICAL
    }
