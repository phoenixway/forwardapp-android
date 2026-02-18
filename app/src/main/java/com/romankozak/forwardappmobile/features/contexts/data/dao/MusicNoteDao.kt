package com.romankozak.forwardappmobile.features.contexts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(musicNote: MusicNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MusicNoteEntity>)

    @Update
    suspend fun update(musicNote: MusicNoteEntity)

    @Query("SELECT * FROM music_notes WHERE id = :id")
    suspend fun getById(id: String): MusicNoteEntity?

    @Query(
        """
        SELECT mn.*
        FROM music_notes AS mn
        INNER JOIN attachments AS a
            ON a.entity_id = mn.id AND a.attachment_type = :attachmentType
        INNER JOIN context_attachment_cross_ref AS link
            ON link.attachment_id = a.id
        WHERE link.context_id = :contextId
        ORDER BY mn.updatedAt DESC
        """,
    )
    fun getForContext(
        contextId: String,
        attachmentType: String,
    ): Flow<List<MusicNoteEntity>>

    @Query("SELECT * FROM music_notes")
    fun getAllAsFlow(): Flow<List<MusicNoteEntity>>

    @Query("SELECT * FROM music_notes")
    suspend fun getAll(): List<MusicNoteEntity>

    @Query("SELECT * FROM music_notes WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): MusicNoteEntity?

    @Query("DELETE FROM music_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM music_notes")
    suspend fun deleteAll()
}
