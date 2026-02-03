package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.TypeConverter
import com.google.gson.Gson
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
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCapabilityIdList(value: String?): List<CapabilityId>? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val listType = object : TypeToken<List<CapabilityId>>() {}.type
        return gson.fromJson(value, listType)
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
        return status?.let { MissionStatus.valueOf(it) }
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
