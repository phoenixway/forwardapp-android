package com.romankozak.forwardappmobile.shared.features.goals.data.mappers

import com.romankozak.forwardappmobile.shared.database.Goals
import com.romankozak.forwardappmobile.shared.data.models.Goal
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun Goals.toDomain(): Goal = Goal(
    id = id,
    text = text,
    description = description,
    completed = completed != 0L,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags?.let { json.decodeFromString(ListSerializer(String.serializer()), it) } ?: emptyList(),
    relatedLinks = relatedLinks?.let { json.decodeFromString(ListSerializer(RelatedLink.serializer()), it) } ?: emptyList(),
    valueImportance = valueImportance,
    valueImpact = valueImpact,
    effort = effort,
    cost = cost,
    risk = risk,
    weightEffort = weightEffort,
    weightCost = weightCost,
    weightRisk = weightRisk,
    rawScore = rawScore,
    displayScore = displayScore
)