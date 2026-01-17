package com.romankozak.forwardappmobile.features.ai.data.repository

import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.ai.data.models.AiInsightEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AiInsightRepository @Inject constructor(
    private val aiInsightDao: AiInsightDao
) {
    fun getAll(): Flow<List<AiInsightEntity>> = aiInsightDao.getAll()

    suspend fun upsertAll(items: List<AiInsightEntity>) = aiInsightDao.upsertAll(items)

    suspend fun deleteById(id: String) = aiInsightDao.deleteById(id)

    suspend fun clearAll() = aiInsightDao.clearAll()

    suspend fun markRead(id: String) = aiInsightDao.markRead(id)

    suspend fun getAllSync(): List<AiInsightEntity> = aiInsightDao.getAllSync()
}
