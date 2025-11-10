package com.romankozak.forwardappmobile.shared.features.goals.data.mappers

import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.database.booleanAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter

fun Goals.toDomain(): Goal = Goal(
    id = id,
    text = text,
    description = description,
    completed = completed,
    tags = tags,
    relatedLinks = relatedLinks,
    valueImportance = valueImportance.toFloat(),
    valueImpact = valueImpact.toFloat(),
    effort = effort.toFloat(),
    cost = cost.toFloat(),
    risk = risk.toFloat(),
    weightEffort = weightEffort.toFloat(),
    weightCost = weightCost.toFloat(),
    weightRisk = weightRisk.toFloat(),
    rawScore = rawScore.toFloat(),
    displayScore = displayScore.toInt(),
    scoringStatus = scoringStatus,
    parentValueImportance = parentValueImportance?.toFloat(),
    impactOnParentGoal = impactOnParentGoal?.toFloat(),
    timeCost = timeCost?.toFloat(),
    financialCost = financialCost?.toFloat()
)
