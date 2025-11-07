package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.core.database.models.ListItem
import com.romankozak.forwardappmobile.core.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.shared.database.LinkItem
import com.romankozak.forwardappmobile.shared.database.LinkItemQueries
import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ListItemRepository @Inject constructor(
    private val listItemQueries: ListItemQueries,
    private val linkItemQueries: LinkItemQueries
) {
    private val TAG = "ListItemRepository"

    suspend fun addProjectLinkToProject(
        targetProjectId: String,
        currentProjectId: String,
    ): String {
        Log.d(TAG, "addProjectLinkToProject: targetProjectId=$targetProjectId, currentProjectId=$currentProjectId")
        val newListItemId = UUID.randomUUID().toString()
        listItemQueries.insertItem(
            id = newListItemId,
            project_id = currentProjectId,
            item_type = ListItemTypeValues.SUBLIST,
            entity_id = targetProjectId,
            item_order = -System.currentTimeMillis()
        )
        return newListItemId
    }

    suspend fun moveListItems(
        itemIds: List<String>,
        targetProjectId: String,
    ) {
        if (itemIds.isNotEmpty()) {
            listItemQueries.updateListItemProjectIds(targetProjectId, itemIds)
        }
    }

    suspend fun deleteListItems(itemIds: List<String>) {
        if (itemIds.isNotEmpty()) {
            listItemQueries.deleteItemsByIds(itemIds)
        }
    }

    suspend fun restoreListItems(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            items.forEach { item ->
                listItemQueries.insertItem(
                    id = item.id,
                    project_id = item.projectId,
                    item_type = item.itemType,
                    entity_id = item.entityId,
                    item_order = item.order
                )
            }
        }
    }

    suspend fun updateListItemsOrder(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            items.forEach { item ->
                listItemQueries.updateItem(item.order, item.id)
            }
        }
    }

    suspend fun doesLinkExist(
        entityId: String,
        projectId: String,
    ): Boolean = listItemQueries.getLinkCount(entityId, projectId).executeAsOne() > 0

    suspend fun deleteLinkByEntityIdAndProjectId(
        entityId: String,
        projectId: String,
    ) = listItemQueries.deleteLinkByEntityAndProject(entityId, projectId)

    suspend fun deleteItemByEntityId(entityId: String) {
        listItemQueries.deleteItemByEntityId(entityId)
    }

    fun getItemsForProjectStream(projectId: String): Flow<List<ListItem>> {
        return listItemQueries.getItemsForProject(projectId)
            .asFlow()
            .mapToList()
            .map { list ->
                list.map {
                    ListItem(
                        id = it.id,
                        projectId = it.project_id,
                        itemType = it.item_type,
                        entityId = it.entity_id,
                        order = it.item_order
                    )
                }
            }
    }

    fun getAllEntitiesAsFlow(): Flow<List<LinkItem>> {
        return linkItemQueries.getAllLinkItems().asFlow().mapToList()
    }

    suspend fun getAll(): List<ListItem> {
        return listItemQueries.getAll().executeAsList().map {
             ListItem(
                id = it.id,
                projectId = it.project_id,
                itemType = it.item_type,
                entityId = it.entity_id,
                order = it.item_order
            )
        }
    }

    suspend fun getLinkItemById(id: String): LinkItem? {
        return linkItemQueries.getLinkItemById(id).executeAsOneOrNull()
    }

    suspend fun deleteItemsForProjects(projectIds: List<String>) {
        listItemQueries.deleteItemsForProjects(projectIds)
    }
}