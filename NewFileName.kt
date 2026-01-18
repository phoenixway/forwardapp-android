package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.data.database.models.AiInsightEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiInsightRepository @Inject constructor(
    private val dao: AiInsightDao,
) {
    fun observeInsights(): Flow<List<AiInsightEntity>> = dao.getAll()

    suspend fun upsertInsights(items: List<AiInsightEntity>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            dao.upsertAll(items)
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }

    suspend fun markRead(id: String) = withContext(Dispatchers.IO) { dao.markRead(id) }
}
