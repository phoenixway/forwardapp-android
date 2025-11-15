package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.repository

import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurringTask
import kotlinx.coroutines.flow.Flow

interface RecurringTaskRepository {
    fun observeRecurringTasks(goalId: String? = null): Flow<List<RecurringTask>>

    suspend fun getRecurringTaskById(id: String): RecurringTask?

    suspend fun upsertRecurringTask(task: RecurringTask)

    suspend fun deleteRecurringTask(id: String)
    suspend fun deleteRecurringTasksByGoal(goalId: String)
    suspend fun deleteAll()
}
