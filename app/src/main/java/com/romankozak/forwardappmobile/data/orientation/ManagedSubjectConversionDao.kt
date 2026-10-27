package com.romankozak.forwardappmobile.data.orientation

import androidx.room.*
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectConversionEntity

@Dao
interface ManagedSubjectConversionDao {
    @Query("SELECT * FROM managed_subject_conversions ORDER BY subjectId, toSubjectVersion")
    suspend fun getAll(): List<ManagedSubjectConversionEntity>
    @Query("SELECT * FROM managed_subject_conversions WHERE syncedAt IS NULL ORDER BY subjectId, toSubjectVersion")
    suspend fun getUnsynced(): List<ManagedSubjectConversionEntity>
    @Query("SELECT * FROM managed_subject_conversions WHERE updatedAt > :since ORDER BY subjectId, toSubjectVersion")
    suspend fun getChangedSince(since: Long): List<ManagedSubjectConversionEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: ManagedSubjectConversionEntity)
    @Query("UPDATE managed_subject_conversions SET syncedAt=:at WHERE id=:id AND version=:version AND syncedAt IS NULL")
    suspend fun acknowledge(id: String, version: Long, at: Long): Int
    @Query("DELETE FROM managed_subject_conversions")
    suspend fun clearForExactRestore()
}
