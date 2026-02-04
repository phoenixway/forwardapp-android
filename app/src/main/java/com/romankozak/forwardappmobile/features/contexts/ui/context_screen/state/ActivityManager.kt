package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

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
    private val contextRepository: ContextRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    val currentActivity: StateFlow<ActivityRecord?> = activityRepository
        .getCurrentActivityFlow()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

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

            // Зупиняємо попередню активність якщо є
            current?.let {
                if (it.isOngoing) {
                    activityRepository.endActivity(it.id)
                }
            }

            // Створюємо нову активність
            val newActivity = ActivityRecord(
                id = UUID.randomUUID().toString(),
                contextId = contextId,
                startTime = System.currentTimeMillis(),
                endTime = null,
                description = null
            )
            
            activityRepository.createActivity(newActivity)
        }
    }

    /**
     * Зупиняє поточну активність
     */
    fun stopActivity() {
        scope.launch {
            val activity = currentActivity.value
            if (activity != null && activity.isOngoing) {
                activityRepository.endActivity(activity.id)
            }
        }
    }

    /**
     * Отримує загальний час для контексту
     */
    suspend fun getTotalTimeForContext(contextId: String): Long {
        val activities = activityRepository.getActivitiesForContext(contextId)
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
        return activityRepository.getActivitiesForContext(contextId)
    }
}
