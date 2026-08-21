package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayThemeDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayThemeDocumentDao {
    @Query("SELECT * FROM day_theme_documents WHERE dayPlanId = :dayPlanId LIMIT 1")
    fun observe(dayPlanId: String): Flow<DayThemeDocumentEntity?>

    @Query("SELECT * FROM day_theme_documents WHERE dayPlanId = :dayPlanId LIMIT 1")
    suspend fun getByDayPlanId(dayPlanId: String): DayThemeDocumentEntity?

    @Query("SELECT * FROM day_theme_documents")
    suspend fun getAllSync(): List<DayThemeDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DayThemeDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(documents: List<DayThemeDocumentEntity>)

    @Query(
        """
        UPDATE day_theme_documents
        SET syncedAt = :syncedAt
        WHERE dayPlanId = :dayPlanId AND version = :expectedVersion
        """,
    )
    suspend fun markSyncedIfVersionMatches(
        dayPlanId: String,
        expectedVersion: Long,
        syncedAt: Long,
    )

    @Query("DELETE FROM day_theme_documents")
    suspend fun deleteAll()
}
