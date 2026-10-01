package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOriginEntity

@Dao
interface HierarchyEstablishmentOriginDao {
    @Query(
        """
        SELECT * FROM hierarchy_establishment_origin
        WHERE hierarchyId = :hierarchyId
        LIMIT 1
        """,
    )
    suspend fun get(hierarchyId: String): HierarchyEstablishmentOriginEntity?

    @Upsert
    suspend fun upsert(state: HierarchyEstablishmentOriginEntity)
}
