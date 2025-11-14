package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository

import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayTask
import kotlinx.coroutines.flow.Flow

interface DayTaskRepository {
    fun observeTasksForPlan(dayPlanId: String): Flow<List<DayTask>>

    suspend fun getTaskById(taskId: String): DayTask?

    suspend fun upsertTask(task: DayTask)

    suspend fun deleteTask(taskId: String)

    suspend fun deleteTasksForPlan(dayPlanId: String)
}
