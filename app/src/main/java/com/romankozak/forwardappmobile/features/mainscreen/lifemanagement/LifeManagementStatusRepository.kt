package com.romankozak.forwardappmobile.features.mainscreen.lifemanagement

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.FreshnessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.GeneralStatus
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelId
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelStatusEntity
import com.romankozak.forwardappmobile.core.data.models.entities.TransferStatus
import com.romankozak.forwardappmobile.data.dao.LifeManagementLevelStatusDao
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeManagementStatusRepository
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val dao: LifeManagementLevelStatusDao,
        private val dependencyResolver: LifeManagementStatusDependencyResolver,
    ) {
        fun observeStatuses(): Flow<List<LifeManagementLevelStatus>> =
            dao.observeAll().map { entities ->
                entities
                    .sortedBy { it.levelId.order }
                    .map(LifeManagementLevelStatusEntity::toModel)
            }

        suspend fun ensureDefaults() {
            appDatabase.withTransaction {
                val existing = dao.getAll()
                if (existing.size == LifeManagementLevelId.entries.size) {
                    return@withTransaction
                }

                val existingByLevel = existing.associateBy { it.levelId }
                val now = System.currentTimeMillis()
                val merged =
                    LifeManagementLevelId.entries.map { levelId ->
                        existingByLevel[levelId]
                            ?: LifeManagementLevelStatusEntity(
                                levelId = levelId,
                                generalStatus = GeneralStatus.CONDITIONAL,
                                transferStatus = TransferStatus.NONE,
                                freshnessStatus = FreshnessStatus.NEEDS_REVIEW,
                                blockerText = null,
                                nextActionText = null,
                                updatedAt = now,
                            )
                    }
                dao.upsertAll(merged)
            }
        }

        suspend fun updateStatus(update: LifeManagementLevelStatusUpdate) {
            val now = System.currentTimeMillis()
            appDatabase.withTransaction {
                dao.upsertAll(
                    listOf(
                        LifeManagementLevelStatusEntity(
                            levelId = update.levelId,
                            generalStatus = update.generalStatus,
                            transferStatus = update.transferStatus,
                            freshnessStatus = update.freshnessStatus,
                            blockerText = update.blockerText.normalizeOptionalText(),
                            nextActionText = update.nextActionText.normalizeOptionalText(),
                            updatedAt = now,
                        ),
                    ),
                )

                val descendants = dependencyResolver.descendantsOf(update.levelId)
                val descendantFreshness = dependencyResolver.freshnessForDescendantsOf(update.levelId)
                if (descendants.isNotEmpty() && descendantFreshness != null) {
                    dao.updateFreshnessForLevels(
                        levelIds = descendants,
                        freshnessStatus = descendantFreshness,
                        updatedAt = now,
                    )
                }
            }
        }
    }

private fun LifeManagementLevelStatusEntity.toModel(): LifeManagementLevelStatus =
    LifeManagementLevelStatus(
        levelId = levelId,
        generalStatus = generalStatus,
        transferStatus = transferStatus,
        freshnessStatus = freshnessStatus,
        blockerText = blockerText.orEmpty(),
        nextActionText = nextActionText.orEmpty(),
        updatedAt = updatedAt,
    )

private fun String.normalizeOptionalText(): String? = trim().takeIf { it.isNotEmpty() }
