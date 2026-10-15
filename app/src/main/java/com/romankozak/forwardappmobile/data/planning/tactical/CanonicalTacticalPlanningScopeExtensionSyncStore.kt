package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.TacticalPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.TacticalPlanningScopeExtensionSyncVersion
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateTacticalPlanningScopeExtensionDependency

class CanonicalTacticalPlanningScopeExtensionSyncStore(
    private val database: AppDatabase,
) {
    private val dao get() = database.tacticalPlanningScopeExtensionDao()

    suspend fun loadAll() = dao.getAll().map { it.toDomainStrict().toSnapshot() }
    suspend fun loadUnsynced() = dao.getUnsynced().map { it.toDomainStrict().toSnapshot() }
    suspend fun loadChangedSince(timestamp: Long) =
        dao.getChangedSince(timestamp).map { it.toDomainStrict().toSnapshot() }

    suspend fun mergeIncoming(snapshots: List<TacticalPlanningScopeExtensionSnapshot>?) {
        database.withTransaction {
            val incoming = decodeUnique(snapshots.orEmpty())
            val local = dao.getAll().map { it.toDomainStrict() }.associateBy { it.scopeId }
            val scopes = loadScopes()
            val prospective = local.toMutableMap()
            val winners = mutableListOf<TacticalPlanningScopeExtension>()
            incoming.forEach { candidate ->
                val current = local[candidate.scopeId]
                current?.let { require(it.createdAt == candidate.createdAt) { "Tactical extension createdAt cannot change" } }
                val winner =
                    when {
                        current == null -> candidate
                        candidate.version > current.version -> candidate
                        candidate.version < current.version -> null
                        candidate.sameTransportState(current) -> null
                        else -> throw TacticalPlanningExtensionMergeConflictException(
                            "Equal-version Tactical planning extension divergence for ${candidate.scopeId.value}",
                        )
                    }
                winner?.copy(syncedAt = null)?.let { accepted ->
                    prospective[accepted.scopeId] = accepted
                    winners += accepted
                }
            }
            prospective.values.forEach { requireValidDependency(it, scopes[it.scopeId]) }
            winners.forEach { dao.upsert(it.toEntity()) }
        }
    }

    fun decodeAndValidateForRestore(
        snapshots: List<TacticalPlanningScopeExtensionSnapshot>,
        scopes: Collection<PlanningScope>,
    ): List<TacticalPlanningScopeExtension> {
        val scopesById = scopes.associateBy { it.id }
        return decodeUnique(snapshots).onEach { requireValidDependency(it, scopesById[it.scopeId]) }
    }

    suspend fun restoreExactDecoded(extensions: List<TacticalPlanningScopeExtension>) {
        val scopes = loadScopes()
        extensions.forEach {
            requireValidDependency(it, scopes[it.scopeId])
            dao.upsert(it.toEntity())
        }
    }

    suspend fun markSynced(versions: List<TacticalPlanningScopeExtensionSyncVersion>) {
        if (versions.isEmpty()) return
        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            versions.forEach { dao.markSyncedIfVersionMatches(it.scopeId, it.version, syncedAt) }
        }
    }

    private suspend fun loadScopes(): Map<PlanningScopeId, PlanningScope> =
        database.planningDao().getAllScopes().map { it.toPlanningScopeStrict() }.associateBy { it.id }

    private fun decodeUnique(snapshots: List<TacticalPlanningScopeExtensionSnapshot>): List<TacticalPlanningScopeExtension> {
        require(snapshots.map { it.scopeId }.toSet().size == snapshots.size) {
            "Tactical planning extension payload contains duplicate scope ids."
        }
        return snapshots.map { it.toDomainStrict() }
    }
}

class TacticalPlanningExtensionMergeConflictException(message: String) : IllegalArgumentException(message)

internal fun TacticalPlanningScopeExtensionSnapshot.toDomainStrict(): TacticalPlanningScopeExtension =
    try {
        TacticalPlanningScopeExtension(
            scopeId = PlanningScopeId(scopeId),
            type = iterationType.toTacticalPlanningScopeTypeStable(),
            weekKey = weekKey,
            closedAt = closedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        )
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException("Malformed Tactical planning extension snapshot scopeId=$scopeId", failure)
    }

internal fun TacticalPlanningScopeExtension.toSnapshot() =
    TacticalPlanningScopeExtensionSnapshot(
        scopeId = scopeId.value,
        iterationType = type.toStableCode(),
        weekKey = weekKey,
        closedAt = closedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun requireValidDependency(extension: TacticalPlanningScopeExtension, scope: PlanningScope?) {
    val violations = validateTacticalPlanningScopeExtensionDependency(extension, scope)
    require(violations.isEmpty()) { "Tactical planning extension violates V3 contract: $violations" }
}

private fun TacticalPlanningScopeExtension.sameTransportState(other: TacticalPlanningScopeExtension): Boolean =
    scopeId == other.scopeId && type == other.type && weekKey == other.weekKey && closedAt == other.closedAt &&
        createdAt == other.createdAt && updatedAt == other.updatedAt && version == other.version && isDeleted == other.isDeleted
