package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListItemRepository @Inject constructor(
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao
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
        }
    }

    suspend fun deleteListItems(itemIds: List<String>) {
        if (itemIds.isNotEmpty()) {
            listItemDao.deleteItemsByIds(itemIds)
        }
    }

    suspend fun restoreListItems(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            listItemDao.insertItems(items)
        }
    }

    suspend fun updateListItemsOrder(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            listItemDao.updateItems(items)
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

    @androidx.room.Transaction
    suspend fun addLinkItemToProjectFromLink(
        projectId: String,
        link: RelatedLink,
    ): String {
        val newLinkEntity =
            LinkItemEntity(
                id = UUID.randomUUID().toString(),
                linkData = link,
                createdAt = System.currentTimeMillis(),
            )
        linkItemDao.insert(newLinkEntity)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                itemType = ListItemTypeValues.LINK_ITEM,
                entityId = newLinkEntity.id,
                order = -System.currentTimeMillis(),
            )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    suspend fun deleteItemByEntityId(entityId: String) {
        listItemDao.deleteItemByEntityId(entityId)
    }

    fun getItemsForProjectStream(projectId: String): kotlinx.coroutines.flow.Flow<List<com.romankozak.forwardappmobile.data.database.models.ListItem>> {
        return listItemDao.getItemsForProjectStream(projectId)
    }

    fun getAllEntitiesAsFlow(): kotlinx.coroutines.flow.Flow<List<com.romankozak.forwardappmobile.data.database.models.LinkItemEntity>> {
        return linkItemDao.getAllEntitiesAsFlow()
    }

    suspend fun getAll(): List<com.romankozak.forwardappmobile.data.database.models.ListItem> {
        return listItemDao.getAll()
    }

    suspend fun getLinkItemById(id: String): com.romankozak.forwardappmobile.data.database.models.LinkItemEntity? {
        return linkItemDao.getLinkItemById(id)
    }

    suspend fun deleteItemsForProjects(projectIds: List<String>) {
        listItemDao.deleteItemsForProjects(projectIds)
    }
}
