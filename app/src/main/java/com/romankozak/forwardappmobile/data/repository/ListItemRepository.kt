package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItem
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItemTypeValues
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListItemRepository @Inject constructor(
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val backlogOrderRepository: BacklogOrderRepository,
) {
    private val TAG = "ListItemRepository"

    @androidx.room.Transaction
    suspend fun addProjectLinkToProject(
        targetProjectId: String,
        currentProjectId: String,
    ): String {
        Log.d(TAG, "addProjectLinkToProject: targetProjectId=$targetProjectId, currentProjectId=$currentProjectId")
        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = currentProjectId,
                itemType = ListItemTypeValues.SUBLIST,
                entityId = targetProjectId,
                order = -System.currentTimeMillis(),
            )
        Log.d(TAG, "Constructed ListItem to insert: $newListItem")
        try {
            Log.d(TAG, "Attempting to insert via listItemDao.insertItems...")
            listItemDao.insertItems(listOf(newListItem))
            Log.d(TAG, "Insertion successful for ListItem ID: ${newListItem.id}")
        } catch (e: Exception) {
            Log.e(TAG, "DATABASE INSERTION FAILED for ListItem: $newListItem", e)
            throw e
        }
        return newListItem.id
    }

    suspend fun moveListItems(
        itemIds: List<String>,
        targetProjectId: String,
    ) {
        if (itemIds.isNotEmpty()) {
            listItemDao.updateListItemProjectIds(itemIds, targetProjectId)
            // bump updatedAt/version for moved items
            val now = System.currentTimeMillis()
            val items = listItemDao.getItemsByIds(itemIds)
            if (items.isNotEmpty()) {
                listItemDao.insertItems(
                    items.map { it.copy(projectId = targetProjectId).bumpSync(now) },
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

    suspend fun restoreListItems(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            listItemDao.insertItems(items)
        }
    }

    suspend fun updateListItemsOrder(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val bumped = items.map {
                val bumpedItem = it.bumpSync(now)
                Log.d(
                    TAG,
                    "[updateListItemsOrder] bump id=${it.id} project=${it.projectId} order=${it.order} v_old=${it.version} v_new=${bumpedItem.version} syncedAt_old=${it.syncedAt}"
                )
                bumpedItem
            }
            Log.d(TAG, "[updateListItemsOrder] applying ${bumped.size} items, now=$now")
            listItemDao.updateItems(bumped)
            // Пишемо порядок у backlog_orders як канонічний
            val orders = bumped.map { bi ->
                BacklogOrder(
                    id = bi.id,
                    listId = bi.projectId,
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
        projectId: String,
    ): Boolean = listItemDao.getLinkCount(entityId, projectId) > 0

    suspend fun deleteLinkByEntityIdAndProjectId(
        entityId: String,
        projectId: String,
    ) = listItemDao.deleteLinkByEntityAndProject(entityId, projectId)

    suspend fun deleteItemByEntityId(entityId: String) {
        listItemDao.deleteItemByEntityId(entityId)
    }

    fun getItemsForProjectStream(projectId: String): kotlinx.coroutines.flow.Flow<List<ListItem>> {
        return listItemDao.getItemsForProjectStream(projectId)
    }

    fun getAllEntitiesAsFlow(): kotlinx.coroutines.flow.Flow<List<LinkItemEntity>> {
        return linkItemDao.getAllEntitiesAsFlow()
    }

    suspend fun getAll(): List<ListItem> {
        return listItemDao.getAll()
    }

    suspend fun getLinkItemById(id: String): LinkItemEntity? {
        return linkItemDao.getLinkItemById(id)
    }

    suspend fun deleteItemsForProjects(projectIds: List<String>) {
        listItemDao.deleteItemsForProjects(projectIds)
    }
}
