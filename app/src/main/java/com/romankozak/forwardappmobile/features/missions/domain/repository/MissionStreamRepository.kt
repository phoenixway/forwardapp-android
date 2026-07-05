package com.romankozak.forwardappmobile.features.missions.domain.repository

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.features.missions.data.MissionStreamDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionStreamRepository
    @Inject
    constructor(
        private val missionStreamDao: MissionStreamDao,
    ) {
        fun observeActiveStreams(): Flow<List<MissionStream>> = missionStreamDao.observeActiveStreams()

        suspend fun ensureDefaultStream() {
            if (missionStreamDao.getById(GENERAL_MISSION_STREAM_ID) != null) return
            val now = System.currentTimeMillis()
            missionStreamDao.insert(
                MissionStream(
                    id = GENERAL_MISSION_STREAM_ID,
                    title = "General",
                    streamOrder = Long.MIN_VALUE,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                ),
            )
        }

        suspend fun addStream(title: String): String? {
            val trimmed = title.trim()
            if (trimmed.isBlank()) return null
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            missionStreamDao.insert(
                MissionStream(
                    id = id,
                    title = trimmed,
                    streamOrder = missionStreamDao.getMaxOrder() + 1L,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                ),
            )
            return id
        }

        suspend fun updateStream(
            stream: MissionStream,
            title: String,
            description: String?,
            budgetPercent: Int? = stream.budgetPercent,
        ) {
            val trimmed = title.trim()
            if (trimmed.isBlank()) return
            missionStreamDao.updateDetails(
                streamId = stream.id,
                title = trimmed,
                description = description?.trim()?.ifBlank { null },
                budgetPercent = budgetPercent?.coerceIn(0, 100),
                updatedAt = System.currentTimeMillis(),
            )
        }

        suspend fun archiveStream(streamId: String) {
            missionStreamDao.archiveNonDefault(streamId, System.currentTimeMillis())
        }

        suspend fun reorder(streams: List<MissionStream>) {
            val now = System.currentTimeMillis()
            streams
                .filterNot { it.isDefault }
                .forEachIndexed { index, stream ->
                    missionStreamDao.updateOrder(
                        streamId = stream.id,
                        order = index.toLong(),
                        updatedAt = now,
                    )
                }
        }
    }
