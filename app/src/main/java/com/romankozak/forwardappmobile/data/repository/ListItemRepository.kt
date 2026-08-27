package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao

import com.romankozak.forwardappmobile.core.context.tombstoneBacklogOrdersForItems

import com.romankozak.forwardappmobile.core.context.isDirectHierarchyChildContext

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
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
        private val contextDao: ContextDao,
    ) {
        private val TAG = "ListItemRepository"

        @androidx.room.Transaction
        suspend fun addContextLinkToContext(
            targetContextId: String,
            currentContextId: String,
        ): String? {
            val target = contextDao.getContextById(targetContextId)
            if (
                target != null &&
                isDirectHierarchyChildContext(currentContextId, target.parentId)
            ) {
                Log.i(
                    TAG,
                    "Skipping direct-child backlog SUBLIST owner=$currentContextId target=$targetContextId",
                )
                return null
            }

            val item =
                BacklogItem(
                    id = UUID.randomUUID().toString(),
                    contextId = currentContextId,
                    itemType = BacklogItemTypeValues.SUBLIST,
                    entityId = targetContextId,
                    order = -System.currentTimeMillis(),
                )
            listItemDao.insertItems(listOf(item))
            return item.id
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
            if (itemIds.isEmpty()) return
            val now = System.currentTimeMillis()
            val items = listItemDao.getItemsByIds(itemIds)

            if (items.isEmpty()) {
                listItemDao.deleteItemsByIds(itemIds)
                return
            }

            val tombstones = items.map { it.softDelete(now) }
            listItemDao.insertItems(tombstones)

            backlogOrderRepository.upsertOrders(
                tombstoneBacklogOrdersForItems(
                    backlogOrderRepository.getAllRaw(),
                    tombstones,
                    now,
                ),
            )
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
        ): Boolean = listItemDao.getLinkCount(entityId, contextId, BacklogItemTypeValues.SUBLIST) > 0

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

        suspend fun getItemsByIds(ids: List<String>): List<BacklogItem> {
            if (ids.isEmpty()) return emptyList()
            return listItemDao.getItemsByIds(ids)
        }

        suspend fun getBacklogItemsForContext(contextId: String): List<BacklogItem> = listItemDao.getItemsForContextSyncForDebug(contextId)

        suspend fun getGoalIdsForContext(contextId: String): List<String> {
            return listItemDao.getGoalIdsForContext(contextId)
        }

        suspend fun addEntityLinksToContext(
            contextId: String,
            entries: List<Pair<String, String>>,
        ): Int {
            if (entries.isEmpty()) return 0
            val now = System.currentTimeMillis()
            val newItems =
                entries.map { (itemType, entityId) ->
                    BacklogItem(
                        id = UUID.randomUUID().toString(),
                        contextId = contextId,
                        itemType = itemType,
                        entityId = entityId,
                        order = -now,
                    )
                }
            listItemDao.insertItems(newItems)
            return newItems.size
        }

        suspend fun deleteItemsForContexts(contextIds: List<String>) {
            listItemDao.deleteItemsForContexts(contextIds)
        }
    }
