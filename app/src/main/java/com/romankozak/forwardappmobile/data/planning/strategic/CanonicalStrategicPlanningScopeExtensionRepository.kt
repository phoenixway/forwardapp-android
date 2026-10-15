package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.planning.PlanningDao
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtensionDependency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalStrategicPlanningScopeExtensionRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: StrategicPlanningScopeExtensionDao,
        private val planningDao: PlanningDao,
    ) {
        suspend fun get(scopeId: PlanningScopeId): StrategicPlanningScopeExtension? =
            dao.get(scopeId.value)?.toDomainStrict()

        suspend fun getAll(): List<StrategicPlanningScopeExtension> =
            dao.getAll().map { it.toDomainStrict() }

        suspend fun save(extension: StrategicPlanningScopeExtension) {
            require(!extension.isDeleted) { "Use tombstone for deletion" }
            database.withTransaction {
                val existing = dao.get(extension.scopeId.value)?.toDomainStrict()
                if (existing == extension) return@withTransaction
                require(existing?.isDeleted != true) { "Use restore for a tombstoned Strategic extension" }
                existing?.let { requireForwardRevision(it, extension) }
                requireValidDependency(extension)
                dao.upsert(extension.toEntity())
            }
        }

        suspend fun restore(extension: StrategicPlanningScopeExtension) {
            require(!extension.isDeleted) { "Restored Strategic extension must be live" }
            database.withTransaction {
                val existing =
                    requireNotNull(dao.get(extension.scopeId.value)?.toDomainStrict()) {
                        "Strategic planning extension does not exist"
                    }
                require(existing.isDeleted) { "Strategic planning extension is already live" }
                requireForwardRevision(existing, extension)
                requireValidDependency(extension)
                dao.upsert(extension.toEntity())
            }
        }

        suspend fun tombstone(
            scopeId: PlanningScopeId,
            now: Long = System.currentTimeMillis(),
        ): StrategicPlanningScopeExtension =
            database.withTransaction {
                val current =
                    requireNotNull(dao.get(scopeId.value)?.toDomainStrict()) {
                        "Strategic planning extension does not exist"
                    }
                if (current.isDeleted) return@withTransaction current
                require(current.version < Long.MAX_VALUE) { "Strategic extension version is exhausted" }
                current.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = current.version + 1L,
                    isDeleted = true,
                ).also { dao.upsert(it.toEntity()) }
            }

        private suspend fun requireValidDependency(extension: StrategicPlanningScopeExtension) {
            val scope = planningDao.getScope(extension.scopeId.value)?.toPlanningScopeStrict()
            val violations = validateStrategicPlanningScopeExtensionDependency(extension, scope)
            require(violations.isEmpty()) { "Strategic planning extension violates V3 contract: $violations" }
        }
    }

private fun requireForwardRevision(
    current: StrategicPlanningScopeExtension,
    incoming: StrategicPlanningScopeExtension,
) {
    require(incoming.createdAt == current.createdAt) { "Strategic extension createdAt is immutable" }
    require(incoming.version > current.version) { "Strategic extension update must advance version" }
}
