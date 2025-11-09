package com.romankozak.forwardappmobile.shared.features.goals.data

import com.romankozak.forwardappmobile.shared.data.database.models.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    suspend fun insertGoal(goal: Goal)
    suspend fun insertGoals(goals: List<Goal>)
    suspend fun updateGoal(goal: Goal)
    suspend fun updateGoals(goals: List<Goal>)
    suspend fun deleteGoalById(id: String)
    suspend fun getGoalById(id: String): Goal?
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>
    suspend fun getGoalsByIdsSuspend(ids: List<String>): List<Goal>
    suspend fun getAll(): List<Goal>
    fun getAllGoalsFlow(): Flow<List<Goal>>
    fun searchGoalsByText(query: String): Flow<List<Goal>>
    fun getAllGoalsCountFlow(): Flow<Long>
    suspend fun updateMarkdown(goalId: String, markdown: String)
    suspend fun deleteAll()
}