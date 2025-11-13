package com.romankozak.forwardappmobile.shared.features.recent.domain.repository

import com.romankozak.forwardappmobile.shared.features.recent.domain.model.RecentItem
import kotlinx.coroutines.flow.Flow

interface RecentItemRepository {
    fun observeRecentItems(limit: Long = 20): Flow<List<RecentItem>>
    suspend fun upsert(item: RecentItem)
    suspend fun deleteAll()
}
