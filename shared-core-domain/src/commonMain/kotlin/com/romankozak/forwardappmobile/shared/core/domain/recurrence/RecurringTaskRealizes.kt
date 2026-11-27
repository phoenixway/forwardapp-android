package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.recurrence.LocalDayKey
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskRealizesDefaults
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate

fun RecurringTaskTemplate.realizesOn(dayKey: LocalDayKey): RecurringTaskRealizesDefaults? =
    realizesChanges.keys.filter { it <= dayKey }.maxOrNull()?.let(realizesChanges::get) ?: realizesDefaults
