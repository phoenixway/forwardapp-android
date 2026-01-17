package com.romankozak.forwardappmobile.data.sync

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.contexts.data.models.RelatedLink
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class GoalDeserializer : JsonDeserializer<Goal> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Goal {
        val jsonObject = json.asJsonObject
        val relatedLinks: List<RelatedLink>? = if (jsonObject.has("relatedLinks")) {
            context.deserialize(jsonObject.get("relatedLinks"), object : TypeToken<List<RelatedLink>>() {}.type)
        } else {
            null
        }

        return Goal(
            id = jsonObject.get("id").asString,
            text = jsonObject.get("text").asString,
            description = jsonObject.get("description")?.asString,
            completed = jsonObject.get("completed").asBoolean,
            createdAt = jsonObject.get("createdAt").asString.toEpochMilli(),
            updatedAt = jsonObject.get("updatedAt")?.asString?.toEpochMilli(),
            tags = if (jsonObject.has("tags")) {
                context.deserialize(jsonObject.get("tags"), object : TypeToken<List<String>>() {}.type)
            } else {
                null
            },
            relatedLinks = relatedLinks,
            valueImportance = jsonObject.get("valueImportance")?.asFloat ?: 0f,
            valueImpact = jsonObject.get("valueImpact")?.asFloat ?: 0f,
            effort = jsonObject.get("effort")?.asFloat ?: 0f,
            cost = jsonObject.get("cost")?.asFloat ?: 0f,
            risk = jsonObject.get("risk")?.asFloat ?: 0f,
            weightEffort = jsonObject.get("weightEffort")?.asFloat ?: 1f,
            weightCost = jsonObject.get("weightCost")?.asFloat ?: 1f,
            weightRisk = jsonObject.get("weightRisk")?.asFloat ?: 1f,
            rawScore = jsonObject.get("rawScore")?.asFloat ?: 0f,
            displayScore = jsonObject.get("displayScore")?.asInt ?: 0,
            scoringStatus = jsonObject.get("scoringStatus")?.asString ?: "NOT_ASSESSED",
            parentValueImportance = jsonObject.get("parentValueImportance")?.asFloat,
            impactOnParentGoal = jsonObject.get("impactOnParentGoal")?.asFloat,
            timeCost = jsonObject.get("timeCost")?.asFloat,
            financialCost = jsonObject.get("financialCost")?.asFloat
        )
    }
}

private fun String.toEpochMilli(): Long {
    return try {
        OffsetDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}
