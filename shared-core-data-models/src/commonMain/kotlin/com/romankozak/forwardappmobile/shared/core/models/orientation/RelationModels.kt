@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.orientation

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

@JsExport
enum class OrientationRelationType {
    PART_OF,
    SUPPORTS,
    REALIZES,
    DEPENDS_ON,
    CONFLICTS_WITH,
    PRECEDES,
    REFINES,
    DERIVED_FROM,
}

@JsExport
data class OrientationRelation(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val fromOrientationId: String,
    val toOrientationId: String,
    val relationType: OrientationRelationType,
    val order: Long?,
) : SyncEntityMeta

@JsExport
enum class AspectOrientationRelationType {
    BELONGS_TO,
    RELEVANT_TO,
}

@JsExport
data class AspectOrientationRef(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val aspectId: String,
    val orientationId: String,
    val relationType: AspectOrientationRelationType,
    val isPrimary: Boolean,
    val order: Long,
) : SyncEntityMeta

@JsExport
enum class ContributionRole {
    ADVANCES,
    MAINTAINS,
    EXPLORES,
    PREVENTS,
    SUPPORTS,
}

@JsExport
enum class AttributionMode {
    INCLUSIVE,
    ALLOCATED,
    PRIMARY_ONLY,
}

@JsExport
data class OrientationContribution(
    val orientationId: String,
    val role: ContributionRole,
    val isPrimary: Boolean,
    val allocationWeight: Double?,
)

@JsExport
data class ContributionSet(
    val mode: AttributionMode,
    val contributions: List<OrientationContribution>,
)
