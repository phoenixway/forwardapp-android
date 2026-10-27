package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Existing persisted Day data is visible before optional, transactional recurrence preparation. */
internal fun observeDayPlanReadFirst(
    plans: Flow<DayPlan?>,
    initialReadReady: CompletableDeferred<Unit>,
    recurrenceChanges: Flow<Unit> = emptyFlow(),
    prepare: suspend (Long) -> Unit,
): Flow<DayPlan?> = channelFlow {
    var preparedDate: Long? = null
    var preparation: Job? = null
    fun startPreparation(date: Long?) {
        preparation?.cancel()
        if (date != null) preparation = launch {
            initialReadReady.await()
            prepare(date)
        }
    }
    merge(plans.map { DayReadEvent.Plan(it) }, recurrenceChanges.map { DayReadEvent.RulesChanged })
        .collect { event ->
            when (event) {
                is DayReadEvent.Plan -> {
                    send(event.value)
                    val date = event.value?.takeUnless { it.isDeleted }?.date
                    if (date != preparedDate) {
                        preparedDate = date
                        startPreparation(date)
                    }
                }
                DayReadEvent.RulesChanged -> startPreparation(preparedDate)
            }
        }
}

private sealed interface DayReadEvent {
    data class Plan(val value: DayPlan?) : DayReadEvent
    data object RulesChanged : DayReadEvent
}
