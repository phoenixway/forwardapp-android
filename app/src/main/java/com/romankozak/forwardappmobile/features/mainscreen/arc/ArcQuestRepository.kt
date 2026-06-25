package com.romankozak.forwardappmobile.features.mainscreen.arc

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArcQuestRepository
    @Inject
    constructor(
        private val dao: ArcQuestDao,
    ) {
        fun observeArcQuests(arcKey: String): Flow<List<ArcQuestEntity>> = dao.observeArcQuests(arcKey)

        suspend fun getById(id: String): ArcQuestEntity? = dao.getById(id)

        suspend fun addQuest(quest: ArcQuestEntity) {
            val order = dao.getMinOrder(quest.arcKey) - 1
            dao.insert(quest.copy(order = order, updatedAt = System.currentTimeMillis(), version = 1))
        }

        suspend fun updateQuest(quest: ArcQuestEntity) {
            dao.update(
                quest.copy(
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = quest.version + 1,
                ),
            )
        }

        suspend fun deleteQuest(quest: ArcQuestEntity) {
            updateQuest(quest.copy(isDeleted = true))
        }

        suspend fun reorder(quests: List<ArcQuestEntity>) {
            val now = System.currentTimeMillis()
            quests.forEachIndexed { index, quest ->
                dao.updateOrder(questId = quest.id, order = index.toLong(), updatedAt = now)
            }
        }
    }
