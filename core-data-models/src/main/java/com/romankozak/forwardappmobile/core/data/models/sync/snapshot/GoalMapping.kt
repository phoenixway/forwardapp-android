package com.romankozak.forwardappmobile.core.data.models.sync.snapshot

import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.GoalSnapshot

fun Goal.toSnapshot(): GoalSnapshot = GoalSnapshot(
    id = this.id,
    text = this.text,
    description = this.description,
    isCompleted = this.completed,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt ?: this.createdAt,
    version = this.version,
    isDeleted = this.isDeleted,
    tags = this.tags ?: emptyList(),
    scoringStatus = this.scoringStatus,
    valueImportance = this.valueImportance,
    valueImpact = this.valueImpact,
    effort = this.effort,
    cost = this.cost,
    risk = this.risk,
    parentValueImportance = this.parentValueImportance,
    impactOnParentGoal = this.impactOnParentGoal,
    timeCost = this.timeCost,
    financialCost = this.financialCost,
    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,
    rawScore = this.rawScore,
    displayScore = this.displayScore
)

fun GoalSnapshot.toEntity(): Goal = Goal(
    id = this.id,
    text = this.text,
    description = this.description,
    completed = this.isCompleted,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    version = this.version,
    isDeleted = this.isDeleted,
    tags = this.tags,
    scoringStatus = this.scoringStatus,
    valueImportance = this.valueImportance,
    valueImpact = this.valueImpact,
    effort = this.effort,
    cost = this.cost,
    risk = this.risk,
    parentValueImportance = this.parentValueImportance,
    impactOnParentGoal = this.impactOnParentGoal,
    timeCost = this.timeCost,
    financialCost = this.financialCost,
    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,
    rawScore = this.rawScore,
    displayScore = this.displayScore
)
