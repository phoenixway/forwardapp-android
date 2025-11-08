package com.romankozak.forwardappmobile.shared.features.recurring_tasks

import com.romankozak.forwardappmobile.shared.data.database.models.RecurringTask
import kotlinx.coroutines.flow.Flow

interface RecurringTaskRepository {
    fun getRecurringTasks(): Flow<List<RecurringTask>>
    fun getRecurringTask(id: String): Flow<RecurringTask?>
    suspend fun addRecurringTask(task: RecurringTask)
    suspend fun deleteRecurringTask(id: String)
}
