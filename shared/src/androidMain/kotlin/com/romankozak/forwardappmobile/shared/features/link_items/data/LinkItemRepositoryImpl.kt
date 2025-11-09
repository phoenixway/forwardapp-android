package com.romankozak.forwardappmobile.shared.features.link_items.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LinkItemRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : LinkItemRepository {

    override suspend fun insert(linkItem: LinkItemEntity) {
        withContext(ioDispatcher) {
            db.linkItemsQueries.insert(linkItem.toSqlDelight())
        }
    }

    override suspend fun getLinkItemById(id: String): LinkItemEntity? {
        return withContext(ioDispatcher) {
            db.linkItemsQueries.getLinkItemById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun getAll(): List<LinkItemEntity> {
        return withContext(ioDispatcher) {
            db.linkItemsQueries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun searchLinksGlobal(query: String): List<LinkItemEntity> {
        return withContext(ioDispatcher) {
            db.linkItemsQueries.searchLinksGlobal(query).executeAsList().map { it.toDomain() }
        }
    }

    override fun getAllEntitiesAsFlow(): Flow<List<LinkItemEntity>> {
        return db.linkItemsQueries.getAllEntities()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { linkItems -> linkItems.map { it.toDomain() } }
    }

    override suspend fun insertAll(entities: List<LinkItemEntity>) {
        withContext(ioDispatcher) {
            entities.forEach { entity ->
                db.linkItemsQueries.insert(entity.toSqlDelight())
            }
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            db.linkItemsQueries.deleteAll()
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(ioDispatcher) {
            db.linkItemsQueries.deleteById(id)
        }
    }
}
