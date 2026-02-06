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
        val newItem = DirectionItemEntity(
            contextId = contextId,
            text = text,
            itemOrder = count + 1
        )
        directionDao.insert(newItem)
    }

    suspend fun updateDirectionItem(item: DirectionItemEntity) {
        directionDao.update(item)
    }

    suspend fun updateAll(items: List<DirectionItemEntity>) {
        directionDao.updateAll(items)
    }

    suspend fun deleteDirectionItem(itemId: String) {
        directionDao.delete(itemId)
    }
}
