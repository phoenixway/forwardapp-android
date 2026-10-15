package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Local-only commit marker for the finite V3.5 Tactical materialization. */
@Entity(tableName = "tactical_planning_authority_state")
data class TacticalPlanningAuthorityStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val version: Int,
    val activatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface TacticalPlanningAuthorityStateDao {
    @Query("SELECT * FROM tactical_planning_authority_state WHERE id = 1 LIMIT 1")
    suspend fun get(): TacticalPlanningAuthorityStateEntity?

    @Query("SELECT * FROM tactical_planning_authority_state WHERE id = 1 LIMIT 1")
    fun observe(): Flow<TacticalPlanningAuthorityStateEntity?>

    @Upsert
    suspend fun upsert(state: TacticalPlanningAuthorityStateEntity)
}
