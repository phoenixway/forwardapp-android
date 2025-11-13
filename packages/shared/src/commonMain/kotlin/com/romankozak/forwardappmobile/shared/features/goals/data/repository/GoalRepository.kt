package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalsByIds(ids: List<String>): Flow<List<Goal>>
}
