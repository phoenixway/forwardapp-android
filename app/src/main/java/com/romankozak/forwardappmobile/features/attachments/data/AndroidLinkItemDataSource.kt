package com.romankozak.forwardappmobile.features.attachments.data

import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.RelatedLink as RoomRelatedLink
import com.romankozak.forwardappmobile.data.database.models.LinkType as RoomLinkType
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemRecord
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink as SharedRelatedLink
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
            linkData = linkData.toShared(),
            createdAt = createdAt,
        )

    private fun LinkItemRecord.toRoom(): LinkItemEntity =
        LinkItemEntity(
            id = id,
            linkData = linkData.toRoom(),
            createdAt = createdAt,
        )

    private fun RoomRelatedLink.toShared(): SharedRelatedLink =
        SharedRelatedLink(
            type = type?.let { SharedLinkType.valueOf(it.name) },
            target = target,
            displayName = displayName,
        )

    private fun SharedRelatedLink.toRoom(): RoomRelatedLink =
        RoomRelatedLink(
            type = type?.let { RoomLinkType.valueOf(it.name) },
            target = target,
            displayName = displayName,
        )
}
