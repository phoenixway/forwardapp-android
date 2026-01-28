package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.TypeConverter

class PathSegmentsConverter {
    private val pathSeparator = " / "

    @TypeConverter
    fun fromPathSegments(pathSegments: List<String>?): String? {
        return pathSegments?.joinToString(pathSeparator)
    }

    @TypeConverter
    fun fromStringToPathSegments(data: String?): List<String>? {
        return data?.split(pathSeparator)?.map { it.trim() }
    }
}
/*
enum class ContextViewMode { BACKLOG, INBOX, ADVANCED, ATTACHMENTS, DASHBOARD }

enum class LinkType { CONTEXT, URL, OBSIDIAN }

enum class DayStatus { PLANNED, IN_PROGRESS, COMPLETED, MISSED, ARCHIVED }

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    NONE,
    ;

    fun getDisplayName(): String {
        return when (this) {
            LOW -> "Низький"
            MEDIUM -> "Середній"
            HIGH -> "Високий"
            CRITICAL -> "Критичний"
            NONE -> "Немає"
        }
    }
}

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, DEFERRED }*/