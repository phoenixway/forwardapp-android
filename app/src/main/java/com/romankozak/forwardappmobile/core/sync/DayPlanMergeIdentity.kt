package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayFocusItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.planning.day.DayPlanningScopeExtensionDao
import com.romankozak.forwardappmobile.data.planning.day.PersonalPlanningDay
import com.romankozak.forwardappmobile.data.planning.day.verifyPersonalPlanningDay
import java.util.Calendar

internal data class DayPlanMerge(
    val rows: List<DayPlanSnapshot>,
    val legacyRemap: Map<String, String>,
)

/** Planning identity and its calendar proof must travel together with the Day carrier. */
internal suspend fun DayPlanDao.planDayMerge(
    bundle: SnapshotBundle,
    localPlans: Map<String, DayPlan>,
    extensionDao: DayPlanningScopeExtensionDao,
): DayPlanMerge {
    val incomingExtensions = bundle.dayPlanningScopeExtensions.orEmpty().associateBy { it.scopeId }
    val hasCanonicalPlanning = bundle.planningFormatVersion != null
    val rows = mutableListOf<DayPlanSnapshot>()
    val remap = mutableMapOf<String, String>()
    for (incoming in bundle.dayPlans) {
        val local = localPlans[incoming.id]
        if (hasCanonicalPlanning) {
            val extension = incomingExtensions[incoming.id]
            val key = extension?.let {
                verifyPersonalPlanningDay(it.scopeId, it.calendarDayStartMillis, it.localDate, it.timeZoneId)
            } ?: extensionDao.personalDay(incoming.id)
            if (key != null) require(incoming.date == key.start) { "Day carrier disagrees with its calendar key" }
            if (local != null || incoming.isDeleted) {
                rows += if (local != null && key != null) mergeKeyedCarrier(local.toSnapshot(), incoming) else incoming
                continue
            }
            val existing = findCalendarCandidate(incoming.date)
            if (existing != null && existing.id != incoming.id &&
                (key == null || extensionDao.personalDay(existing.id) == null)) throw DayPlanMergeIdentityConflict()
            rows += incoming
        } else {
            val existing = findCalendarCandidate(incoming.date)
            if (existing == null || existing.id == incoming.id) rows += incoming else remap[incoming.id] = existing.id
        }
    }
    return DayPlanMerge(rows, remap)
}

private suspend fun DayPlanningScopeExtensionDao.personalDay(id: String): PersonalPlanningDay? =
    get(id)?.let { verifyPersonalPlanningDay(it.scopeId, it.calendarDayStartMillis, it.localDate, it.timeZoneId) }

private suspend fun DayPlanDao.findCalendarCandidate(timestamp: Long): DayPlan? {
    val day = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = day.timeInMillis
    return getPlanForDateSync(start) ?: run {
        day.add(Calendar.DATE, 1)
        getPlanForDateWindowSync(start, day.timeInMillis, timestamp)
    }
}

private fun mergeKeyedCarrier(local: DayPlanSnapshot, incoming: DayPlanSnapshot): DayPlanSnapshot {
    val winner = when {
        local.version > incoming.version -> local
        incoming.version > local.version -> incoming
        else -> {
            // These columns project canonical scope/extension facts. Their own versions decide them.
            val comparable = incoming.copy(
                name = local.name, status = local.status, predictedDurationMinutes = local.predictedDurationMinutes,
                createdAt = local.createdAt, updatedAt = local.updatedAt,
            )
            require(local == comparable) { "Equal-version personal Day content differs between devices" }
            if (incoming.updatedAt > local.updatedAt) incoming else local
        }
    }
    return winner.copy(createdAt = minOf(local.createdAt, incoming.createdAt))
}

internal class DayPlanMergeIdentityConflict : IllegalArgumentException(
    "Day plan merge stopped. The calendar lookup matched a different Planning scope without a verified local date and time zone. " +
        "Keep a full backup from each device for reconciliation.",
)

/** A recurrence occurrence still has just one physical parent per (series, civil date). */
internal fun requirePersonalDayRecurrenceOwners(
    keyedPlanIds: Set<String>,
    incomingTasks: List<DayTaskSnapshot>,
    incomingFocus: List<DayFocusItemSnapshot>,
    localTasks: List<DayTask>,
    localFocus: List<DayFocusItem>,
) {
    if (keyedPlanIds.isEmpty()) return
    fun requireOwners(
        incoming: List<Pair<Pair<String, String>, String>>,
        local: List<Pair<Pair<String, String>, String>>,
    ) {
        val owners = (incoming + local).groupBy({ it.first }, { it.second })
        for ((key, _) in incoming) {
            val plans = owners.getValue(key).toSet()
            require(plans.size == 1 || plans.none { it in keyedPlanIds }) {
                "The same recurring occurrence belongs to two Day plans. Their creation zones require separate plans."
            }
        }
    }
    requireOwners(
        incomingTasks.mapNotNull { t -> t.recurrence?.let { (it.seriesId to it.occurrenceDayKey) to t.dayPlanId } },
        localTasks.mapNotNull { t -> t.recurrenceSeriesId?.let { s -> t.recurrenceOccurrenceDayKey?.let { (s to it) to t.dayPlanId } } },
    )
    requireOwners(
        incomingFocus.mapNotNull { f -> f.recurrence?.let { (it.seriesId to it.occurrenceDayKey) to f.dayPlanId } },
        localFocus.mapNotNull { f -> f.recurrenceSeriesId?.let { s -> f.recurrenceOccurrenceDayKey?.let { (s to it) to f.dayPlanId } } },
    )
}
