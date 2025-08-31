// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/repository/ActivityRepository.kt ---
package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityRecordDao: ActivityRecordDao
) {
    fun getLogStream(): Flow<List<ActivityRecord>> = activityRecordDao.getAllRecordsStream()

    suspend fun addTimelessRecord(text: String) {
        if (text.isBlank()) return
        // ✨ ВИПРАВЛЕНО: Додано всі необхідні параметри до конструктора
        val record = ActivityRecord(
            id = UUID.randomUUID().toString(),
            text = text,
            createdAt = System.currentTimeMillis(),
            startTime = null,
            endTime = null
        )
        activityRecordDao.insert(record)
    }

    suspend fun startActivity(text: String, startTime: Long) {
        if (text.isBlank()) return
        // ✨ ВИПРАВЛЕНО: Додано всі необхідні параметри до конструктора
        val newRecord = ActivityRecord(
            id = UUID.randomUUID().toString(),
            text = text,
            createdAt = System.currentTimeMillis(),
            startTime = startTime,
            endTime = null
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

    suspend fun updateRecord(record: ActivityRecord) {
        activityRecordDao.update(record)
    }

    suspend fun clearLog() {
        activityRecordDao.clearAll()
    }

    suspend fun deleteRecord(record: ActivityRecord) {
        activityRecordDao.delete(record)
    }
}