// Файл: app/src/main/java/com/romankozak/forwardappmobile/data/dao/ActivityRecordDao.kt

package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActivityRecord)

    @Update
    suspend fun update(record: ActivityRecord)

    // ✨ ОНОВЛЕНО: Змінено сортування на ASC, щоб старіші записи були першими
    @Query("SELECT * FROM activity_records ORDER BY createdAt ASC")
    fun getAllRecordsStream(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records WHERE endTime IS NULL AND startTime IS NOT NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun findLastOngoingActivity(): ActivityRecord?

    @Query("DELETE FROM activity_records")
    suspend fun clearAll()

    @Delete
    suspend fun delete(record: ActivityRecord)
}