package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Extension для перевірки чи активність ще триває
 */
private val ActivityRecord.isOngoing: Boolean
    get() = this.startTime != null && this.endTime == null

/**
 * Управляє відстеженням активності для контексту
 */
class ActivityManager(
    private val activityRepository: ActivityRepository,
    private val scope: CoroutineScope,
) {
    val currentActivity: StateFlow<ActivityRecord?> =
        activityRepository
            .findLastOngoingActivityFlow()
            .stateIn(scope, SharingStarted.WhileSubscribed(ACTIVITY_SUBSCRIPTION_TIMEOUT_MS), null)

    /**
     * Спостерігає за поточною активністю
     */
    fun observeCurrentActivity() {
        scope.launch {
            currentActivity.collect { activity ->
                // Можна додати додаткову логіку при зміні активності
            }
        }
    }

    /**
     * Запускає відстеження активності для контексту
     */
    fun startActivity(contextId: String) {
        scope.launch {
            val current = currentActivity.value

            // Якщо вже є активна активність для цього контексту, не створюємо нову
            if (current?.contextId == contextId && current.isOngoing) {
                return@launch
            }

            activityRepository.startContextActivity(contextId)
        }
    }

    /**
     * Зупиняє поточну активність
     */
    fun stopActivity() {
        scope.launch {
            val activity = currentActivity.value
            if (activity != null && activity.isOngoing) {
                activityRepository.endLastActivity(System.currentTimeMillis())
            }
        }
    }

    /**
     * Отримує загальний час для контексту
     */
    suspend fun getTotalTimeForContext(contextId: String): Long {
        val activities = activityRepository.getActivitiesForContextStream(contextId).first()
        return activities.sumOf { activity ->
            val end = activity.endTime ?: System.currentTimeMillis()
            val start = activity.startTime ?: 0L
            end - start
        }
    }

    /**
     * Отримує всі активності для контексту
     */
    suspend fun getActivitiesForContext(contextId: String): List<ActivityRecord> {
        return activityRepository.getActivitiesForContextStream(contextId).first()
    }
}

private const val ACTIVITY_SUBSCRIPTION_TIMEOUT_MS = 5000L
