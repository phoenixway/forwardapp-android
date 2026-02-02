package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextLogEntryTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTimeMetrics
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextTimeTrackingRepository
    @Inject
    constructor(
        private val activityRepository: ActivityRepository,
        private val listItemDao: ListItemDao,
        private val contextLogRepository: ContextLogRepository,
    ) {
        suspend fun logContextTimeSummaryForDate(
            contextId: String,
            dayToLog: Calendar,
        ) {
            val calendar = dayToLog.clone() as Calendar
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endTime = calendar.timeInMillis - 1

            val goalIds = listItemDao.getGoalIdsForContext(contextId)

            val activities =
                activityRepository.getCompletedActivitiesForContext(
                    contextId = contextId,
                    goalIds = goalIds,
                    startTime = startTime,
                    endTime = endTime,
                )

            if (activities.isEmpty()) {
                return
            }

            var totalDurationMillis: Long = 0
            val activitiesByText = activities.groupBy { it.text }

            val detailsBuilder = StringBuilder()
            detailsBuilder.append("### Деталізація за день:\n\n")

            activitiesByText.forEach { (text, records) ->
                val durationForText = records.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }
                if (durationForText > 0) {
                    totalDurationMillis += durationForText
                    val formattedDuration = formatDuration(durationForText)
                    detailsBuilder.append("- **$text**: $formattedDuration\n")
                }
            }

            if (totalDurationMillis <= 0) {
                return
            }

            val totalFormattedDuration = formatDuration(totalDurationMillis)
            val description = "Загальний час за день: $totalFormattedDuration."
            val details = detailsBuilder.toString()

            contextLogRepository.addContextLogEntry(
                contextId = contextId,
                type = ContextLogEntryTypeValues.AUTOMATIC,
                description = description,
                details = details,
            )
        }

        private fun formatDuration(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

            return if (hours > 0) {
                String.format(Locale.ROOT, "%d год %02d хв %02d с", hours, minutes, seconds)
            } else if (minutes > 0) {
                String.format(Locale.ROOT, "%d хв %02d с", minutes, seconds)
            } else {
                String.format(Locale.ROOT, "%d с", seconds)
            }
        }

        private suspend fun logTotalContextTimeSummary(contextId: String) {
            val goalIds = listItemDao.getGoalIdsForContext(contextId)
            val activities = activityRepository.getAllCompletedActivitiesForContext(contextId, goalIds)

            if (activities.isEmpty()) return

            val totalDurationMillis = activities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

            if (totalDurationMillis <= 0) return

            val totalFormattedDuration = formatDuration(totalDurationMillis)
            val description = "Загальний час по контексту: $totalFormattedDuration."

            contextLogRepository.addContextLogEntry(
                contextId = contextId,
                type = ContextLogEntryTypeValues.AUTOMATIC,
                description = description,
                details = "Розраховано на запит користувача.",
            )
        }

        suspend fun recalculateAndLogContextTime(contextId: String) {
            logContextTimeSummaryForDate(contextId, Calendar.getInstance())
            logTotalContextTimeSummary(contextId)
        }

        suspend fun calculateContextTimeMetrics(contextId: String): ContextTimeMetrics {
            val todayCalendar = Calendar.getInstance()
            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            val startTime = todayCalendar.timeInMillis
            todayCalendar.add(Calendar.DAY_OF_YEAR, 1)
            val endTime = todayCalendar.timeInMillis - 1

            val goalIds = listItemDao.getGoalIdsForContext(contextId)
            val todayActivities = activityRepository.getCompletedActivitiesForContext(contextId, goalIds, startTime, endTime)
            val timeToday = todayActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

            val allActivities = activityRepository.getAllCompletedActivitiesForContext(contextId, goalIds)
            val timeTotal = allActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

            return ContextTimeMetrics(timeToday = timeToday, timeTotal = timeTotal)
        }
    }
