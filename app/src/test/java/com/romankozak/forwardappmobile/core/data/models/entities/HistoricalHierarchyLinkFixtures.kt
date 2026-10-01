package com.romankozak.forwardappmobile.core.data.models.entities

/** Test-only historical V1 evidence; neither type is a schema-180 Room entity. */
data class ContextParentLink(
    val parentContextId: String,
    val childContextId: String,
    val order: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val version: Long = 0L,
)

data class MainBeaconParentLink(
    val parentBeaconId: String,
    val childBeaconId: String,
    val order: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)
