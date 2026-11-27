package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.recurrence.reconcileRecurringTaskLinks
import com.romankozak.forwardappmobile.database.AppDatabase

internal suspend fun AppDatabase.reconcileMergedRealizations(bundle: SnapshotBundle, affectedDays: List<String>) = withTransaction {
    val plans = affectedDays.toMutableSet()
    bundle.planningCommitments.orEmpty().filter { it.role == "DAY.THEME" }.forEach { plans.add(it.scopeId) }
    bundle.recurringSeries.filter { it.kind == "TASK" }.forEach { series ->
        canonicalRecurringSeriesDao().getTaskOccurrencesForSeries(series.id).forEach { plans.add(it.dayPlanId) }
    }
    val now = System.currentTimeMillis()
    plans.forEach { reconcileRecurringTaskLinks(it, now) }
}
