package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.DirectionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectionRepository @Inject constructor(private val directionDao: DirectionDao) {

    fun getDirectionItemsForContext(contextId: String): Flow<List<DirectionItemEntity>> {
        return directionDao.getDirectionItemsForContext(contextId)
    }

    suspend fun addDirectionItem(contextId: String, text: String) {
        val count = directionDao.count(contextId)
        val now = System.currentTimeMillis()
        val newItem = DirectionItemEntity(
            contextId = contextId,
            text = text,
            itemOrder = count + 1,
            updatedAt = now,
            version = 1
        )
        directionDao.insert(newItem)
    }

    suspend fun updateDirectionItem(item: DirectionItemEntity) {
        val now = System.currentTimeMillis()
        directionDao.update(
            item.copy(
                updatedAt = now,
                version = item.version + 1,
            ),
        )
    }

    suspend fun updateAll(items: List<DirectionItemEntity>) {
        val now = System.currentTimeMillis()
        directionDao.updateAll(
            items.map { item ->
                item.copy(
                    updatedAt = now,
                    version = item.version + 1,
                )
            },
        )
    }

    suspend fun deleteDirectionItem(itemId: String) {
        val now = System.currentTimeMillis()
        val existing = directionDao.getById(itemId) ?: return
        directionDao.markDeleted(itemId, now, existing.version + 1)
    }
}
