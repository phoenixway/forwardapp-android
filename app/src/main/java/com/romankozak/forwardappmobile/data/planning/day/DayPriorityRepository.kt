package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceRole
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.repository.DayFocusesRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import javax.inject.Inject

/** Creates the Day row and its exact Orientation source in one transaction. */
class DayPriorityRepository @Inject constructor(
    private val db: AppDatabase,
    private val focuses: DayFocusesRepository,
    private val references: CanonicalPlanningReferenceRepository,
) {
    suspend fun create(
        dayPlanId: String,
        orientationId: String,
        title: String,
        type: DayFocusType,
    ): DayFocusItem = db.withTransaction {
        require(type == DayFocusType.FOCUS || type == DayFocusType.RESPONSIBILITY)
        val plan = requireNotNull(db.dayPlanDao().getPlanById(dayPlanId)) {
            "Day plan is unavailable: $dayPlanId"
        }
        require(!plan.isDeleted) { "Day plan is deleted: $dayPlanId" }
        val source = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, orientationId)
        references.requireTarget(source, live = true)
        val item = focuses.addItem(
            dayPlanId = dayPlanId,
            title = title.trim().also { require(it.isNotEmpty()) },
            notes = null,
            relatedLinks = emptyList(),
            type = type,
            order = focuses.nextOrderForDayPlan(dayPlanId),
            budgetPercent = null,
        )
        references.attach(
            PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_FOCUS, item.id),
            source,
            PlanningReferenceRole.SOURCE,
        )
        item
    }

    suspend fun deleteCurrent(itemId: String) = db.withTransaction {
        focuses.deleteItem(itemId)
        references.retireOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_FOCUS, itemId), System.currentTimeMillis())
    }

    suspend fun deleteEverywhere(item: DayFocusItem) = db.withTransaction {
        val seriesId = requireNotNull(item.recurrenceSeriesId)
        val occurrences = db.canonicalRecurringSeriesDao().getFocusOccurrencesForSeries(seriesId)
        focuses.deleteItemEverywhere(item)
        val now = System.currentTimeMillis()
        occurrences.forEach { occurrence ->
            references.retireOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_FOCUS, occurrence.id), now)
        }
    }
}
