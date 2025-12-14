package com.romankozak.forwardappmobile.domain.ai.actuators

import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.romankozak.forwardappmobile.domain.ai.policy.AiDecision
import com.romankozak.forwardappmobile.domain.ai.policy.UiAdaptationMode
import javax.inject.Inject

interface AiActuator {
    fun apply(decision: AiDecision)
}

class UiAdaptationActuator @Inject constructor() : AiActuator {
    override fun apply(decision: AiDecision) {
        if (decision is AiDecision.AdaptUi) {
            Log.d("AiActuator", "UI adaptation requested: ${decision.mode}")
            // Hook for future: update shared UI settings or feature flags.
        }
    }
}

class RecommendationActuator @Inject constructor() : AiActuator {
    override fun apply(decision: AiDecision) {
        if (decision is AiDecision.ShowRecommendation) {
            Log.d("AiActuator", "Show recommendation ${decision.id} priority=${decision.priority}")
        }
    }
}

class WorkerSchedulerActuator @Inject constructor(
    private val workManager: WorkManager,
) : AiActuator {
    override fun apply(decision: AiDecision) {
        if (decision is AiDecision.ScheduleWorker) {
            val request = OneTimeWorkRequestBuilder<DummyScheduledWorker>()
                .setInitialDelay(decision.delayMinutes, java.util.concurrent.TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniqueWork(decision.worker, ExistingWorkPolicy.KEEP, request)
        }
    }
}

/**
 * Placeholder worker for scheduled actions; replace with concrete workers when wiring policies.
 */
class DummyScheduledWorker(appContext: android.content.Context, params: androidx.work.WorkerParameters) :
    androidx.work.CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Log.d("AiActuator", "DummyScheduledWorker executed")
        return Result.success()
    }
}
