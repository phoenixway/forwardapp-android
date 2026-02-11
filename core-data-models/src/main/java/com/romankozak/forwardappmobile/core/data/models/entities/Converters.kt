package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus

class Converters {
    private val gson = Gson()
    private val pathSeparator = " / "

    // --- Конвертери для CapabilityId (Крок 1.1) ---

    @TypeConverter
    fun fromCapabilityIdList(value: List<CapabilityId>?): String? {
        if (value.isNullOrEmpty()) return "[]"
        val normalized =
            value
                .mapNotNull { capability ->
                    runCatching { capability.raw.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
                }
        return gson.toJson(normalized)
    }

    @TypeConverter
    fun toCapabilityIdList(value: String?): List<CapabilityId> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        return runCatching {
            val parsed = JsonParser.parseString(value)
            val rawIds =
                when {
                    parsed.isJsonArray -> {
                        parsed.asJsonArray.mapNotNull { element ->
                            when {
                                element.isJsonNull -> null
                                element.isJsonPrimitive -> element.asString
                                element.isJsonObject ->
                                    element.asJsonObject
                                        .get("raw")
                                        ?.takeIf { it.isJsonPrimitive && !it.isJsonNull }
                                        ?.asString
                                else -> null
                            }
                        }
                    }
                    parsed.isJsonPrimitive -> listOf(parsed.asString)
                    parsed.isJsonObject ->
                        listOfNotNull(
                            parsed.asJsonObject
                                .get("raw")
                                ?.takeIf { it.isJsonPrimitive && !it.isJsonNull }
                                ?.asString,
                        )
                    else -> emptyList()
                }

            rawIds
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { CapabilityId(it) }
        }.getOrElse {
            // Fallback for legacy malformed payloads.
            val listType = object : TypeToken<List<String?>>() {}.type
            val fallback = gson.fromJson<List<String?>>(value, listType).orEmpty()
            fallback
                .mapNotNull { it?.trim()?.takeIf { raw -> raw.isNotEmpty() } }
                .map { CapabilityId(it) }
        }
    }

    // --- Базові конвертери ---

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(pathSeparator)?.map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString(pathSeparator)
    }

    // --- Конвертери для RelatedLink ---

    @TypeConverter
    fun fromRelatedLinkList(value: List<RelatedLink>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRelatedLinkList(value: String?): List<RelatedLink>? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val listType = object : TypeToken<List<RelatedLink>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromRelatedLink(value: RelatedLink?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRelatedLink(value: String?): RelatedLink? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val objectType = object : TypeToken<RelatedLink>() {}.type
        return gson.fromJson(value, objectType)
    }

    // --- Конвертери для статусів та пріоритетів місій ---

    @TypeConverter
    fun fromMissionStatus(status: MissionStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toMissionStatus(status: String?): MissionStatus? {
        return status?.let { MissionStatus.fromRaw(it) }
    }

    @TypeConverter
    fun fromMissionPriority(priority: MissionPriority?): String? {
        return priority?.name
    }

    @TypeConverter
    fun toMissionPriority(priority: String?): MissionPriority? {
        return priority?.let { MissionPriority.valueOf(it) }
    }
}
