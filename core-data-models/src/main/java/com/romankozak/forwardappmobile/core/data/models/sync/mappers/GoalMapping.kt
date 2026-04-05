package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.GoalSnapshot

fun Goal.toSnapshot(): GoalSnapshot = GoalSnapshot(
    id = this.id,
    text = this.text,
    description = this.description,
    isCompleted = this.completed, // Entity: completed -> Snapshot: isCompleted
    createdAt = this.createdAt,
    // Використовуємо логіку пріоритетності часових міток
    updatedAt = this.updatedAt ?: this.createdAt,
    version = this.version,
    isDeleted = this.isDeleted,
    tags = this.tags ?: emptyList(),
    scoringStatus = this.scoringStatus,

    // Явне перетворення Float -> Int для Snapshot
    valueImportance = this.valueImportance.toInt(),
    valueImpact = this.valueImpact.toInt(),
    effort = this.effort.toInt(),
    cost = this.cost.toInt(),
    risk = this.risk.toInt(),

    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,

    // Double для збереження точності розрахунків
    rawScore = this.rawScore.toDouble(),
    displayScore = this.displayScore.toDouble(),
    relativeSize = this.relativeSize,

    parentValueImportance = this.parentValueImportance,
    impactOnParentGoal = this.impactOnParentGoal,
    timeCost = this.timeCost,
    financialCost = this.financialCost,
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

    // Повертаємо типи до Float, які очікує Entity
    valueImportance = this.valueImportance.toFloat(),
    valueImpact = this.valueImpact.toFloat(),
    effort = this.effort.toFloat(),
    cost = this.cost.toFloat(),
    risk = this.risk.toFloat(),

    parentValueImportance = this.parentValueImportance,
    impactOnParentGoal = this.impactOnParentGoal,
    timeCost = this.timeCost,
    financialCost = this.financialCost,

    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,

    rawScore = this.rawScore.toFloat(),
    displayScore = this.displayScore.toInt(), // Конвертація Double назад в Int
    relativeSize = this.relativeSize,
)
