package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecordRepository @Inject constructor(
    private val activityRecordDao: ActivityRecordDao,
) {
    suspend fun getRecentRecords(daysBack: Int = 7): List<ActivityRecord> {
        val now = System.currentTimeMillis()
        val from = now - TimeUnit.DAYS.toMillis(daysBack.toLong())
        return activityRecordDao.getRecordsFrom(from)
    }

    suspend fun getRecordsBetween(
        startTimestamp: Long,
        endTimestamp: Long,
    ): List<ActivityRecord> = activityRecordDao.getRecordsBetween(startTimestamp, endTimestamp)
}
