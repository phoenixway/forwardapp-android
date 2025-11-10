package com.romankozak.forwardappmobile.shared.features.goals.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.goals.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher
) : GoalRepository {

    override fun getGoalsByIds(ids: List<String>): Flow<List<Goal>> {
        return db.goalsQueries.getByIds(ids)
            .asFlow()
            .mapToList(dispatcher)
            .map { goals -> goals.map { it.toDomain() } }
    }
}
