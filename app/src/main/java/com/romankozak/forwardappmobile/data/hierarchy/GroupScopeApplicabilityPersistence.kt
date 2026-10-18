package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.*
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.GroupScopeApplicabilityRecord

enum class GroupScopeApplicabilityMode { NATIVE, HISTORICAL, LEGACY_UNKNOWN }

@Entity(
    tableName = "hierarchy_group_scope_applicability",
    foreignKeys = [ForeignKey(entity = HierarchyPlacementEntity::class,
        parentColumns = ["id", "hierarchyId"], childColumns = ["placementId", "hierarchyId"],
        onDelete = ForeignKey.NO_ACTION, onUpdate = ForeignKey.NO_ACTION, deferred = true)],
    indices = [Index(value = ["placementId", "hierarchyId"]), Index("updatedAt"), Index("syncedAt")],
)
data class GroupScopeApplicabilityEntity(
    @PrimaryKey val placementId: String,
    val hierarchyId: String,
    val mode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
) {
    constructor(row: GroupScopeApplicabilityRecord) : this(row.placementId, row.hierarchyId, row.mode,
        row.createdAt, row.updatedAt, row.syncedAt, row.version)
    fun record() = GroupScopeApplicabilityRecord(placementId, hierarchyId, mode, createdAt, updatedAt, syncedAt, version)
}

@Dao
interface GroupScopeApplicabilityDao {
    @Query("SELECT * FROM hierarchy_group_scope_applicability ORDER BY placementId")
    suspend fun all(): List<GroupScopeApplicabilityEntity>
    @Upsert suspend fun upsert(rows: List<GroupScopeApplicabilityEntity>)
    @Query("DELETE FROM hierarchy_group_scope_applicability") suspend fun clear()
    @Query("UPDATE hierarchy_group_scope_applicability SET syncedAt=:now WHERE placementId=:id AND version=:version")
    suspend fun ack(id: String, version: Long, now: Long)
}
