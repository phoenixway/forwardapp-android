package com.romankozak.forwardappmobile.shared.features.goals.data

import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.data.database.models.Goal as DomainGoal
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun Goals.toDomain(): DomainGoal {
    return DomainGoal(
        id = this.id,
        text = this.text,
        description = this.description,
        completed = this.completed,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        tags = this.tags?.split(","),
        relatedLinks = this.relatedLinks?.let { Json.decodeFromString<List<RelatedLink>>(it) },
        valueImportance = this.valueImportance.toFloat(),
        valueImpact = this.valueImpact.toFloat(),
        effort = this.effort.toFloat(),
        cost = this.cost.toFloat(),
        risk = this.risk.toFloat(),
        weightEffort = this.weightEffort.toFloat(),
        weightCost = this.weightCost.toFloat(),
        weightRisk = this.weightRisk.toFloat(),
        rawScore = this.rawScore.toFloat(),
        displayScore = this.displayScore.toInt(),
        scoringStatus = this.scoringStatus,
        parentValueImportance = this.parentValueImportance?.toFloat(),
        impactOnParentGoal = this.impactOnParentGoal?.toFloat(),
        timeCost = this.timeCost?.toFloat(),
        financialCost = this.financialCost?.toFloat(),
    )
}

fun DomainGoal.toSqlDelight(): Goals {
    return Goals(
        id = this.id,
        text = this.text,
        description = this.description,
        completed = this.completed,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        tags = this.tags?.joinToString(","),
        relatedLinks = this.relatedLinks?.let { Json.encodeToString(it) },
        valueImportance = this.valueImportance.toDouble(),
        valueImpact = this.valueImpact.toDouble(),
        effort = this.effort.toDouble(),
        cost = this.cost.toDouble(),
        risk = this.risk.toDouble(),
        weightEffort = this.weightEffort.toDouble(),
        weightCost = this.weightCost.toDouble(),
        weightRisk = this.weightRisk.toDouble(),
        rawScore = this.rawScore.toDouble(),
        displayScore = this.displayScore.toLong(),
        scoringStatus = this.scoringStatus,
        parentValueImportance = this.parentValueImportance?.toDouble(),
        impactOnParentGoal = this.impactOnParentGoal?.toDouble(),
        timeCost = this.timeCost?.toDouble(),
        financialCost = this.financialCost?.toDouble(),
    )
}
