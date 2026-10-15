package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle

internal fun String.toPlanningScopeKindStable(): PlanningScopeKind =
    when (this) {
        "DAY" -> PlanningScopeKind.DAY
        "TACTICAL_CYCLE" -> PlanningScopeKind.TACTICAL_CYCLE
        "STRATEGIC_ARC" -> PlanningScopeKind.STRATEGIC_ARC
        else -> throw IllegalArgumentException("Unknown PlanningScopeKind stable code: $this")
    }

internal fun PlanningScopeKind.toStableCode(): String =
    when (this) {
        PlanningScopeKind.DAY -> "DAY"
        PlanningScopeKind.TACTICAL_CYCLE -> "TACTICAL_CYCLE"
        PlanningScopeKind.STRATEGIC_ARC -> "STRATEGIC_ARC"
    }

internal fun String.toPlanningScopeLifecycleStable(): PlanningScopeLifecycle =
    when (this) {
        "DRAFT" -> PlanningScopeLifecycle.DRAFT
        "ACTIVE" -> PlanningScopeLifecycle.ACTIVE
        "CLOSED" -> PlanningScopeLifecycle.CLOSED
        "ARCHIVED" -> PlanningScopeLifecycle.ARCHIVED
        else -> throw IllegalArgumentException("Unknown PlanningScopeLifecycle stable code: $this")
    }

internal fun PlanningScopeLifecycle.toStableCode(): String =
    when (this) {
        PlanningScopeLifecycle.DRAFT -> "DRAFT"
        PlanningScopeLifecycle.ACTIVE -> "ACTIVE"
        PlanningScopeLifecycle.CLOSED -> "CLOSED"
        PlanningScopeLifecycle.ARCHIVED -> "ARCHIVED"
    }

internal fun String.toPlanningPriorityLevelStable(): PlanningPriorityLevel =
    when (this) {
        "LOW" -> PlanningPriorityLevel.LOW
        "MEDIUM" -> PlanningPriorityLevel.MEDIUM
        "HIGH" -> PlanningPriorityLevel.HIGH
        "CRITICAL" -> PlanningPriorityLevel.CRITICAL
        else -> throw IllegalArgumentException("Unknown PlanningPriorityLevel stable code: $this")
    }

internal fun PlanningPriorityLevel.toStableCode(): String =
    when (this) {
        PlanningPriorityLevel.LOW -> "LOW"
        PlanningPriorityLevel.MEDIUM -> "MEDIUM"
        PlanningPriorityLevel.HIGH -> "HIGH"
        PlanningPriorityLevel.CRITICAL -> "CRITICAL"
    }

internal fun String.toPlanningCommitmentStatusStable(): PlanningCommitmentStatus =
    when (this) {
        "PLANNED" -> PlanningCommitmentStatus.PLANNED
        "ACTIVE" -> PlanningCommitmentStatus.ACTIVE
        "PAUSED" -> PlanningCommitmentStatus.PAUSED
        "COMPLETED" -> PlanningCommitmentStatus.COMPLETED
        "DROPPED" -> PlanningCommitmentStatus.DROPPED
        else -> throw IllegalArgumentException("Unknown PlanningCommitmentStatus stable code: $this")
    }

internal fun PlanningCommitmentStatus.toStableCode(): String =
    when (this) {
        PlanningCommitmentStatus.PLANNED -> "PLANNED"
        PlanningCommitmentStatus.ACTIVE -> "ACTIVE"
        PlanningCommitmentStatus.PAUSED -> "PAUSED"
        PlanningCommitmentStatus.COMPLETED -> "COMPLETED"
        PlanningCommitmentStatus.DROPPED -> "DROPPED"
    }

internal fun String.toPlanningProvenanceKindStable(): PlanningProvenanceKind =
    when (this) {
        "MANUAL" -> PlanningProvenanceKind.MANUAL
        "MIGRATED" -> PlanningProvenanceKind.MIGRATED
        "DERIVED" -> PlanningProvenanceKind.DERIVED
        "CARRIED_FORWARD" -> PlanningProvenanceKind.CARRIED_FORWARD
        "IMPORTED" -> PlanningProvenanceKind.IMPORTED
        else -> throw IllegalArgumentException("Unknown PlanningProvenanceKind stable code: $this")
    }

internal fun PlanningProvenanceKind.toStableCode(): String =
    when (this) {
        PlanningProvenanceKind.MANUAL -> "MANUAL"
        PlanningProvenanceKind.MIGRATED -> "MIGRATED"
        PlanningProvenanceKind.DERIVED -> "DERIVED"
        PlanningProvenanceKind.CARRIED_FORWARD -> "CARRIED_FORWARD"
        PlanningProvenanceKind.IMPORTED -> "IMPORTED"
    }
