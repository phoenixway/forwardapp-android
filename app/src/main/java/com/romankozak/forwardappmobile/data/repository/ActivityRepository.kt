package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.domain.ai.events.ActivityFinishedEvent
import com.romankozak.forwardappmobile.domain.ai.events.ActivityLoggedEvent
import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository
    @Inject
    constructor(
        private val activityRecordDao: ActivityRecordDao,
        private val goalDao: GoalDao,
        private val projectDao: ProjectDao,
        private val aiEventRepository: AiEventRepository,
    ) {
        fun getLogStream(): Flow<List<ActivityRecord>> = activityRecordDao.getAllRecordsStream()

        suspend fun addTimelessRecord(text: String, timestamp: Long = System.currentTimeMillis()) {
            if (text.isBlank()) return
            val record =
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    createdAt = timestamp,
                    startTime = null,
                    endTime = null,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = timestamp,
                    syncedAt = null,
                    version = 1,
                )
            activityRecordDao.insert(record)
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(timestamp),
                    durationMinutes = 0,
                    xp = record.xpGained ?: 0,
                    antiXp = record.antyXp ?: 0,
                    isOngoing = false,
                )
            )
        }

        suspend fun startActivity(
            text: String,
            startTime: Long,
        ): ActivityRecord {
            endLastActivity(startTime)
            val now = System.currentTimeMillis()
            val newRecord =
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    createdAt = now,
                    startTime = startTime,
                    endTime = null,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            activityRecordDao.insert(newRecord)
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = 0,
                    antiXp = 0,
                    isOngoing = true,
                )
            )
            return newRecord
        }

        suspend fun endLastActivity(endTime: Long) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivity()
            ongoingActivity?.let {
                val finishedActivity = it.copy(
                    endTime = endTime,
                    updatedAt = endTime,
                    syncedAt = null,
                    version = it.version + 1,
                )
                activityRecordDao.update(finishedActivity)
            }
        }

        suspend fun startGoalActivity(goalId: String): ActivityRecord? {
            val goal = goalDao.getGoalById(goalId) ?: return null
            val now = System.currentTimeMillis()
            endLastActivity(now)
            val newRecord =
                ActivityRecord(
                    text = goal.text,
                    startTime = now,
                    goalId = goalId,
                    createdAt = now,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            activityRecordDao.insert(newRecord)
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = 0,
                    antiXp = 0,
                    isOngoing = true,
                )
            )
            return newRecord
        }

        suspend fun endGoalActivity(goalId: String) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivityForGoal(goalId)
            ongoingActivity?.let {
                val finishedActivity = it.copy(
                    endTime = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncedAt = null,
                    version = it.version + 1,
                )
                activityRecordDao.update(finishedActivity)
                val end = finishedActivity.endTime ?: finishedActivity.createdAt
                val duration = ((end - (finishedActivity.startTime ?: end)) / 60000L).toInt().coerceAtLeast(0)
                aiEventRepository.emit(
                    ActivityFinishedEvent(
                        timestamp = java.time.Instant.ofEpochMilli(end),
                        durationMinutes = duration,
                        xp = finishedActivity.xpGained ?: 0,
                        antiXp = finishedActivity.antyXp ?: 0,
                    )
                )
            }
        }

        suspend fun startProjectActivity(projectId: String): ActivityRecord? {
            val project = projectDao.getProjectById(projectId) ?: return null
            val now = System.currentTimeMillis()
            endLastActivity(now)
            val newRecord =
                ActivityRecord(
                    text = project.name,
                    startTime = now,
                    projectId = projectId,
                    createdAt = now,
                    xpGained = null,
                    antyXp = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            activityRecordDao.insert(newRecord)
            aiEventRepository.emit(
                ActivityLoggedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = 0,
                    antiXp = 0,
                    isOngoing = true,
                )
            )
            return newRecord
        }

        suspend fun addCompletedActivity(text: String, xpGained: Int?, antyXp: Int?) {
            if (text.isBlank()) return
            val now = System.currentTimeMillis()
            val record =
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    createdAt = now,
                    startTime = now,
                    endTime = now,
                    xpGained = xpGained,
                    antyXp = antyXp,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            activityRecordDao.insert(record)
            aiEventRepository.emit(
                ActivityFinishedEvent(
                    timestamp = java.time.Instant.ofEpochMilli(now),
                    durationMinutes = 0,
                    xp = xpGained ?: 0,
                    antiXp = antyXp ?: 0,
                )
            )
        }

        suspend fun endProjectActivity(projectId: String) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivityForProject(projectId)
            ongoingActivity?.let {
                val now = System.currentTimeMillis()
                val finishedActivity = it.copy(
                    endTime = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = it.version + 1,
                )
                activityRecordDao.update(finishedActivity)
                val duration = ((now - (finishedActivity.startTime ?: now)) / 60000L).toInt().coerceAtLeast(0)
                aiEventRepository.emit(
                    ActivityFinishedEvent(
                        timestamp = java.time.Instant.ofEpochMilli(now),
                        durationMinutes = duration,
                        xp = finishedActivity.xpGained ?: 0,
                        antiXp = finishedActivity.antyXp ?: 0,
                    )
                )
            }
        }

        suspend fun updateRecord(record: ActivityRecord) {
            activityRecordDao.update(record.bumpSync())
        }

        suspend fun clearLog() {
            activityRecordDao.clearAll()
        }

        suspend fun deleteRecord(record: ActivityRecord) {
            // Changed the logic here to correctly delete an activity
            activityRecordDao.deleteById(record.id)
        }

        suspend fun searchActivities(query: String): List<ActivityRecord> = activityRecordDao.search(query)

        suspend fun getCompletedActivitiesForProject(
            projectId: String,
            goalIds: List<String>,
            startTime: Long,
            endTime: Long,
        ): List<ActivityRecord> = activityRecordDao.getCompletedActivitiesForProject(projectId, goalIds, startTime, endTime)

        suspend fun getAllCompletedActivitiesForProject(
            projectId: String,
            goalIds: List<String>,
        ): List<ActivityRecord> = activityRecordDao.getAllCompletedActivitiesForProject(projectId, goalIds)

        suspend fun getActivityRecordById(recordId: String): ActivityRecord? {
            return activityRecordDao.findById(recordId)
        }
    }
