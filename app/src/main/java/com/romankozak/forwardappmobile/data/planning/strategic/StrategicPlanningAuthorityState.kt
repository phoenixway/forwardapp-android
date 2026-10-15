package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/** Local-only commit marker for finite V3.6 Strategic planning establishment. */
@Entity(tableName = "strategic_planning_authority_state")
data class StrategicPlanningAuthorityStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val version: Int,
    val activatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface StrategicPlanningAuthorityStateDao {
    @Query("SELECT * FROM strategic_planning_authority_state WHERE id = 1 LIMIT 1")
    suspend fun get(): StrategicPlanningAuthorityStateEntity?

    @Upsert
    suspend fun upsert(state: StrategicPlanningAuthorityStateEntity)
}
