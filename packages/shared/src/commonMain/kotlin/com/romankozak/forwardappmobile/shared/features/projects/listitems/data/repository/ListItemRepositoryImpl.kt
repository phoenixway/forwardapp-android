package com.romankozak.forwardappmobile.shared.features.projects.listitems.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.listitems.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository.ListItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import kotlinx.datetime.Clock

class ListItemRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : ListItemRepository {

    override fun getListItems(projectId: String): Flow<List<ListItem>> {
        return db.listItemsQueries.getItemsForProject(projectId)
            .asFlow()
            .mapToList(dispatcher)
            .map { listItems -> listItems.map { it.toDomain() } }
    }

    override suspend fun addProjectLinkToProject(targetProjectId: String, currentProjectId: String): String {
        val newListItem = ListItem(
            id = uuid4().toString(),
            projectId = currentProjectId,
            itemType = "SUBLIST",
            entityId = targetProjectId,
            itemOrder = -Clock.System.now().toEpochMilliseconds(),
        )
        withContext(dispatcher) {
            db.listItemsQueries.insertItem(
                id = newListItem.id,
                projectId = newListItem.projectId,
                itemOrder = newListItem.itemOrder,
                entityId = newListItem.entityId,
                itemType = newListItem.itemType
            )
        }
        return newListItem.id
    }

    override suspend fun moveListItems(itemIds: List<String>, targetProjectId: String) {
        if (itemIds.isNotEmpty()) {
            withContext(dispatcher) {
                db.listItemsQueries.updateListItemProjectIds(targetProjectId, itemIds)
            }
        }
    }

    override suspend fun deleteListItems(itemIds: List<String>) {
        if (itemIds.isNotEmpty()) {
            withContext(dispatcher) {
                db.listItemsQueries.deleteItemsByIds(itemIds)
            }
        }
    }

    override suspend fun restoreListItems(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            withContext(dispatcher) {
                db.transaction {
                    items.forEach {
                        db.listItemsQueries.insertListItem(
                            id = it.id,
                            projectId = it.projectId,
                            itemOrder = it.itemOrder,
                            entityId = it.entityId,
                            itemType = it.itemType
                        )
                    }
                }
            }
        }
    }

    override suspend fun updateListItemsOrder(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            withContext(dispatcher) {
                db.transaction {
                    items.forEach {
                        db.listItemsQueries.updateItem(
                            id = it.id,
                            projectId = it.projectId,
                            itemOrder = it.itemOrder,
                            entityId = it.entityId,
                            itemType = it.itemType
                        )
                    }
                }
            }
        }
    }

    override suspend fun doesLinkExist(entityId: String, projectId: String): Boolean {
        return withContext(dispatcher) {
            db.listItemsQueries.getLinkCount(entityId, projectId).executeAsOne() > 0
        }
    }

    override suspend fun deleteLinkByEntityIdAndProjectId(entityId: String, projectId: String) {
        withContext(dispatcher) {
            db.listItemsQueries.deleteLinkByEntityAndProject(entityId, projectId)
        }
    }

    override suspend fun deleteItemByEntityId(entityId: String) {
        withContext(dispatcher) {
            db.listItemsQueries.deleteItemByEntityId(entityId)
        }
    }

    override suspend fun deleteItemsForProjects(projectIds: List<String>) {
        if (projectIds.isNotEmpty()) {
            withContext(dispatcher) {
                db.listItemsQueries.deleteItemsForProjects(projectIds)
            }
        }
    }

    override suspend fun insertListItem(item: ListItem) {
        withContext(dispatcher) {
            db.listItemsQueries.insertItem(
                id = item.id,
                projectId = item.projectId,
                itemOrder = item.itemOrder,
                entityId = item.entityId,
                itemType = item.itemType
            )
        }
    }

    override suspend fun updateListItemOrder(id: String, newOrder: Long) {
        withContext(dispatcher) {
            db.listItemsQueries.updateItemOrder(itemOrder = newOrder, id = id)
        }
    }
}
