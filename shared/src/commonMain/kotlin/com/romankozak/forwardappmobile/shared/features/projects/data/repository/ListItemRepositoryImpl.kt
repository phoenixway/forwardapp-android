package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.projects.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ListItemRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : ListItemRepository {

    override fun getListItems(projectId: String): Flow<List<ListItem>> {
        return db.listItemsQueries.getByProjectId(projectId)
            .asFlow()
            .mapToList(dispatcher)
            .map { listItems -> listItems.map { it.toDomain() } }
    }
}
