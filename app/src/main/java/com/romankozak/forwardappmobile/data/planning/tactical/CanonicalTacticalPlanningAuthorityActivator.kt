package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import javax.inject.Inject
import javax.inject.Singleton

data class TacticalPlanningActivationReport(
    val performed: Boolean,
    val scopes: Int,
    val commitments: Int,
    val deferredMissions: Int,
)

/**
 * Explicit one-shot Tactical materialization boundary.
 *
 * This is deliberately not called by ordinary Tactical readers or writers.
 * V3.5g owns production runtime admission after its canonical adapters exist.
 */
@Singleton
class CanonicalTacticalPlanningAuthorityActivator
    @Inject
    internal constructor(
        private val database: AppDatabase,
        private val manualIntentAdopter: LegacyManualTacticalMissionIntentAdopter,
        private val questOrientationBridge: QuestOrientationBridge,
    ) {
        suspend fun ensureActivated(now: Long = System.currentTimeMillis()): TacticalPlanningActivationReport {
            database.tacticalPlanningAuthorityStateDao().get()?.let { marker ->
                require(marker.version == CURRENT_VERSION) {
                    "Unsupported Tactical planning authority marker version ${marker.version}"
                }
                return database.withTransaction {
                    validateEstablishedState()
                    currentReport(performed = false)
                }
            }
            return database.withTransaction {
                prepareSemanticTargets()
                val markerDao = database.tacticalPlanningAuthorityStateDao()
                val marker = markerDao.get()
                if (marker != null) {
                    require(marker.version == CURRENT_VERSION) {
                        "Unsupported Tactical planning authority marker version ${marker.version}"
                    }
                    validateEstablishedState()
                    return@withTransaction currentReport(performed = false)
                }

                val plan = buildPlan()
                requirePristineOrExact(plan)
                val planningDao = database.planningDao()
                val extensionDao = database.tacticalPlanningScopeExtensionDao()
                planningDao.upsertScopes(
                    plan.scopes.mapNotNull { expected ->
                        expected.takeIf { planningDao.getScope(it.id.value) == null }?.toPlanningScopeEntity()
                    },
                )
                extensionDao.upsertAll(
                    plan.extensions.mapNotNull { expected ->
                        expected.takeIf { extensionDao.get(it.scopeId.value) == null }?.toEntity()
                    },
                )
                planningDao.upsertCommitments(
                    plan.commitments.mapNotNull { expected ->
                        expected.takeIf { planningDao.getCommitment(it.id.value) == null }
                            ?.toPlanningCommitmentEntity()
                    },
                )
                requireExactParity(plan)
                markerDao.upsert(
                    TacticalPlanningAuthorityStateEntity(
                        version = CURRENT_VERSION,
                        activatedAt = now,
                    ),
                )
                plan.report(performed = true)
            }
        }

        suspend fun requireActivated() {
            val marker = requireNotNull(database.tacticalPlanningAuthorityStateDao().get()) {
                "Canonical Tactical planning materialization is not activated"
            }
            require(marker.version == CURRENT_VERSION) {
                "Unsupported Tactical planning authority marker version ${marker.version}"
            }
        }

        /**
         * Explicit canonical transport ingress admission.
         *
         * Unlike finite V3.5f migration, this path never derives authority from
         * TacticalIteration/TacticalMission. Canonical Planning has already been
         * restored or merged; the local marker merely admits that canonical state.
         */
        suspend fun admitCanonicalIngress(
            now: Long = System.currentTimeMillis(),
        ): TacticalPlanningActivationReport =
            database.withTransaction {
                val markerDao = database.tacticalPlanningAuthorityStateDao()
                val existing = markerDao.get()
                existing?.let { marker ->
                    require(marker.version == CURRENT_VERSION) {
                        "Unsupported Tactical planning authority marker version ${marker.version}"
                    }
                }

                validateEstablishedState()

                if (existing == null) {
                    markerDao.upsert(
                        TacticalPlanningAuthorityStateEntity(
                            version = CURRENT_VERSION,
                            activatedAt = now,
                        ),
                    )
                }

                currentReport(performed = existing == null)
            }

        /**
         * Refreshes physical Tactical compatibility carriers from canonical authority.
         *
         * TacticalIteration rows are projections of canonical Tactical scopes.
         * Existing TacticalMission rows remain the carrier for Tactical-only fields
         * and transport compatibility, while canonical Planning + Orientation own
         * their semantic/common planning fields.
         *
         * This projection never synthesizes a missing TacticalMission carrier.
         */
        suspend fun projectCompatibilityRows() {
            requireActivated()
            database.withTransaction {
                val extensionByScope =
                    database.tacticalPlanningScopeExtensionDao()
                        .getAll()
                        .map { it.toDomainStrict() }
                        .associateBy { it.scopeId.value }

                database.planningDao()
                    .getAllScopes()
                    .map { it.toPlanningScopeStrict() }
                    .filter {
                        it.kind ==
                            com.romankozak.forwardappmobile.shared.core.domain.planning
                                .PlanningScopeKind.TACTICAL_CYCLE
                    }
                    .forEach { scope ->
                        val extension =
                            requireNotNull(extensionByScope[scope.id.value]) {
                                "Canonical Tactical scope ${scope.id.value} is missing its extension"
                            }
                        database.tacticalIterationDao().insert(
                            scope.toTacticalIteration(extension),
                        )
                    }

                projectMissionCompatibilityRows()
            }
        }

        private suspend fun projectMissionCompatibilityRows() {
            val missionDao = database.tacticalMissionDao()
            val rows = missionDao.getAllMissionsSync()
            val rowIds = rows.mapTo(hashSetOf()) { it.id }

            val commitments =
                database.planningDao()
                    .getAllCommitments()
                    .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                    .filter {
                        it.role == PlanningRoleCode.MISSION &&
                            it.provenance.sourceType == TACTICAL_MISSION_PROVENANCE_SOURCE
                    }

            val commitmentsByMissionId =
                commitments.groupBy { commitment ->
                    val missionId =
                        requireNotNull(commitment.provenance.sourceId?.toLongOrNull()) {
                            "Canonical Tactical MISSION commitment ${commitment.id.value} has invalid source id"
                        }
                    require(missionId > 0L) {
                        "Canonical Tactical MISSION commitment ${commitment.id.value} has invalid source id $missionId"
                    }
                    missionId
                }

            val orphanMissionId =
                commitmentsByMissionId.keys
                    .sorted()
                    .firstOrNull { it !in rowIds }
            require(orphanMissionId == null) {
                "Canonical Tactical MISSION commitment references missing compatibility row $orphanMissionId"
            }

            val subjectsById =
                database.orientationDao()
                    .getAllManagedSubjects()
                    .associateBy { it.id }

            rows.sortedBy { it.id }.forEach { row ->
                val candidates = commitmentsByMissionId[row.id].orEmpty()

                if (candidates.isEmpty()) {
                    if (!row.isDeleted) {
                        require(
                            row.sourceType == MissionSourceType.CONTEXT_BACKLOG_ITEM ||
                                row.sourceType == MissionSourceType.SLOT_BACKLOG_ITEM,
                        ) {
                            "Eligible TacticalMission ${row.id} is missing canonical MISSION participation"
                        }
                    }
                    // No canonical MISSION commitment is created for deferred
                    // carriers. Their operational project owner must still
                    // converge independently of Planning participation.
                    //
                    // The validated DAO writer rejects unproven or divergent
                    // owners, clears the physical Context branch, and keeps
                    // the specialized Tactical carrier fields intact.
                    if (row.projectId != null || row.projectWorkspaceId != null) {
                        missionDao.updateMission(row)
                    }
                    return@forEach
                }

                val live = candidates.filterNot(PlanningCommitment::isDeleted)
                require(live.size <= 1) {
                    "TacticalMission ${row.id} has ${live.size} live canonical MISSION commitments"
                }

                val selected =
                    live.singleOrNull()
                        ?: requireNotNull(
                            candidates.maxWithOrNull(
                                compareBy<PlanningCommitment> {
                                    it.updatedAt ?: it.createdAt
                                }.thenBy { it.version }
                                    .thenBy { it.id.value },
                            ),
                        )

                val canonicalSubject =
                    requireNotNull(subjectsById[selected.orientationId]) {
                        "Canonical Tactical MISSION ${selected.id.value} is missing Orientation subject"
                    }

                missionDao.updateMission(
                    projectCanonicalTacticalMissionCompatibility(
                        row = row,
                        commitment = selected,
                        canonicalSubject = canonicalSubject,
                    ),
                )
            }
        }

        private suspend fun prepareSemanticTargets() {
            questOrientationBridge.ensureCutOver()
            val iterationIds = database.tacticalIterationDao().getAllSync().map { it.id }.toSet()
            database.tacticalMissionDao().getAllMissionsSync()
                .filter { mission ->
                    !mission.isDeleted &&
                        mission.sourceType.name == "MANUAL" &&
                        (
                            mission.iterationId in iterationIds ||
                                mission.iterationId == null ||
                                (
                                    mission.weekKey.isNotBlank() &&
                                        mission.iterationId == mission.weekKey
                                )
                        )
                }
                .sortedBy { it.id }
                .forEach { manualIntentAdopter.adopt(it.id) }
        }

        private suspend fun buildPlan(): TacticalPlanningMaterializationPlan =
            planCanonicalTacticalPlanningMaterialization(
                iterations = database.tacticalIterationDao().getAllSync(),
                resolutionSnapshot =
                    TacticalMissionOrientationResolutionSnapshot(
                        missions = database.tacticalMissionDao().getAllMissionsSync(),
                        backlogEntries = database.workspaceBacklogEntryDao().getAll(),
                        arcQuests = database.arcQuestDao().getAllSync(),
                        mappings = database.orientationDao().getAllLegacyMappings(),
                        subjects = database.orientationDao().getAllManagedSubjects(),
                        orientations = database.orientationDao().getAllOrientations(),
                    ),
            )

        private suspend fun requirePristineOrExact(plan: TacticalPlanningMaterializationPlan) {
            val planningDao = database.planningDao()
            val extensionDao = database.tacticalPlanningScopeExtensionDao()
            plan.scopes.forEach { expected ->
                planningDao.getScope(expected.id.value)?.toPlanningScopeStrict()?.let { actual ->
                    require(actual == expected) {
                        "Existing Tactical scope ${expected.id.value} diverges from deterministic migration"
                    }
                }
            }
            plan.extensions.forEach { expected ->
                extensionDao.get(expected.scopeId.value)?.toDomainStrict()?.let { actual ->
                    require(actual == expected) {
                        "Existing Tactical extension ${expected.scopeId.value} diverges from deterministic migration"
                    }
                }
            }
            plan.commitments.forEach { expected ->
                planningDao.getCommitment(expected.id.value)
                    ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                    ?.let { actual ->
                        require(actual == expected) {
                            "Existing Tactical commitment ${expected.id.value} diverges from deterministic migration"
                        }
                    }
                val competing =
                    planningDao.getParticipationRecords(
                        scopeId = expected.scopeId.value,
                        orientationId = expected.orientationId,
                        role = expected.role.value,
                    ).filter { !it.isDeleted && it.id != expected.id.value }
                require(competing.isEmpty()) {
                    "Tactical participation ${expected.scopeId.value}/${expected.orientationId} has competing ids"
                }
            }
        }

        private suspend fun requireExactParity(plan: TacticalPlanningMaterializationPlan) {
            val planningDao = database.planningDao()
            val extensionDao = database.tacticalPlanningScopeExtensionDao()

            val actualScopes =
                planningDao.getAllScopes()
                    .filter { it.kind == "TACTICAL_CYCLE" }
                    .map { it.toPlanningScopeStrict() }
                    .sortedBy { it.id.value }
            val expectedScopes = plan.scopes.sortedBy { it.id.value }
            require(actualScopes == expectedScopes) {
                "Tactical scope aggregate parity failed: " +
                    "expected=${expectedScopes.map { it.id.value }} " +
                    "actual=${actualScopes.map { it.id.value }}"
            }

            val actualExtensions =
                extensionDao.getAll()
                    .map { it.toDomainStrict() }
                    .sortedBy { it.scopeId.value }
            val expectedExtensions = plan.extensions.sortedBy { it.scopeId.value }
            require(actualExtensions == expectedExtensions) {
                "Tactical extension aggregate parity failed: " +
                    "expected=${expectedExtensions.map { it.scopeId.value }} " +
                    "actual=${actualExtensions.map { it.scopeId.value }}"
            }

            val actualCommitments =
                planningDao.getAllCommitments()
                    .filter {
                        it.role == "MISSION" ||
                            it.provenanceSourceType == TACTICAL_MISSION_PROVENANCE_SOURCE
                    }
                    .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                    .sortedBy { it.id.value }
            val expectedCommitments = plan.commitments.sortedBy { it.id.value }
            require(actualCommitments == expectedCommitments) {
                "Tactical commitment aggregate parity failed: " +
                    "expected=${expectedCommitments.map { it.id.value }} " +
                    "actual=${actualCommitments.map { it.id.value }}"
            }
        }

        private suspend fun validateEstablishedState() {
            val planningDao = database.planningDao()
            val extensionDao = database.tacticalPlanningScopeExtensionDao()

            val scopes =
                planningDao.getAllScopes()
                    .map { it.toPlanningScopeStrict() }
                    .filter {
                        it.kind ==
                            com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind.TACTICAL_CYCLE
                    }
            val scopesById = scopes.associateBy { it.id.value }

            val extensions = extensionDao.getAll().map { it.toDomainStrict() }
            val extensionsByScopeId = extensions.associateBy { it.scopeId.value }

            require(extensionsByScopeId.keys == scopesById.keys) {
                "Activated Tactical planning scope/extension set is structurally incomplete"
            }

            extensions.forEach { extension ->
                val scope = scopesById[extension.scopeId.value]
                val violations =
                    com.romankozak.forwardappmobile.shared.core.domain.planning
                        .validateTacticalPlanningScopeExtensionDependency(extension, scope)
                require(violations.isEmpty()) {
                    "Activated Tactical extension ${extension.scopeId.value} violates V3 contract: $violations"
                }
            }

            val allCommitments =
                planningDao.getAllCommitments()
                    .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }

            allCommitments
                .filter {
                    it.role ==
                        com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode.MISSION
                }
                .forEach { commitment ->
                    require(commitment.scopeId.value in scopesById) {
                        "Activated MISSION commitment ${commitment.id.value} is outside a Tactical scope"
                    }
                }
        }

        private suspend fun currentReport(performed: Boolean): TacticalPlanningActivationReport {
            val planningDao = database.planningDao()
            val scopes =
                planningDao.getAllScopes()
                    .map { it.toPlanningScopeStrict() }
                    .count {
                        it.kind ==
                            com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind.TACTICAL_CYCLE
                    }
            val commitments =
                planningDao.getAllCommitments()
                    .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                    .count {
                        it.role ==
                            com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode.MISSION
                    }
            return TacticalPlanningActivationReport(
                performed = performed,
                scopes = scopes,
                commitments = commitments,
                deferredMissions = 0,
            )
        }

        private fun TacticalPlanningMaterializationPlan.report(performed: Boolean) =
            TacticalPlanningActivationReport(
                performed = performed,
                scopes = scopes.size,
                commitments = commitments.size,
                deferredMissions = deferredMissions.size,
            )

        companion object {
            const val CURRENT_VERSION = 1
        }
    }

internal fun PlanningScope.sameTacticalPlanningFacts(other: PlanningScope): Boolean = this == other

internal fun TacticalPlanningScopeExtension.sameTacticalPlanningFacts(
    other: TacticalPlanningScopeExtension,
): Boolean = this == other

internal fun PlanningCommitment.sameTacticalPlanningFacts(other: PlanningCommitment): Boolean = this == other
