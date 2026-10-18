package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy

/** Presentation identity only; never a ManagedSubject or HierarchyPlacement target. */
data class BigPictureContainerSnapshot(
    val id: String,
    val hierarchyId: String,
    val title: String,
    val anchorPlacementId: String?,
    val parentContainerId: String?,
    val siblingOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

/** One exact occurrence can belong to at most one section at its H1 sibling level. */
data class BigPictureMembershipSnapshot(
    val placementId: String,
    val hierarchyId: String,
    val containerId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

data class BigPicturePresentationSnapshot(
    val containers: List<BigPictureContainerSnapshot> = emptyList(),
    val memberships: List<BigPictureMembershipSnapshot> = emptyList(),
    val formatVersion: Int = 1,
)

data class BigPictureRecordVersion(val id: String, val version: Long)
data class BigPicturePresentationAck(
    val containers: List<BigPictureRecordVersion> = emptyList(),
    val memberships: List<BigPictureRecordVersion> = emptyList(),
)
