package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.DirectionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectionRepository
    @Inject
    constructor(private val directionDao: DirectionDao) {
        fun getDirectionItemsForContext(contextId: String): Flow<List<DirectionItemEntity>> {
            return directionDao.getDirectionItemsForContext(contextId)
        }

        suspend fun addDirectionItem(
            contextId: String,
            text: String,
            linkedContextId: String? = null,
        ) {
            val count = directionDao.count(contextId)
            val now = System.currentTimeMillis()
            val newItem =
                DirectionItemEntity(
                    contextId = contextId,
                    text = text,
                    linkedContextId = linkedContextId,
                    itemOrder = count + 1,
                    updatedAt = now,
                    version = 1,
                )
            directionDao.insert(newItem)
        }

        suspend fun addDirectionItems(
            contextId: String,
            items: List<Pair<String, String?>>,
        ): Int {
            if (items.isEmpty()) return 0
            val now = System.currentTimeMillis()
            val startOrder = directionDao.count(contextId)
            val newItems =
                items.mapIndexed { index, (text, linkedContextId) ->
                    DirectionItemEntity(
                        contextId = contextId,
                        text = text,
                        linkedContextId = linkedContextId,
                        itemOrder = startOrder + index + 1,
                        updatedAt = now,
                        version = 1,
                    )
                }
            directionDao.insertAll(newItems)
            return newItems.size
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

        suspend fun deleteDirectionItems(itemIds: List<String>): Int {
            if (itemIds.isEmpty()) return 0
            val now = System.currentTimeMillis()
            var deleted = 0
            itemIds.distinct().forEach { itemId ->
                val existing = directionDao.getById(itemId) ?: return@forEach
                directionDao.markDeleted(itemId, now, existing.version + 1)
                deleted += 1
            }
            return deleted
        }

        suspend fun getDirectionItemsForContextSync(contextId: String): List<DirectionItemEntity> {
            return directionDao.getDirectionItemsForContextSync(contextId)
        }

        suspend fun getDirectionItemsByIds(itemIds: List<String>): List<DirectionItemEntity> {
            if (itemIds.isEmpty()) return emptyList()
            return directionDao.getByIds(itemIds)
        }
    }
