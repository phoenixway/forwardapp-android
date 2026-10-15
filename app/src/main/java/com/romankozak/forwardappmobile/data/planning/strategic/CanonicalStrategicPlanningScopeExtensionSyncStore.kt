package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.StrategicPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.StrategicPlanningScopeExtensionSyncVersion
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtensionDependency

class CanonicalStrategicPlanningScopeExtensionSyncStore(
    private val database: AppDatabase,
) {
    private val dao get() = database.strategicPlanningScopeExtensionDao()

    suspend fun loadAll() = dao.getAll().map { it.toDomainStrict().toSnapshot() }
    suspend fun loadUnsynced() = dao.getUnsynced().map { it.toDomainStrict().toSnapshot() }
    suspend fun loadChangedSince(timestamp: Long) =
        dao.getChangedSince(timestamp).map { it.toDomainStrict().toSnapshot() }

    suspend fun mergeIncoming(snapshots: List<StrategicPlanningScopeExtensionSnapshot>?) {
        database.withTransaction {
            val incoming = decodeUnique(snapshots.orEmpty())
            val local = dao.getAll().map { it.toDomainStrict() }.associateBy { it.scopeId }
            val scopes = loadScopes()
            val prospective = local.toMutableMap()
            val winners = mutableListOf<StrategicPlanningScopeExtension>()
            incoming.forEach { candidate ->
                val current = local[candidate.scopeId]
                current?.let {
                    require(it.createdAt == candidate.createdAt) {
                        "Strategic extension createdAt cannot change"
                    }
                }
                val winner =
                    when {
                        current == null -> candidate
                        candidate.version > current.version -> candidate
                        candidate.version < current.version -> null
                        candidate.sameTransportState(current) -> null
                        else -> throw StrategicPlanningExtensionMergeConflictException(
                            "Equal-version Strategic planning extension divergence for ${candidate.scopeId.value}",
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
        snapshots: List<StrategicPlanningScopeExtensionSnapshot>,
        scopes: Collection<PlanningScope>,
    ): List<StrategicPlanningScopeExtension> {
        val scopesById = scopes.associateBy { it.id }
        return decodeUnique(snapshots).onEach { requireValidDependency(it, scopesById[it.scopeId]) }
    }

    suspend fun restoreExactDecoded(extensions: List<StrategicPlanningScopeExtension>) {
        val scopes = loadScopes()
        extensions.forEach {
            requireValidDependency(it, scopes[it.scopeId])
            dao.upsert(it.toEntity())
        }
    }

    suspend fun markSynced(versions: List<StrategicPlanningScopeExtensionSyncVersion>) {
        if (versions.isEmpty()) return
        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            versions.forEach { dao.markSyncedIfVersionMatches(it.scopeId, it.version, syncedAt) }
        }
    }

    private suspend fun loadScopes(): Map<PlanningScopeId, PlanningScope> =
        database.planningDao().getAllScopes().map { it.toPlanningScopeStrict() }.associateBy { it.id }

    private fun decodeUnique(
        snapshots: List<StrategicPlanningScopeExtensionSnapshot>,
    ): List<StrategicPlanningScopeExtension> {
        require(snapshots.map { it.scopeId }.toSet().size == snapshots.size) {
            "Strategic planning extension payload contains duplicate scope ids."
        }
        return snapshots.map { it.toDomainStrict() }
    }
}

class StrategicPlanningExtensionMergeConflictException(message: String) : IllegalArgumentException(message)

internal fun StrategicPlanningScopeExtensionSnapshot.toDomainStrict(): StrategicPlanningScopeExtension =
    try {
        StrategicPlanningScopeExtension(
            scopeId = PlanningScopeId(scopeId),
            legacyArcKey = legacyArcKey,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also { extension ->
            require(validateStrategicPlanningScopeExtension(extension).isEmpty()) {
                "Strategic planning extension snapshot violates the V3 contract"
            }
        }
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException("Malformed Strategic planning extension snapshot scopeId=$scopeId", failure)
    }

internal fun StrategicPlanningScopeExtension.toSnapshot() =
    StrategicPlanningScopeExtensionSnapshot(
        scopeId = scopeId.value,
        legacyArcKey = legacyArcKey,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun requireValidDependency(extension: StrategicPlanningScopeExtension, scope: PlanningScope?) {
    val violations = validateStrategicPlanningScopeExtensionDependency(extension, scope)
    require(violations.isEmpty()) { "Strategic planning extension violates V3 contract: $violations" }
}

private fun StrategicPlanningScopeExtension.sameTransportState(other: StrategicPlanningScopeExtension): Boolean =
    scopeId == other.scopeId && legacyArcKey == other.legacyArcKey && createdAt == other.createdAt &&
        updatedAt == other.updatedAt && version == other.version && isDeleted == other.isDeleted
