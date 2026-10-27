package com.romankozak.forwardappmobile.data.orientation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectTagRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedSubjectTagDao {
    @Query("SELECT * FROM managed_subject_tag_refs ORDER BY id")
    suspend fun getAll(): List<ManagedSubjectTagRefEntity>

    @Query("SELECT t.* FROM managed_subject_tag_refs t JOIN managed_subjects s ON s.id=t.subjectId AND s.subjectType=t.subjectType WHERE t.isDeleted=0 AND s.isDeleted=0 AND ((t.subjectType='ORIENTATION' AND EXISTS(SELECT 1 FROM orientations o WHERE o.subjectId=s.id)) OR (t.subjectType='ASPECT' AND EXISTS(SELECT 1 FROM aspects a WHERE a.subjectId=s.id))) ORDER BY t.subjectType,t.subjectId,t.normalizedTag")
    fun observeLive(): Flow<List<ManagedSubjectTagRefEntity>>

    @Query("SELECT * FROM managed_subject_tag_refs WHERE id=:id")
    suspend fun get(id: String): ManagedSubjectTagRefEntity?

    @Query("SELECT * FROM managed_subject_tag_refs WHERE subjectType=:type AND subjectId=:id")
    suspend fun getForSubject(type: String, id: String): List<ManagedSubjectTagRefEntity>

    @Query("SELECT * FROM managed_subject_tag_refs WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<ManagedSubjectTagRefEntity>

    @Query("SELECT * FROM managed_subject_tag_refs WHERE updatedAt > :timestamp")
    suspend fun getChangedSince(timestamp: Long): List<ManagedSubjectTagRefEntity>

    @Upsert suspend fun upsert(rows: List<ManagedSubjectTagRefEntity>)

    @Query("DELETE FROM managed_subject_tag_refs")
    suspend fun clear()

    @Query("UPDATE managed_subject_tag_refs SET syncedAt=:now WHERE id=:id AND version=:expectedVersion AND syncedAt IS NULL")
    suspend fun markSynced(id: String, expectedVersion: Long, now: Long): Int
}
