package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InboxRecord)

    @Update
    suspend fun update(record: InboxRecord)

    @Delete
    suspend fun delete(record: InboxRecord)

    @Query("SELECT * FROM inbox_records WHERE projectId = :projectId ORDER BY item_order DESC")
    fun getRecordsForProjectStream(projectId: String): Flow<List<InboxRecord>>

    @Query("DELETE FROM inbox_records WHERE id = :recordId")
    suspend fun deleteById(recordId: String)
}