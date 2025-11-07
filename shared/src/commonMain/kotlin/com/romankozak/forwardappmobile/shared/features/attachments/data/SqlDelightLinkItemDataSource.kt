package com.romankozak.forwardappmobile.shared.features.attachments.data

import kotlinx.coroutines.Dispatchers
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Link_items
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SqlDelightLinkItemDataSource(
    private val db: ForwardAppDatabase,
    private val json: Json,
) : LinkItemDataSource {

    override fun observeAll(): Flow<List<LinkItemRecord>> {
        return db.linkItemQueries.getAllLinkItems()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    override suspend fun insert(item: LinkItemRecord) {
        db.linkItemQueries.insertLinkItem(
            id = item.id,
            link_data = json.encodeToString(RelatedLink.serializer(), item.linkData),
            createdAt = item.createdAt
        )
    }

    override suspend fun deleteById(id: String) {
        db.linkItemQueries.deleteLinkItemById(id)
    }

    private fun Link_items.toModel(): LinkItemRecord {
        return LinkItemRecord(
            id = id,
            linkData = json.decodeFromString(RelatedLink.serializer(), link_data),
            createdAt = createdAt
        )
    }
}