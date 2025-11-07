package com.romankozak.forwardappmobile.ui.screens.mainscreen.domain.usecases

import com.romankozak.forwardappmobile.core.database.models.ActivityRecord
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
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
