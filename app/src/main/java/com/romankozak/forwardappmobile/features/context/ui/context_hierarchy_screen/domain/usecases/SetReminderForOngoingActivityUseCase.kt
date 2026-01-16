package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.domain.usecases

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.MainScreenUiState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class SetReminderForOngoingActivityUseCase @Inject constructor() {
    operator fun invoke(
        lastOngoingActivity: ActivityRecord?,
        uiState: MutableStateFlow<MainScreenUiState>
    ) {
        lastOngoingActivity?.let {
            uiState.value = uiState.value.copy(recordForReminderDialog = lastOngoingActivity)
        }
    }
}
