package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextInboxSortingRepository
    @Inject
    constructor(
        private val canonicalRepository: CanonicalInboxSortingRepository,
    ) {
        data class InboxSortingSettings(
            val rulesText: String = "",
        )

        fun observe(contextId: String): Flow<InboxSortingSettings> =
            canonicalRepository.observeConfiguration(contextId).map { configuration ->
                InboxSortingSettings(rulesText = InboxSortingLegacyTextAdapter.encode(configuration))
            }

        suspend fun get(contextId: String): InboxSortingSettings {
            val configuration = canonicalRepository.getConfiguration(contextId)
            return InboxSortingSettings(rulesText = InboxSortingLegacyTextAdapter.encode(configuration))
        }

        suspend fun updateRulesText(
            contextId: String,
            rulesText: String,
        ) {
            canonicalRepository.updateConfiguration(
                workspaceId = contextId,
                configuration = InboxSortingLegacyTextAdapter.decode(rulesText),
            )
        }
    }
