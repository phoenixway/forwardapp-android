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
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
    relatedLinks = relatedLinks,
    valueImportance = valueImportance,
    valueImpact = valueImpact,
    effort = effort,
    cost = cost,
    risk = risk,
    weightEffort = weightEffort,
    weightCost = weightCost,
    weightRisk = weightRisk,
    rawScore = rawScore,
    displayScore = displayScore,
    scoringStatus = scoringStatus,
    parentValueImportance = parentValueImportance,
    impactOnParentGoal = impactOnParentGoal,
    timeCost = timeCost,
    financialCost = financialCost,
    markdown = markdown

)
