package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalDayThemeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayThemeAssignmentDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.data.database.DayThemeCanonicalBootstrapStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanonicalDayThemeDao {
    @Query("SELECT version FROM day_theme_canonical_bootstrap_state WHERE id = 1 LIMIT 1")
    suspend fun getBootstrapVersion(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBootstrapState(state: DayThemeCanonicalBootstrapStateEntity)

    @Query("SELECT * FROM theme_definitions")
    suspend fun getAllThemeDefinitionsSync(): List<ThemeDefinitionEntity>

    @Query(
        """
        SELECT * FROM theme_definitions
        WHERE syncedAt IS NULL
        ORDER BY updatedAt ASC, id ASC
        """,
    )
    suspend fun getUnsyncedThemeDefinitionsForSync(): List<ThemeDefinitionEntity>

    @Query(
        """
        SELECT * FROM theme_definitions
        WHERE updatedAt > :timestamp
        ORDER BY updatedAt ASC, id ASC
        """,
    )
    suspend fun getThemeDefinitionsChangedSinceForSync(timestamp: Long): List<ThemeDefinitionEntity>

    @Query("SELECT * FROM theme_definitions")
    fun observeAllThemeDefinitions(): Flow<List<ThemeDefinitionEntity>>

    @Query("SELECT * FROM theme_definitions WHERE id = :id LIMIT 1")
    suspend fun getThemeDefinitionById(id: String): ThemeDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThemeDefinitions(items: List<ThemeDefinitionEntity>)

    @Query("SELECT * FROM day_themes")
    suspend fun getAllDayThemesSync(): List<CanonicalDayThemeEntity>

    @Query("SELECT * FROM day_themes WHERE isDeleted = 0 AND isActive = 1 ORDER BY dayPlanId ASC, `order` ASC")
    fun observeAllActiveDayThemes(): Flow<List<CanonicalDayThemeEntity>>

    @Query(
        """
        SELECT * FROM day_themes
        WHERE syncedAt IS NULL
        ORDER BY updatedAt ASC, id ASC
        """,
    )
    suspend fun getUnsyncedDayThemesForSync(): List<CanonicalDayThemeEntity>

    @Query(
        """
        SELECT * FROM day_themes
        WHERE updatedAt > :timestamp
        ORDER BY updatedAt ASC, id ASC
        """,
    )
    suspend fun getDayThemesChangedSinceForSync(timestamp: Long): List<CanonicalDayThemeEntity>

    @Query("SELECT * FROM day_themes WHERE id = :id LIMIT 1")
    suspend fun getDayThemeById(id: String): CanonicalDayThemeEntity?

    @Query("SELECT * FROM day_themes WHERE dayPlanId = :dayPlanId")
    suspend fun getDayThemesForDay(dayPlanId: String): List<CanonicalDayThemeEntity>

    @Query("SELECT * FROM day_themes WHERE dayPlanId = :dayPlanId")
    fun observeDayThemesForDay(dayPlanId: String): Flow<List<CanonicalDayThemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayThemes(items: List<CanonicalDayThemeEntity>)

    @Query("SELECT * FROM day_theme_assignment_documents")
    suspend fun getAllAssignmentDocumentsSync(): List<DayThemeAssignmentDocumentEntity>

    @Query(
        """
        SELECT * FROM day_theme_assignment_documents
        WHERE syncedAt IS NULL
        ORDER BY updatedAt ASC, dayPlanId ASC
        """,
    )
    suspend fun getUnsyncedAssignmentDocumentsForSync(): List<DayThemeAssignmentDocumentEntity>

    @Query(
        """
        SELECT * FROM day_theme_assignment_documents
        WHERE updatedAt > :timestamp
        ORDER BY updatedAt ASC, dayPlanId ASC
        """,
    )
    suspend fun getAssignmentDocumentsChangedSinceForSync(timestamp: Long): List<DayThemeAssignmentDocumentEntity>

    @Query("SELECT * FROM day_theme_assignment_documents WHERE dayPlanId = :dayPlanId LIMIT 1")
    suspend fun getAssignmentDocumentByDayPlanId(dayPlanId: String): DayThemeAssignmentDocumentEntity?

    @Query("SELECT * FROM day_theme_assignment_documents WHERE dayPlanId = :dayPlanId LIMIT 1")
    fun observeAssignmentDocumentByDayPlanId(dayPlanId: String): Flow<DayThemeAssignmentDocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignmentDocuments(items: List<DayThemeAssignmentDocumentEntity>)

    @Query(
        """
        UPDATE theme_definitions
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markThemeDefinitionSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query(
        """
        UPDATE day_themes
        SET syncedAt = :syncedAt
        WHERE id = :id
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markDayThemeSyncedIfVersionMatches(
        id: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query(
        """
        UPDATE day_theme_assignment_documents
        SET syncedAt = :syncedAt
        WHERE dayPlanId = :dayPlanId
          AND version = :expectedVersion
          AND syncedAt IS NULL
        """,
    )
    suspend fun markAssignmentDocumentSyncedIfVersionMatches(
        dayPlanId: String,
        expectedVersion: Long,
        syncedAt: Long,
    ): Int

    @Query("DELETE FROM day_theme_assignment_documents")
    suspend fun deleteAllAssignmentDocuments()

    @Query("DELETE FROM day_themes")
    suspend fun deleteAllDayThemes()

    @Query("DELETE FROM theme_definitions")
    suspend fun deleteAllThemeDefinitions()
}
