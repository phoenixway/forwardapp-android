package com.romankozak.forwardappmobile.domain.lifecontext

import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import javax.inject.Inject

class StartContextTrackingUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val lifeContextProcessor: LifeContextProcessor,
) {
    suspend operator fun invoke(text: String) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        activityRepository.startActivity(text, now)
        lifeContextProcessor.process(
            TrackerCommentSignal(
                text = text,
                timestamp = now,
            )
        )
    }
}
