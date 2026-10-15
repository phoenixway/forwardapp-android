package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.planning.PlanningDao
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateDayPlanningScopeExtensionDependency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalDayPlanningScopeExtensionRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: DayPlanningScopeExtensionDao,
        private val planningDao: PlanningDao,
    ) {
        suspend fun get(scopeId: PlanningScopeId): DayPlanningScopeExtension? =
            dao.get(scopeId.value)?.toDomainStrict()

        suspend fun save(extension: DayPlanningScopeExtension) {
            require(!extension.isDeleted) { "Use tombstone for deletion" }
            database.withTransaction {
                val existing = dao.get(extension.scopeId.value)?.toDomainStrict()
                if (existing == extension) return@withTransaction
                require(existing?.isDeleted != true) { "Use restore for a tombstoned Day extension" }
                existing?.let { requireForwardRevision(it, extension) }
                requireValidDependency(extension)
                dao.upsert(extension.toEntity())
            }
        }

        suspend fun restore(extension: DayPlanningScopeExtension) {
            require(!extension.isDeleted) { "Restored Day extension must be live" }
            database.withTransaction {
                val existing =
                    requireNotNull(dao.get(extension.scopeId.value)?.toDomainStrict()) {
                        "Day planning extension does not exist"
                    }
                require(existing.isDeleted) { "Day planning extension is already live" }
                requireForwardRevision(existing, extension)
                requireValidDependency(extension)
                dao.upsert(extension.toEntity())
            }
        }

        suspend fun tombstone(
            scopeId: PlanningScopeId,
            now: Long = System.currentTimeMillis(),
        ): DayPlanningScopeExtension =
            database.withTransaction {
                val current =
                    requireNotNull(dao.get(scopeId.value)?.toDomainStrict()) {
                        "Day planning extension does not exist"
                    }
                if (current.isDeleted) return@withTransaction current
                require(current.version < Long.MAX_VALUE) { "Day extension version is exhausted" }
                val tombstone =
                    current.copy(
                        updatedAt = now,
                        syncedAt = null,
                        version = current.version + 1L,
                        isDeleted = true,
                    )
                dao.upsert(tombstone.toEntity())
                tombstone
            }

        private suspend fun requireValidDependency(extension: DayPlanningScopeExtension) {
            val scope = planningDao.getScope(extension.scopeId.value)?.toPlanningScopeStrict()
            val violations = validateDayPlanningScopeExtensionDependency(extension, scope)
            require(violations.isEmpty()) { "Day planning extension violates V3 contract: $violations" }
        }
    }

private fun requireForwardRevision(
    current: DayPlanningScopeExtension,
    incoming: DayPlanningScopeExtension,
) {
    require(incoming.createdAt == current.createdAt) { "Day extension createdAt is immutable" }
    require(incoming.version > current.version) { "Day extension update must advance version" }
}
