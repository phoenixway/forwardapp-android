package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolutionSnapshot
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateProspectivePlanningState
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtensionDependency
import javax.inject.Inject
import javax.inject.Singleton

data class StrategicPlanningActivationReport(
    val performed: Boolean,
    val scopes: Int,
    val commitments: Int,
    val deferredParticipations: Int,
)

/**
 * Finite V3.6 Strategic establishment boundary.
 *
 * Ordinary Strategic readers and writers do not call this class. It prepares
 * semantic prerequisites, writes deterministic Planning rows, proves set-exact
 * parity, and inserts the local authority marker as the transaction's final
 * mutation.
 */
@Singleton
class CanonicalStrategicPlanningAuthorityActivator
    @Inject
    internal constructor(
        private val database: AppDatabase,
        private val questOrientationBridge: QuestOrientationBridge,
        private val tacticalPlanningAuthorityActivator: CanonicalTacticalPlanningAuthorityActivator,
    ) {
        suspend fun admitCanonicalIngress(
            now: Long = System.currentTimeMillis(),
        ): StrategicPlanningActivationReport =
            database.withTransaction {
                val markerDao = database.strategicPlanningAuthorityStateDao()
                markerDao.get()?.let { marker ->
                    require(marker.version == CURRENT_VERSION)
                    val existing = buildPlan()
                    requireRuntimeAdmission(existing)
                    return@withTransaction existing.report(performed = false)
                }

                val plan = buildPlan()
                requireRuntimeAdmission(plan)
                markerDao.upsert(
                    StrategicPlanningAuthorityStateEntity(
                        version = CURRENT_VERSION,
                        activatedAt = now,
                    ),
                )
                plan.report(performed = true)
            }

        suspend fun ensureActivated(now: Long = System.currentTimeMillis()): StrategicPlanningActivationReport =
            database.withTransaction {
                val markerDao = database.strategicPlanningAuthorityStateDao()
                markerDao.get()?.let { marker ->
                    require(marker.version == CURRENT_VERSION) {
                        "Unsupported Strategic planning authority marker version ${marker.version}"
                    }
                    val plan = buildPlan()
                    repairHistoricalStrategicTombstoneStatuses(plan, now)
                    requireRuntimeAdmission(plan)
                    return@withTransaction plan.report(performed = false)
                }

                prepareSemanticTargets()
                val plan = buildPlan()
                requireActivatable(plan)

                requirePristineOrExact(plan)
                val planningDao = database.planningDao()
                val extensionDao = database.strategicPlanningScopeExtensionDao()

                planningDao.upsertScopes(
                    plan.scopes.mapNotNull { expected ->
                        expected.takeIf { planningDao.getScope(it.id.value) == null }
                            ?.toPlanningScopeEntity()
                    },
                )
                extensionDao.upsertAll(
                    plan.extensions.mapNotNull { expected ->
                        expected.takeIf { extensionDao.get(it.scopeId.value) == null }
                            ?.toEntity()
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
                    StrategicPlanningAuthorityStateEntity(
                        version = CURRENT_VERSION,
                        activatedAt = now,
                    ),
                )
                plan.report(performed = true)
            }

        suspend fun requireActivated() {
            val marker = requireNotNull(database.strategicPlanningAuthorityStateDao().get()) {
                "Canonical Strategic planning materialization is not activated"
            }
            require(marker.version == CURRENT_VERSION) {
                "Unsupported Strategic planning authority marker version ${marker.version}"
            }
        }

        /** Revalidates active local authority after canonical Restore/merge ingress. */
        suspend fun revalidateIfActivated() {
            if (database.strategicPlanningAuthorityStateDao().get() == null) return
            database.withTransaction {
                val marker = requireNotNull(database.strategicPlanningAuthorityStateDao().get())
                require(marker.version == CURRENT_VERSION) {
                    "Unsupported Strategic planning authority marker version ${marker.version}"
                }
                val plan = buildPlan()
                requireRuntimeAdmission(plan)
            }
        }

        /**
         * Post-cutover admission validates canonical structure and exact
         * eligible/deferred accounting without rematerializing canonical facts
         * from subordinate ArcQuest compatibility copies.
         */
        /**
         * Repair the historical generic tombstone bug without weakening admission.
         *
         * Only the exact canonical FOCUS identity expected from an already-deleted
         * ArcQuest is eligible. Unrelated or live ownership inconsistencies still
         * fail strict admission. This runs in the outer Room transaction, so any
         * subsequent admission failure rolls back all repairs.
         */
        private suspend fun repairHistoricalStrategicTombstoneStatuses(
            plan: StrategicPlanningMapping,
            now: Long,
        ) {
            val planningDao = database.planningDao()
            plan.commitments.filter { it.isDeleted }.forEach { expected ->
                val actual =
                    planningDao.getCommitment(expected.id.value)
                        ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                        ?: return@forEach

                if (!actual.isDeleted || actual.status == PlanningCommitmentStatus.DROPPED) {
                    return@forEach
                }

                require(
                    actual.scopeId == expected.scopeId &&
                        actual.orientationId == expected.orientationId &&
                        actual.role == PlanningRoleCode.FOCUS &&
                        actual.provenance == expected.provenance,
                ) {
                    "Historical Strategic FOCUS ${expected.id.value} has divergent canonical ownership"
                }
                require(actual.version < Long.MAX_VALUE) {
                    "Historical Strategic FOCUS ${expected.id.value} exhausted its version"
                }

                planningDao.upsertCommitment(
                    actual.copy(
                        status = PlanningCommitmentStatus.DROPPED,
                        updatedAt = now,
                        syncedAt = null,
                        version = actual.version + 1L,
                    ).toPlanningCommitmentEntity(),
                )
            }
        }

        private suspend fun requireRuntimeAdmission(plan: StrategicPlanningMapping) {
            requireActivatable(plan)
            val planningDao = database.planningDao()
            val scopes =
                planningDao.getAllScopes().map { it.toPlanningScopeStrict() }
                    .filter { it.kind == PlanningScopeKind.STRATEGIC_ARC }
            val scopeIds = scopes.mapTo(hashSetOf()) { it.id }
            require(scopeIds == plan.scopes.mapTo(hashSetOf()) { it.id }) {
                "Canonical Strategic scope ownership diverges from compatibility carrier set"
            }
            val extensions =
                database.strategicPlanningScopeExtensionDao().getAll().map { it.toDomainStrict() }
            require(extensions.mapTo(hashSetOf()) { it.scopeId } == scopeIds) {
                "Canonical Strategic extension ownership diverges from scope ownership"
            }
            extensions.forEach { extension ->
                require(extension.scopeId == canonicalStrategicArcScopeId(extension.legacyArcKey)) {
                    "Strategic extension ${extension.scopeId.value} has divergent legacyArcKey identity"
                }
                val scope = requireNotNull(scopes.singleOrNull { it.id == extension.scopeId })
                require(
                    validateStrategicPlanningScopeExtensionDependency(extension, scope).isEmpty(),
                ) { "Strategic extension ${extension.scopeId.value} violates its scope dependency" }
            }

            val expectedEligible = plan.commitments.associateBy { it.id }
            val actual =
                planningDao.getAllCommitments()
                    .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                    .filter { it.isStrategicOwned(scopeIds.mapTo(hashSetOf()) { id -> id.value }) }
                    .associateBy { it.id }
            require(actual.keys == expectedEligible.keys) {
                "Canonical Strategic FOCUS ownership diverges from eligible ArcQuest participation"
            }
            actual.forEach { (id, commitment) ->
                val expected = requireNotNull(expectedEligible[id])
                require(
                    commitment.scopeId == expected.scopeId &&
                        commitment.orientationId == expected.orientationId &&
                        commitment.role == PlanningRoleCode.FOCUS &&
                        commitment.provenance == expected.provenance,
                ) { "Canonical Strategic commitment ${id.value} has divergent identity/provenance" }
                // buildPlan() has already proven canonical target existence,
                // semantic kind, and source-mapping identity for expected rows.
                // A historical tombstone may reference a deleted Orientation,
                // but it must never admit a live or resurrected FOCUS.
                require(commitment.isDeleted == expected.isDeleted) {
                    "Canonical Strategic commitment ${id.value} has divergent deletion state"
                }
                // Preserve strict admission while exposing the precise
                // failing branch for historical database diagnostics.
                val orientationLive =
                    if (commitment.isDeleted) {
                        null
                    } else {
                        planningDao.isOrientationLive(commitment.orientationId)
                    }
                require(
                    if (commitment.isDeleted) {
                        commitment.status == PlanningCommitmentStatus.DROPPED
                    } else {
                        orientationLive == true
                    }
                ) {
                    "Canonical Strategic commitment ${id.value} violates Orientation liveness/tombstone admission: " +
                        "isDeleted=${commitment.isDeleted}, status=${commitment.status}, " +
                        "orientationId=${commitment.orientationId}, orientationLive=$orientationLive"
                }
            }
        }

        private suspend fun prepareSemanticTargets() {
            // V3.5 is an established prerequisite. Calling the finite boundary
            // here is idempotent and keeps any missing semantic preparation in
            // this same outer Room transaction.
            tacticalPlanningAuthorityActivator.ensureActivated()
            questOrientationBridge.ensureCutOver()
        }

        private suspend fun buildPlan(): StrategicPlanningMapping {
            val mappings = database.orientationDao().getAllLegacyMappings()
            val subjects = database.orientationDao().getAllManagedSubjects()
            val orientations = database.orientationDao().getAllOrientations()
            val arcQuests = database.arcQuestDao().getAllSync()
            return mapCanonicalStrategicPlanning(
                arcQuests = arcQuests,
                snapshot =
                    StrategicPlanningResolutionSnapshot(
                        mappings = mappings,
                        subjects = subjects,
                        orientations = orientations,
                        tactical =
                            TacticalMissionOrientationResolutionSnapshot(
                                missions = database.tacticalMissionDao().getAllMissionsSync(),
                                backlogEntries = database.workspaceBacklogEntryDao().getAll(),
                                arcQuests = arcQuests,
                                mappings = mappings,
                                subjects = subjects,
                                orientations = orientations,
                            ),
                    ),
            )
        }

        private fun requireActivatable(plan: StrategicPlanningMapping) {
            require(plan.integrityFailures.isEmpty()) {
                "Strategic planning activation has integrity failures: " +
                    plan.integrityFailures.joinToString { "${it.arcQuestId}:${it.code}" }
            }
            require(plan.participants.size == plan.commitments.size + plan.deferred.size) {
                "Strategic participant eligibility accounting is incomplete"
            }
            require(plan.scopes.map { it.id }.distinct().size == plan.scopes.size) {
                "Deterministic Strategic scope id collision"
            }
            require(plan.extensions.map { it.scopeId }.distinct().size == plan.extensions.size) {
                "Deterministic Strategic extension id collision"
            }
            require(plan.commitments.map { it.id }.distinct().size == plan.commitments.size) {
                "Deterministic Strategic commitment id collision"
            }
            val scopesById = plan.scopes.associateBy { it.id }
            val contractViolations =
                validateProspectivePlanningState(
                    scopes = plan.scopes,
                    commitments = plan.commitments,
                    // Target existence/liveness is resolved before the plan is built.
                    isOrientationLive = { true },
                ) +
                    plan.extensions.flatMap { extension ->
                        validateStrategicPlanningScopeExtensionDependency(
                            extension = extension,
                            scope = scopesById[extension.scopeId],
                        )
                    }
            require(contractViolations.isEmpty()) {
                "Strategic planning activation violates the canonical contract: " +
                    contractViolations.joinToString { "${it.path}:${it.code}" }
            }
        }

        private suspend fun requirePristineOrExact(plan: StrategicPlanningMapping) {
            val planningDao = database.planningDao()
            val extensionDao = database.strategicPlanningScopeExtensionDao()

            plan.scopes.forEach { expected ->
                planningDao.getScope(expected.id.value)?.toPlanningScopeStrict()?.let { actual ->
                    require(actual == expected) {
                        "Existing Strategic scope ${expected.id.value} diverges from deterministic migration"
                    }
                }
            }
            plan.extensions.forEach { expected ->
                extensionDao.get(expected.scopeId.value)?.toDomainStrict()?.let { actual ->
                    require(actual == expected) {
                        "Existing Strategic extension ${expected.scopeId.value} diverges from deterministic migration"
                    }
                }
            }
            plan.commitments.forEach { expected ->
                planningDao.getCommitment(expected.id.value)
                    ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                    ?.let { actual ->
                        require(actual == expected) {
                            "Existing Strategic commitment ${expected.id.value} diverges from deterministic migration"
                        }
                    }
                val competing =
                    planningDao.getParticipationRecords(
                        scopeId = expected.scopeId.value,
                        orientationId = expected.orientationId,
                        role = expected.role.value,
                    ).filter { !it.isDeleted && it.id != expected.id.value }
                require(competing.isEmpty()) {
                    "Strategic participation ${expected.scopeId.value}/${expected.orientationId} has competing ids"
                }
            }

            requireOwnedRowsEqualExpected(plan, allowMissingExpected = true)
        }

        private suspend fun requireExactParity(plan: StrategicPlanningMapping) {
            requireOwnedRowsEqualExpected(plan, allowMissingExpected = false)
        }

        private suspend fun requireOwnedRowsEqualExpected(
            plan: StrategicPlanningMapping,
            allowMissingExpected: Boolean,
        ) {
            val planningDao = database.planningDao()
            val actualScopes =
                planningDao.getAllScopes()
                    .map { it.toPlanningScopeStrict() }
                    .filter { it.kind == PlanningScopeKind.STRATEGIC_ARC }
                    .sortedBy { it.id.value }
            val expectedScopes = plan.scopes.sortedBy { it.id.value }
            require(
                if (allowMissingExpected) {
                    actualScopes.all { it in expectedScopes }
                } else {
                    actualScopes == expectedScopes
                },
            ) {
                "Strategic scope aggregate parity failed: expected=${expectedScopes.map { it.id.value }} " +
                    "actual=${actualScopes.map { it.id.value }}"
            }

            val actualExtensions =
                database.strategicPlanningScopeExtensionDao().getAll()
                    .map { it.toDomainStrict() }
                    .sortedBy { it.scopeId.value }
            val expectedExtensions = plan.extensions.sortedBy { it.scopeId.value }
            require(
                if (allowMissingExpected) {
                    actualExtensions.all { it in expectedExtensions }
                } else {
                    actualExtensions == expectedExtensions
                },
            ) {
                "Strategic extension aggregate parity failed: expected=${expectedExtensions.map { it.scopeId.value }} " +
                    "actual=${actualExtensions.map { it.scopeId.value }}"
            }

            val strategicScopeIds = actualScopes.mapTo(hashSetOf()) { it.id.value }
            val actualCommitments =
                planningDao.getAllCommitments()
                    .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                    .filter { it.isStrategicOwned(strategicScopeIds) }
                    .sortedBy { it.id.value }
            val expectedCommitments = plan.commitments.sortedBy { it.id.value }
            require(
                if (allowMissingExpected) {
                    actualCommitments.all { it in expectedCommitments }
                } else {
                    actualCommitments == expectedCommitments
                },
            ) {
                "Strategic commitment aggregate parity failed: expected=${expectedCommitments.map { it.id.value }} " +
                    "actual=${actualCommitments.map { it.id.value }}"
            }
        }

        private fun StrategicPlanningMapping.report(performed: Boolean) =
            StrategicPlanningActivationReport(
                performed = performed,
                scopes = scopes.size,
                commitments = commitments.size,
                deferredParticipations = deferred.size,
            )

        companion object {
            const val CURRENT_VERSION = 1
        }
    }

private fun PlanningCommitment.isStrategicOwned(strategicScopeIds: Set<String>): Boolean =
    provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE ||
        (scopeId.value in strategicScopeIds && role == PlanningRoleCode.FOCUS)

internal fun PlanningScope.sameStrategicPlanningFacts(other: PlanningScope): Boolean = this == other

internal fun StrategicPlanningScopeExtension.sameStrategicPlanningFacts(
    other: StrategicPlanningScopeExtension,
): Boolean = this == other

internal fun PlanningCommitment.sameStrategicPlanningFacts(other: PlanningCommitment): Boolean = this == other
