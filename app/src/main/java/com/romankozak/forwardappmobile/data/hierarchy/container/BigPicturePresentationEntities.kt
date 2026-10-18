package com.romankozak.forwardappmobile.data.hierarchy.container

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureContainerSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureMembershipSnapshot

@Entity(
    tableName = "big_picture_containers", primaryKeys = ["id"],
    indices = [Index(value = ["id", "hierarchyId"], unique = true), Index("parentContainerId", "hierarchyId"),
        Index("anchorPlacementId", "hierarchyId"), Index("updatedAt"), Index("syncedAt")],
    foreignKeys = [
        ForeignKey(entity = BigPictureContainerEntity::class, parentColumns = ["id", "hierarchyId"],
            childColumns = ["parentContainerId", "hierarchyId"], deferred = true),
        ForeignKey(entity = HierarchyPlacementEntity::class, parentColumns = ["id", "hierarchyId"],
            childColumns = ["anchorPlacementId", "hierarchyId"], deferred = true),
    ],
)
data class BigPictureContainerEntity(@Embedded val record: BigPictureContainerSnapshot)

@Entity(
    tableName = "big_picture_memberships", primaryKeys = ["placementId"],
    indices = [Index("placementId", "hierarchyId"), Index("containerId", "hierarchyId"),
        Index("updatedAt"), Index("syncedAt")],
    foreignKeys = [
        ForeignKey(entity = BigPictureContainerEntity::class, parentColumns = ["id", "hierarchyId"],
            childColumns = ["containerId", "hierarchyId"], deferred = true),
        ForeignKey(entity = HierarchyPlacementEntity::class, parentColumns = ["id", "hierarchyId"],
            childColumns = ["placementId", "hierarchyId"], deferred = true),
    ],
)
data class BigPictureMembershipEntity(@Embedded val record: BigPictureMembershipSnapshot)
