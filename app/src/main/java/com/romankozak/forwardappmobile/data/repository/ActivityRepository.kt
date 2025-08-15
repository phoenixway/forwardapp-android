// Файл: app/src/main/java/com/romankozak/forwardappmobile/data/repository/ActivityRepository.kt

package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityRecordDao: ActivityRecordDao
) {
    fun getLogStream(): Flow<List<ActivityRecord>> = activityRecordDao.getAllRecordsStream()

    suspend fun addTimelessRecord(text: String) {
        if (text.isBlank()) return
        val record = ActivityRecord(text = text)
        activityRecordDao.insert(record)
    }

    suspend fun startActivity(text: String, startTime: Long) {
        if (text.isBlank()) return
        val newRecord = ActivityRecord(
            text = text,
            startTime = startTime
        )
        activityRecordDao.insert(newRecord)
    }


    suspend fun endLastActivity(endTime: Long) {
        val ongoingActivity = activityRecordDao.findLastOngoingActivity()
        ongoingActivity?.let {
            val finishedActivity = it.copy(endTime = endTime)
            activityRecordDao.update(finishedActivity)
        }
    }

    // ✨ НОВА ФУНКЦІЯ: Оновлює існуючий запис в базі даних
    suspend fun updateRecord(record: ActivityRecord) {
        activityRecordDao.update(record)
    }

    suspend fun clearLog() {
        activityRecordDao.clearAll()
    }
}