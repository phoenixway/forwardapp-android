package com.romankozak.forwardappmobile.features.attachments.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.romankozak.forwardappmobile.shared.database.LinkItems

class SqlDelightLinkItemDataSource(
    db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : LinkItemDataSource {

    private val queries = db.linkItemsQueries

    override fun observeAll(): Flow<List<LinkItemRecord>> {
        return queries.getAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { linkItems -> linkItems.map { it.toLinkItemRecord() } }
    }

    override suspend fun insert(item: LinkItemRecord) {
        withContext(ioDispatcher) {
            queries.insert(
                id = item.id,
                linkData = item.linkData,
                createdAt = item.createdAt
            )
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(ioDispatcher) {
            queries.deleteById(id)
        }
    }
}

fun LinkItems.toLinkItemRecord(): LinkItemRecord {
    return LinkItemRecord(
        id = this.id,
        linkData = this.linkData,
        createdAt = this.createdAt
    )
}
