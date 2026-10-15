package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.planning.PlanningDao
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateTacticalPlanningScopeExtensionDependency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalTacticalPlanningScopeExtensionRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: TacticalPlanningScopeExtensionDao,
        private val planningDao: PlanningDao,
    ) {
        suspend fun get(scopeId: PlanningScopeId): TacticalPlanningScopeExtension? =
            dao.get(scopeId.value)?.toDomainStrict()

        suspend fun save(extension: TacticalPlanningScopeExtension) {
            require(!extension.isDeleted) { "Use tombstone for deletion" }
            database.withTransaction {
                val existing = dao.get(extension.scopeId.value)?.toDomainStrict()
                if (existing == extension) return@withTransaction
                require(existing?.isDeleted != true) { "Use restore for a tombstoned Tactical extension" }
                existing?.let { requireForwardRevision(it, extension) }
                requireValidDependency(extension)
                dao.upsert(extension.toEntity())
            }
        }

        suspend fun restore(extension: TacticalPlanningScopeExtension) {
            require(!extension.isDeleted) { "Restored Tactical extension must be live" }
            database.withTransaction {
                val existing =
                    requireNotNull(dao.get(extension.scopeId.value)?.toDomainStrict()) {
                        "Tactical planning extension does not exist"
                    }
                require(existing.isDeleted) { "Tactical planning extension is already live" }
                requireForwardRevision(existing, extension)
                requireValidDependency(extension)
                dao.upsert(extension.toEntity())
            }
        }

        suspend fun tombstone(
            scopeId: PlanningScopeId,
            now: Long = System.currentTimeMillis(),
        ): TacticalPlanningScopeExtension =
            database.withTransaction {
                val current =
                    requireNotNull(dao.get(scopeId.value)?.toDomainStrict()) {
                        "Tactical planning extension does not exist"
                    }
                if (current.isDeleted) return@withTransaction current
                require(current.version < Long.MAX_VALUE) { "Tactical extension version is exhausted" }
                current.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = current.version + 1L,
                    isDeleted = true,
                ).also { dao.upsert(it.toEntity()) }
            }

        private suspend fun requireValidDependency(extension: TacticalPlanningScopeExtension) {
            val scope = planningDao.getScope(extension.scopeId.value)?.toPlanningScopeStrict()
            val violations = validateTacticalPlanningScopeExtensionDependency(extension, scope)
            require(violations.isEmpty()) { "Tactical planning extension violates V3 contract: $violations" }
        }
    }

private fun requireForwardRevision(
    current: TacticalPlanningScopeExtension,
    incoming: TacticalPlanningScopeExtension,
) {
    require(incoming.createdAt == current.createdAt) { "Tactical extension createdAt is immutable" }
    require(incoming.version > current.version) { "Tactical extension update must advance version" }
}
