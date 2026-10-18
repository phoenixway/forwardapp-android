package com.romankozak.forwardappmobile.data.hierarchy.container

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BigPicturePresentationDao {
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
