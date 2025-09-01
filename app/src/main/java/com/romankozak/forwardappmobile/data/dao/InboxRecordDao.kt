package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxRecordDao {

    @Query("SELECT * FROM inbox_records WHERE projectId = :projectId ORDER BY item_order DESC")
    fun getRecordsForProjectStream(projectId: String): Flow<List<InboxRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InboxRecord)

    @Update
    suspend fun update(record: InboxRecord)

    @Query("DELETE FROM inbox_records WHERE id = :id")
    suspend fun deleteById(id: String)

    // --- Методи для імпорту/експорту ---

    @Query("SELECT * FROM inbox_records")
    suspend fun getAll(): List<InboxRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InboxRecord>)

    @Query("DELETE FROM inbox_records")
    suspend fun deleteAll()
}