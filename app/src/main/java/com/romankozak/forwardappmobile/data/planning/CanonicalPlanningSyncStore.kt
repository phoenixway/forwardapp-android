package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

class CanonicalPlanningSyncStore(
    private val database: AppDatabase,
) {
    private val dao: PlanningDao
        get() = database.planningDao()

    private val roleRegistry = PlanningRoleRegistry.INITIAL

    suspend fun loadAllScopes(): List<PlanningScopeSnapshot> =
        dao.getAllScopes().map { it.toPlanningScopeStrict().toTransportSnapshot() }

    suspend fun loadAllCommitments(): List<PlanningCommitmentSnapshot> =
        dao.getAllCommitments().map {
            it.toPlanningCommitmentStrict(roleRegistry).toTransportSnapshot()
        }

    suspend fun loadUnsyncedScopes(): List<PlanningScopeSnapshot> =
        dao.getUnsyncedScopes().map { it.toPlanningScopeStrict().toTransportSnapshot() }

    suspend fun loadUnsyncedCommitments(): List<PlanningCommitmentSnapshot> =
        dao.getUnsyncedCommitments().map {
            it.toPlanningCommitmentStrict(roleRegistry).toTransportSnapshot()
        }

    suspend fun loadScopesChangedSince(timestamp: Long): List<PlanningScopeSnapshot> =
        dao.getScopesChangedSince(timestamp).map {
            it.toPlanningScopeStrict().toTransportSnapshot()
        }

    suspend fun loadCommitmentsChangedSince(timestamp: Long): List<PlanningCommitmentSnapshot> =
        dao.getCommitmentsChangedSince(timestamp).map {
            it.toPlanningCommitmentStrict(roleRegistry).toTransportSnapshot()
        }

    suspend fun mergeIncoming(
        scopes: List<PlanningScopeSnapshot>?,
        commitments: List<PlanningCommitmentSnapshot>?,
        relations: List<com.romankozak.forwardappmobile.shared.core.models.PlanningCommitmentRelationRecord>? = null,
    ) {
        if (scopes == null && commitments == null && relations == null) return
        require(scopes != null && commitments != null) {
            "Canonical Planning merge requires atomic planningScopes/planningCommitments presence."
        }

        database.withTransaction {
            val incomingScopes = decodeScopesStrict(scopes)
            val incomingCommitments = decodeCommitmentsStrict(commitments)

            val localScopes =
                dao.getAllScopes().map { it.toPlanningScopeStrict() }.associateBy { it.id }
            val localCommitments =
                dao.getAllCommitments()
                    .map { it.toPlanningCommitmentStrict(roleRegistry) }
                    .associateBy { it.id }

            incomingScopes.forEach { incoming ->
                localScopes[incoming.id]?.let { requireStableScopeIdentity(it, incoming) }
            }
            incomingCommitments.forEach { incoming ->
                localCommitments[incoming.id]?.let { requireStableCommitmentIdentity(it, incoming) }
            }

            val prospectiveScopes = localScopes.toMutableMap()
            val prospectiveCommitments = localCommitments.toMutableMap()
            val scopeWinners = mutableListOf<PlanningScope>()
            val commitmentWinners = mutableListOf<PlanningCommitment>()

            incomingScopes.forEach { incoming ->
                val local = localScopes[incoming.id]
                val winner =
                    when {
                        local == null -> incoming
                        incoming.version > local.version -> incoming
                        incoming.version < local.version -> null
                        incoming.sameTransportSemanticState(local) -> null
                        else ->
                            throw PlanningMergeConflictException(
                                "Equal-version Planning scope divergence for ${incoming.id.value}",
                            )
                    }

                winner?.copy(syncedAt = null)?.let {
                    prospectiveScopes[it.id] = it
                    scopeWinners += it
                }
            }

            incomingCommitments.forEach { incoming ->
                val local = localCommitments[incoming.id]
                val winner =
                    when {
                        local == null -> incoming
                        incoming.version > local.version -> incoming
                        incoming.version < local.version -> null
                        incoming.sameTransportSemanticState(local) -> null
                        else ->
                            throw PlanningMergeConflictException(
                                "Equal-version Planning commitment divergence for ${incoming.id.value}",
                            )
                    }

                winner?.copy(syncedAt = null)?.let {
                    prospectiveCommitments[it.id] = it
                    commitmentWinners += it
                }
            }

            requireValidProspectiveState(
                prospectiveScopes.values,
                prospectiveCommitments.values,
            )

            scopeWinners.forEach { dao.upsertScope(it.toPlanningScopeEntity()) }
            commitmentWinners.forEach { dao.upsertCommitment(it.toPlanningCommitmentEntity()) }
            CanonicalPlanningCommitmentRelationSyncStore(database).merge(relations)
        }
    }

    fun decodeAndValidateForRestore(
        scopes: List<PlanningScopeSnapshot>,
        commitments: List<PlanningCommitmentSnapshot>,
        liveOrientationIds: Set<String>,
    ): CanonicalPlanningDecodedPayload {
        val decodedScopes = decodeScopesStrict(scopes)
        val decodedCommitments = decodeCommitmentsStrict(commitments)

        requireValidProspectiveDecodedState(
            decodedScopes,
            decodedCommitments,
            liveOrientationIds::contains,
        )

        return CanonicalPlanningDecodedPayload(decodedScopes, decodedCommitments)
    }

    suspend fun restoreExactDecoded(payload: CanonicalPlanningDecodedPayload,
        relations: List<com.romankozak.forwardappmobile.shared.core.models.PlanningCommitmentRelationRecord>? = null) = database.withTransaction {
        requireValidProspectiveState(payload.scopes, payload.commitments)
        payload.scopes.forEach { dao.upsertScope(it.toPlanningScopeEntity()) }
        payload.commitments.forEach { dao.upsertCommitment(it.toPlanningCommitmentEntity()) }
        val composition = CanonicalPlanningCommitmentRelationSyncStore(database)
        if (relations == null) composition.merge(null) else composition.restore(relations)
    }

    suspend fun markSynced(
        scopes: List<PlanningScopeSyncVersion>,
        commitments: List<PlanningCommitmentSyncVersion>,
    ) {
        if (scopes.isEmpty() && commitments.isEmpty()) return

        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            scopes.forEach {
                dao.markScopeSyncedIfVersionMatches(it.id, it.version, syncedAt)
            }
            commitments.forEach {
                dao.markCommitmentSyncedIfVersionMatches(it.id, it.version, syncedAt)
            }
        }
    }

    private suspend fun requireValidProspectiveState(
        scopes: Collection<PlanningScope>,
        commitments: Collection<PlanningCommitment>,
    ) {
        val liveOrientationIds =
            database.orientationDao()
                .getAllManagedSubjects()
                .asSequence()
                .filter { it.subjectType == "ORIENTATION" && !it.isDeleted }
                .map { it.id }
                .toSet()
                .intersect(
                    database.orientationDao()
                        .getAllOrientations()
                        .mapTo(hashSetOf()) { it.subjectId },
                )

        requireValidProspectiveDecodedState(
            scopes,
            commitments,
            liveOrientationIds::contains,
        )
    }

    private fun requireValidProspectiveDecodedState(
        scopes: Collection<PlanningScope>,
        commitments: Collection<PlanningCommitment>,
        isOrientationLive: (String) -> Boolean,
    ) {
        val violations =
            validateProspectivePlanningState(
                scopes = scopes,
                commitments = commitments,
                isOrientationLive = isOrientationLive,
                roleRegistry = roleRegistry,
            )
        require(violations.isEmpty()) {
            "Canonical Planning payload violates the V3 contract: ${violations.first()}"
        }
    }

    private fun decodeScopesStrict(
        snapshots: List<PlanningScopeSnapshot>,
    ): List<PlanningScope> {
        require(snapshots.map { it.id }.toSet().size == snapshots.size) {
            "Canonical Planning scope payload contains duplicate ids."
        }
        return snapshots.map { it.toPlanningScopeTransportStrict() }
    }

    private fun decodeCommitmentsStrict(
        snapshots: List<PlanningCommitmentSnapshot>,
    ): List<PlanningCommitment> {
        require(snapshots.map { it.id }.toSet().size == snapshots.size) {
            "Canonical Planning commitment payload contains duplicate ids."
        }
        return snapshots.map { it.toPlanningCommitmentTransportStrict(roleRegistry) }
    }
}

data class CanonicalPlanningDecodedPayload(
    val scopes: List<PlanningScope>,
    val commitments: List<PlanningCommitment>,
)

class PlanningMergeConflictException(
    message: String,
) : IllegalArgumentException(message)

private fun PlanningScopeSnapshot.toPlanningScopeTransportStrict(): PlanningScope =
    try {
        PlanningScope(
            id = PlanningScopeId(id),
            kind = kind.toPlanningScopeKindStable(),
            lifecycle = lifecycle.toPlanningScopeLifecycleStable(),
            title = title,
            startsAt = startsAt,
            endsAt = endsAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also {
            val violations = validatePlanningScope(it)
            require(violations.isEmpty()) { violations.first().message }
        }
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException("Malformed Planning scope snapshot id=$id", failure)
    }

private fun PlanningCommitmentSnapshot.toPlanningCommitmentTransportStrict(
    roleRegistry: PlanningRoleRegistry,
): PlanningCommitment =
    try {
        PlanningCommitment(
            id = PlanningCommitmentId(id),
            scopeId = PlanningScopeId(scopeId),
            orientationId = orientationId,
            role = PlanningRoleCode.fromStableCode(role),
            order = order,
            priorityLevel = priorityLevel?.toPlanningPriorityLevelStable(),
            status = status.toPlanningCommitmentStatusStable(),
            provenance =
                PlanningProvenance(
                    kind = provenanceKind.toPlanningProvenanceKindStable(),
                    sourceType = provenanceSourceType,
                    sourceId = provenanceSourceId,
                ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also {
            val violations = validatePlanningCommitment(it, roleRegistry)
            require(violations.isEmpty()) { violations.first().message }
        }
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException("Malformed Planning commitment snapshot id=$id", failure)
    }

internal fun PlanningScope.toTransportSnapshot(): PlanningScopeSnapshot =
    PlanningScopeSnapshot(
        id = id.value,
        kind = kind.toStableCode(),
        lifecycle = lifecycle.toStableCode(),
        title = title,
        startsAt = startsAt,
        endsAt = endsAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

internal fun PlanningCommitment.toTransportSnapshot(): PlanningCommitmentSnapshot =
    PlanningCommitmentSnapshot(
        id = id.value,
        scopeId = scopeId.value,
        orientationId = orientationId,
        role = role.value,
        order = order,
        priorityLevel = priorityLevel?.toStableCode(),
        status = status.toStableCode(),
        provenanceKind = provenance.kind.toStableCode(),
        provenanceSourceType = provenance.sourceType,
        provenanceSourceId = provenance.sourceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun requireStableScopeIdentity(
    local: PlanningScope,
    incoming: PlanningScope,
) {
    require(local.kind == incoming.kind && local.createdAt == incoming.createdAt) {
        "Planning scope durable identity cannot change for ${local.id.value}"
    }
}

private fun requireStableCommitmentIdentity(
    local: PlanningCommitment,
    incoming: PlanningCommitment,
) {
    require(
        local.scopeId == incoming.scopeId &&
            local.orientationId == incoming.orientationId &&
            local.role == incoming.role &&
            local.createdAt == incoming.createdAt,
    ) {
        "Planning commitment durable identity cannot change for ${local.id.value}"
    }
}

private fun PlanningScope.sameTransportSemanticState(other: PlanningScope): Boolean =
    id == other.id &&
        kind == other.kind &&
        lifecycle == other.lifecycle &&
        title == other.title &&
        startsAt == other.startsAt &&
        endsAt == other.endsAt &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        version == other.version &&
        isDeleted == other.isDeleted

private fun PlanningCommitment.sameTransportSemanticState(
    other: PlanningCommitment,
): Boolean =
    id == other.id &&
        scopeId == other.scopeId &&
        orientationId == other.orientationId &&
        role == other.role &&
        order == other.order &&
        priorityLevel == other.priorityLevel &&
        status == other.status &&
        provenance == other.provenance &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        version == other.version &&
        isDeleted == other.isDeleted
