package com.romankozak.forwardappmobile.features.missions.domain.repository

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalActivitySlot
import com.romankozak.forwardappmobile.features.missions.data.TacticalActivitySlotDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TacticalActivitySlotRepository
    @Inject
    constructor(
        private val tacticalActivitySlotDao: TacticalActivitySlotDao,
    ) {
        fun observeSlots(): Flow<List<TacticalActivitySlot>> = tacticalActivitySlotDao.observeSlots()

        suspend fun isActivitySlotContext(contextId: String): Boolean =
            tacticalActivitySlotDao.getActiveSlotForContext(contextId) != null

        suspend fun addSlot(contextId: String) {
            val existing = tacticalActivitySlotDao.getSlotForContext(contextId)
            val now = System.currentTimeMillis()
            if (existing != null) {
                if (!existing.isDeleted) return
                tacticalActivitySlotDao.updateSlot(
                    existing.copy(
                        isDeleted = false,
                        slotOrder = tacticalActivitySlotDao.getMaxSlotOrder() + 1L,
                        updatedAt = now,
                        syncedAt = null,
                        version = existing.version + 1L,
                    ),
                )
                return
            }
            tacticalActivitySlotDao.insertSlot(
                TacticalActivitySlot(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    slotOrder = tacticalActivitySlotDao.getMaxSlotOrder() + 1L,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                ),
            )
        }

        suspend fun removeSlot(contextId: String) {
            tacticalActivitySlotDao.softDeleteSlotByContextId(
                contextId = contextId,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }
