package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionPriority
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionStatus

@TypeConverters(Converters::class)
class Converters {
    private val gson = Gson()
    private val pathSeparator = " / "

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(pathSeparator)?.map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString(pathSeparator)
    }

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