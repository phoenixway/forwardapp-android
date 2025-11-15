package com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.data.database.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.mappers.toSearchResult
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.domain.repository.LinkItemsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LinkItemsRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : LinkItemsRepository {

    override fun observeLinkItems(): Flow<List<LinkItemEntity>> =
        database.linkItemsQueries.getAllLinkItems()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getLinkItemById(id: String): LinkItemEntity? = withContext(dispatcher) {
        database.linkItemsQueries.getLinkItemById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertLinkItem(linkItem: LinkItemEntity) = withContext(dispatcher) {
        database.linkItemsQueries.insertLinkItem(
            id = linkItem.id,
            linkData = linkItem.linkData,
            createdAt = linkItem.createdAt,
        )
    }

    override suspend fun deleteLinkItemById(id: String) = withContext(dispatcher) {
        database.linkItemsQueries.deleteLinkItemById(id)
    }

    override suspend fun deleteAllLinkItems() = withContext(dispatcher) {
        database.linkItemsQueries.deleteAllLinkItems()
    }

    override suspend fun searchLinkItems(query: String): List<GlobalLinkSearchResult> = withContext(dispatcher) {
        val pattern = "%$query%"
        database.linkItemsQueries.searchLinkItems(pattern)
            .executeAsList()
            .map { it.toSearchResult() }
    }
}
