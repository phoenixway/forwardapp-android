package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.lifecycle.ViewModel
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.common.ParsedTextData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoalItemViewModel(
    private val goal: Goal,
    private val parsedData: ParsedTextData,
    private val reminder: Reminder?
) : ViewModel() {

    private val _shouldShowStatusIcons = MutableStateFlow(false)
    val shouldShowStatusIcons: StateFlow<Boolean> = _shouldShowStatusIcons.asStateFlow()

    init {
        _shouldShowStatusIcons.value = (
            (goal.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                (reminder != null) ||
                (parsedData.icons.isNotEmpty()) ||
                (!goal.description.isNullOrBlank()) ||
                (!goal.relatedLinks.isNullOrEmpty())
            )
    }
}
