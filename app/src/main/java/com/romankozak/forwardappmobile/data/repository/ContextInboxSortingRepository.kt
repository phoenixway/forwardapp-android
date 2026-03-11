package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextInboxSortingEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextInboxSortingDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextInboxSortingRepository
    @Inject
    constructor(
        private val dao: ContextInboxSortingDao,
    ) {
        data class InboxSortingSettings(
            val rulesText: String = "",
        )

        fun observe(contextId: String): Flow<InboxSortingSettings> =
            dao.observeForContext(contextId).map { entity ->
                InboxSortingSettings(rulesText = entity?.rulesText.orEmpty())
            }

        suspend fun get(contextId: String): InboxSortingSettings {
            val entity = dao.getForContext(contextId)
            return InboxSortingSettings(rulesText = entity?.rulesText.orEmpty())
        }

        suspend fun updateRulesText(
            contextId: String,
            rulesText: String,
        ) {
            dao.upsert(
                ContextInboxSortingEntity(
                    contextId = contextId,
                    rulesText = rulesText,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
