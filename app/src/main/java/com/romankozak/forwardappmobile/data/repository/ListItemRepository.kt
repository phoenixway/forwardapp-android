package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListItemRepository
    @Inject
    constructor(
        private val listItemDao: ListItemDao,
        private val linkItemDao: LinkItemDao,
        private val backlogOrderRepository: BacklogOrderRepository,
    ) {
        private val TAG = "ListItemRepository"

        @androidx.room.Transaction
        suspend fun addContextLinkToContext(
            targetContextId: String,
            currentContextId: String,
        ): String {
            Log.d(TAG, "addContextLinkToContext: targetContextId=$targetContextId, currentContextId=$currentContextId")
            val newBacklogItem =
                BacklogItem(
                    id = UUID.randomUUID().toString(),
                    contextId = currentContextId,
                    itemType = BacklogItemTypeValues.SUBLIST,
                    entityId = targetContextId,
                    order = -System.currentTimeMillis(),
                )
            Log.d(TAG, "Constructed ListItem to insert: $newBacklogItem")
            try {
                Log.d(TAG, "Attempting to insert via listItemDao.insertItems...")
                listItemDao.insertItems(listOf(newBacklogItem))
                Log.d(TAG, "Insertion successful for ListItem ID: ${newBacklogItem.id}")
            } catch (e: Exception) {
                Log.e(TAG, "DATABASE INSERTION FAILED for ListItem: $newBacklogItem", e)
                throw e
            }
            return newBacklogItem.id
        }

        suspend fun moveListItemsToContext(
            itemIds: List<String>,
            targetContextId: String,
        ) {
            if (itemIds.isNotEmpty()) {
                listItemDao.updateListItemContextIds(itemIds, targetContextId)
                // bump updatedAt/version for moved items
                val now = System.currentTimeMillis()
                val items = listItemDao.getItemsByIds(itemIds)
                if (items.isNotEmpty()) {
                    listItemDao.insertItems(
                        items.map { it.copy(contextId = targetContextId).bumpSync(now) },
                    )
                }
            }
        }

        suspend fun deleteListItems(itemIds: List<String>) {
            if (itemIds.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val items = listItemDao.getItemsByIds(itemIds)
                if (items.isNotEmpty()) {
                    listItemDao.insertItems(
                        items.map { it.softDelete(now) },
                    )
                } else {
                    listItemDao.deleteItemsByIds(itemIds)
                }
            }
        }

        suspend fun restoreListItems(items: List<BacklogItem>) {
            if (items.isNotEmpty()) {
                listItemDao.insertItems(items)
            }
        }

        suspend fun updateListItemsOrder(items: List<BacklogItem>) {
            if (items.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val bumped =
                    items.map {
                        val bumpedItem = it.bumpSync(now)
                        Log.d(
                            TAG,
                            "[updateListItemsOrder] bump id=${it.id} context=${it.contextId} order=${it.order} v_old=${it.version} v_new=${bumpedItem.version} syncedAt_old=${it.syncedAt}",
                        )
                        bumpedItem
                    }
                Log.d(TAG, "[updateListItemsOrder] applying ${bumped.size} items, now=$now")
                listItemDao.updateItems(bumped)
                // Пишемо порядок у backlog_orders як канонічний
                val orders =
                    bumped.map { bi ->
                        BacklogOrder(
                            id = bi.id,
                            listId = bi.contextId,
                            itemId = bi.entityId,
                            order = bi.order,
                            orderVersion = bi.version,
                            updatedAt = bi.updatedAt,
                            syncedAt = bi.syncedAt,
                            isDeleted = bi.isDeleted,
                        )
                    }
                backlogOrderRepository.upsertOrders(orders)
            }
        }

        suspend fun doesLinkExist(
            entityId: String,
            contextId: String,
        ): Boolean = listItemDao.getLinkCount(entityId, contextId) > 0

        suspend fun deleteLinkByEntityIdAndContextId(
            entityId: String,
            contextId: String,
        ) = listItemDao.deleteLinkByEntityAndContext(entityId, contextId)

        suspend fun deleteItemByEntityId(entityId: String) {
            listItemDao.deleteItemByEntityId(entityId)
        }

        fun getItemsForContextStream(contextId: String): kotlinx.coroutines.flow.Flow<List<BacklogItem>> {
            return listItemDao.getItemsForContextStream(contextId)
        }

        fun getAllEntitiesAsFlow(): kotlinx.coroutines.flow.Flow<List<LinkItemEntity>> {
            return linkItemDao.getAllEntitiesAsFlow()
        }

        suspend fun getAll(): List<BacklogItem> {
            return listItemDao.getAll()
        }

        suspend fun getLinkItemById(id: String): LinkItemEntity? {
            return linkItemDao.getLinkItemById(id)
        }

        suspend fun deleteItemsForContexts(contextIds: List<String>) {
            listItemDao.deleteItemsForContexts(contextIds)
        }
    }
