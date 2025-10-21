package com.romankozak.forwardappmobile.ui.screens.mainscreen.domain.usecases

import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import javax.inject.Inject

class StopOngoingActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke() {
        activityRepository.endLastActivity(System.currentTimeMillis())
    }
}
