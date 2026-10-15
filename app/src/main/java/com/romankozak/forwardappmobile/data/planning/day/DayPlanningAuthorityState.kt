package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Local-only marker for the finite Day planning authority cutover.
 *
 * Presence promises that every legacy DayPlan has an equivalent canonical DAY
 * scope and extension and that every eligible Day Theme participation has an
 * equivalent DAY.THEME commitment. It is never transported.
 */
@Entity(tableName = "day_planning_authority_state")
data class DayPlanningAuthorityStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val version: Int,
    val activatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface DayPlanningAuthorityStateDao {
    @Query("SELECT * FROM day_planning_authority_state WHERE id = 1 LIMIT 1")
    suspend fun get(): DayPlanningAuthorityStateEntity?

    @Upsert
    suspend fun upsert(state: DayPlanningAuthorityStateEntity)
}
