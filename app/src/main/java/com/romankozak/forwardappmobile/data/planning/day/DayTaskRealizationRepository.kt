package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*

/** Task-owned typed DayFocusItem reference set. Not a semantic relation or Planning participation. */
@Singleton
class DayTaskRealizationRepository @Inject constructor(private val db: AppDatabase) {
    fun observeTask(id: String) = db.dayTaskDao().observeById(id).map { it?.takeUnless { row -> row.isDeleted } }
    fun observePriorities(dayPlanId: String) = db.dayFocusItemDao().getItemsForDayPlan(dayPlanId)
    fun observePriorityCatalog() = db.dayFocusItemDao().getAllVisibleItemsFlow().map { it.groupBy { row -> row.dayPlanId } }

    suspend fun toggle(taskId: String, priorityId: String, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val task = requireNotNull(db.dayTaskDao().getTaskById(taskId)?.takeUnless { it.isDeleted }) { "Завдання недоступне" }
        val ids = task.realizedDayFocusItemIds.toSet()
        val next = if (priorityId in ids) ids - priorityId else {
            val priority = requireNotNull(db.dayFocusItemDao().getByIdForCanonicalRecurrenceSync(priorityId)?.takeUnless { it.isDeleted }) { "Пріоритет недоступний" }
            require(priority.dayPlanId == task.dayPlanId) { "Пріоритет має належати дню завдання" }
            ids + priority.id
        }
        db.dayTaskDao().update(task.copy(realizedDayFocusItemIds = next.sorted(), version = task.version + 1, updatedAt = now, syncedAt = null))
    }
}

fun liveTaskPriorities(task: DayTask, priorities: List<DayFocusItem>): List<DayFocusItem> = priorities
    .filter { !it.isDeleted && it.dayPlanId == task.dayPlanId && it.id in task.realizedDayFocusItemIds }
    .distinctBy { it.id }.sortedWith(compareBy({ it.type.name }, { it.order }, { it.id }))
