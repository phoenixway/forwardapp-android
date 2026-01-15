package com.romankozak.forwardappmobile.domain.lifecontext

import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import javax.inject.Inject

class SubmitContextInputUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val lifeContextProcessor: LifeContextProcessor,
) {

    suspend operator fun invoke(text: String) {
        val timestamp = System.currentTimeMillis()
        activityRepository.addTimelessRecord(text, timestamp)
        lifeContextProcessor.process(
            TrackerCommentSignal(
                text = text,
                timestamp = timestamp,
            )
        )
    }
}
