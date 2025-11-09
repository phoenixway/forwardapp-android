package com.romankozak.forwardappmobile.shared.features.list_items.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.ListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ListItemRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : ListItemRepository {

    override fun getItemsForProjectStream(projectId: String): Flow<List<ListItem>> {
        return db.listItemsQueries.getItemsForProject(projectId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { listItems -> listItems.map { it.toDomain() } }
    }

    override suspend fun insertItem(item: ListItem) {
        withContext(ioDispatcher) {
            db.listItemsQueries.insertItem(item.toSqlDelight(0, null, null))
        }
    }

    override suspend fun insertItems(items: List<ListItem>) {
        withContext(ioDispatcher) {
            items.forEach { item ->
                db.listItemsQueries.insertItem(item.toSqlDelight(0, null, null))
            }
        }
    }

    override suspend fun updateItem(item: ListItem) {
        withContext(ioDispatcher) {
            db.listItemsQueries.updateItem(item.toSqlDelight(0, null, null))
        }
    }

    override suspend fun updateItems(items: List<ListItem>) {
        withContext(ioDispatcher) {
            items.forEach { item ->
                db.listItemsQueries.updateItem(item.toSqlDelight(0, null, null))
            }
        }
    }

    override suspend fun deleteItemsByIds(itemIds: List<String>) {
        withContext(ioDispatcher) {
            db.listItemsQueries.deleteItemsByIds(itemIds)
        }
    }

    override suspend fun deleteItemsForProjects(projectIds: List<String>) {
        withContext(ioDispatcher) {
            db.listItemsQueries.deleteItemsForProjects(projectIds)
        }
    }

    override suspend fun getAll(): List<ListItem> {
        return withContext(ioDispatcher) {
            db.listItemsQueries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getLinkCount(entityId: String, projectId: String): Int {
        return withContext(ioDispatcher) {
            db.listItemsQueries.getLinkCount(entityId, projectId).executeAsOne().toInt()
        }
    }

    override suspend fun deleteLinkByEntityAndProject(entityId: String, projectId: String) {
        withContext(ioDispatcher) {
            db.listItemsQueries.deleteLinkByEntityAndProject(entityId, projectId)
        }
    }

    override suspend fun updateListItemProjectIds(itemIds: List<String>, targetProjectId: String) {
        withContext(ioDispatcher) {
            db.listItemsQueries.updateListItemProjectIds(itemIds, targetProjectId)
        }
    }

    override suspend fun getItemsForProjectSyncForDebug(projectId: String): List<ListItem> {
        return withContext(ioDispatcher) {
            db.listItemsQueries.getItemsForProjectSyncForDebug(projectId).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            db.listItemsQueries.deleteAll()
        }
    }

    override suspend fun getGoalIdsForProject(projectId: String): List<String> {
        return withContext(ioDispatcher) {
            db.listItemsQueries.getGoalIdsForProject(projectId).executeAsList()
        }
    }

    override suspend fun deleteItemByEntityId(entityId: String) {
        withContext(ioDispatcher) {
            db.listItemsQueries.deleteItemByEntityId(entityId)
        }
    }

    override suspend fun getListItemByEntityId(entityId: String): ListItem? {
        return withContext(ioDispatcher) {
            db.listItemsQueries.getListItemByEntityId(entityId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun findProjectIdForGoal(goalId: String): String? {
        return withContext(ioDispatcher) {
            db.listItemsQueries.findProjectIdForGoal(goalId).executeAsOneOrNull()
        }
    }
}
