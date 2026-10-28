package com.romankozak.forwardappmobile.shared.contracts.contexts

import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import kotlinx.serialization.Serializable

const val COMPOSITION_SHARED_PLANNING_FORMAT_VERSION = 5

@Serializable
data class SharedPlanningCommitmentRelationSnapshot(
    val id: String, val sourceCommitmentId: String, val targetCommitmentId: String, val relationType: String,
    val order: Long, val createdAt: Long, val updatedAt: Long?, val syncedAt: Long?, val isDeleted: Boolean, val version: Long,
)

internal fun DesktopWorkspaceSnapshot.requireValidPlanningComposition() {
    require(if (planningFormatVersion == COMPOSITION_SHARED_PLANNING_FORMAT_VERSION) planningCommitmentRelations != null
        else planningCommitmentRelations == null) { "Planning composition requires format v5" }
    val rows = planningCommitmentRelations ?: return
    val scopes = planningScopes.orEmpty().filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.id }
    val endpoints = planningCommitments.orEmpty().filter { !it.isDeleted && it.scopeId in scopes }
        .mapTo(hashSetOf()) { PlanningCommitmentId(it.id) }
    val relations = rows.map { PlanningCommitmentRelation(
        PlanningCommitmentRelationId(it.id), PlanningCommitmentId(it.sourceCommitmentId), PlanningCommitmentId(it.targetCommitmentId),
        PlanningCommitmentRelationType.valueOf(it.relationType), it.order, it.createdAt, it.updatedAt, it.syncedAt, it.isDeleted, it.version) }
    require(validatePlanningCommitmentRelations(relations, endpoints).isEmpty()) { "Invalid Planning composition graph" }
}
