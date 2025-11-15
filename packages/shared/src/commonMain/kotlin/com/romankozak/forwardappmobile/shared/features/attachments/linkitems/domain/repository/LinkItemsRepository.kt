package com.romankozak.forwardappmobile.shared.features.attachments.linkitems.domain.repository

import com.romankozak.forwardappmobile.shared.data.database.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity
import kotlinx.coroutines.flow.Flow

interface LinkItemsRepository {
    fun observeLinkItems(): Flow<List<LinkItemEntity>>

    suspend fun getLinkItemById(id: String): LinkItemEntity?

    suspend fun upsertLinkItem(linkItem: LinkItemEntity)
    suspend fun deleteLinkItemById(id: String)
    suspend fun deleteAllLinkItems()

    suspend fun searchLinkItems(query: String): List<GlobalLinkSearchResult>
}
