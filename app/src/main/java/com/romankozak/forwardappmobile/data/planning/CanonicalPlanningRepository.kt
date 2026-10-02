package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.validatePlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateProspectivePlanningState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CanonicalPlanningRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: PlanningDao,
    ) {
        private val roleRegistry = PlanningRoleRegistry.INITIAL

        fun observeScope(id: PlanningScopeId): Flow<PlanningScope?> =
            dao.observeScope(id.value).map { it?.toPlanningScopeStrict() }

        suspend fun getScope(id: PlanningScopeId): PlanningScope? =
            dao.getScope(id.value)?.toPlanningScopeStrict()

        suspend fun saveScope(scope: PlanningScope) {
            require(!scope.isDeleted) { "Use tombstoneScope for deletion" }
            requireValidScope(scope)

            database.withTransaction {
                val existing = dao.getScope(scope.id.value)?.toPlanningScopeStrict()
                if (existing == scope) return@withTransaction

                require(existing?.isDeleted != true) {
                    "Use restoreScope to restore a tombstoned planning scope"
                }
                existing?.let { current ->
                    require(current.kind == scope.kind) {
                        "Planning scope kind is immutable"
                    }
                    requireForwardRevision(
                        currentVersion = current.version,
                        incomingVersion = scope.version,
                        currentCreatedAt = current.createdAt,
                        incomingCreatedAt = scope.createdAt,
                        record = "Planning scope",
                    )
                }
                dao.upsertScope(scope.toPlanningScopeEntity())
            }
        }

        suspend fun restoreScope(scope: PlanningScope) {
            require(!scope.isDeleted) { "Restored planning scope must be live" }
            requireValidScope(scope)

            database.withTransaction {
                val existing =
                    requireNotNull(dao.getScope(scope.id.value)?.toPlanningScopeStrict()) {
                        "Planning scope does not exist"
                    }
                require(existing.isDeleted) { "Planning scope is already live" }
                require(existing.kind == scope.kind) { "Planning scope kind is immutable" }
                requireForwardRevision(
                    currentVersion = existing.version,
                    incomingVersion = scope.version,
                    currentCreatedAt = existing.createdAt,
                    incomingCreatedAt = scope.createdAt,
                    record = "Planning scope",
                )
                dao.upsertScope(scope.toPlanningScopeEntity())
            }
        }

        suspend fun tombstoneScope(
            id: PlanningScopeId,
            now: Long = System.currentTimeMillis(),
        ): PlanningScope =
            database.withTransaction {
                val current =
                    requireNotNull(dao.getScope(id.value)?.toPlanningScopeStrict()) {
                        "Planning scope does not exist"
                    }
                if (current.isDeleted) return@withTransaction current

                val tombstonedCommitments =
                    dao.getLiveCommitmentsForScope(id.value).map { entity ->
                        entity.toPlanningCommitmentStrict(roleRegistry)
                            .tombstoned(now)
                            .toPlanningCommitmentEntity()
                    }
                val tombstonedScope = current.tombstoned(now)

                dao.upsertCommitments(tombstonedCommitments)
                dao.upsertScope(tombstonedScope.toPlanningScopeEntity())
                tombstonedScope
            }

        fun observeLiveCommitments(scopeId: PlanningScopeId): Flow<List<PlanningCommitment>> =
            dao.observeLiveCommitmentsForScope(scopeId.value).map { entities ->
                entities.map { it.toPlanningCommitmentStrict(roleRegistry) }
            }

        suspend fun getCommitment(id: PlanningCommitmentId): PlanningCommitment? =
            dao.getCommitment(id.value)?.toPlanningCommitmentStrict(roleRegistry)

        suspend fun getCommitments(scopeId: PlanningScopeId): List<PlanningCommitment> =
            dao.getCommitmentsForScope(scopeId.value).map {
                it.toPlanningCommitmentStrict(roleRegistry)
            }

        suspend fun getLiveCommitments(
            scopeId: PlanningScopeId,
            role: PlanningRoleCode,
        ): List<PlanningCommitment> =
            dao.getLiveCommitmentsForRole(scopeId.value, role.value).map {
                it.toPlanningCommitmentStrict(roleRegistry)
            }

        suspend fun saveCommitment(commitment: PlanningCommitment) {
            require(!commitment.isDeleted) { "Use tombstoneCommitment for deletion" }

            database.withTransaction {
                val existing =
                    dao.getCommitment(commitment.id.value)
                        ?.toPlanningCommitmentStrict(roleRegistry)
                if (existing == commitment) return@withTransaction

                require(existing?.isDeleted != true) {
                    "Use restoreCommitment to restore a tombstoned planning commitment"
                }
                existing?.let { current ->
                    requireForwardRevision(
                        currentVersion = current.version,
                        incomingVersion = commitment.version,
                        currentCreatedAt = current.createdAt,
                        incomingCreatedAt = commitment.createdAt,
                        record = "Planning commitment",
                    )
                }
                requireValidLiveCommitment(commitment)
                dao.upsertCommitment(commitment.toPlanningCommitmentEntity())
            }
        }

        suspend fun restoreCommitment(commitment: PlanningCommitment) {
            require(!commitment.isDeleted) { "Restored planning commitment must be live" }

            database.withTransaction {
                val existing =
                    requireNotNull(
                        dao.getCommitment(commitment.id.value)
                            ?.toPlanningCommitmentStrict(roleRegistry),
                    ) {
                        "Planning commitment does not exist"
                    }
                require(existing.isDeleted) { "Planning commitment is already live" }
                requireForwardRevision(
                    currentVersion = existing.version,
                    incomingVersion = commitment.version,
                    currentCreatedAt = existing.createdAt,
                    incomingCreatedAt = commitment.createdAt,
                    record = "Planning commitment",
                )
                requireValidLiveCommitment(commitment)
                dao.upsertCommitment(commitment.toPlanningCommitmentEntity())
            }
        }

        suspend fun tombstoneCommitment(
            id: PlanningCommitmentId,
            now: Long = System.currentTimeMillis(),
        ): PlanningCommitment =
            database.withTransaction {
                val current =
                    requireNotNull(
                        dao.getCommitment(id.value)
                            ?.toPlanningCommitmentStrict(roleRegistry),
                    ) {
                        "Planning commitment does not exist"
                    }
                if (current.isDeleted) return@withTransaction current

                val tombstoned = current.tombstoned(now)
                dao.upsertCommitment(tombstoned.toPlanningCommitmentEntity())
                tombstoned
            }

        private suspend fun requireValidLiveCommitment(commitment: PlanningCommitment) {
            val scope = dao.getScope(commitment.scopeId.value)?.toPlanningScopeStrict()
            val related =
                dao.getParticipationRecords(
                    scopeId = commitment.scopeId.value,
                    orientationId = commitment.orientationId,
                    role = commitment.role.value,
                ).filterNot { it.id == commitment.id.value }
                    .map { it.toPlanningCommitmentStrict(roleRegistry) } + commitment
            val orientationLive = dao.isOrientationLive(commitment.orientationId)

            val violations =
                validateProspectivePlanningState(
                    scopes = listOfNotNull(scope),
                    commitments = related,
                    isOrientationLive = { orientationId ->
                        orientationId == commitment.orientationId && orientationLive
                    },
                    roleRegistry = roleRegistry,
                )
            require(violations.isEmpty()) {
                "Planning commitment violates the V3 contract: $violations"
            }
        }

        private fun requireValidScope(scope: PlanningScope) {
            val violations = validatePlanningScope(scope)
            require(violations.isEmpty()) {
                "Planning scope violates the V3 contract: $violations"
            }
        }
    }

private fun requireForwardRevision(
    currentVersion: Long,
    incomingVersion: Long,
    currentCreatedAt: Long,
    incomingCreatedAt: Long,
    record: String,
) {
    require(incomingCreatedAt == currentCreatedAt) {
        "$record createdAt is immutable"
    }
    require(incomingVersion > currentVersion) {
        "$record update must advance version"
    }
}

private fun PlanningScope.tombstoned(now: Long): PlanningScope =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = nextVersion(version),
        isDeleted = true,
    )

private fun PlanningCommitment.tombstoned(now: Long): PlanningCommitment =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = nextVersion(version),
        isDeleted = true,
    )

private fun nextVersion(version: Long): Long {
    require(version < Long.MAX_VALUE) { "Planning record version is exhausted" }
    return version + 1L
}
