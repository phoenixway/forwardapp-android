package com.romankozak.forwardappmobile.features.daymanagement.ui

import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementPhase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState

fun defaultTodayTabForRuntimeState(state: DayManagementRuntimeState): DayManagementTab =
    when {
        !state.hasOpenOperationalDay -> DayManagementTab.DAY_START
        state.finalizationStartedAt != null || state.currentPhase == DayManagementPhase.FINALIZATION ->
            DayManagementTab.FINALIZATION
        state.dayPlanFinalizedAt != null || state.currentPhase == DayManagementPhase.IMPLEMENTATION ->
            DayManagementTab.JOURNAL
        state.dayFocusFinalizedAt != null -> DayManagementTab.DAY_PLAN
        else -> DayManagementTab.DAY_FOCUSES
    }
