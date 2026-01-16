package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.domain.usecases

import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import javax.inject.Inject

class StopOngoingActivityUseCase @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke() {
        activityRepository.endLastActivity(System.currentTimeMillis())
    }
}
