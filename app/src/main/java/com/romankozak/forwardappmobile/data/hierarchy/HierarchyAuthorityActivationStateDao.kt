package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.data.database.HierarchyAuthorityActivationStateEntity

@Dao
interface HierarchyAuthorityActivationStateDao {
    @Query(
        """
        SELECT * FROM hierarchy_authority_activation_state
        WHERE hierarchyId = :hierarchyId
        LIMIT 1
        """,
    )
    suspend fun get(hierarchyId: String): HierarchyAuthorityActivationStateEntity?

    @Upsert
    suspend fun upsert(state: HierarchyAuthorityActivationStateEntity)
}
