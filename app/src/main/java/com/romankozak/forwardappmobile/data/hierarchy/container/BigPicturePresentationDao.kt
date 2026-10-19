package com.romankozak.forwardappmobile.data.hierarchy.container

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BigPicturePresentationDao {
    /** Fast steady-state admission gate. Tombstones count as durable materialization receipts. */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM legacy_subject_mappings m
            JOIN managed_subjects s ON s.id = m.subjectId AND s.isDeleted = 0 AND s.subjectType = 'ORIENTATION'
            JOIN orientations o ON o.subjectId = s.id AND o.kind = 'MAIN_BEACON_GROUP'
            LEFT JOIN big_picture_containers c ON c.id = 'historical-group:v1:' || m.subjectId
            WHERE m.isDeleted = 0 AND m.state = 'CUT_OVER' AND m.sourceType = 'MAIN_BEACON_GROUP' AND c.id IS NULL
        ) OR EXISTS(
            SELECT 1 FROM hierarchy_placement_group_scopes WHERE isDeleted = 0 AND groupSubjectId IS NOT NULL
        )
    """)
    suspend fun needsHistoricalGroupCutover(): Boolean

    @Query("SELECT * FROM big_picture_containers ORDER BY id")
    suspend fun containers(): List<BigPictureContainerEntity>
    @Query("SELECT * FROM big_picture_memberships ORDER BY placementId")
    suspend fun memberships(): List<BigPictureMembershipEntity>
    @Upsert suspend fun upsertContainers(rows: List<BigPictureContainerEntity>)
    @Upsert suspend fun upsertMemberships(rows: List<BigPictureMembershipEntity>)
    @Query("DELETE FROM big_picture_memberships") suspend fun clearMemberships()
    @Query("DELETE FROM big_picture_containers") suspend fun clearContainers()
    @Query("UPDATE big_picture_containers SET syncedAt = :now WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun acknowledgeContainer(id: String, version: Long, now: Long)
    @Query("UPDATE big_picture_memberships SET syncedAt = :now WHERE placementId = :id AND version = :version AND syncedAt IS NULL")
    suspend fun acknowledgeMembership(id: String, version: Long, now: Long)
}
