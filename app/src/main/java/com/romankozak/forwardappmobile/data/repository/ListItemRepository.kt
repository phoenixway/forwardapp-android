package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogCompatibilityReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListItemRepository
    @Inject
    constructor(
        private val linkItemDao: LinkItemDao,
        private val canonicalBacklogReader: CanonicalBacklogCompatibilityReader,
    ) {
        suspend fun doesLinkExist(
            entityId: String,
            contextId: String,
        ): Boolean =
            canonicalBacklogReader
                .getItemsForContext(contextId)
                .any { item ->
                    item.associationOwnerContextId == null &&
                        item.itemType == BacklogItemTypeValues.SUBLIST &&
                        item.entityId == entityId
                }

        fun getItemsForContextStream(contextId: String): kotlinx.coroutines.flow.Flow<List<BacklogItem>> {
            return canonicalBacklogReader.observeItemsForContext(contextId)
        }

        fun getAllEntitiesAsFlow(): kotlinx.coroutines.flow.Flow<List<LinkItemEntity>> {
            return linkItemDao.getAllEntitiesAsFlow()
        }

        suspend fun getLinkItemById(id: String): LinkItemEntity? {
            return linkItemDao.getLinkItemById(id)
        }

        suspend fun getItemsByIds(ids: List<String>): List<BacklogItem> {
            if (ids.isEmpty()) return emptyList()
            return canonicalBacklogReader.getItemsByIds(ids)
        }

        suspend fun getBacklogItemsForContext(contextId: String): List<BacklogItem> =
            canonicalBacklogReader.getDirectItemsForContext(contextId)

        suspend fun getGoalIdsForContext(contextId: String): List<String> =
            canonicalBacklogReader
                .getItemsForContext(contextId)
                .asSequence()
                .filter { it.itemType == BacklogItemTypeValues.GOAL }
                .map { it.entityId }
                .distinct()
                .toList()

        suspend fun getRuntimeItemForEntityInContext(
            entityId: String,
            itemType: String,
            contextId: String,
        ): BacklogItem? =
            canonicalBacklogReader
                .getItemsForContext(contextId)
                .firstOrNull { item ->
                    item.itemType == itemType &&
                        item.entityId == entityId
                }

    }
