package com.romankozak.forwardappmobile.shared.features.link_items.data

import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity
import kotlinx.coroutines.flow.Flow

interface LinkItemRepository {
    suspend fun insert(linkItem: LinkItemEntity)

    suspend fun getLinkItemById(id: String): LinkItemEntity?

    suspend fun getAll(): List<LinkItemEntity>

    suspend fun searchLinksGlobal(query: String): List<LinkItemEntity>

    fun getAllEntitiesAsFlow(): Flow<List<LinkItemEntity>>

    suspend fun insertAll(entities: List<LinkItemEntity>)

    suspend fun deleteAll()

    suspend fun deleteById(id: String)
}
