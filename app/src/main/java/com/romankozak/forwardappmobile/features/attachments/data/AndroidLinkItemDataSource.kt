package com.romankozak.forwardappmobile.features.attachments.data

import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemRecord
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AndroidLinkItemDataSource @Inject constructor(
    private val linkItemDao: LinkItemDao,
) : LinkItemDataSource {

    override fun observeAll(): Flow<List<LinkItemRecord>> =
        linkItemDao
            .getAllEntitiesAsFlow()
            .map { entities -> entities.map { it.toRecord() } }

    override suspend fun insert(item: LinkItemRecord) {
        linkItemDao.insert(item.toRoom())
    }

    override suspend fun deleteById(id: String) {
        linkItemDao.deleteById(id)
    }

    private fun LinkItemEntity.toRecord(): LinkItemRecord =
        LinkItemRecord(
            id = id,
            linkData = linkData,
            createdAt = createdAt,
        )

    private fun LinkItemRecord.toRoom(): LinkItemEntity =
        LinkItemEntity(
            id = id,
            linkData = linkData,
            createdAt = createdAt,
        )
}
