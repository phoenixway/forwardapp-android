package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
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
        private val goalListDao: GoalListDao,
    ) {
        fun getLogStream(): Flow<List<ActivityRecord>> = activityRecordDao.getAllRecordsStream()

        suspend fun addTimelessRecord(text: String) {
            if (text.isBlank()) return
            val record =
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    createdAt = System.currentTimeMillis(),
                    startTime = null,
                    endTime = null,
                )
            activityRecordDao.insert(record)
        }

        suspend fun startActivity(
            text: String,
            startTime: Long,
        ): ActivityRecord {
            endLastActivity(startTime)
            val newRecord =
                ActivityRecord(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    createdAt = System.currentTimeMillis(),
                    startTime = startTime,
                    endTime = null,
                )
            activityRecordDao.insert(newRecord)
            return newRecord
        }

        suspend fun endLastActivity(endTime: Long) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivity()
            ongoingActivity?.let {
                val finishedActivity = it.copy(endTime = endTime)
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
                )
            activityRecordDao.insert(newRecord)
            return newRecord
        }

        suspend fun endGoalActivity(goalId: String) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivityForGoal(goalId)
            ongoingActivity?.let {
                val finishedActivity = it.copy(endTime = System.currentTimeMillis())
                activityRecordDao.update(finishedActivity)
            }
        }

        suspend fun startListActivity(listId: String): ActivityRecord? {
            val list = goalListDao.getGoalListById(listId) ?: return null
            val now = System.currentTimeMillis()
            endLastActivity(now)
            val newRecord =
                ActivityRecord(
                    text = list.name,
                    startTime = now,
                    listId = listId,
                )
            activityRecordDao.insert(newRecord)
            return newRecord
        }

        suspend fun endListActivity(listId: String) {
            val ongoingActivity = activityRecordDao.findLastOngoingActivityForList(listId)
            ongoingActivity?.let {
                val finishedActivity = it.copy(endTime = System.currentTimeMillis())
                activityRecordDao.update(finishedActivity)
            }
        }

        suspend fun updateRecord(record: ActivityRecord) {
            activityRecordDao.update(record)
        }

        suspend fun clearLog() {
            activityRecordDao.clearAll()
        }

        suspend fun deleteRecord(record: ActivityRecord) {
            activityRecordDao.delete(record)
        }

        suspend fun searchActivities(query: String): List<ActivityRecord> = activityRecordDao.search(query)

        suspend fun getCompletedActivitiesForProject(
            listId: String,
            goalIds: List<String>,
            startTime: Long,
            endTime: Long,
        ): List<ActivityRecord> = activityRecordDao.getCompletedActivitiesForProject(listId, goalIds, startTime, endTime)

        suspend fun getAllCompletedActivitiesForProject(
            projectId: String,
            goalIds: List<String>,
        ): List<ActivityRecord> = activityRecordDao.getAllCompletedActivitiesForProject(projectId, goalIds)

    // Цей метод потрібен для DayManagementRepository
    suspend fun getActivityRecordById(recordId: String): ActivityRecord? {
        // Припускається, що у вас є activityRecordDao в цьому репозиторії
        return activityRecordDao.findById(recordId) // Назва методу в DAO може відрізнятись
    }

}
