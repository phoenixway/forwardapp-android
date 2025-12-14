package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.LifeSystemStateDao
import com.romankozak.forwardappmobile.data.database.models.LifeSystemStateEntity
import com.romankozak.forwardappmobile.domain.ai.state.EntropyLevel
import com.romankozak.forwardappmobile.domain.ai.state.ExecutionMode
import com.romankozak.forwardappmobile.domain.ai.state.LifeSystemState
import com.romankozak.forwardappmobile.domain.ai.state.LoadLevel
import com.romankozak.forwardappmobile.domain.ai.state.StabilityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface LifeSystemStateRepository {
    suspend fun get(): LifeSystemState?
    suspend fun save(state: LifeSystemState)
}

@Singleton
class LifeSystemStateRepositoryImpl @Inject constructor(
    private val dao: LifeSystemStateDao,
) : LifeSystemStateRepository {
    override suspend fun get(): LifeSystemState? = withContext(Dispatchers.IO) {
        dao.getState()?.toDomain()
    }

    override suspend fun save(state: LifeSystemState) = withContext(Dispatchers.IO) {
        dao.upsert(
            LifeSystemStateEntity(
                loadLevel = state.loadLevel.name,
                executionMode = state.executionMode.name,
                stability = state.stability.name,
                entropy = state.entropy.name,
                updatedAt = state.updatedAt.toEpochMilli(),
            )
        )
    }
}

private fun LifeSystemStateEntity.toDomain(): LifeSystemState =
    LifeSystemState(
        loadLevel = LoadLevel.valueOf(loadLevel),
        executionMode = ExecutionMode.valueOf(executionMode),
        stability = StabilityLevel.valueOf(stability),
        entropy = EntropyLevel.valueOf(entropy),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
