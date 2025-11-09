package com.romankozak.forwardappmobile.shared.features.list_items.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.ListItem
import com.romankozak.forwardappmobile.shared.database.ListItems
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ListItemRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ListItemRepository {

    private val queries = db.listItemsQueries

    override fun getItemsForProjectStream(projectId: String): Flow<List<ListItem>> {
        return queries.getItemsForProject(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { listItems -> listItems.map { it.toDomain() } }
    }

    override suspend fun insertItem(item: ListItem, order: Long, entityId: String?, itemType: String?) {
        withContext(ioDispatcher) {
            queries.insertItem(
                id = item.id,
                projectId = item.projectId,
                itemOrder = order,
                entityId = entityId,
                itemType = itemType
            )
        }
    }

    override suspend fun insertItems(items: List<ListItem>, order: Long, entityId: String?, itemType: String?) {
        withContext(ioDispatcher) {
            items.forEach { item ->
                queries.insertItem(
                    id = item.id,
                    projectId = item.projectId,
                    itemOrder = order,
                    entityId = entityId,
                    itemType = itemType
                )
            }
        }
    }

    override suspend fun updateItem(item: ListItem, order: Long, entityId: String?, itemType: String?) {
        withContext(ioDispatcher) {
            queries.updateItem(
                id = item.id,
                projectId = item.projectId,
                itemOrder = order,
                entityId = entityId,
                itemType = itemType
            )
        }
    }

    override suspend fun updateItems(items: List<ListItem>, order: Long, entityId: String?, itemType: String?) {
        withContext(ioDispatcher) {
            items.forEach { item ->
                queries.updateItem(
                    id = item.id,
                    projectId = item.projectId,
                    itemOrder = order,
                    entityId = entityId,
                    itemType = itemType
                )
            }
        }
    }

    override suspend fun deleteItemsByIds(itemIds: List<String>) {
        withContext(ioDispatcher) {
            queries.deleteItemsByIds(itemIds)
        }
    }

    override suspend fun deleteItemsForProjects(projectIds: List<String>) {
        withContext(ioDispatcher) {
            queries.deleteItemsForProjects(projectIds)
        }
    }

    override suspend fun getAll(): List<ListItem> {
        return withContext(ioDispatcher) {
            queries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getLinkCount(entityId: String, projectId: String): Int {
        return withContext(ioDispatcher) {
            queries.getLinkCount(entityId, projectId).executeAsOne().toInt()
        }
    }

    override suspend fun deleteLinkByEntityAndProject(entityId: String, projectId: String) {
        withContext(ioDispatcher) {
            queries.deleteLinkByEntityAndProject(entityId, projectId)
        }
    }

    override suspend fun updateListItemProjectIds(itemIds: List<String>, targetProjectId: String) {
        withContext(ioDispatcher) {
            queries.updateListItemProjectIds(targetProjectId, itemIds)
        }
    }

    override suspend fun getItemsForProjectSyncForDebug(projectId: String): List<ListItem> {
        return withContext(ioDispatcher) {
            queries.getItemsForProjectSyncForDebug(projectId).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            queries.deleteAll()
        }
    }

    override suspend fun getGoalIdsForProject(projectId: String): List<String> {
        return withContext(ioDispatcher) {
            queries.getGoalIdsForProject(projectId).executeAsList().map { it.entityId }
        }
    }

    override suspend fun deleteItemByEntityId(entityId: String) {
        withContext(ioDispatcher) {
            queries.deleteItemByEntityId(entityId)
        }
    }

    override suspend fun getListItemByEntityId(entityId: String): ListItem? {
        return withContext(ioDispatcher) {
            queries.getListItemByEntityId(entityId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun findProjectIdForGoal(goalId: String): String? {
        return withContext(ioDispatcher) {
            queries.findProjectIdForGoal(goalId).executeAsOneOrNull()
        }
    }
}

fun ListItems.toDomain(): ListItem {
    return ListItem(
        id = id,
        projectId = projectId
    )
}
