package com.romankozak.forwardappmobile.shared.features.search.domain.repository

import com.romankozak.forwardappmobile.shared.data.database.models.GlobalSearchResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String): Flow<List<GlobalSearchResult>>
}
