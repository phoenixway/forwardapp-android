package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectLogEntryTypeValues
import com.romankozak.forwardappmobile.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.ProjectExecutionLogRepository
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectTimeTrackingRepository @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val listItemDao: ListItemDao,
    private val projectLogRepository: ProjectExecutionLogRepository
) {
    suspend fun logProjectTimeSummaryForDate(
        projectId: String,
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

        val goalIds = listItemDao.getGoalIdsForProject(projectId)

        val activities =
            activityRepository.getCompletedActivitiesForProject(
                projectId = projectId,
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

        projectLogRepository.addProjectLogEntry(projectId = projectId, type = ProjectLogEntryTypeValues.AUTOMATIC, description = description, details = details)
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

    private suspend fun logTotalProjectTimeSummary(projectId: String) {
        val goalIds = listItemDao.getGoalIdsForProject(projectId)
        val activities = activityRepository.getAllCompletedActivitiesForProject(projectId, goalIds)

        if (activities.isEmpty()) return

        val totalDurationMillis = activities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        if (totalDurationMillis <= 0) return

        val totalFormattedDuration = formatDuration(totalDurationMillis)
        val description = "Загальний час по проекту: $totalFormattedDuration."

        projectLogRepository.addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryTypeValues.AUTOMATIC,
            description = description,
            details = "Розраховано на запит користувача.",
        )
    }

    suspend fun recalculateAndLogProjectTime(projectId: String) {
        logProjectTimeSummaryForDate(projectId, Calendar.getInstance())
        logTotalProjectTimeSummary(projectId)
    }

    suspend fun calculateProjectTimeMetrics(projectId: String): ProjectTimeMetrics {
        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        val startTime = todayCalendar.timeInMillis
        todayCalendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = todayCalendar.timeInMillis - 1

        val goalIds = listItemDao.getGoalIdsForProject(projectId)
        val todayActivities = activityRepository.getCompletedActivitiesForProject(projectId, goalIds, startTime, endTime)
        val timeToday = todayActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        val allActivities = activityRepository.getAllCompletedActivitiesForProject(projectId, goalIds)
        val timeTotal = allActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        return ProjectTimeMetrics(timeToday = timeToday, timeTotal = timeTotal)
    }
}
