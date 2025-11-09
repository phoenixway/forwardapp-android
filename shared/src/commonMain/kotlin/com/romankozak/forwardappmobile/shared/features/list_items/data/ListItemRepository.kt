package com.romankozak.forwardappmobile.shared.features.list_items.data

import com.romankozak.forwardappmobile.shared.data.database.models.ListItem
import kotlinx.coroutines.flow.Flow

interface ListItemRepository {
    fun getItemsForProjectStream(projectId: String): Flow<List<ListItem>>

    suspend fun insertItem(item: ListItem, order: Long, entityId: String?, itemType: String?)

    suspend fun insertItems(items: List<ListItem>, order: Long, entityId: String?, itemType: String?)

    suspend fun updateItem(item: ListItem, order: Long, entityId: String?, itemType: String?)

    suspend fun updateItems(items: List<ListItem>, order: Long, entityId: String?, itemType: String?)

    suspend fun deleteItemsByIds(itemIds: List<String>)

    suspend fun deleteItemsForProjects(projectIds: List<String>)

    suspend fun getAll(): List<ListItem>

    suspend fun getLinkCount(entityId: String, projectId: String): Int

    suspend fun deleteLinkByEntityAndProject(entityId: String, projectId: String)

    suspend fun updateListItemProjectIds(itemIds: List<String>, targetProjectId: String)

    suspend fun getItemsForProjectSyncForDebug(projectId: String): List<ListItem>

    suspend fun deleteAll()

    suspend fun getGoalIdsForProject(projectId: String): List<String>

    suspend fun deleteItemByEntityId(entityId: String)

    suspend fun getListItemByEntityId(entityId: String): ListItem?

    suspend fun findProjectIdForGoal(goalId: String): String?
}
