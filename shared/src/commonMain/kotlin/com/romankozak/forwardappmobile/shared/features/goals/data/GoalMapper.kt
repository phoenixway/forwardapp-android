package com.romankozak.forwardappmobile.shared.features.goals.data

import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.data.database.models.Goal as DomainGoal
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

fun Goals.toDomain(): DomainGoal {
    return DomainGoal(
        id = this.id,
        text = this.text,
        description = this.description,
        completed = this.completed,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        tags = this.tags?.split(","),
        relatedLinks = this.relatedLinks,
        valueImportance = this.valueImportance,
        valueImpact = this.valueImpact,
        effort = this.effort,
        cost = this.cost,
        risk = this.risk,
        weightEffort = this.weightEffort,
        weightCost = this.weightCost,
        weightRisk = this.weightRisk,
        rawScore = this.rawScore,
        displayScore = this.displayScore,
        scoringStatus = this.scoringStatus,
        parentValueImportance = this.parentValueImportance,
        impactOnParentGoal = this.impactOnParentGoal,
        timeCost = this.timeCost,
        financialCost = this.financialCost,
        markdown = this.markdown
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
        relatedLinks = this.relatedLinks,
        valueImportance = this.valueImportance,
        valueImpact = this.valueImpact,
        effort = this.effort,
        cost = this.cost,
        risk = this.risk,
        weightEffort = this.weightEffort,
        weightCost = this.weightCost,
        weightRisk = this.weightRisk,
        rawScore = this.rawScore,
        displayScore = this.displayScore,
        scoringStatus = this.scoringStatus,
        parentValueImportance = this.parentValueImportance,
        impactOnParentGoal = this.impactOnParentGoal,
        timeCost = this.timeCost,
        financialCost = this.financialCost,
        markdown = this.markdown
    )
}