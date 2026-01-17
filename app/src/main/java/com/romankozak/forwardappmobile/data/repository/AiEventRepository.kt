package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.models.AiEventEntity
import com.romankozak.forwardappmobile.domain.ai.events.AiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface AiEventRepository {
    suspend fun emit(event: AiEvent)
    suspend fun getEvents(since: Instant): List<AiEvent>
}

@Singleton
class AiEventRepositoryImpl @Inject constructor(
    private val dao: AiEventDao,
    private val json: Json,
) : AiEventRepository {
    override suspend fun emit(event: AiEvent) = withContext(Dispatchers.IO) {
        val entity = AiEventEntity(
            id = UUID.randomUUID().toString(),
            type = event::class.simpleName ?: "unknown",
            timestamp = event.timestamp.toEpochMilli(),
            payload = json.encodeToString(AiEvent.serializer(), event),
        )
        dao.insert(entity)
    }

    override suspend fun getEvents(since: Instant): List<AiEvent> = withContext(Dispatchers.IO) {
        dao.getEventsSince(since.toEpochMilli()).mapNotNull { entity ->
            runCatching { json.decodeFromString(AiEvent.serializer(), entity.payload) }.getOrNull()
        }
    }
}
