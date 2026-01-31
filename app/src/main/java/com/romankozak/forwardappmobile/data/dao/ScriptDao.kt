package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts WHERE contextId = :contextId")
    fun getForContext(contextId: String): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: ScriptEntity)

    @Query("DELETE FROM scripts")
    suspend fun deleteAll()

    @Update
    suspend fun update(script: ScriptEntity)

    @Delete
    suspend fun delete(script: ScriptEntity)

    @Query("SELECT * FROM scripts")
    suspend fun getAll(): List<ScriptEntity>

    @Query("SELECT * FROM scripts")
    suspend fun getAllRaw(): List<ScriptEntity>

    @Query("SELECT * FROM scripts")
    fun getAllFlow(): Flow<List<ScriptEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scripts: List<ScriptEntity>)
}
