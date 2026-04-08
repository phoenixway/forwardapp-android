package com.romankozak.forwardappmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.romankozak.forwardappmobile.core.data.models.entities.FreshnessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelId
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeManagementLevelStatusDao {
    @Query("SELECT * FROM life_management_level_statuses ORDER BY `levelId` ASC")
    fun observeAll(): Flow<List<LifeManagementLevelStatusEntity>>

    @Query("SELECT * FROM life_management_level_statuses ORDER BY `levelId` ASC")
    suspend fun getAll(): List<LifeManagementLevelStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LifeManagementLevelStatusEntity>)

    @Query(
        """
        UPDATE life_management_level_statuses
        SET freshnessStatus = :freshnessStatus, updatedAt = :updatedAt
        WHERE levelId IN (:levelIds)
        """,
    )
    suspend fun updateFreshnessForLevels(
        levelIds: List<LifeManagementLevelId>,
        freshnessStatus: FreshnessStatus,
        updatedAt: Long,
    )
}
