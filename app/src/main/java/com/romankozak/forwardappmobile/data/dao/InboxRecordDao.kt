package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.core.database.models.InboxRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxRecordDao {
    @Query("SELECT * FROM inbox_records WHERE projectId = :projectId ORDER BY item_order DESC")
    fun getRecordsForProjectStream(projectId: String): Flow<List<InboxRecord>>

    @Query("SELECT * FROM inbox_records WHERE id = :id")
    suspend fun getRecordById(id: String): InboxRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InboxRecord)

    @Update
    suspend fun update(record: InboxRecord)

    @Query("DELETE FROM inbox_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM inbox_records WHERE text LIKE :query ORDER BY createdAt DESC")
    suspend fun searchInboxRecordsGlobal(query: String): List<InboxRecord>

    @Query("SELECT * FROM inbox_records")
    suspend fun getAll(): List<InboxRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InboxRecord>)

    @Query("DELETE FROM inbox_records")
    suspend fun deleteAll()
}
