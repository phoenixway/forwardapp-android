package com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import kotlinx.coroutines.flow.Flow

interface ListItemRepository {
    fun getListItems(projectId: String): Flow<List<ListItem>>
    suspend fun addProjectLinkToProject(targetProjectId: String, currentProjectId: String): String
    suspend fun moveListItems(itemIds: List<String>, targetProjectId: String)
    suspend fun deleteListItems(itemIds: List<String>)
    suspend fun restoreListItems(items: List<ListItem>)
    suspend fun updateListItemsOrder(items: List<ListItem>)
    suspend fun doesLinkExist(entityId: String, projectId: String): Boolean
    suspend fun deleteLinkByEntityIdAndProjectId(entityId: String, projectId: String)
    suspend fun deleteItemByEntityId(entityId: String)
    suspend fun deleteItemsForProjects(projectIds: List<String>)
    suspend fun insertListItem(item: ListItem)
}
