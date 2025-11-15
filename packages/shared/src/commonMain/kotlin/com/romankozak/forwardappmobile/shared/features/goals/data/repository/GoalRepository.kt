package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<Goal>>
    fun searchGoals(query: String): Flow<List<Goal>>
    fun getGoalById(id: String): Flow<Goal?>
    suspend fun insertGoal(goal: Goal): String
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(id: String)
    suspend fun deleteAllGoals()
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>
}

