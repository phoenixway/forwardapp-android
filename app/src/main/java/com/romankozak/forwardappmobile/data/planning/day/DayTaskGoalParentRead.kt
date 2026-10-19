package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.data.product.CanonicalGoalSemanticReadRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

data class DayTaskGoalParent(val id: String, val title: String, val workspaceId: String?)

/** One canonical Goal projection and one bulk placement stream, independent of task count. */
class DayTaskGoalParentRead @Inject constructor(private val db: AppDatabase, private val goals: CanonicalGoalSemanticReadRepository) {
    fun observe() = combine(goals.projectGoals(), db.workspaceBacklogEntryDao().observeGoalWorkspaceParents()) { rows, placements ->
        val parents = placements.groupBy { it.goalId }.mapValues { (_, values) -> values.first().workspaceId }
        rows.associate { it.id to DayTaskGoalParent(it.id, it.text, parents[it.id]) }
    }.flowOn(Dispatchers.Default)
}
