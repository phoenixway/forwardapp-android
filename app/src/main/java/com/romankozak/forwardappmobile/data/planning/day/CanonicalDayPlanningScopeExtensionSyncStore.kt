package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.DayPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.DayPlanningScopeExtensionSyncVersion
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateDayPlanningScopeExtensionDependency

class CanonicalDayPlanningScopeExtensionSyncStore(
    private val database: AppDatabase,
) {
    private val dao: DayPlanningScopeExtensionDao
        get() = database.dayPlanningScopeExtensionDao()

    suspend fun loadAll(): List<DayPlanningScopeExtensionSnapshot> =
        dao.getAll().map { it.toDomainStrict().toSnapshot() }

    suspend fun loadUnsynced(): List<DayPlanningScopeExtensionSnapshot> =
        dao.getUnsynced().map { it.toDomainStrict().toSnapshot() }

    suspend fun loadChangedSince(timestamp: Long): List<DayPlanningScopeExtensionSnapshot> =
        dao.getChangedSince(timestamp).map { it.toDomainStrict().toSnapshot() }

    suspend fun mergeIncoming(snapshots: List<DayPlanningScopeExtensionSnapshot>?) {
        database.withTransaction {
            val incoming = decodeUnique(snapshots.orEmpty())
            val local = dao.getAll().map { it.toDomainStrict() }.associateBy { it.scopeId }
            val scopes = loadScopes()
            val prospective = local.toMutableMap()
            val winners = mutableListOf<DayPlanningScopeExtension>()

            incoming.forEach { candidate ->
                val current = local[candidate.scopeId]
                current?.let { requireStableIdentity(it, candidate) }
                val winner =
                    when {
                        current == null -> candidate
                        candidate.version > current.version -> candidate
                        candidate.version < current.version -> null
                        candidate.sameTransportState(current) -> null
                        else ->
                            throw DayPlanningExtensionMergeConflictException(
                                "Equal-version Day planning extension divergence for ${candidate.scopeId.value}",
                            )
                    }
                winner?.copy(syncedAt = null)?.let { accepted ->
                    prospective[accepted.scopeId] = accepted
                    winners += accepted
                }
            }
            prospective.values.forEach { extension ->
                requireValidDependency(extension, scopes[extension.scopeId])
            }
            winners.forEach { accepted -> dao.upsert(accepted.toEntity()) }
        }
    }

    fun decodeAndValidateForRestore(
        snapshots: List<DayPlanningScopeExtensionSnapshot>,
        scopes: Collection<PlanningScope>,
    ): List<DayPlanningScopeExtension> {
        val scopesById = scopes.associateBy { it.id }
        return decodeUnique(snapshots).onEach { extension ->
            requireValidDependency(extension, scopesById[extension.scopeId])
        }
    }

    suspend fun restoreExactDecoded(extensions: List<DayPlanningScopeExtension>) {
        val scopes = loadScopes()
        extensions.forEach { extension ->
            requireValidDependency(extension, scopes[extension.scopeId])
            dao.upsert(extension.toEntity())
        }
    }

    suspend fun markSynced(versions: List<DayPlanningScopeExtensionSyncVersion>) {
        if (versions.isEmpty()) return
        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            versions.forEach { version ->
                dao.markSyncedIfVersionMatches(version.scopeId, version.version, syncedAt)
            }
        }
    }

    private suspend fun loadScopes(): Map<PlanningScopeId, PlanningScope> =
        database.planningDao().getAllScopes()
            .map { it.toPlanningScopeStrict() }
            .associateBy { it.id }

    private fun decodeUnique(
        snapshots: List<DayPlanningScopeExtensionSnapshot>,
    ): List<DayPlanningScopeExtension> {
        require(snapshots.map { it.scopeId }.toSet().size == snapshots.size) {
            "Day planning extension payload contains duplicate scope ids."
        }
        return snapshots.map { it.toDomainStrict() }
    }
}

class DayPlanningExtensionMergeConflictException(message: String) :
    IllegalArgumentException(message)

internal fun DayPlanningScopeExtensionSnapshot.toDomainStrict(): DayPlanningScopeExtension =
    try {
        DayPlanningScopeExtension(
            scopeId = PlanningScopeId(scopeId),
            calendarDayStartMillis = calendarDayStartMillis,
            outcome = outcome?.toDayPlanningOutcomeStable(),
            plannedBudgetMinutes = plannedBudgetMinutes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        )
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException("Malformed Day planning extension snapshot scopeId=$scopeId", failure)
    }

internal fun DayPlanningScopeExtension.toSnapshot(): DayPlanningScopeExtensionSnapshot =
    DayPlanningScopeExtensionSnapshot(
        scopeId = scopeId.value,
        calendarDayStartMillis = calendarDayStartMillis,
        outcome = outcome?.toStableCode(),
        plannedBudgetMinutes = plannedBudgetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

private fun requireValidDependency(
    extension: DayPlanningScopeExtension,
    scope: PlanningScope?,
) {
    val violations = validateDayPlanningScopeExtensionDependency(extension, scope)
    require(violations.isEmpty()) { "Day planning extension violates V3 contract: $violations" }
}

private fun requireStableIdentity(
    local: DayPlanningScopeExtension,
    incoming: DayPlanningScopeExtension,
) {
    require(local.createdAt == incoming.createdAt) {
        "Day planning extension createdAt cannot change for ${local.scopeId.value}"
    }
}

private fun DayPlanningScopeExtension.sameTransportState(other: DayPlanningScopeExtension): Boolean =
    scopeId == other.scopeId &&
        calendarDayStartMillis == other.calendarDayStartMillis &&
        outcome == other.outcome &&
        plannedBudgetMinutes == other.plannedBudgetMinutes &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        version == other.version &&
        isDeleted == other.isDeleted
