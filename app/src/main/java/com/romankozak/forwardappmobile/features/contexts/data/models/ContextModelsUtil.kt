package com.romankozak.forwardappmobile.features.contexts.data.models

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionPriority
import com.romankozak.forwardappmobile.features.missions.domain.model.MissionStatus

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

object ProjectStatusValues {
    const val NO_PLAN = "NO_PLAN"
    const val PLANNING = "PLANNING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val ON_HOLD = "ON_HOLD"
    const val PAUSED = "PAUSED"

    fun getDisplayName(status: String?): String {
        return when (status) {
            NO_PLAN -> "Без плану"
            PLANNING -> "Планується"
            IN_PROGRESS -> "В реалізації"
            COMPLETED -> "Завершено"
            ON_HOLD -> "Відкладено"
            PAUSED -> "На паузі"
            else -> "Без плану"
        }
    }
}

object ScoringStatusValues {
    const val NOT_ASSESSED = "NOT_ASSESSED"
    const val IMPOSSIBLE_TO_ASSESS = "IMPOSSIBLE_TO_ASSESS"
    const val ASSESSED = "ASSESSED"
}

object ProjectLogLevelValues {
    const val DETAILED = "DETAILED"
    const val NORMAL = "NORMAL"
}

object ProjectLogEntryTypeValues {
    const val STATUS_CHANGE = "STATUS_CHANGE"
    const val COMMENT = "COMMENT"
    const val AUTOMATIC = "AUTOMATIC"
    const val INSIGHT = "INSIGHT"
    const val MILESTONE = "MILESTONE"
}

object ListItemTypeValues {
    const val GOAL = "GOAL"
    const val SUBLIST = "SUBLIST"
    const val LINK_ITEM = "LINK_ITEM"
    const val NOTE = "NOTE"
    const val NOTE_DOCUMENT = "NOTE_DOCUMENT"
    const val CHECKLIST = "CHECKLIST"
    const val SCRIPT = "SCRIPT"
}

enum class ProjectViewMode { BACKLOG, INBOX, ADVANCED, ATTACHMENTS, DASHBOARD }

enum class LinkType { PROJECT, URL, OBSIDIAN }

enum class DayStatus { PLANNED, IN_PROGRESS, COMPLETED, MISSED, ARCHIVED }

enum class TaskPriority { LOW, MEDIUM, HIGH, CRITICAL, NONE;

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

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, DEFERRED }

data class RelatedLink(
    val type: LinkType?,
    val target: String,
    val displayName: String? = null,
)
