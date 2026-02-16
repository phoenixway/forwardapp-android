package com.romankozak.forwardappmobile.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.entities.ContextKeyProblemsEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextKeyProblemsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextKeyProblemsRepository
    @Inject
    constructor(
        private val dao: ContextKeyProblemsDao,
    ) {
        data class KeyProblemsData(
            val description: String = "",
            val focusContextIds: List<String> = emptyList(),
        )

        private data class KeyProblemsPayload(
            @SerializedName("description")
            val description: String = "",
            @SerializedName("focusContextIds")
            val focusContextIds: List<String> = emptyList(),
        )

        private val gson = Gson()

        fun observe(contextId: String): Flow<KeyProblemsData> {
            return dao.observeForContext(contextId).map { entity ->
                entity?.toData() ?: KeyProblemsData()
            }
        }

        suspend fun updateDescription(
            contextId: String,
            description: String,
        ) {
            val current = dao.getForContext(contextId)?.toData() ?: KeyProblemsData()
            val next = current.copy(description = description)
            if (next == current) return
            upsert(contextId, next)
        }

        suspend fun addFocusContext(
            contextId: String,
            focusContextId: String,
        ) {
            val current = dao.getForContext(contextId)?.toData() ?: KeyProblemsData()
            if (current.focusContextIds.contains(focusContextId)) return
            upsert(contextId, current.copy(focusContextIds = current.focusContextIds + focusContextId))
        }

        suspend fun removeFocusContext(
            contextId: String,
            focusContextId: String,
        ) {
            val current = dao.getForContext(contextId)?.toData() ?: KeyProblemsData()
            if (!current.focusContextIds.contains(focusContextId)) return
            upsert(
                contextId,
                current.copy(
                    focusContextIds = current.focusContextIds.filterNot { it == focusContextId },
                ),
            )
        }

        private suspend fun upsert(
            contextId: String,
            data: KeyProblemsData,
        ) {
            val entity =
                ContextKeyProblemsEntity(
                    contextId = contextId,
                    payloadJson = gson.toJson(KeyProblemsPayload(data.description, data.focusContextIds.distinct())),
                    updatedAt = System.currentTimeMillis(),
                )
            dao.upsert(entity)
        }

        private fun ContextKeyProblemsEntity.toData(): KeyProblemsData {
            val payload = runCatching { gson.fromJson(payloadJson, KeyProblemsPayload::class.java) }.getOrNull()
            return KeyProblemsData(
                description = payload?.description.orEmpty(),
                focusContextIds = payload?.focusContextIds?.distinct().orEmpty(),
            )
        }
    }
