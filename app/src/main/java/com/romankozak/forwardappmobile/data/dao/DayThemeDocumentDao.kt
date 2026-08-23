package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayThemeDocumentEntity

/**
 * Quarantined legacy Day Theme storage.
 *
 * Modern runtime, restore, merge and sync must never write this table. The only
 * remaining read is the one-time pre-canonical bootstrap. deleteAll exists only
 * for legacy database maintenance/clearing.
 */
@Dao
interface DayThemeDocumentDao {
    @Query("SELECT * FROM day_theme_documents")
    suspend fun getAllSync(): List<DayThemeDocumentEntity>

    @Query("DELETE FROM day_theme_documents")
    suspend fun deleteAll()
}
