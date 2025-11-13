package com.romankozak.forwardappmobile.shared.features.recent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.recent.data.mappers.toDb
import com.romankozak.forwardappmobile.shared.features.recent.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.recent.domain.model.RecentItem
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecentItemRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : RecentItemRepository {

    override fun observeRecentItems(limit: Long): Flow<List<RecentItem>> =
        database.recentItemsQueries.getRecentItems(limit)
            .asFlow()
            .mapToList(dispatcher)
            .map { items -> items.map { it.toDomain() } }

    override suspend fun upsert(item: RecentItem) = withContext(dispatcher) {
        database.recentItemsQueries.insertRecentItem(
            id = item.id,
            type = item.type,
            lastAccessed = item.lastAccessed,
            displayName = item.displayName,
            target = item.target,
            isPinned = item.isPinned,
        )
    }

    override suspend fun deleteAll() = withContext(dispatcher) {
        database.recentItemsQueries.deleteAllRecentItems()
    }
}
