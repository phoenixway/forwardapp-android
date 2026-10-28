package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningCommitmentRelationEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import com.romankozak.forwardappmobile.shared.core.models.PlanningCommitmentRelationRecord

fun PlanningCommitmentRelationEntity.toDomain() = PlanningCommitmentRelation(
    PlanningCommitmentRelationId(id), PlanningCommitmentId(sourceCommitmentId), PlanningCommitmentId(targetCommitmentId),
    PlanningCommitmentRelationType.valueOf(relationType), relationOrder, createdAt, updatedAt, syncedAt, isDeleted, version)
fun PlanningCommitmentRelation.toEntity() = PlanningCommitmentRelationEntity(
    id.value, sourceCommitmentId.value, targetCommitmentId.value, relationType.name, order, createdAt, updatedAt, syncedAt, isDeleted, version)
fun PlanningCommitmentRelationEntity.toRecord() = PlanningCommitmentRelationRecord(
    id, sourceCommitmentId, targetCommitmentId, relationType, relationOrder, createdAt, updatedAt, syncedAt, isDeleted, version)
fun PlanningCommitmentRelationRecord.toEntity() = PlanningCommitmentRelationEntity(
    id, sourceCommitmentId, targetCommitmentId, relationType, order, createdAt, updatedAt, syncedAt, isDeleted, version)
